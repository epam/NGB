import {BP_OFFSET} from '../bam/internal/renderer/features';
import * as modes from './reference.modes';
import {aminoAcidsConst, CachedTrackRenderer, drawingConfiguration} from '../../core';

import PIXI from 'pixi.js';

const Math = window.Math;

export default class ReferenceRenderer extends CachedTrackRenderer {

    _noGCContentLabel;

    constructor(config, renderer) {
        super();
        this._config = config;
        this._height = config.height;
        this.initializeLettersTextures(renderer);
    }

    initializeLettersTextures(renderer) {
        this._lettersCache = {};
        this._aminoAcidEvenCache = {};
        this._aminoAcidOddCache = {};
        for (const letter of ['A', 'G', 'C', 'T', 'N', 'a', 'g', 'c', 't', 'n']) {
            const text = new PIXI.Text(letter, this._config.largeScale.labelStyle);
            text.resolution = drawingConfiguration.resolution;
            const texture = text.generateTexture(renderer, drawingConfiguration.resolution, drawingConfiguration.scale);
            if (texture.baseTexture) {
                this._lettersCache[letter] = {texture, width: text.width, height: text.height};
            }
        }
        const aminoacidsDictionary = [
            ...(Object.values(aminoAcidsConst).map(o => ({key: o.toLowerCase(), value: o}))),
            {
                key: 'stop',
                value: '*'
            },
            {
                key: 'uncovered',
                value: 'n'
            }
        ];
        for (let a = 0; a < aminoacidsDictionary.length; a++) {
            const {key, value} = aminoacidsDictionary[a];
            const textOdd = new PIXI.Text(value, this._getLabelStyle(key, true).labelStyle);
            textOdd.resolution = drawingConfiguration.resolution;
            const textureOdd = textOdd.generateTexture(renderer, drawingConfiguration.resolution, drawingConfiguration.scale);
            if (textureOdd.baseTexture) {
                this._aminoAcidOddCache[key] = {
                    height: textOdd.height,
                    texture: textureOdd,
                    width: textOdd.width
                };
            }
            const textEven = new PIXI.Text(value, this._getLabelStyle(key, false).labelStyle);
            textEven.resolution = drawingConfiguration.resolution;
            const textureEven = textOdd.generateTexture(renderer, drawingConfiguration.resolution, drawingConfiguration.scale);
            if (textureEven.baseTexture) {
                this._aminoAcidEvenCache[key] = {
                    height: textEven.height,
                    texture: textureEven,
                    width: textEven.width
                };
            }
        }
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

    correctCanvasXPosition (position, viewport) {
        return Math.max(
            Math.min(position, 2.0 * viewport.canvasSize),
            -viewport.canvasSize
        );
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
            let endX = Math.round(this.correctedXPosition(item.xEnd) + padding);
            if (pixelsPerBp >= this._config.largeScale.separateBarsAfterBp && prevX !== null && prevX === startX) {
                startX++;
            }
            startX = this.correctCanvasXPosition(startX, viewport);
            endX = this.correctCanvasXPosition(endX, viewport);
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
                let label;
                let labelHeight, labelWidth;
                if (this._lettersCache[item.value]) {
                    const {texture, width, height} = this._lettersCache[item.value];
                    label = new PIXI.Sprite(texture);
                    label.resolution = drawingConfiguration.resolution;
                    labelHeight = height;
                    labelWidth = width;
                } else {
                    label = new PIXI.Text(item.value, this._config.largeScale.labelStyle);
                    label.resolution = drawingConfiguration.resolution;
                    labelHeight = label.height;
                    labelWidth = label.width;
                }
                if (label) {
                    label.x = this.correctCanvasXPosition(
                        Math.round(this.correctedXPosition(item.xStart) - labelWidth / 2.0),
                        viewport
                    );
                    label.y = Math.round(isReverse ? startY - heightBlock / 2.0 - labelHeight / 2.0 : startY + heightBlock / 2.0 - labelHeight / 2.0 - 1);

                    this.dataContainer.addChild(label);
                }
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
                x: this.correctCanvasXPosition(this.correctedXPosition(item.xStart), viewport),
                y: 0
            };
            const size = {
                height: this.height,
                width: Math.min(
                    Math.max(this.correctedXMeasureValue(item.xEnd - item.xStart), 1),
                    3.0 * viewport.canvasSize
                )
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
        return this._getLabelStyle(acid.value, acid.startIndex % 2 === 1);
    }

    _getLabelStyle(letter, odd) {
        let fill = this._config.aminoacid.even.fill;
        let labelStyle = Object.assign({}, this._config.aminoacid.label.defaultStyle, this._config.aminoacid.even.label);

        if (letter.toLowerCase() === 'stop') {
            fill = odd ? this._config.aminoacid.stop.oddFill : this._config.aminoacid.stop.fill;
            labelStyle = Object.assign({}, this._config.aminoacid.label.defaultStyle, this._config.aminoacid.stop.label);
        } else if (letter.toLowerCase() === 'm') {
            fill = odd ? this._config.aminoacid.start.oddFill : this._config.aminoacid.start.fill;
            labelStyle = Object.assign({}, this._config.aminoacid.label.defaultStyle, this._config.aminoacid.start.label);
        } else if (letter.toLowerCase() === 'uncovered') {
            fill = odd ? this._config.aminoacid.uncovered.oddFill : this._config.aminoacid.uncovered.fill;
            labelStyle = Object.assign({}, this._config.aminoacid.label.defaultStyle, this._config.aminoacid.uncovered.label);
        } else if (odd) {
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
            const padding = pixelsPerBp / 2.0;
            const lowScaleMarginOffset = 0.5;

            for (let i = 0; i < aminoAcids.length; i++) {
                const item = aminoAcids[i];
                const {fill} = this._getLabelStyleConfig(item);

                if (viewport.isShortenedIntronsMode && !viewport.shortenedIntronsViewport.checkFeature(item))
                    continue;
                const startX = this.correctCanvasXPosition(Math.round(this.correctedXPosition(item.xStart) - padding), viewport);
                const endX = this.correctCanvasXPosition(Math.round(this.correctedXPosition(item.xEnd) + padding), viewport);

                block.beginFill(fill, 1).lineStyle(0, fill, 0);
                block.moveTo(startX, isReverse ? startY + lowScaleMarginOffset : startY - lowScaleMarginOffset);
                block.lineTo(startX, isReverse ? startY + heightBlock : startY - heightBlock);
                block.lineTo(endX, isReverse ? startY + heightBlock : startY - heightBlock);
                block.lineTo(endX, isReverse ? startY + lowScaleMarginOffset : startY - lowScaleMarginOffset);
                block.lineTo(startX, isReverse ? startY + lowScaleMarginOffset : startY - lowScaleMarginOffset);
                block.endFill();
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
                    let labelWidth;
                    let labelHeight;
                    const dictionary = item.startIndex % 2 === 1 ? this._aminoAcidOddCache : this._aminoAcidEvenCache;
                    if (dictionary[item.value.toLowerCase()]) {
                        const {texture, width, height} = dictionary[item.value.toLowerCase()];
                        label = new PIXI.Sprite(texture);
                        label.resolution = drawingConfiguration.resolution;
                        labelWidth = width;
                        labelHeight = height;
                    }
                    switch (item.value.toLowerCase()) {
                        case 'stop':
                            if (!label) {
                                label = new PIXI.Text('*', labelStyle);
                                labelWidth = label.width;
                                labelHeight = label.height;
                            }
                            labelPadding = labelHeight / 4.0;
                            break;
                        case 'uncovered':
                            if (!label) {
                                label = new PIXI.Text('n', labelStyle);
                                labelWidth = label.width;
                                labelHeight = label.height;
                            }
                            labelPadding = labelHeight / 2.0;
                            break;
                        default:
                            if (!label) {
                                label = new PIXI.Text(item.value, labelStyle);
                                labelWidth = label.width;
                                labelHeight = label.height;
                            }
                            labelPadding = labelHeight / 2.0;
                            break;
                    }
                    label.resolution = drawingConfiguration.resolution;
                    label.x = this.correctCanvasXPosition(
                        Math.round(this.correctedXPosition(item.xStart) + (item.xEnd - item.xStart) / 2.0 - labelWidth / 2.0),
                        viewport
                    );
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