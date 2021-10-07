import {interpolateColors} from './helpers';
import {white} from './colors';

export class GradientStop {
    /**
     *
     * @param {number|value} stop
     * @param {number} color
     */
    constructor(stop, color) {
        this.stop = stop;
        this.color = color;
        this.include = true;
    }

    get include() {
        return this._include;
    }

    set include(include) {
        if (Number.isNaN(Number(this.stop)) || !Number.isFinite(Number(this.stop))) {
            this._include = true;
        } else {
            this._include = include;
        }
    }

    check(o, less = true) {
        switch (this.include) {
            case false:
                return less ? o < this.stop : o > this.stop;
            case true:
            default:
                return less ? o <= this.stop : o >= this.stop;
        }
    }
}

export class Gradient {
    /**
     * Initializes gradient for single color for all values
     * @param {number} color
     * @returns {Gradient}
     */
    static fromColor(color) {
        return new Gradient(new GradientStop(-Infinity, color), new GradientStop(Infinity, color));
    }

    /**
     * Initializes gradient as single color for values range
     * @param {ColorConfiguration} configuration
     */
    static fromColorConfiguration(configuration) {
        if (!configuration) {
            return undefined;
        }
        return new Gradient(
            new GradientStop(configuration.from, configuration.colorValue),
            new GradientStop(configuration.to, configuration.colorValue),
        );
    }
    /**
     *
     * @param {GradientStop} gradientStop1
     * @param {GradientStop} gradientStop2
     */
    constructor(gradientStop1, gradientStop2) {
        this.gradientStop1 = gradientStop1;
        this.gradientStop2 = gradientStop2;
        /**
         * Gradient unique identifier
         * @type {string}
         */
        this.key = 'invalid';
        this.description = 'Data';
        if (this.gradientStop1 && this.gradientStop2) {
            const {stop: from} = this.gradientStop1;
            const {stop: to} = this.gradientStop2;
            if (this.isSingleValue) {
                this.key = `${from}`;
                this.description = `${from}`;
            } else if (!this.isFinite) {
                this.key = 'infinite';
                this.description = 'Data';
            } else {
                this.key = `${from}/${to}`;
                this.description = `${from} - ${to}`;
            }
        }
    }

    get from() {
        if (this.gradientStop1) {
            return this.gradientStop1.stop;
        }
        return undefined;
    }

    get to() {
        if (this.gradientStop2) {
            return this.gradientStop2.stop;
        }
        return undefined;
    }

    get range() {
        if (!Number.isNaN(Number(this.from)) && !Number.isNaN(Number(this.to))) {
            return Math.abs(Number(this.to) - Number(this.from));
        }
        return 0;
    }

    /**
     *
     * @param {Gradient[]} otherGradients
     */
    setGradientStopsRegionOpenness(otherGradients = []) {
        const toUnique = otherGradients
            .filter(otherGradient => otherGradient.from === this.to)
            .length === 0;
        if (this.gradientStop2) {
            this.gradientStop2.include = toUnique;
        }
    }

    get isFinite() {
        if (this.gradientStop1 && this.gradientStop2) {
            const {stop: from} = this.gradientStop1;
            const {stop: to} = this.gradientStop2;
            return !Number.isNaN(Number(from)) &&
                !Number.isNaN(Number(to)) &&
                Number.isFinite(Number(from)) &&
                Number.isFinite(Number(to));
        }
        return false;
    }

    get isSingleValue() {
        if (this.gradientStop1 && this.gradientStop2) {
            const {stop: from} = this.gradientStop1;
            const {stop: to} = this.gradientStop2;
            return from === to;
        }
        return false;
    }

    get value() {
        if (this.isSingleValue) {
            return this.gradientStop1 ? this.gradientStop1.stop : 0;
        }
        return undefined;
    }

    /**
     * Tests if value hits gradient range
     * @param {number|string|undefined} value
     */
    test(value) {
        if (value === undefined) {
            return false;
        }
        if (this.isSingleValue) {
            let equals = this.value === value;
            if (
                !equals && this.value !== undefined && this.value.toString &&
                value !== undefined && value.toString
            ) {
                equals = value.toString() === this.value.toString();
            }
            return equals;
        }
        if (!this.isFinite) {
            return true;
        }
        if (this.gradientStop1 && this.gradientStop2) {
            return this.gradientStop1.check(value, false) &&
                this.gradientStop2.check(value, true);
        }
        return false;
    }

    /**
     * Returns value ratio for gradient
     * @param {number} value
     * @returns {number}
     */
    getRatio(value) {
        const {
            stop: stop1 = 0
        } = this.gradientStop1 || {};
        const {
            stop: stop2 = 0
        } = this.gradientStop2 || {};
        if (
            this.isSingleValue ||
            !Number.isFinite(stop1 - stop2) ||
            typeof stop1 !== 'number' ||
            typeof stop2 !== 'number'
        ) {
            return 0;
        }
        return (value - stop1) / (stop2 - stop1);
    }

