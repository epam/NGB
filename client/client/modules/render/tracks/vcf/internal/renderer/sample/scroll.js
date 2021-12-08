import * as PIXI from 'pixi.js-legacy';

export class VCFSamplesScroll extends PIXI.Container {
    _totalHeight = 0;
    _displayedHeight = 0;
    _scrollPosition = 0;
    _hovered = false;

    get totalHeight () {
        return this._totalHeight;
    }

    set totalHeight (value) {
        this._totalHeight = value;
    }

    get displayedHeight () {
        return this._displayedHeight;
    }

    set displayedHeight (value) {
        this._displayedHeight = value;
    }

    get scrollerVisible () {
        return this.totalHeight > this.displayedHeight;
    }

    get scrollPosition () {
        return this._scrollPosition;
    }

    set scrollPosition (value) {
        this._scrollPosition = value;
    }

    get scrollerHeight () {
        if (this.scrollerVisible) {
            const ratio = this.displayedHeight / this.totalHeight;
            return Math.max(
                ratio * this.displayedHeight,
                this._config.scroll.minHeight
            );
        }
        return 0;
    }

    get scrollerHovered () {
        return this._hovered;
    }

    set scrollerHovered (hovered) {
        this._hovered = hovered;
        this.renderScroller();
    }

    get scrollIndicatorBoundaries () {
        if (this.scrollerVisible) {
            const config = this._config.scroll;
            const r = Math.max(
                0,
                this.scrollPosition / (this.totalHeight - this.displayedHeight)
            );
            const y = r * (this.displayedHeight - this.scrollerHeight);
            return {
                x: 0,
                y,
                width: config.width,
                height: this.scrollerHeight
            };
        }
        return undefined;
    }

    getWorldPosition (localPosition) {
        if (this.scrollerVisible) {
            const ratio = (this.displayedHeight) / this.totalHeight;
            return localPosition / ratio;
        }
        return localPosition;
    }

    constructor(config) {
        super();
        this._config = config;
        this.graphics = new PIXI.Graphics();
        this.addChild(this.graphics);
    }

    reset () {
        this.scrollPosition = 0;
    }

    renderScroller () {
        this.graphics.clear();
        if (this.scrollerVisible) {
            const {
                x,
                y,
                width,
                height
            } = this.scrollIndicatorBoundaries;
            const config = this._config.scroll;
            this.graphics.beginFill(
                config.fill,
                this._hovered ? config.hoveredAlpha : config.alpha
            );
            this.graphics.drawRect(
                x,
                y,
                width,
                height
            );
            this.graphics.endFill();
        }
        return true;
    }
}
