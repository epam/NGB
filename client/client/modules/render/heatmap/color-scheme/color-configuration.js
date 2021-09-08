import * as colors from './colors';
import * as helpers from './helpers';
import {HeatmapDataType} from '../heatmap-data';

const {ColorFormats} = helpers;

/**
 * @typedef {Object} ColorConfigurationOptions
 * @property {number} [_color]
 * @property {number|string} [color=0xffffff]
 * @property {number|string} [from=1]
 * @property {number|string} [to=1]
 * @property {string} [colorFormat=number]
 * @property {HeatmapDataType} [dataType=HeatmapDataType.number]
 */

export default class ColorConfiguration {
    static uniqueIdentifier = 0;
    constructor(options = {}) {
        const {
            _color,
            color = colors.white,
            dataType = HeatmapDataType.number,
            from = (dataType === HeatmapDataType.number ? 0 : undefined),
            to = (dataType === HeatmapDataType.number ? 1 : undefined),
            colorFormat = ColorFormats.number
        } = options;
        ColorConfiguration.uniqueIdentifier += 1;
        this._uid = ColorConfiguration.uniqueIdentifier;
        this._color = _color !== undefined ? _color : helpers.systemColorValue(color);
        this.from = from;
        this.to = to;
        this._dataType = dataType;
        /**
         * Color format
         * @type {ColorFormats}
         */
        this.colorFormat = colorFormat;
    }

    get uid() {
        return this._uid;
    }

    get color() {
        return helpers.formatColor(this._color, this.colorFormat);
    }

    get colorValue() {
        return this._color;
    }

    set color(color) {
        this._color = helpers.systemColorValue(color);
    }

    get value() {
        return this.from;
    }

    set value(value) {
        this.from = value;
        this.to = value;
    }

    get dataType() {
        return this._dataType;
    }

    set dataType(dataType) {
        if (this._dataType !== dataType) {
            this._dataType = dataType;
            if (this._dataType === HeatmapDataType.number) {
                this.from = !Number.isNaN(Number(this.from)) ? Number(this.from) : 0;
                this.to = !Number.isNaN(Number(this.to)) ? Number(this.to) : 1;
            }
        }
    }

    copy (options = {}) {
        return new ColorConfiguration({
            color: this.color,
            colorFormat: this.colorFormat,
            from: this.from,
            to: this.to,
            dataType: this.dataType,
            ...options
        });
    }
}