    /**
     * Returns interpolated color for value
     * @param {number|string} value
     * @param {number} missing
     * @returns {number}
     */
    getSimpleGradient(value, missing = white) {
        const ratio = this.getRatio(value);
        const {
            color: color1 = missing,
        } = this.gradientStop1 || {};
        const {
            color: color2 = missing,
        } = this.gradientStop2 || {};
        return interpolateColors(color1, color2, ratio);
    }

    /**
     * Returns any color from gradient
     * @returns {number}
     */
    getAnyColor() {
        const {
            color: color1
        } = this.gradientStop1 || {};
        const {
            color: color2
        } = this.gradientStop2 || {};
        return color1 || color2;
    }
}

/**
 * @typedef {Object} ContinuousColors
 * @property {number} high
 * @property {number} medium
 * @property {number} low
 */

export default class GradientCollection {
    /**
     * Returns gradient collection for continuous color scheme
     * @param {number} from
     * @param {number} to
     * @param {ContinuousColors} colors
     * @returns {GradientCollection}
     */
    static continuousCollection(from, to, colors) {
        const {
            high,
            medium,
            low
        } = colors;
        const lowStop = new GradientStop((from || 0), low);
        const mediumStop1 = new GradientStop((from + to) / 2.0, medium);
        const mediumStop2 = new GradientStop((from + to) / 2.0, medium);
        const highStop = new GradientStop(to, high);
        const lowGradient = new Gradient(lowStop, mediumStop1);
        const highGradient = new Gradient(mediumStop2, highStop);
        return new GradientCollection(lowGradient, highGradient);
    }

    /**
     * Returns gradient collection for single color
     * @param {number} color
     * @returns {GradientCollection}
     */
    static singleColorCollection(color) {
        return new GradientCollection(Gradient.fromColor(color));
    }

    /**
     * Returns gradient collection for color configurations
     * @param {ColorConfiguration[]} configurations
     */
    static fromColorConfigurations(configurations) {
        return new GradientCollection(
            ...configurations.map(configuration => Gradient.fromColorConfiguration(configuration))
        );
    }
    /**
     *
     * @param {Gradient[]} gradients
     */
    constructor(...gradients) {
        /**
         *
         * @type {Gradient[]}
         */
        this.gradients = gradients;
        this.gradients.sort((a, b) => a.range - b.range);
        this.checkGradientRegions();
    }

    checkGradientRegions() {
        for (const gradient of this.gradients) {
            gradient.setGradientStopsRegionOpenness(
                this.gradients.filter(g => g !== gradient)
            );
        }
    }

    /**
     * Gets gradient stops length
     * @returns {number}
     */
    get length() {
        return (this.gradients || []).length;
    }

    /**
     * Returns all gradient keys
     * @returns {string[]}
     */
    get keys() {
        return (this.gradients || []).map(gradient => gradient.key);
    }

    /**
     * Gets gradient by unique identifier
     * @param {string} key
     * @returns {Gradient|undefined}
     */
    getGradientByKey(key) {
        return (this.gradients || []).filter(gradient => gradient.key === key).pop();
    }

    /**
     * Gets gradient configuration for value
     * @param {number|string|undefined} value
     * @returns {Gradient}
     */
    getGradientForValue (value) {
        if (value === undefined || !this.gradients || this.gradients.length === 0) {
            return undefined;
        }
        for (let i = 0; i < this.gradients.length; i += 1) {
            const gradient = this.gradients[i];
            if (gradient && gradient.test(value)) {
                return gradient;
            }
        }
        return undefined;
    }

    /**
     * Returns color for value
     * @param {number|string|undefined} value
     * @param {number} missing - missing color
     * @param {boolean} [reportMissingAsUndefined=false]
     * @returns {number|undefined}
     */
    getColorForValue (
        value,
        missing = white,
        reportMissingAsUndefined = false
    ) {
        const gradient = this.getGradientForValue(value);
        if (gradient !== undefined) {
            return gradient.getSimpleGradient(value, missing);
        }
        if (reportMissingAsUndefined) {
            return undefined;
        }
        return missing;
    }

    /**
     * Gets gradient by index
     * @param {number} index
     * @returns {undefined|Gradient}
     */
    get(index) {
        if (index >= 0 && index < this.length) {
            return this.gradients[index];
        }
        return undefined;
    }

    /**
     * Iterates gradients
     * @returns {Generator<Gradient|*, void, *>}
     */
    *values() {
        for (const gradient of (this.gradients || [])) {
            yield gradient;
        }
    }
}
