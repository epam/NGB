import {Animated, Animation} from '../animation';
import events from '../utilities/events';

/**
 * @typedef {Object} HeatmapAxisOptions
 * @property {HeatmapScale} scale
 * @property {number} size
 * @property {number} deviceSize
 */

class HeatmapAxisMoveAnimation extends Animation {
    /**
     *
     * @param {HeatmapAxis} axis
     * @param {number} to
     */
    constructor(axis, to) {
        super({from: axis.center, to});
    }
}

class HeatmapAxis extends Animated {
    /**
     *
     * @param {HeatmapAxisOptions} options
     */
    constructor(options = {}) {
        super();
        this.initialize(options);
    }
    /**
     *
     * @param {HeatmapAxisOptions} [options]
     */
    initialize(options = {}) {
        const {
            scale = this.scale,
            size = 0,
            deviceSize = 0
        } = options;
        this._scale = scale;
        this._size = size;
        this._deviceSize = deviceSize;
        const newCenter = this.invalid
            ? size / 2.0
            : this.scale.getScaleDimension(deviceSize) / 2.0;
        this._center = this.getCorrectedCenter(this._center || newCenter);
    }

    get description() {
        if (this.invalid) {
            return '<invalid>';
        }
        return `${this.center.toFixed(2)} (${this.start.toFixed(2)}-${this.end.toFixed(2)})`;
    }

    /**
     * Heatmap scale
     * @return {HeatmapScale}
     */
    get scale() {
        return this._scale;
    }

    get size() {
        return Number(this._size);
    }

    get deviceSize() {
        return Number(this._deviceSize);
    }

    get totalDeviceSize() {
        if (this.invalid) {
            return 0;
        }
        return this.scale.getDeviceDimension(this.size);
    }

    get center() {
        if (this.invalid) {
            return 0;
        }
        return Number(this._center);
    }

    get deviceCenter() {
        if (this.invalid) {
            return 0;
        }
        return this.deviceSize / 2.0;
    }

    get start() {
        if (this.invalid) {
            return 0;
        }
        return this.getCorrectedPosition(
            this.center - this.scale.getScaleDimension(this.deviceSize) / 2.0
        );
    }

    get end() {
        if (this.invalid) {
            return 0;
        }
        return this.getCorrectedPosition(
            this.center + this.scale.getScaleDimension(this.deviceSize) / 2.0
        );
    }

    get range() {
        return this.end - this.start;
    }

    set deviceSize(deviceSize) {
        if (Number.isNaN(Number(deviceSize)) || Number(deviceSize) <= 0) {
            return;
        }
        const deviceSizeN = Number(deviceSize);
        if (Number.isNaN(this.deviceSize) || this.deviceSize <= 0) {
            this._deviceSize = deviceSizeN;
        } else if (this.deviceSize !== deviceSizeN) {
            const delta = this.scale.getScaleDimension(deviceSizeN - this._deviceSize);
            const center = this.center + delta / 2.0;
            this._deviceSize = deviceSizeN;
            this.move({center}, false);
        }
    }

    get invalid() {
        return Number.isNaN(this.size) ||
            this.size <= 0 ||
            Number.isNaN(this.deviceSize) ||
            this.deviceSize <= 0 ||
            !this.scale ||
            this.scale.invalid;
    }

    getCorrectedCenter(center, scale = this.scale) {
        if (this.invalid) {
            return center;
        }
        const currentViewportSize = Math.min(
            scale.getScaleDimension(this.deviceSize),
            this.size
        );
        const min = currentViewportSize / 2.0;
        const max = this.size - currentViewportSize / 2.0;
        return Math.max(min, Math.min(max, center));
    }

    getCorrectedPosition(position) {
        if (this.invalid) {
            return position;
        }
        const min = 0;
        const max = this.size;
        return Math.max(min, Math.min(max, position));
    }

    getCorrectedPositionWithinAxis(position) {
        if (this.invalid) {
            return position;
        }
        const min = this.start;
        const max = this.end;
        return Math.max(min, Math.min(max, position));
    }

    /**
     * Moves current axis center to the new position; preserves "anchor" on the same device position
     * @param {Object} [options]
     * @param {number} [options.anchor = center]
     * @param {HeatmapScale} [options.futureScale]
     * @param {boolean} animate
     */
    preservePositionOnScale(options = {}, animate = true) {
        const {
            anchor = this.center,
            futureScale
        } = options;
        if (futureScale && !futureScale.invalid) {
            const newCenter = this.getCorrectedCenter(
                anchor + futureScale.getScaleDimension(
                    this.scale.getDeviceDimension(
                        this.center - anchor
                    )
                ),
                futureScale
            );
            return this.move({center: newCenter, correct: false}, animate);
        }
        return false;
    }

    /**
     * Checks if axis can be moved to a new center
     * @param {Object} [options]
     * @param {number} [options.center]
     * @param {boolean} [options.correct = true]
     * @return {boolean}
     */
    canMove(options = {}) {
        if (this.invalid) {
            return false;
        }
        const {
            center: to = this.center,
            correct = true
        } = options;
        const toCorrected = correct ? this.getCorrectedCenter(to) : to;
        return toCorrected !== this.center;
    }

    /**
     * Checks if axis can be moved by a value
     * @param {number} value
     * @return {boolean}
     */
    canMoveBy(value) {
        return this.canMove({center: this.center + value});
    }

    /**
     * Moves axis center
     * @param {Object} [options]
     * @param {number} [options.center]
     * @param {boolean} [options.correct = true]
     * @param {boolean} [animate = true]
     * @return {boolean}
     */
    move(options = {}, animate = true) {
        if (this.invalid) {
            return false;
        }
        const {
            center: to = this.center,
            correct = true
        } = options;
        const toCorrected = correct ? this.getCorrectedCenter(to) : to;
        if (toCorrected === this.center) {
            this.destroyAnimation();
            return false;
        }
        if (animate) {
            const animation = (new HeatmapAxisMoveAnimation(this, toCorrected))
                .onAnimation((o, center) => {
                    this._center = center;
                    this.emit(events.move);
                });
            this.startAnimation(animation);
        } else {
            this.destroyAnimation();
            this._center = toCorrected;
            this.emit(events.move);
        }
        return true;
    }

    /**
     * Moves current axis by value
     * @param {number} value
     * @param {boolean} animate
     * @return {boolean}
     */
    moveBy(value, animate = true) {
        return this.move({center: this.center + value}, animate);
    }

    getScalePosition(devicePosition) {
        if (this.invalid) {
            return 0;
        }
        return this.center
            + this.scale.getScaleDimension(devicePosition - this.deviceCenter);
    }

    getDevicePosition(scalePosition) {
        if (this.invalid) {
            return 0;
        }
        return this.deviceCenter
            + this.scale.getDeviceDimension(scalePosition - this.center);
    }

    onMove(callback) {
        this.addEventListener(events.move, callback);
        return this;
    }
}

export default HeatmapAxis;
