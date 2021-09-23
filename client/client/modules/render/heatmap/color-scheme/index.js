import * as helpers from './helpers';
import * as predefinedColors from './colors';
import ColorConfiguration from './color-configuration';
import GradientCollection from './gradient';
import {HeatmapDataType} from '../heatmap-data';
import HeatmapEventDispatcher from '../utilities/heatmap-event-dispatcher';
import colorSchemes from './schemes';
import events from '../utilities/events';
import makeInitializable from '../utilities/make-initializable';

const {ColorFormats} = helpers;

const ColorCollection = [
    predefinedColors.red,
    predefinedColors.yellow,
    predefinedColors.green
];

/**
 * @typedef {Object} GradientStop
 * @property {number|string} stop
 * @property {number} color
 */

/**
 * @typedef {Object} ColorsSet
 * @property {number} missing
 * @property {number} low
 * @property {number} medium
 * @property {number} high
 * @property {ColorConfigurationOptions[]} configurations
 */

/**
 * @typedef {Object} ColorSchemeOptions
 * @property {ColorSchemes} [type=continuous]
 * @property {ColorFormats} [colorFormat=ColorFormats.number]
 * @property {ColorsSet} colors
 * @property {HeatmapDataType} [dataType=HeatmapDataType.number]
 * @property {number} [minimum]
 * @property {number} [maximum]
 * @property {*[]} [values=[]]
 */

class ColorScheme extends HeatmapEventDispatcher {
    /**
     *
     * @param {ColorSchemeOptions} options
     */
    constructor(options = {}) {
        super();
        makeInitializable(
            this,
            {
                isInitialized: () => this.valid
            }
        );
        this.initialized = false;
        this.initialize(options);
    }

    get valid() {
        if (this.type === colorSchemes.continuous) {
            return this.minimum !== undefined && this.maximum !== undefined;
        }
        return true;
    }

    /**
     * Initializes color scheme
     * @param {ColorSchemeOptions} options
     */
    initialize(options) {
        const {
            type = colorSchemes.continuous,
            colors = {},
            colorFormat = ColorFormats.number,
            ...dataOptions
        } = options;
        const {
            missing = predefinedColors.white,
            high = predefinedColors.red,
            medium = predefinedColors.yellow,
            low = predefinedColors.green,
            configurations = []
        } = colors;
        this._dataType = HeatmapDataType.number;
        this.colorFormat = colorFormat;
        this.colors = {
            missing,
            high,
            medium,
            low
        };
        this.colorConfigurations = configurations
            .map(entry => new ColorConfiguration({
                ...entry,
                colorFormat
            }));
        this.type = type;
        const {
            dataType,
            ...restDataOptions
        } = dataOptions;
        this.assignData({
            ...restDataOptions,
            type: dataType
        });
        this.initialized = true;
    }

    /**
     * Initializes color scheme from another one
     * @param {ColorScheme} colorScheme
     */
    initializeFrom(colorScheme) {
        this.initialize({
            type: colorScheme.type,
            colorFormat: colorScheme.colorFormat,
            colors: {
                ...(colorScheme.colors || {}),
                configurations: colorScheme.colorConfigurations
                    .map(config => config.copy({colorFormat: colorScheme.colorFormat}))
            },
            dataType: colorScheme.dataType,
            minimum: colorScheme.minimum,
            maximum: colorScheme.maximum,
            values: (colorScheme.values || []).slice()
        });
    }

    onChanged(callback) {
        this.addEventListener(events.changed, callback);
    }

    get type () {
        return this._type;
    }

    set type (type) {
        this._type = this.correctType(type);
        if (this._type === colorSchemes.discrete && this.colorConfigurations.length === 0) {
            this.colorConfigurations.push(new ColorConfiguration({
                color: ColorCollection[0],
                colorFormat: this.colorFormat,
                dataType: this.dataType
            }));
        } else if (this._type === colorSchemes.continuous) {
            this.colorConfigurations = [];
        }
        this.updateGradientStops();
    }

    get dataType () {
        return this._dataType;
    }

    set dataType (dataType) {
        this._dataType = dataType;
        this.colorConfigurations.forEach(configuration => {
            configuration.dataType = this._dataType;
        });
        this.type = this.correctType(this.type);
    }

