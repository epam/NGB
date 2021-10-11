import {readHeatmapState, writeHeatmapState} from './manager';
import ColorScheme from '../color-scheme';
import HeatmapData from '../heatmap-data';
import HeatmapEventDispatcher from '../utilities/heatmap-event-dispatcher';
import events from '../utilities/events';

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

class HeatmapViewOptions extends HeatmapEventDispatcher {
    static SERIALIZATION_SEPARATOR = '|';

    /**
     *
     * @param {string} serialized
     * @param {HeatmapDataOptions} [dataConfig]
     * @returns {HeatmapViewOptions}
     */
    static parse(serialized, dataConfig) {
        let colorScheme,
            dendrogram = true;
        let payload = serialized;
        if (!serialized && dataConfig && dataConfig.id) {
            payload = readHeatmapState(dataConfig.id);
        }
        console.log('parse', serialized, payload);
        if (!payload) {
            payload = '';
        }
        try {
            const [
                colorSchemeSerialized,
                dendrogramEnabled
            ] = payload.split(HeatmapViewOptions.SERIALIZATION_SEPARATOR);
            dendrogram = /^true$/i.test(dendrogramEnabled);
            if (colorSchemeSerialized) {
                colorScheme = ColorScheme.parse(colorSchemeSerialized);
            }
            // eslint-disable-next-line no-empty
        } catch (_) {}
        const heatmapOptions = new HeatmapViewOptions(colorScheme, dendrogram);
        if (dataConfig) {
            heatmapOptions.setDataConfigurationCache(dataConfig);
        }
        return heatmapOptions;
    }
    /**
     *
     * @param {ColorScheme} colorScheme
     * @param {boolean} [dendrogram=true]
     */
    constructor(colorScheme, dendrogram = true) {
        super();
        this._data = new HeatmapData();
        this._colorScheme = colorScheme || (new ColorScheme());
        this._dendrogramAvailable = false;
        this._dendrogram = dendrogram;
        this.handleMetadataLoaded = this.metadataLoaded.bind(this);
        this.changedCallback = () => {
            if (this._cacheOptions && this._cacheOptions.id) {
                writeHeatmapState(this._cacheOptions.id, this.serialize());
            }
            this.emit(events.changed);
        };
        this._colorScheme.onInitialized(this.changedCallback);
        this._colorScheme.onChanged(this.changedCallback);
        this._data.onMetadataLoaded(this.handleMetadataLoaded);
    }

    serialize() {
        return [
            this.colorScheme.serialize(),
            this.dendrogram
        ].join(HeatmapViewOptions.SERIALIZATION_SEPARATOR);
    }

    get dendrogramAvailable() {
        return this._dendrogramAvailable;
    }

    get colorScheme() {
        return this._colorScheme;
    }

    get data() {
        return this._data;
    }

    get dendrogram() {
        return this.dendrogramAvailable && this._dendrogram;
    }

    set dendrogram(dendrogram) {
        if (dendrogram !== this.dendrogram) {
            this._dendrogram = dendrogram;
            this.emit(events.data.dendrogram);
            this.emit(events.changed);
        }
    }

    onChange(callback) {
        this.addEventListener(events.changed, callback);
    }

    setDataConfigurationCache(cache) {
        this._cacheOptions = cache;
    }

    metadataLoaded() {
        const {
            maximum,
            minimum,
            type: dataType,
            values
        } = this._data.metadata;
        let reset = false;
        if (this._cacheOptions && this._data.anotherOptions(this._cacheOptions)) {
            reset = true;
        }
        this._cacheOptions = {...this._data.options};
        let storedOptions;
        if (reset) {
            const serialized = readHeatmapState(this._cacheOptions.id);
            storedOptions = HeatmapViewOptions.parse(serialized);
        }
        if (storedOptions) {
            storedOptions.colorScheme.initialize({
                dataType,
                maximum,
                minimum,
                values,
                reset: false
            });
            this._colorScheme.initializeFrom(storedOptions.colorScheme, true);
            storedOptions.destroy();
        } else {
            this._colorScheme.initialize({
                dataType,
                maximum,
                minimum,
                values,
                reset
            });
        }
        storedOptions = undefined;
        this._dendrogramAvailable = this.data &&
            this.data.metadata &&
            this.data.metadata.dendrogramAvailable;
        this.emit(events.changed);
    }

    onColumnsRowsReordered(callback) {
        this.addEventListener(events.data.sorting, callback);
    }

    destroy() {
        super.destroy();
        if (this._colorScheme) {
            this._colorScheme.destroy();
        }
        this._colorScheme = undefined;
        if (this._data) {
            this._data.destroy();
        }
        this._data = undefined;
    }
}

export default HeatmapViewOptions;
