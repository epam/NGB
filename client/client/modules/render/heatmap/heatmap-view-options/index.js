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
        let annotations = true,
            colorScheme,
            dendrogram = true;
        let payload = serialized;
        if (!serialized && dataConfig && dataConfig.id) {
            payload = readHeatmapState(dataConfig.id);
        }
        if (!payload) {
            payload = '';
        }
        try {
            const [
                colorSchemeSerialized,
                dendrogramEnabled = 'true',
                showAnnotations = 'true'
            ] = payload.split(HeatmapViewOptions.SERIALIZATION_SEPARATOR);
            dendrogram = /^true$/i.test(dendrogramEnabled);
            annotations = /^true$/i.test(showAnnotations);
            if (colorSchemeSerialized) {
                colorScheme = ColorScheme.parse(colorSchemeSerialized);
            }
            // eslint-disable-next-line no-empty
        } catch (_) {}
        const heatmapOptions = new HeatmapViewOptions(colorScheme, {annotations, dendrogram});
        if (dataConfig) {
            heatmapOptions.setDataConfigurationCache(dataConfig);
        }
        return heatmapOptions;
    }
    /**
     *
     * @param {ColorScheme} colorScheme
     * @param {Object} [options]
     * @param {boolean} [options.annotations=true]
     * @param {boolean} [options.dendrogram=true]
     */
    constructor(colorScheme, options = {}) {
        super();
        const {
            annotations = true,
            dendrogram = true
        } = options;
        this._data = new HeatmapData();
        this._colorScheme = colorScheme || (new ColorScheme());
        this._dendrogramAvailable = false;
        this._annotationsAvailable = false;
        this._dendrogram = dendrogram;
        this._annotations = annotations;
        this.handleMetadataLoaded = this.metadataLoaded.bind(this);
        this._colorScheme.onChanged(this.changedCallback.bind(this));
        this._data.onMetadataLoaded(this.handleMetadataLoaded);
    }

    changedCallback = () => {
        if (this._cacheOptions && this._cacheOptions.id) {
            writeHeatmapState(this._cacheOptions.id, this.serialize());
        }
        this.emit(events.changed);
    };

    serialize() {
        return [
            this.colorScheme.serialize(),
            this._dendrogram,
            this._annotations
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

    get dataConfig() {
        return this._cacheOptions;
    }

    get dendrogram() {
        return this.dendrogramAvailable && this._dendrogram;
    }

    set dendrogram(dendrogram) {
        if (dendrogram !== this.dendrogram) {
            this._dendrogram = dendrogram;
            this.emit(events.data.dendrogram);
            this.changedCallback();
        }
    }

    get annotationsAvailable() {
        return this._annotationsAvailable;
    }

    get annotations() {
        return this.annotationsAvailable && this._annotations;
    }

    set annotations(annotations) {
        if (annotations !== this.annotations) {
            this._annotations = annotations;
            this.changedCallback();
            this.emit(events.data.annotations);
        }
    }

    onChange(callback) {
        this.addEventListener(events.changed, callback);
    }

    setDataConfigurationCache(cache) {
        this._cacheOptions = cache;
    }

    updateOptionsAvailability() {
        this._dendrogramAvailable = this.data &&
            this.data.metadata &&
            this.data.metadata.dendrogramAvailable;
        this._annotationsAvailable = this.data &&
            this.data.metadata &&
            this.data.metadata.annotationsAvailable;
    }

    reloadFromStorage() {
        if (this.colorScheme) {
            const serialized = readHeatmapState(this._cacheOptions.id);
            const storedOptions = HeatmapViewOptions.parse(serialized);
            if (storedOptions) {
                storedOptions.colorScheme.initialize({
                    dataType: this.colorScheme.dataType,
                    maximum: this.colorScheme.maximum,
                    minimum: this.colorScheme.minimum,
                    values: this.colorScheme.values,
                    reset: false
                });
                this._colorScheme.initializeFrom(storedOptions.colorScheme, true);
                storedOptions.destroy();
            }
            const currentDendrogram = this._dendrogram;
            const currentAnnotations = this._annotations;
            this._dendrogram = storedOptions ? storedOptions._dendrogram : this._dendrogram;
            this._annotations = storedOptions ? storedOptions._annotations : this._annotations;
            this.updateOptionsAvailability();
            if (currentDendrogram !== this._dendrogram) {
                this.emit(events.data.dendrogram);
            }
            if (currentAnnotations !== this._annotations) {
                this.emit(events.data.annotations);
            }
            this.emit(events.changed);
        }
    }

    metadataLoaded() {
        const {
            maximum,
            minimum,
            type: dataType,
            values
        } = this._data.metadata;
        let reset = false;
        if (this._data.anotherOptions(this._cacheOptions)) {
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
        const currentDendrogram = this._dendrogram;
        const currentAnnotations = this._annotations;
        this._dendrogram = storedOptions ? storedOptions._dendrogram : this._dendrogram;
        this._annotations = storedOptions ? storedOptions._annotations : this._annotations;
        storedOptions = undefined;
        this.updateOptionsAvailability();
        if (currentDendrogram !== this._dendrogram) {
            this.emit(events.data.dendrogram);
        }
        if (currentAnnotations !== this._annotations) {
            this.emit(events.data.annotations);
        }
        this.emit(events.changed);
    }

    onAnnotationsModeChanged(callback) {
        this.addEventListener(events.data.annotations, callback);
    }

    onColumnsRowsReordered(callback) {
        this.addEventListener(events.data.sorting, callback);
    }

    onDendrogramModeChanged(callback) {
        this.addEventListener(events.data.dendrogram, callback);
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
