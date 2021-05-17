const ANIMATION_DURATION_MS = 100;
const EXPAND_FACTOR_COLLAPSED = 0;
const EXPAND_FACTOR_EXPANDED = 1;
const ANIMATION_VALUE_RATIO = Math.abs(EXPAND_FACTOR_EXPANDED - EXPAND_FACTOR_COLLAPSED) / ANIMATION_DURATION_MS;

export default class ExpandedState {
    expandFactor: number = EXPAND_FACTOR_COLLAPSED;
    expandable: boolean = false;
    trimmedLabelText: string;
    animationCallback;

    _expandFactorTo: number = EXPAND_FACTOR_COLLAPSED;
    _animating: boolean = false;
    _animationTimestamp: number;

    constructor(expandable, trimmedLabelText, animationCallback) {
        this.expandable = expandable;
        this.trimmedLabelText = trimmedLabelText;
        this.animationCallback = animationCallback;
    }

    get expanded () {
        return this.expandFactor >= EXPAND_FACTOR_EXPANDED;
    }

    set expanded (expanded) {
        this.expandFactor = expanded
            ? EXPAND_FACTOR_EXPANDED
            : EXPAND_FACTOR_COLLAPSED;
    }

    setExpandedAnimated (expanded) {
        if (this.expanded !== expanded) {
            this._expandFactorTo = expanded
                ? EXPAND_FACTOR_EXPANDED
                : EXPAND_FACTOR_COLLAPSED;
            this.startAnimation();
        }
    }

    startAnimation () {
        this.buildAnimationConfig();
        if (!this._animating) {
            this._animating = true;
            requestAnimationFrame(this.doAnimation.bind(this));
        }
    }

    buildAnimationConfig () {
        this._animationTimestamp = undefined;
    }

    doAnimation (timestamp) {
        if (this._animationTimestamp && this._animating) {
            const valueDelta = this._expandFactorTo - this.expandFactor;
            const timestampDelta = timestamp - this._animationTimestamp;
            const valueIncrement = ANIMATION_VALUE_RATIO * timestampDelta * Math.sign(valueDelta);
            const rawValue = this.expandFactor + valueIncrement;
            this.expandFactor = Math.max(
                EXPAND_FACTOR_COLLAPSED,
                Math.min(
                    EXPAND_FACTOR_EXPANDED,
                    rawValue
                )
            );
            if (this.animationCallback) {
                this.animationCallback();
            }
            this._animating = rawValue > EXPAND_FACTOR_COLLAPSED && rawValue < EXPAND_FACTOR_EXPANDED;
        }
        this._animationTimestamp = this._animating ? timestamp : undefined;
        if (this._animating) {
            requestAnimationFrame(this.doAnimation.bind(this));
        }
    }

    destroyAnimation () {
        this._animating = false;
    }
}