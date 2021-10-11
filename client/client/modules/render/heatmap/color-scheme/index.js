import * as helpers from './helpers';
import {
    getAvailableColor,
    green,
    red,
    white,
    yellow
} from './colors';
import ColorConfiguration from './color-configuration';
import GradientCollection from './gradient';
import {HeatmapDataType} from '../heatmap-data';
import HeatmapEventDispatcher from '../utilities/heatmap-event-dispatcher';
import colorSchemes from './schemes';
import events from '../utilities/events';
import {linearDimensionsConflict} from '../../utilities';
import makeInitializable from '../utilities/make-initializable';

const {ColorFormats} = helpers;

const SINGLE_VALUE_AVAILABLE_BEFORE = 20;

const uid = (() => {
    let identifier = 0;
    return () => ++identifier;
})();

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
 * @property {boolean} [reset=true]
 */

class ColorScheme extends HeatmapEventDispatcher {
    static SERIALIZATION_SEPARATOR = ',';
    static parse(serialized) {
        if (!serialized || typeof serialized !== 'string') {
            return new ColorScheme();
        }
        const [
            dataTypeSerialized,
            ...rest
        ] = serialized.split(ColorScheme.SERIALIZATION_SEPARATOR);
        const missingColorSerialized = rest.pop();
        const dataType = HeatmapDataType.parse(dataTypeSerialized);
        const parseColor = color => helpers.systemColorValue(color);
        const missingColor = parseColor(missingColorSerialized);
        const colorRegExp = /^#[0-9a-fA-F]{6}$/;
        let highColor, lowColor, mediumColor;
        const configurations = [];
        const type = rest.length === 3 && rest.map(o => colorRegExp.test(o)).filter(Boolean).length === 3
            ? colorSchemes.continuous
            : colorSchemes.discrete;
        if (type === colorSchemes.continuous) {
            highColor = parseColor(rest[0]);
            mediumColor = parseColor(rest[1]);
            lowColor = parseColor(rest[2]);
        } else {
            configurations.push(
                ...rest.map(configurationSerialized => ColorConfiguration.parse(configurationSerialized, dataType))
            );
        }
        console.log(
            'ColorScheme.parse',
            dataType,
            missingColorSerialized,
            missingColor,
            rest,
            type,
            configurations
        );
        return new ColorScheme({
            type,
            dataType,
            colors: {
                high: highColor,
                medium: mediumColor,
                low: lowColor,
                missing: missingColor,
                configurations
            }
        });
    }
    /**
     *
     * @param {ColorSchemeOptions} options
     */
    constructor(options = {}) {
        super();
        /**
         * Unique identifier
         * @type {number}
         */
        this.id = uid();
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

    destroy() {
        this.detachDispatcher();
        super.destroy();
    }

    serialize() {
        const dataType = HeatmapDataType.serialize(this.dataType, true);
        const format = color => helpers.formatColor(color, ColorFormats.hex);
        const parts = [dataType];
        if (this.type === colorSchemes.continuous) {
            parts.push(
                format(this.highColor),
                format(this.mediumColor),
                format(this.lowColor)
            );
        }
        if (this.colorConfigurations.length === 1) {
            parts.push(
                format(this.colorConfigurations[0].color)
            );
        } else if (this.colorConfigurations.length > 1) {
            parts.push(
                ...this.colorConfigurations.map(configuration => configuration.serialize(format))
            );
        }
        parts.push(format(this.missingColor));
        return parts.join(ColorScheme.SERIALIZATION_SEPARATOR);
    }

    detachDispatcher() {
        if (typeof this.removeDispatcherListeners === 'function') {
            this.removeDispatcherListeners();
        }
    }

    /**
     *
     * @param {dispatcher} dispatcher
     */
    attachDispatcher(dispatcher) {
        this.detachDispatcher();
        const replaceColorScheme = (colorScheme) => this.initializeFrom(colorScheme);
        const configureColorScheme = () => dispatcher.emit(
            'heatmap:colorscheme:configure',
            {
                config: {
                    id: this.id,
                    scheme: this.copy({colorFormat: ColorFormats.hex})
                },
            }
        );
        this.removeDispatcherListeners = () => {
            dispatcher.removeListener(`heatmap:colorscheme:configure:done:${this.id}`, replaceColorScheme);
            this.removeEventListeners(configureColorScheme);
        };
        dispatcher.on(`heatmap:colorscheme:configure:done:${this.id}`, replaceColorScheme);
        this.onConfigureRequest(configureColorScheme);
    }

    /**
     * Initializes color scheme
     * @param {ColorSchemeOptions} options
     */
    initialize(options = {}) {
        const {
            reset = true
        } = options || {};
        if (reset) {
            this.initialized = false;
        }
        const defaultOptionValue = (currentValue, defaultValue) => reset
            ? defaultValue
            : (currentValue || defaultValue);
        const {
            type = defaultOptionValue(this.type, colorSchemes.continuous),
            colors = defaultOptionValue(this.colors, {}),
            colorFormat = defaultOptionValue(this.colorFormat, ColorFormats.number),
            ...dataOptions
        } = options;
        const {
            missing = white,
            high = red,
            medium = yellow,
            low = green,
            configurations = defaultOptionValue(this.colorConfigurations, [])
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
            dataType = defaultOptionValue(this.dataType),
            ...restDataOptions
        } = dataOptions;
        this.assignData({
            ...restDataOptions,
            type: dataType
        });
        if (!reset && this.initialized) {
            this.emit(events.changed);
        }
        this.initialized = true;
    }

    /**
     * Initializes color scheme from another one
     * @param {ColorScheme} colorScheme
     * @param {boolean} [reset=false]
     */
    initializeFrom(colorScheme, reset = false) {
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
            values: (colorScheme.values || []).slice(),
            reset: false
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
            if (this.singleValueModeAvailable) {
                const usedColors = [];
                this.values.forEach(value => {
                    const color = getAvailableColor(usedColors);
                    usedColors.push(color);
                    this.colorConfigurations.push(new ColorConfiguration({
                        color: helpers.formatColor(color, this.colorFormat),
                        colorFormat: this.colorFormat,
                        dataType: this.dataType,
                        validate: this.validate.bind(this),
                        from: value,
                        to: value,
                        singleValue: true
                    }));
                });
            } else {
                this.colorConfigurations.push(new ColorConfiguration({
                    color: getAvailableColor(),
                    colorFormat: this.colorFormat,
                    dataType: this.dataType,
                    validate: this.validate.bind(this)
                }));
            }
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
        if (this.initialized) {
            this.emit(events.changed);
        }
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
        const usedColors = this.colorConfigurations
            .map(configuration => helpers.systemColorValue(configuration.color));
        this.colorConfigurations.push(new ColorConfiguration({
            colorFormat: this.colorFormat,
            color: getAvailableColor(usedColors),
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
                color: getAvailableColor(),
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
