import {Animated, Animation} from '../animation';
import events from '../utilities/events';

const DEFAULT_MINIMUM_TICK_DEVICE_SIZE_PX = 1;
const DEFAULT_MAXIMUM_TICK_DEVICE_SIZE_PX = 150;

/**
 * @typedef {Object} HeatmapScalerOptions
 * @property {number} tickSize
 * @property {number} minimumTickSize
 * @property {number} maximumTickSize
 */

class HeatmapScale extends Animated {
    /**
     *
     * @param {HeatmapScalerOptions} options
     */
    constructor(options = {}) {
        super();
        this.initialize(options);
    }
    /**
     *
     * @param {HeatmapScalerOptions} [options]
     */
    initialize(options = {}) {
        const {
            minimumTickSize = DEFAULT_MINIMUM_TICK_DEVICE_SIZE_PX,
            maximumTickSize = DEFAULT_MAXIMUM_TICK_DEVICE_SIZE_PX,
            tickSize = this._tickSize || minimumTickSize
        } = options;
        this._minimumTickSize = minimumTickSize;
        this._maximumTickSize = maximumTickSize;
        this._tickSize = this.getCorrectedTickSize(tickSize);
        return this;
    }

    onScale(callback) {
        this.addEventListener(events.scale, callback);
        return this;
    }

    get minimumTickSize() {
        return this._minimumTickSize;
    }

    set minimumTickSize(minimumTickSize) {
        if (
            !Number.isNaN(Number(minimumTickSize)) &&
            Number(minimumTickSize) > 0 &&
            Number(minimumTickSize) !== this._minimumTickSize
        ) {
            this._minimumTickSize = Math.min(
                Math.max(DEFAULT_MINIMUM_TICK_DEVICE_SIZE_PX, Number(minimumTickSize)),
                this._maximumTickSize
            );
            if (this.tickSize < this._minimumTickSize) {
                this.setTickSize(this.tickSize, false);
            }
        }
    }

    get invalid() {
        return Number.isNaN(this.tickSize) || this.tickSize <= 0;
    }

    get tickSize() {
        return Number(this._tickSize);
    }

    /**
     * Sets tick size immediately
     * @param {number} tickSize
     */
    set tickSize(tickSize) {
        this.setTickSize(tickSize, false);
    }

    get description() {
        if (this.invalid) {
            return '<invalid>';
        }
        return this.tickSize.toFixed(2);
    }

    getCorrectedTickSize(tickSize) {
        return Math.max(
            this._minimumTickSize,
            Math.min(this._maximumTickSize, tickSize)
        );
    }

    getDeviceDimension(axisDimension) {
        if (this.invalid) {
            return 0;
        }
        return this.tickSize * axisDimension;
    }

    getScaleDimension(deviceDimension) {
        if (this.invalid) {
            return 0;
        }
        return deviceDimension / this.tickSize;
    }

    getFutureScale(tickSize) {
        return new HeatmapScale({
            minimumTickSize: this._minimumTickSize,
            maximumTickSize: this._maximumTickSize,
            tickSize
        });
    }

    /**
     * Sets tick size animated (optionally)
     * @param {number} tickSize
     * @param {boolean} animate
     * @return {boolean} true if tick size was changed
     */
    setTickSize(tickSize, animate = true) {
        const tickSizeCorrected = this.getCorrectedTickSize(tickSize);
        if (tickSizeCorrected === this._tickSize) {
            this.destroyAnimation();
            return false;
        }
        if (this._tickSize !== undefined && animate) {
            const animation = new Animation({from: this._tickSize, to: tickSizeCorrected})
                .onAnimation((o, animatedTickSize) => {
                    this._tickSize = animatedTickSize;
                    this.emit(events.scale);
                });
            this.startAnimation(animation);
        } else {
            this.destroyAnimation();
            this._tickSize = tickSizeCorrected;
            this.emit(events.scale);
        }
        return true;
    }
}

export default HeatmapScale;
