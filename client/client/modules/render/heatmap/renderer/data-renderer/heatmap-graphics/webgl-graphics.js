import HeatmapGraphics from './heatmap-mesh';
import HeatmapGraphicsBase from './base';

export default class HeatmapWebGlGraphics extends HeatmapGraphicsBase {
    constructor(options) {
        super(options);
        /**
         *
         * @type {HeatmapGraphics}
         */
        this.graphicsData = new HeatmapGraphics();
    }

    applyColorScheme(colorScheme) {
        if (colorScheme) {
            if (this.graphicsData) {
                this.graphicsData.updateGeometryGradients(colorScheme.gradientCollection);
            }
        }
        return super.applyColorScheme(colorScheme);
    }

    initializePixiObjects() {
        this.updateGraphics(this.graphicsData)
            .then(() => {
                this._initialized = true;
            });
    }

    destroy() {
        if (this.container) {
            this.container.destroy();
        }
        if (this.graphicsData) {
            this.graphicsData.destroy();
        }
        this.container = undefined;
        super.destroy();
    }

    /**
     *
     * @param {HeatmapGraphics} data
     * @returns {Promise<void>}
     */
    updateGraphics(data) {
        if (!data) {
            return;
        }
        data.build();
        this.container.addChild(data.container);
        return Promise.resolve();
    }

    getFullRebuildOptions(callback) {
        const options = super.getFullRebuildOptions(callback);
        const data = new HeatmapGraphics();
        let processKeys = [];
        if (options.reset) {
            this.graphicsData.clear();
        }
        if (options.colorScheme) {
            processKeys = this.graphicsData && !options.reset
                ? this.graphicsData.getNonExistingGeometryKeys(options.colorScheme.gradientCollection)
                : options.colorScheme.gradientCollection.keys;
        }
        return {
            ...options,
            data,
            processGradients: processKeys,
            vertexIndex: 0,
            omit: options.omit || processKeys.length === 0,
            callback: (cancelled = false) => {
                if (cancelled) {
                    options.callback(cancelled);
                } else {
                    if (this.graphicsData) {
                        this.graphicsData.merge(data);
                    } else {
                        this.graphicsData = data;
                    }
                    this.updateGraphics(this.graphicsData)
                        .then(() => {
                            options.callback(cancelled);
                        });
                }
            }
        };
    }

    getPartialRebuildOptions(callback) {
        const options = super.getPartialRebuildOptions(callback);
        const data = new HeatmapGraphics();
        const processKeys = options.colorScheme
            ? options.colorScheme.gradientCollection.keys
            : [];
        return {
            ...options,
            data,
            processGradients: processKeys,
            callback: (cancelled = false) => {
                this.updateGraphics(data)
                    .then(() => {
                        if (this.graphicsData) {
                            this.graphicsData.merge(data);
                        } else {
                            this.graphicsData = data;
                        }
                        options.callback(cancelled);
                    });
            }
        };
    }

    /**
     *
     * @param {HeatmapDataItemWithSize} item
     * @param {Object} options
     * @param {HeatmapGraphics} options.data
     * @param {ColorScheme} options.colorScheme
     * @param {string[]} options.processGradients
     */
    appendGraphics(item, options) {
        if (
            item &&
            options &&
            options.colorScheme &&
            options.data
        ) {
            options.data.append(item, options.colorScheme.gradientCollection, options.processGradients || []);
        }
    }

    getSessionFlags() {
        const flags = super.getSessionFlags();
        return {
            ...flags,
            graphicsChanged: flags.graphicsChanged || (this.graphicsData && this.graphicsData.changed)
        };
    }

    updateSessionFlags() {
        if (this.graphicsData) {
            this.graphicsData.handleChange();
        }
        super.updateSessionFlags();
    }

    render() {
        const changed = super.render();
        return (this.graphicsData ? this.graphicsData.render() : false) || changed;
    }
}
