import {CachedTrackRenderer, drawingConfiguration} from '../../core';
import PIXI from 'pixi.js';

const Math = window.Math;

export default class SegRenderer extends CachedTrackRenderer {

    _data;
    _config;
    _currentY = 0;
    _actualHeight = 0;
    _height = 0;
    _blocks = [];

    get config() {
        return this._config;
    }

    get data() {
        return this._data;
    }

    set data(value) {
        this._data = value;
    }

    get actualHeight() {
        return this._actualHeight;
    }

    set actualHeight(value) {
        this._actualHeight = value;
    }

    get height() {
        return this._height;
    }

    set height(value) {
        this._height = value;
    }

    get needScroll() {
        return this.actualHeight > this.height;
    }

    get y() {
        return this._currentY;
    }

    set y(value) {
        this._currentY = value;
    }

    get sampleHeight() {
        return this.config.rowHeight + this.config.headerHeight;
    }

    get blocks() {
        return this._blocks;
    }

    set blocks(value) {
        this._blocks = value;
    }

    constructor(config) {
        super();
        this._config = config;
    }

    render(viewport, cache, heightChanged) {
        if (heightChanged) {
            this.scroll(viewport, 0);
        }
        else {
            super.render(viewport, cache, false);
        }
    }

    rebuildContainer(viewport, cache) {
        super.rebuildContainer(viewport, cache);
        this.data = cache.data;
        this.actualHeight = (this.config.rowHeight + this.config.headerHeight) * this.data.tracks.length;
        this._redrawAll(viewport);
    }

    _verticalScrollIsHovered = false;

    hoverVerticalScroll(viewport) {
        if (!this._verticalScrollIsHovered) {
            this._verticalScrollIsHovered = true;
            this._redrawAll(viewport);
            return true;
        }
        return false;
    }

    unhoverVerticalScroll(viewport) {
        if (this._verticalScrollIsHovered) {
            this._verticalScrollIsHovered = false;
            this._redrawAll(viewport);
            return true;
        }
        return false;
    }

    canScroll(yDelta) {
        if (this.actualHeight && this.height < this.actualHeight) {
            const __y = this.y + (yDelta !== null ? yDelta * this.sampleHeight : 0);
            return __y <= 0 && __y >= this.height - this.actualHeight;
        }
        return false;
    }

    isScrollable() {
        return this.actualHeight && this.height < this.actualHeight;
    }

    scrollIndicatorBoundaries(viewport) {
        if (this.actualHeight && this.height < this.actualHeight) {
            const scrollHeight = Math.max(this.height * this.height / this.actualHeight,
                this.config.scroll.minHeight);
            return {
                height: scrollHeight,
                width: this.config.scroll.width,
                x: viewport.canvasSize - this.config.scroll.width - this.config.scroll.margin,
                y: -this.y / this.actualHeight * this.height
            };
        }
        return null;
    }

    setScrollPosition(viewport, indicatorPosition) {
        this.scroll(viewport, - indicatorPosition * this.actualHeight / this.height - this.y);
    }

    scroll(viewport, yDelta) {
        if (this.actualHeight && this.height < this.actualHeight) {
            let __y = this.y + (yDelta !== null ? yDelta : 0);
            __y = Math.min(0, Math.max(this.height - this.actualHeight, __y));
            this.y = __y;
        }
        else {
            this.y = 0;
        }
        this._redrawAll(viewport);
    }

    drag(viewport, yDelta) {
        if ((this.y + yDelta) >= 0) {
            this.y = 0;
            return;
        }
        if ((this.y + yDelta) <= (-this.actualHeight + this.config.maxHeight)) {
            this.y = -this.actualHeight + this.config.maxHeight;
            return;
        }
        this.y += yDelta;
        this._redrawAll(viewport);
    }

