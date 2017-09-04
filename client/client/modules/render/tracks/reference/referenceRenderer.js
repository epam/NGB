import * as modes from './reference.modes';
import {CachedTrackRenderer, drawingConfiguration} from '../../core';
import PIXI from 'pixi.js';

const Math = window.Math;

export default class ReferenceRenderer extends CachedTrackRenderer {

    _noGCContentLabel;

    constructor(config) {
        super();
        this._config = config;
        this._height = config.height;
    }

    get height() {
        return this._height;
    }

    set height(value) {
        this._height = value;
    }

    rebuildContainer(viewport, cache) {
        super.rebuildContainer(viewport, cache);
        this._changeReferenceGraph(viewport, cache.data);
    }

    translateContainer(viewport, cache) {
        super.translateContainer(viewport, cache);
        this._updateNoGCContentLable(viewport, cache.data);
    }

    render(viewport, cache, forseRedraw = false, _gffShowNumbersAminoacid, _showCenterLine, state){
        this.showTranslation = state.referenceTranslation;
        this.showForwardStrand = state.referenceShowForwardStrand;
        this.showReverseStrand = state.referenceShowReverseStrand;

        super.render(viewport, cache, forseRedraw, _gffShowNumbersAminoacid, _showCenterLine);
    }

    _changeNucleotidesReferenceGraph(viewport, items, isReverse) {
        const height = this._config.nucleotidesHeight;
        const block = new PIXI.Graphics();
        const pixelsPerBp = viewport.factor;
        let padding = pixelsPerBp / 2.0;
        const lowScaleMarginThreshold = 4;
        const lowScaleMarginOffset = -0.5;
        if (pixelsPerBp > lowScaleMarginThreshold)
            padding += lowScaleMarginOffset;
        let prevX = null;
        for (let i = 0; i < items.length; i++) {
            const item = items[i];
            if (viewport.isShortenedIntronsMode && !viewport.shortenedIntronsViewport.checkFeature(item))
                continue;
            let startX = Math.round(this.correctedXPosition(item.xStart) - padding);
            const endX = Math.round(this.correctedXPosition(item.xEnd) + padding);
            if (pixelsPerBp >= this._config.largeScale.separateBarsAfterBp && prevX !== null && prevX === startX) {
                startX++;
            }
            block.beginFill(this._config.largeScale[item.value.toUpperCase()], 1);
            block.moveTo(startX, isReverse ? height - lowScaleMarginOffset : 0);
            block.lineTo(startX, isReverse ? 2 * height : height);
            block.lineTo(endX, isReverse ? 2 * height : height);
            block.lineTo(endX, isReverse ? height - lowScaleMarginOffset : 0);
            block.lineTo(startX, isReverse ? height - lowScaleMarginOffset : 0);
            block.endFill();
            prevX = endX;
        }
        this.dataContainer.addChild(block);

        for (let i = 0; i < items.length; i++) {
            const item = items[i];
            if (viewport.isShortenedIntronsMode && !viewport.shortenedIntronsViewport.checkFeature(item))
                continue;
            if (pixelsPerBp >= this._config.largeScale.labelDisplayAfterPixelsPerBp) {
                const label = new PIXI.Text(item.value, this._config.largeScale.labelStyle);
                label.resolution = drawingConfiguration.resolution;
                label.x = Math.round(this.correctedXPosition(item.xStart) - label.width / 2.0);
                label.y = Math.round(height / 2.0 - label.height / 2.0 + (isReverse ? height : 0));
                this.dataContainer.addChild(label);
            }

        }
    }

    _changeGCContentReferenceGraph(viewport, reference) {
        const block = new PIXI.Graphics();
        for (let i = 0; i < reference.items.length; i++) {
            const item = reference.items[i];
            if (viewport.isShortenedIntronsMode && !viewport.shortenedIntronsViewport.checkFeature(item))
                continue;
            const color = this._gradientColor(item.value);
            const position = {
                x: this.correctedXPosition(item.xStart),
                y: 0
            };
            const size = {
                height: this.height,
                width: Math.max(this.correctedXMeasureValue(item.xEnd - item.xStart), 1)
            };

            block.beginFill(color.color, color.alpha);
            block.moveTo(position.x, position.y);
            block.lineTo(position.x, position.y + size.height);
            block.lineTo(position.x + size.width, position.y + size.height);
            block.lineTo(position.x + size.width, position.y);
            block.lineTo(position.x, position.y);
            block.endFill();
        }
        this.dataContainer.addChild(block);
    }

    _changeReferenceGraph(viewport, reference) {
        if (reference === null || reference === undefined)
            return;
        this.dataContainer.removeChildren();
        this._updateNoGCContentLable(viewport, reference);
        switch (reference.mode) {
            case modes.gcContent:
                this._changeGCContentReferenceGraph(viewport, reference);
                break;
            case modes.nucleotides: {
                debugger;
                if(this.showForwardStrand) {
                    this._changeNucleotidesReferenceGraph(viewport, reference.items, false);
                }
                if(this.showReverseStrand) {
                    this._changeNucleotidesReferenceGraph(viewport, reference.reverseItems, true);
                }
                break;
            }
        }
    }

    _updateNoGCContentLable(viewport, reference) {
        if (!this._noGCContentLabel) {
            this._noGCContentLabel = new PIXI.Text(this._config.noGCContent.text, this._config.noGCContent.labelStyle);
            this._noGCContentLabel.resolution = drawingConfiguration.resolution;
            this.container.addChild(this._noGCContentLabel);
        }
        this._noGCContentLabel.x = Math.round(viewport.canvasSize / 2 - this._noGCContentLabel.width / 2.0);
        this._noGCContentLabel.y = Math.round(this.height / 2.0 - this._noGCContentLabel.height / 2.0);
        this._noGCContentLabel.visible = reference.mode === modes.gcContentNotProvided;
    }

    _gradientColor(value) {
        let baseColor = this._config.lowScale.color1;
        let alphaChannel = 1.0 - value / this._config.lowScale.sensitiveValue;
        if (value > this._config.lowScale.sensitiveValue) {
            baseColor = this._config.lowScale.color2;
            alphaChannel = 1.0 - (1 - value) / (1.0 - this._config.lowScale.sensitiveValue);
        }
        return {
            alpha: alphaChannel,
            color: baseColor
        };
    }
}