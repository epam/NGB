import HeatmapEventDispatcher from '../utilities/heatmap-event-dispatcher';

const ANIMATION = Symbol('animation');

class Animated extends HeatmapEventDispatcher {
    get isAnimating () {
        return this[ANIMATION] && this[ANIMATION].started && !this[ANIMATION].finished;
    }
    destroy() {
        this.destroyAnimation();
        super.destroy();
    }
    /**
     * Stops and destroys current animation
     */
    destroyAnimation() {
        if (this[ANIMATION]) {
            this[ANIMATION].stop();
            this[ANIMATION] = undefined;
        }
    }
    /**
     * Starts animation and sets destroy handlers
     * @param {Animation} animation
     */
    startAnimation(animation) {
        const continuous = this.isAnimating && this[ANIMATION]
            ? this[ANIMATION].time
            : undefined;
        this.destroyAnimation();
        this[ANIMATION] = animation
            .onFinish(this.destroyAnimation.bind(this))
            .start(continuous);
    }
}

export default Animated;
