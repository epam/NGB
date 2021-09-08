import * as PIXI from 'pixi.js-legacy';
import HeatmapGeometryCollection from './heatmap-geometry-collection';
import {mergeMaps} from './helpers';

export default class HeatmapMesh {
    /**
     *
     * @param {Map<number|string, HeatmapGeometryCollection>} collections
     */
    constructor(collections) {
        /**
         * Heatmap geometry collections
         * @type {Map<number|string, HeatmapGeometryCollection>}
         */
        this.collections = collections || new Map();
        this._alpha = 1;
        this.container = new PIXI.Container();
    }

    /**
     *
     * @param {HeatmapMesh} heatmapMesh
     */
    merge(heatmapMesh) {
        this.collections = mergeMaps(this.collections, heatmapMesh.collections);
        this.alpha = this._alpha;
    }

    get alpha() {
        return this._alpha;
    }

    set alpha(alpha) {
        this._alpha = alpha;
        for (const geometryCollection of this.collections.values()) {
            geometryCollection.alpha = alpha;
        }
    }

    get changed() {
        for (const geometryCollection of this.collections.values()) {
            if (geometryCollection.changed) {
                return true;
            }
        }
        return false;
    }

    handleChange() {
        for (const geometryCollection of this.collections.values()) {
            geometryCollection.handleChange();
        }
    }

    /**
     *
     * @param {HeatmapDataItem} item
     * @param {GradientCollection} [gradients]
     * @param {string[]} processGradientKeys
     */
    append(item, gradients, processGradientKeys = []) {
        if (!item || !gradients) {
            return;
        }
        const gradient = gradients.getGradientForValue(item.value);
        if (gradient && processGradientKeys.includes(gradient.key)) {
            if (!this.collections.has(gradient.key)) {
                const collection = new HeatmapGeometryCollection(gradient);
                this.collections.set(gradient.key, collection);
                collection.alpha = this.alpha;
            }
            const collection = this.collections.get(gradient.key);
            collection.append(item);
        }
    }

    clear() {
        this.clearGraphics();
        for (const geometryCollection of this.collections.values()) {
            geometryCollection.clear();
        }
    }

    clearGraphics() {
        for (const geometryCollection of this.collections.values()) {
            geometryCollection.clearGraphics();
        }
    }

    /**
     * Updates geometries gradients
     * @param {GradientCollection} gradientCollection
     */
    updateGeometryGradients(gradientCollection) {
        if (gradientCollection) {
            const removed = [];
            for (const [key, collection] of this.collections.entries()) {
                const gradient = gradientCollection.getGradientByKey(key);
                if (gradient) {
                    collection.updateGradient(gradient);
                } else {
                    removed.push(key);
                }
            }
            removed.forEach(key => {
                if (this.collections.has(key)) {
                    const collection = this.collections.get(key);
                    collection.destroy();
                    this.collections.delete(key);
                }
            });
        }
        return [];
    }

    /**
     * Returns gradient keys for which geometry does not exist
     * @param {GradientCollection} gradientCollection
     * @returns {string[]}
     */
    getNonExistingGeometryKeys(gradientCollection) {
        if (gradientCollection) {
            return gradientCollection.keys.filter(key => !this.collections.has(key));
        }
        return [];
    }

    build() {
        let changed = false;
        for (const geometryCollection of this.collections.values()) {
            changed = geometryCollection.build(this.container) || changed;
        }
        return changed;
    }

    destroy() {
        for (const geometryCollection of this.collections.values()) {
            geometryCollection.destroy();
        }
        this.collections = undefined;
        if (this.container) {
            this.container.removeChildren();
            if (this.container.parent) {
                this.container.parent.removeChild(this.container);
            }
        }
    }

    render() {
        this.alpha = this.container.parent ? this.container.parent.alpha : this.alpha;
        return this.changed;
    }
}
