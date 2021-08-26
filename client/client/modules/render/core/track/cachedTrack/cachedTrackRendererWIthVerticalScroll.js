import PIXI from 'pixi.js';
import CachedTrackRenderer from './cachedTrackRenderer';

export default class CachedTrackRendererWithVerticalScroll extends CachedTrackRenderer {
    constructor(...props) {
        super(...props);
        this._verticalScroll = new PIXI.Graphics();
        this._verticalScrollIsHovered = false;
        this.container.addChild(this._verticalScroll);
    }

    get config() {
        return this._config;
    }

    get verticalScroll(): PIXI.Graphics {
        return this._verticalScroll;
    }

    get height() {
        return this._height;
    }

    set height(value) {
        this._height = value;
    }

    get actualHeight() {
        return this._actualHeight;
    }

    get scrollConfig () {
        if (this.config && this.config.scroll) {
            return this.config.scroll;
        }
        return {
            alpha: 0.5,
            fill: 0x92AEE7,
            hoveredAlpha: 1,
            margin: 2,
            width: 13
        };
    }

    scrollIndicatorBoundaries(viewport) {
        if (this.actualHeight && this.height < this.actualHeight) {
            return {
                height: this.height * this.height / this.actualHeight,
                width: this.scrollConfig.width,
                x: viewport.canvasSize - this.scrollConfig.width - this.scrollConfig.margin,
                y: -this.dataContainer.y / this.actualHeight * this.height
            };
        }
        return null;
    }

    drawVerticalScroll(viewport) {
        this.verticalScroll.clear();
        if (this.actualHeight && this.height < this.actualHeight) {
            const scrollHeight = this.height * this.height / this.actualHeight;
            this.verticalScroll
                .beginFill(this.scrollConfig.fill, this._verticalScrollIsHovered ? this.scrollConfig.hoveredAlpha : this.scrollConfig.alpha)
                .drawRect(
                    viewport.canvasSize - this.scrollConfig.width - this.scrollConfig.margin,
                    -this.dataContainer.y / this.actualHeight * this.height,
                    this.scrollConfig.width,
                    scrollHeight
                )
                .endFill();
        }
    }

    hoverVerticalScroll(viewport) {
        if (!this._verticalScrollIsHovered) {
            this._verticalScrollIsHovered = true;
            this.drawVerticalScroll(viewport);
            return true;
        }
        return false;
    }

    unhoverVerticalScroll(viewport) {
        if (this._verticalScrollIsHovered) {
            this._verticalScrollIsHovered = false;
            this.drawVerticalScroll(viewport);
            return true;
        }
        return false;
    }

    isScrollable() {
        return this.actualHeight && this.height < this.actualHeight;
    }

    canScroll(yDelta) {
        if (this.actualHeight && this.height < this.actualHeight) {
            let __y = this.dataContainer.y;
            if (yDelta !== null) {
                __y += yDelta;
            }
            return __y <= 0 && __y >= this.height - this.actualHeight;
        }
        return false;
    }

    setScrollPosition(viewport, indicatorPosition) {
        this.scroll(viewport, - indicatorPosition * this.actualHeight / this.height - this.dataContainer.y);
    }

    scroll(viewport, yDelta) {
        if (this.actualHeight && this.height < this.actualHeight) {
            let __y = this.dataContainer.y;
            if (yDelta !== null) {
                __y += yDelta;
            }
            __y = Math.min(0, Math.max(this.height - this.actualHeight, __y));
            this.dataContainer.y = __y;
            this.drawVerticalScroll(viewport);
        } else {
            this.dataContainer.y = 0;
            this.drawVerticalScroll(null);
        }
    }
}
