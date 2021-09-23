import * as PIXI from 'pixi.js-legacy';
import {A_VERTEX_POSITION} from './helpers';

const MAX_ITEMS_PER_GEOMETRY = 10000;
const ITEM_VERTEX_COUNT = 4;
const ITEM_VERTEX_SIZE = 3;
const MAX_VERTEX_PER_GEOMETRY = MAX_ITEMS_PER_GEOMETRY * ITEM_VERTEX_COUNT * ITEM_VERTEX_SIZE;

class HeatmapGeometry {
    constructor() {
        /**
         * Elements array (holds {x, y, ratio} data for each heatmap item)
         * @type {Float32Array}
         */
        this.array = new Float32Array(MAX_VERTEX_PER_GEOMETRY);
        /**
         * An amount of heatmap items within array
         * @type {number}
         */
        this.pointer = 0;
        /**
         *
         * @type {PIXI.Geometry|undefined}
         */
        this.geometry = undefined;
        this.dirty = false;
        this.elementsBuffer = undefined;
        this.indexBuffer = undefined;
    }

    /**
     * Whether this buffer is completed (i.e. holds maximum amount of heatmap items)
     * @returns {boolean}
     */
    get isComplete() {
        return this.pointer >= MAX_ITEMS_PER_GEOMETRY;
    }

    /**
     * Appends heatmap item to the buffer if it is not completed
     * @param {HeatmapDataItemWithSize} [item]
     * @param {number} [ratio=0]
     */
    append(item, ratio = 0) {
        if (!item || this.isComplete) {
            return;
        }
        const pointer = this.pointer;
        const points = [
            {x: item.column, y: item.row},
            {x: item.column + (item.width || 1), y: item.row},
            {x: item.column + (item.width || 1), y: item.row + (item.height || 1)},
            {x: item.column, y: item.row + (item.height || 1)}
        ];
        for (let p = 0; p < points.length; p += 1) {
            const i = pointer * ITEM_VERTEX_COUNT * ITEM_VERTEX_SIZE + p * 3;
            this.array[i] = points[p].x;
            this.array[i + 1] = points[p].y;
            this.array[i + 2] = ratio;
        }
        this.pointer += 1;
        this.dirty = true;
    }

    /**
     * Builds elements & index buffers and returns PIXI.Geometry
     * @returns {PIXI.Geometry}
     */
    buildGeometry() {
        if (this.dirty || !this.elementsBuffer || !this.indexBuffer) {
            const itemsCount = this.pointer;
            const index = new Int32Array(itemsCount * 6);
            for (let i = 0; i < itemsCount; i++) {
                const v = i * ITEM_VERTEX_COUNT;
                index[i * 6] = v;
                index[i * 6 + 1] = v + 1;
                index[i * 6 + 2] = v + 2;
                index[i * 6 + 3] = v;
                index[i * 6 + 4] = v + 2;
                index[i * 6 + 5] = v + 3;
            }
            this.elementsBuffer = new PIXI.Buffer(this.array, true, false);
            this.indexBuffer = new PIXI.Buffer(index, true, true);
        }
        const geometry = new PIXI.Geometry();
        geometry
            .addAttribute(A_VERTEX_POSITION, this.elementsBuffer, ITEM_VERTEX_SIZE)
            .addIndex(this.indexBuffer);
        return geometry;
    }

    clear() {
        this.elementsBuffer = undefined;
        this.indexBuffer = undefined;
        this.pointer = 0;
    }

    clearGraphics() {
        if (!this.mesh) {
            return;
        }
        if (this.mesh.parent) {
            this.mesh.parent.removeChild(this.mesh);
        }
        this.mesh.destroy();
        this.mesh = undefined;
    }

    /**
     *
     * @param {PIXI.Container} container
     * @param {PIXI.Shader} program
     */
    build(container, program) {
        if (program && container) {
            const geometry = this.buildGeometry();
            if (geometry) {
                /**
                 *
                 * @type {PIXI.Mesh}
                 */
                const mesh = new PIXI.Mesh(geometry, program);
                container.addChild(mesh);
                this.clearGraphics();
                this.mesh = mesh;
                return true;
            }
        }
        return false;
    }

    destroy() {
        this.clearGraphics();
        this.indexBuffer = undefined;
        this.elementsBuffer = undefined;
        this.array = undefined;
    }
}

export default HeatmapGeometry;
