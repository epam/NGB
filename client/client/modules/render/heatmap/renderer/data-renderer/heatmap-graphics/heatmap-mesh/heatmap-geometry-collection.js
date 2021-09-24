import * as PIXI from 'pixi.js-legacy';
import {getFragmentShader, getVertexShader} from './helpers';
import HeatmapGeometry from './heatmap-geometry';

class HeatmapGeometryCollection {
    /**
     *
     * @param {Gradient} gradient
     */
    constructor(gradient) {
        /**
         *
         * @type {number|string}
         */
        this.key = gradient ? gradient.key : undefined;
        this.geometries = [new HeatmapGeometry()];
        this.changed = false;
        this._alpha = 1;
        this.updateGradient(gradient);
    }

    get alpha() {
        return this._alpha;
    }

    set alpha(alpha) {
        if (this._alpha !== alpha) {
            this._alpha = alpha;
            if (this.program && this.program.uniforms) {
                this.program.uniforms.alpha = alpha;
            }
            this.changed = true;
        }
    }

    /**
     * Updates gradient and WebGL shaders
     * @param {Gradient} gradient
     */
    updateGradient(gradient) {
        if (gradient) {
            /**
             *
             * @type {Gradient}
             */
            this.gradient = gradient;
            const vertexShader = getVertexShader();
            const fragmentShader = getFragmentShader(gradient.gradientStop1, gradient.gradientStop2);
            if (vertexShader && fragmentShader) {
                /**
                 * @type {PIXI.Shader}
                 */
                this.program = PIXI.Shader.from(vertexShader, fragmentShader, {alpha: this.alpha});
                this.changed = true;
            }
        }
    }

    handleChange() {
        this.changed = false;
    }

    /**
     * Returns current (last) geometry
     * @returns {HeatmapGeometry}
     */
    get current() {
        return this.geometries[this.geometries.length - 1];
    }

    /**
     * Appends heatmap item to the last not completed geometry
     * @param {HeatmapDataItemWithSize} [item]
     */
    append(item) {
        if (this.current.isComplete) {
            this.geometries.push(new HeatmapGeometry());
        }
        if (this.gradient) {
            const ratio = this.gradient.getRatio(item.value);
            this.current.append(item, ratio);
        }
    }

    clear() {
        this.clearGraphics();
        for (const geometry of this.geometries) {
            geometry.clear();
        }
        this.geometries = [new HeatmapGeometry()];
    }

    clearGraphics() {
        for (const geometry of this.geometries) {
            geometry.clearGraphics();
        }
    }

    build(container) {
        let changed = false;
        for (const geometry of this.geometries) {
            changed = geometry.build(container, this.program) || changed;
        }
        return changed;
    }

    destroy() {
        for (const geometry of this.geometries) {
            geometry.destroy();
        }
        this.geometries = undefined;
        this.program = undefined;
    }
}

export default HeatmapGeometryCollection;
