const Math = window.Math;

export class VariantBaseContainer {

    _variant = null;
    _container = null;
    _graphics = null;
    _linesGraphics = null;
    _componentIsBuilt = false;
    _isFaded = false;
    _fadeFactor = 1;
    _bpLength = 0;
    _isHovered = false;
    _hoverFactor = 0;
    _config = null;

    get variant() {
        return this._variant;
    }
    get container(): PIXI.Container {
        return this._container;
    }
    get linesGraphics(): PIXI.Graphics {
        return this._linesGraphics;
    }

    constructor(variant, config) {
        this._container = new PIXI.Container();
        this._config = config;
        this._variant = variant;

        this._graphics = new PIXI.Graphics();
        this._linesGraphics = new PIXI.Graphics();
        this._container.addChild(this._graphics);
    }

    buildComponent() {
        this._componentIsBuilt = true;
    }

    render(viewport) {
        this._bpLength = Math.max(viewport.factor, this._config.variant.thickness);
    }

    isHovers(cursor) {
        if (!cursor) {
            return false;
        }
        const {x} = cursor;
        return Math.abs(x - this.container.x) < this._bpLength / 2;
    }

    unhover() {
        this._isHovered = false;
    }

    hover() {
        this._isHovered = true;
    }

    unfade() {
        this._isFaded = false;
    }

    fade() {
        this._isFaded = true;
    }

    animate(time) {
        const needAnimateFade = (this._isFaded && this._fadeFactor > this._config.animation.fade.minimum)
            || (!this._isFaded && this._fadeFactor < 1);
        if (needAnimateFade) {
            const oneSecond = 1000;
            const timeDelta = (time) / oneSecond;
            const fadeDelta = (1 - this._config.animation.fade.minimum) / this._config.animation.fade.duration
                * timeDelta;
            this._fadeFactor =
                Math.max(this._config.animation.fade.minimum,
                    Math.min(1, this._fadeFactor + (this._isFaded ? -fadeDelta : fadeDelta)));
            this.container.alpha = this._fadeFactor;
            this.linesGraphics.alpha = this._fadeFactor;
        }
        return needAnimateFade;
    }
}