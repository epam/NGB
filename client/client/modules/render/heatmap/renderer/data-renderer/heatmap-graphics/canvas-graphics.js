import * as PIXI from 'pixi.js-legacy';
import HeatmapGraphicsBase from './base';
import buildItemGraphics from './build-item-graphics';

export default class HeatmapCanvasGraphics extends HeatmapGraphicsBase {
    initializePixiObjects() {
        let geometry;
        if (this.graphics) {
            geometry = this.graphics.geometry;
        }
        this.container.removeChildren();
        if (this.graphics) {
            this.graphics.destroy();
        }
        this.graphics = new PIXI.Graphics(geometry);
        this.container.addChild(this.graphics);
        this._initialized = true;
        if (geometry) {
            this.graphics.cacheAsBitmap = true;
        }
        return Promise.resolve();
    }

    destroy() {
        if (this.container) {
            this.container.destroy();
        }
        this.graphics = undefined;
        this.container = undefined;
        super.destroy();
    }

    getGraphicsForUpdate() {
        const graphics = new PIXI.Graphics();
        graphics.cacheAsBitmap = false;
        return graphics;
    }

    updateGraphics(graphics, rebuild = false) {
        if (rebuild) {
            if (this.graphics) {
                this.graphics.clear();
                if (this.graphics.parent) {
                    this.graphics.parent.removeChild(this.graphics);
                }
                this.graphics.destroy();
            }
            this.container.addChild(graphics);
        }
        this.graphics = graphics;
        this._changed = true;
        this.graphics.cacheAsBitmap = true;
    }

    getFullRebuildOptions(callback) {
        const options = super.getFullRebuildOptions(callback);
        const graphics = this.getGraphicsForUpdate();
        graphics.clear();
        return {
            ...options,
            graphics,
            callback: (cancelled = false) => {
                this.updateGraphics(graphics, true);
                options.callback(cancelled);
            }
        };
    }

    getPartialRebuildOptions(callback) {
        const options = super.getPartialRebuildOptions(callback);
        const graphics = this.graphics;
        graphics.cacheAsBitmap = false;
        return {
            ...options,
            graphics,
            callback: (cancelled = false) => {
                graphics.cacheAsBitmap = true;
                options.callback(cancelled);
            }
        };
    }

    /**
     * Appends sized heatmap item to the graphics
     * @param {HeatmapDataItemWithSize} item
     * @param {Object} options
     */
    appendGraphics(item, options) {
        if (item && options && options.graphics && options.colorScheme) {
            const {
                graphics,
                colorScheme
            } = options;
            buildItemGraphics(item, graphics, colorScheme);
        }
    }
}