    correctType (type) {
        if (!this.colorSchemeAvailable(type)) {
            const [scheme] = Object.values(colorSchemes)
                .filter(cs => this.colorSchemeAvailable(cs));
            return scheme || Object.values(colorSchemes).pop();
        }
        return type;
    }

    colorSchemeAvailable(scheme) {
        switch (scheme) {
            case colorSchemes.continuous:
                return this.dataType === HeatmapDataType.number;
            case colorSchemes.discrete:
            default:
                return true;
        }
    }

    /**
     * Assigns heatmap metadata
     * @param {HeatmapMetadata} options
     */
    assignData(options = {}) {
        const {
            minimum,
            maximum,
            type: dataType = HeatmapDataType.number,
            values = []
        } = options;
        this.minimum = minimum;
        this.maximum = maximum;
        this.values = values.slice();
        this.dataType = dataType;
    }

    updateGradientStops () {
        const minimum = this.minimum || 0;
        const maximum = this.maximum || 1;
        if (this.type === colorSchemes.continuous && this.dataType === HeatmapDataType.number) {
            /**
             * @type {GradientCollection}
             */
            this.gradientCollection = GradientCollection.continuousCollection(minimum, maximum, this.colors);
        } else if (this.colorConfigurations.length < 2) {
            const [configuration = {}] = this.colorConfigurations;
            const {color = this.colors.missing} = configuration;
            /**
             * @type {GradientCollection}
             */
            this.gradientCollection = GradientCollection.singleColorCollection(color);
        } else {
            /**
             * @type {GradientCollection}
             */
            this.gradientCollection = GradientCollection.fromColorConfigurations(this.colorConfigurations);
        }
        this.emit(events.changed);
    }

    get missingColor() {
        return helpers.formatColor(this.colors.missing, this.colorFormat);
    }

    set missingColor(color) {
        this.colors.missing = helpers.systemColorValue(color);
        this.updateGradientStops();
    }

    get highColor() {
        return helpers.formatColor(this.colors.high, this.colorFormat);
    }

    set highColor(color) {
        this.colors.high = helpers.systemColorValue(color);
        this.updateGradientStops();
    }

    get lowColor() {
        return helpers.formatColor(this.colors.low, this.colorFormat);
    }

    set lowColor(color) {
        this.colors.low = helpers.systemColorValue(color);
        this.updateGradientStops();
    }

    get mediumColor() {
        return helpers.formatColor(this.colors.medium, this.colorFormat);
    }

    set mediumColor(color) {
        this.colors.medium = helpers.systemColorValue(color);
        this.updateGradientStops();
    }

    getColorForValue(value, reportMissingAsUndefined = false) {
        if (this.gradientCollection) {
            return this.gradientCollection.getColorForValue(value, this.colors.missing, reportMissingAsUndefined);
        }
        return reportMissingAsUndefined ? undefined : this.colors.missing;
    }

    addColorConfiguration() {
        if (this.type === colorSchemes.continuous) {
            return;
        }
        this.colorConfigurations.push(new ColorConfiguration({
            colorFormat: this.colorFormat,
            color: ColorCollection[this.colorConfigurations.length % ColorCollection.length],
            dataType: this.dataType
        }));
        this.updateGradientStops();
    }

    removeColorConfiguration(configuration) {
        if (this.type === colorSchemes.continuous) {
            return;
        }
        const index = this.colorConfigurations.indexOf(configuration);
        this.colorConfigurations.splice(index, 1);
        if (this.colorConfigurations.length === 0) {
            this.colorConfigurations.push(new ColorConfiguration({
                color: ColorCollection[0],
                colorFormat: this.colorFormat,
                dataType: this.dataType
            }));
        }
        this.updateGradientStops();
    }

    copy(options = {}) {
        return new ColorScheme({
            type: this.type,
            colors: {
                ...this.colors,
                configurations: this.colorConfigurations.map(entry => entry.copy({
                    colorFormat: this.colorFormat,
                    ...options
                }))
            },
            minimum: this.minimum,
            maximum: this.maximum,
            dataType: this.dataType,
            values: this.values.slice(),
            colorFormat: this.colorFormat,
            ...options
        });
    }
}

export {colorSchemes, ColorFormats};
export default ColorScheme;
