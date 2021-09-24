import Animation from './animation';
import events from '../utilities/events';

export default class AnimationSet extends Animation {
    constructor(options = {}) {
        super(options);
        /**
         * Animations set
         * @type {Animation[]}
         */
        this.animations = [];
    }

    add(from, to, callback) {
        const animation = new Animation({
            from,
            to,
            duration: this.duration,
            animate: false
        });
        animation
            .onAnimation(callback ? (oAnimation, value) => callback(value) : undefined)
            .onFinish(this.childAnimationDone.bind(this));
        this.animations.push(animation);
        return this;
    }

    childAnimationDone(animation) {
        this.animations.splice(this.animations.indexOf(animation), 1);
    }

    start() {
        this.animations.forEach(animation => animation.start());
        super.start();
    }

    animate(time) {
        this.animations.forEach(animation => animation.animate(time));
        this.emit(events.animation.tick);
        if (this.animations.length > 0) {
            this.requestNextAnimationFrame();
        } else {
            this.stop();
        }
    }

    stop() {
        const animations = this.animations.slice();
        animations.forEach(animation => animation.stop());
        super.stop();
    }
}
