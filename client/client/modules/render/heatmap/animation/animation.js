import HeatmapEventDispatcher from '../utilities/heatmap-event-dispatcher';
import events from '../utilities/events';

const DEFAULT_ANIMATION_DURATION = 250;

/**
 * @typedef {Object} AnimationOptions
 * @property {number} [from=0]
 * @property {number} [to=1]
 * @property {number} [duration=DEFAULT_ANIMATION_DURATION]
 * @property {function} [onStart]
 * @property {function} [onFinish]
 * @property {function} [onAnimate]
 * @property {boolean} [animate=true]
 */

export default class Animation extends HeatmapEventDispatcher {
    /**
     *
     * @param {AnimationOptions} options
     */
    constructor(options = {}) {
        super();
        const {
            from = 0,
            to = 1,
            duration = DEFAULT_ANIMATION_DURATION,
            onStart,
            onAnimate,
            onFinish,
            animate = true
        } = options;
        this.from = from;
        this.to = to;
        this.duration = duration;
        this.startTime = undefined;
        this.time = undefined;
        this.started = false;
        this.finished = false;
        if (onAnimate) {
            this.addEventListener(events.animation.tick, onAnimate);
        }
        if (onStart) {
            this.addEventListener(events.animation.start, onStart);
        }
        if (onFinish) {
            this.addEventListener(events.animation.finish, onFinish);
        }
        this.shouldRequestFrame = animate;
    }

    start(time) {
        this.startTime = time;
        this.started = true;
        this.emit(events.animation.start);
        this.requestNextAnimationFrame();
        return this;
    }

    animate(time) {
        if (!this.started || this.finished) {
            return;
        }
        this.time = time;
        if (!this.startTime) {
            this.startTime = time;
            this.requestNextAnimationFrame();
            return;
        }
        if (this.emitAnimationFrame()) {
            this.requestNextAnimationFrame();
        } else {
            this.stop();
        }
    }

    /**
     * Emits 'animation' event with current value
     * @param {boolean} [emitOnFinished = false]
     * @return {boolean} true if animation is not finished yet
     */
    emitAnimationFrame(emitOnFinished = false) {
        const passed = this.time - this.startTime;
        const ratio = Math.min(passed / this.duration, 1);
        const value = this.from + (this.to - this.from) * ratio;
        if (!passed || !emitOnFinished) {
            this.emit(events.animation.tick, value);
        }
        return passed < this.duration;
    }

    requestNextAnimationFrame() {
        if (this.shouldRequestFrame) {
            this.frameId = requestAnimationFrame(this.animate.bind(this));
        }
    }

    stop() {
        if (this.finished) {
            return this;
        }
        this.finished = true;
        if (this.frameId) {
            cancelAnimationFrame(this.frameId);
        }
        this.emitAnimationFrame(true);
        this.emit(events.animation.finish);
        this.destroy();
        return this;
    }

    onStart(callback) {
        this.addEventListener(
            events.animation.start,
            callback
        );
        return this;
    }

    onAnimation(callback) {
        this.addEventListener(
            events.animation.tick,
            callback
        );
        return this;
    }

    onFinish(callback) {
        this.addEventListener(
            events.animation.finish,
            callback
        );
        return this;
    }
}
