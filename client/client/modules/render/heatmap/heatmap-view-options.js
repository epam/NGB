import HeatmapEventDispatcher from './utilities/heatmap-event-dispatcher';
import events from './utilities/events';

/**
 * @typedef {Object} HeatmapDataOptions
 * @property {number|string} id
 * @property {number} [projectId]
 */

/**
 * @typedef {Object} DisplayOptions
 * @property {HTMLElement} [domElement]
 * @property {PIXI.AbstractRenderer} [pixiRenderer]
 */

/**
 * @typedef {Object} HeatmapOptions
 * @property {HeatmapDataOptions} dataConfig
 * @property {DisplayOptions} displayOptions
 * @property {number} width
 * @property {number} height
 * @property {number|number[]} padding
 * @property {dispatcher} dispatcher
 */

class HeatmapViewOptions extends HeatmapEventDispatcher {
    /**
     *
     * @param {HeatmapData} data
     * @param {ColorScheme} colorScheme
     */
    constructor(data, colorScheme) {
        super();
        this.data = data;
        this._colorScheme = colorScheme;
        this._dendrogramAvailable = false;
        this._dendrogram = true;
        this.handleMetadataLoaded = this.metadataLoaded.bind(this);
        this.data.onMetadataLoaded(this.handleMetadataLoaded);
    }

    get dendrogramAvailable() {
        return this._dendrogramAvailable;
    }

    get colorScheme() {
        return this._colorScheme;
    }

    get dendrogram() {
        return this.dendrogramAvailable && this._dendrogram;
    }

    set dendrogram(dendrogram) {
        if (dendrogram !== this.dendrogram) {
            this._dendrogram = dendrogram;
            this.emit(events.data.dendrogram);
        }
    }

    metadataLoaded() {
        this._dendrogramAvailable = this.data &&
            this.data.metadata &&
            this.data.metadata.dendrogramAvailable;
    }

    onColumnsRowsReordered(callback) {
        this.addEventListener(events.data.sorting, callback);
    }

    destroy() {
        super.destroy();
        this.data = undefined;
        this.colorScheme = undefined;
    }
}

export default HeatmapViewOptions;