    _redrawAll(viewport) {
        this._clearAll();
        this._getViewData().forEach((segItem, index) => {
            this._drawTrackLabel(index, segItem.name);
            this._drawTrackRow(viewport, index, segItem);
        });
        this._drawVerticalScroll(viewport);
    }


    _clearAll() {
        const backgroundItemsCount = this._backgroundContainer.children.length;
        const dataItemsCount = this.dataContainer.children.length;
        this.blocks = [];
        (dataItemsCount > 0) && ( this.dataContainer.removeChildren(0, dataItemsCount));
        (backgroundItemsCount) && (this._backgroundContainer.removeChildren(0, backgroundItemsCount));
    }

    _drawTrackRowItem(item, index) {
        const block = new PIXI.Graphics();
        const color = this._gradientColor(item.value);
        const position = {
            x: this.correctedXPosition(item.xStart),
            y: index * (this._config.rowHeight) + (index + 1) * this._config.headerHeight
        };
        const size = {
            height: this._config.rowHeight,
            width: Math.max(this.correctedXMeasureValue(item.xEnd - item.xStart), 1)
        };
        block.beginFill(color.color, color.alpha);
        block.moveTo(position.x, position.y);
        block.lineTo(position.x, position.y + size.height);
        block.lineTo(position.x + size.width, position.y + size.height);
        block.lineTo(position.x + size.width, position.y);
        block.lineTo(position.x, position.y);
        block.endFill();
        if (!this.blocks[index]) this.blocks[index] = [];
        this.blocks[index].push(Object.assign(item, {
            width: size.width,
            x: position.x
        }));
        this.dataContainer.addChild(block);
    }

    _drawTrackRow(viewport, index, row) {
        const itemsFilterFn = function(item) {
            return !(viewport.brush.start > item.endIndex || viewport.brush.end < item.startIndex);
        };
        const self = this;
        const drawItemFn = function(item) {
            self._drawTrackRowItem(item, index);
        };
        row.items.filter(itemsFilterFn).forEach(drawItemFn);
    }

    _drawTrackLabel(index, name) {
        const label = new PIXI.Text(name, this._config.label);
        label.resolution = drawingConfiguration.resolution;
        label.x = 0;
        label.y = index * (this.sampleHeight);
        this._backgroundContainer.addChild(label);
    }

    _drawVerticalScroll(viewport) {
        if (this.needScroll) {
            const scrollHeight = Math.max(this.height * this.height / this.actualHeight,
                this.config.scroll.minHeight),
                verticalScroll = new PIXI.Graphics();
            verticalScroll
                .beginFill(this.config.scroll.fill, this._verticalScrollIsHovered ? this.config.scroll.hoveredAlpha : this.config.scroll.alpha)
                .drawRect(
                    viewport.canvasSize - this.config.scroll.width - this.config.scroll.margin,
                    -this.y / this.actualHeight * this.height,
                    this.config.scroll.width,
                    scrollHeight
                )
                .endFill();
            this._backgroundContainer.addChild(verticalScroll);
        }
    }


    _gradientColor(value) {
        const color = value > 0 ? this.config.colors.red : this.config.colors.blue;
        const alpha = Math.min(Math.abs(value), this.config.edgeValue) / this.config.edgeValue;
        return {alpha, color};
    }

    _getViewData() {
        if (!this.data) return [];
        const begin = Math.abs(this.y / this.sampleHeight);
        const end = Math.ceil(this.height / this.sampleHeight);
        return this.data.tracks.slice(begin, begin + end);

    }

    checkPosition(position) {
        const rowData = this.blocks[parseInt(position.y / this.sampleHeight)];
        if (rowData) {
            for (const data of rowData) {
                if (data.x <= position.x && position.x <= (data.x + data.width)) {
                    const tooltipData = JSON.parse(JSON.stringify(data));
                    delete tooltipData.x;
                    delete tooltipData.width;
                    delete tooltipData.xStart;
                    delete tooltipData.xEnd;
                    return tooltipData;
                }
            }
        }
    }
}