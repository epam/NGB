import * as helpers from './helpers';
import * as predefinedColors from './colors';
import ColorConfiguration from './color-configuration';
import GradientCollection from './gradient';
import {HeatmapDataType} from '../heatmap-data';
import HeatmapEventDispatcher from '../utilities/heatmap-event-dispatcher';
import colorSchemes from './schemes';
import events from '../utilities/events';
import {linearDimensionsConflict} from '../../utilities';
import makeInitializable from '../utilities/make-initializable';

const {ColorFormats} = helpers;

const SINGLE_VALUE_AVAILABLE_BEFORE = 100;

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
        /**
         *
         * @type {ColorConfiguration[]}
         */
        this.colorConfigurations = configurations
            .map(entry => new ColorConfiguration({
                ...entry,
                colorFormat,
                validate: this.validate.bind(this)
            }));
        this.validate();
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

    onConfigureRequest(callback) {
        this.addEventListener(events.colorScheme.configure, callback);
    }

    configureRequest() {
        this.emit(events.colorScheme.configure);
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
                dataType: this.dataType,
                validate: this.validate.bind(this)
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

    get singleValueModeAvailable() {
        return this.dataType !== HeatmapDataType.number ||
            (
                this.values.length > 0 &&
                this.values.length < SINGLE_VALUE_AVAILABLE_BEFORE
            );
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
        this.validate();
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
        let from = undefined;
        let to = undefined;
        if (
            this.colorConfigurations.length > 0 &&
            !this.singleValueModeAvailable
        ) {
            const valid = this.colorConfigurations.filter(c => !Number.isNaN(Number(c.from)) &&
                !Number.isNaN(Number(c.to))
            );
            if (valid.length > 0) {
                from = Math.max(...valid.map(v => Math.max(v.from, v.to)));
                if (Number.isNaN(Number(from))) {
                    from = undefined;
                } else {
                    to = from + 1;
                }
            }
        }
        if (this.colorConfigurations.length === 1) {
            this.colorConfigurations[0].singleValue = this.singleValueModeAvailable;
        }
        this.colorConfigurations.push(new ColorConfiguration({
            colorFormat: this.colorFormat,
            color: ColorCollection[this.colorConfigurations.length % ColorCollection.length],
            dataType: this.dataType,
            validate: this.validate.bind(this),
            from,
            to,
            singleValue: this.singleValueModeAvailable
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
                dataType: this.dataType,
                validate: this.validate.bind(this)
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

    validate() {
        this.error = this.colorConfigurations.length > 0 &&
            this.colorConfigurations
                .filter(configuration => configuration.validate())
                .length === 0;
        if (!this.error) {
            for (const configuration of this.colorConfigurations) {
                const other = this.colorConfigurations.filter(c => c !== configuration);
                if (this.dataType === HeatmapDataType.number) {
                    const conflicts = other.filter(o => linearDimensionsConflict(
                        o.from,
                        o.to,
                        configuration.from,
                        configuration.to
                    ))
                        .length > 0;
                    if (conflicts) {
                        configuration.error = 'This configuration conflicts with another one';
                        this.error = true;
                    }
                } else if (other.filter(o => o.value === configuration.value).length > 0) {
                    configuration.error = 'This configuration conflicts with another one';
                    this.error = true;
                }
            }
        }
    }
}

export {colorSchemes, ColorFormats};
export default ColorScheme;
