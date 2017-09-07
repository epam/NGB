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

    render(viewport, cache, forseRedraw = false, _gffShowNumbersAminoacid, _showCenterLine, state) {
        this.showTranslation = state.referenceShowTranslation;
        this.showForwardStrand = state.referenceShowForwardStrand;
        this.showReverseStrand = state.referenceShowReverseStrand;

        this.isRenderingStartsAtMiddle = !(this.showForwardStrand && !this.showReverseStrand || !this.showForwardStrand && this.showReverseStrand);
        super.render(viewport, cache, forseRedraw, _gffShowNumbersAminoacid, _showCenterLine);
    }

    _changeNucleotidesReferenceGraph(viewport, items, isReverse) {
        const height = this.height;
        const heightBlock = this._config.nucleotidesHeight;
        let startY;

        if (this.isRenderingStartsAtMiddle) {
            startY = isReverse ?
                height / 2.0 + heightBlock :
                height / 2.0 - heightBlock;
        } else {
            startY = isReverse ?
                heightBlock :
                height - heightBlock;
        }

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
            block.moveTo(startX, isReverse ? startY + lowScaleMarginOffset : startY - lowScaleMarginOffset);
            if (this.isRenderingStartsAtMiddle) {
                block.lineTo(startX, height / 2.0);
                block.lineTo(endX, height / 2.0);
            } else {
                block.lineTo(startX, isReverse ? 0 : height);
                block.lineTo(endX, isReverse ? 0 : height);
            }
            block.lineTo(endX, isReverse ? startY + lowScaleMarginOffset : startY - lowScaleMarginOffset);
            block.lineTo(startX, isReverse ? startY + lowScaleMarginOffset : startY - lowScaleMarginOffset);
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
                if (this.isRenderingStartsAtMiddle) {
                    label.y = Math.round(isReverse ? height / 2 + label.width / 2.0 : startY + label.width / 2.0 - 1);
                } else {
                    label.y = Math.round(isReverse ? label.width / 2.0 : height - heightBlock + label.width / 2.0 - 1);

                }
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

    _getLabelStyleConfig(acid) {
        let fill = this._config.aminoacid.even.fill;
        let labelStyle = Object.assign({}, this._config.aminoacid.label.defaultStyle, this._config.aminoacid.even.label);

        if (acid.value.toLowerCase() === 'stop') {
            fill = acid.startIndex % 2 === 1 ? this._config.aminoacid.stop.oddFill : this._config.aminoacid.stop.fill;
            labelStyle = Object.assign({}, this._config.aminoacid.label.defaultStyle, this._config.aminoacid.stop.label);
        } else if (acid.value.toLowerCase() === 'm') {
            fill = acid.startIndex % 2 === 1 ? this._config.aminoacid.start.oddFill : this._config.aminoacid.start.fill;
            labelStyle = Object.assign({}, this._config.aminoacid.label.defaultStyle, this._config.aminoacid.start.label);
        }  else if (acid.value.toLowerCase() === 'uncovered') {
            fill = acid.startIndex % 2 === 1 ? this._config.aminoacid.uncovered.oddFill : this._config.aminoacid.uncovered.fill;
            labelStyle = Object.assign({}, this._config.aminoacid.label.defaultStyle, this._config.aminoacid.uncovered.label);
        }
        else if (acid.startIndex % 2 === 1) {
            fill = this._config.aminoacid.odd.fill;
            labelStyle = Object.assign({}, this._config.aminoacid.label.defaultStyle, this._config.aminoacid.odd.label);
        }
        return {
            fill,
            labelStyle
        };
    }

    _changeAminoAcidsGraph(viewport, aminoAcidsData, isReverse) {
        let index = 0;
        aminoAcidsData.forEach(aminoAcids => {
            const heightBlock = this._config.aminoAcidsHeight;
            let startY;
            if (this.isRenderingStartsAtMiddle) {
                startY = isReverse ?
                    this.height / 2.0 + this._config.nucleotidesHeight + index * heightBlock :
                    this.height / 2.0 - this._config.nucleotidesHeight - index * heightBlock;
            } else {
                startY = isReverse ?
                    this._config.nucleotidesHeight + index * heightBlock :
                    this.height - this._config.nucleotidesHeight - index * heightBlock;
            }

            const block = new PIXI.Graphics();
            const pixelsPerBp = viewport.factor;
            let padding = pixelsPerBp / 2.0;
            const lowScaleMarginOffset = 0.5;

            let prevX = null;
            for (let i = 0; i < aminoAcids.length; i++) {
                const item = aminoAcids[i];
                const {fill} = this._getLabelStyleConfig(item);

                if (viewport.isShortenedIntronsMode && !viewport.shortenedIntronsViewport.checkFeature(item))
                    continue;
                let startX = Math.round(this.correctedXPosition(item.xStart) - padding);
                const endX = Math.round(this.correctedXPosition(item.xEnd) + padding);

                block.beginFill(fill, 1).lineStyle(0, fill, 0);
                block.moveTo(startX, isReverse ? startY + lowScaleMarginOffset : startY - lowScaleMarginOffset);
                block.lineTo(startX, isReverse ? startY + heightBlock : startY - heightBlock);
                block.lineTo(endX, isReverse ? startY + heightBlock : startY - heightBlock);
                block.lineTo(endX, isReverse ? startY + lowScaleMarginOffset : startY - lowScaleMarginOffset);
                block.lineTo(startX, isReverse ? startY + lowScaleMarginOffset : startY - lowScaleMarginOffset);
                block.endFill();
                prevX = endX;
            }
            this.dataContainer.addChild(block);

            for (let i = 0; i < aminoAcids.length; i++) {
                const item = aminoAcids[i];
                const {labelStyle} = this._getLabelStyleConfig(item);
                if (viewport.isShortenedIntronsMode && !viewport.shortenedIntronsViewport.checkFeature(item))
                    continue;
                if (pixelsPerBp >= this._config.aminoacid.labelDisplayAfterPixelsPerBp) {
                    let label;
                    let labelPadding;
                    switch (item.value.toLowerCase()) {
                        case 'stop':
                            label = new PIXI.Text('*', labelStyle);
                            labelPadding = label.height / 4.0;
                            break;
                        case 'uncovered':
                            label = new PIXI.Text('n', labelStyle);
                            labelPadding = label.height / 2.0;
                            break;
                        default:
                            label = new PIXI.Text(item.value, labelStyle);
                            labelPadding = label.height / 2.0;
                            break;
                    }
                    label.resolution = drawingConfiguration.resolution;

                    label.x = Math.round(this.correctedXPosition(item.xStart) + (item.xEnd - item.xStart) / 2.0 - label.width / 2.0);
                    label.y = Math.round(isReverse ? startY + heightBlock / 2.0 - labelPadding : startY - heightBlock / 2.0 - labelPadding - 1);
                    this.dataContainer.addChild(label);
                }
            }
            index++;
        });
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
                if (this.showForwardStrand) {
                    this._changeNucleotidesReferenceGraph(viewport, reference.items, false);
                }
                if (this.showReverseStrand) {
                    this._changeNucleotidesReferenceGraph(viewport, reference.reverseItems, true);
                }
                if (this.showForwardStrand && this.showTranslation) {
                    this._changeAminoAcidsGraph(viewport, reference.aminoAcidsData, false);
                }
                if (this.showReverseStrand && this.showTranslation) {
                    this._changeAminoAcidsGraph(viewport, reference.reverseAminoAcidsData, true);
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