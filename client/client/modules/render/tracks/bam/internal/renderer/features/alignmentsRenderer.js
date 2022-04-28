import * as PIXI from 'pixi.js-legacy';
import {drawingConfiguration} from '../../../../../core';
import {ColorProcessor} from '../../../../../utilities';
import {partTypes} from '../../../modes';

const Math = window.Math;

export const CAN_SHOW_DETAILS_FACTOR = 0.5;
export const BP_OFFSET = 0.5;
const ALIGNMENT_ARROW_DIRECTION_LEFT = -1;
const ALIGNMENT_ARROW_DIRECTION_RIGHT = 1;

const baseLabelStyle = {
    fill: 0xFFFFFF,
    fontFamily: 'arial',
    fontSize: '6pt',
    fontWeight: 'normal'
};
const METHYLATED_BASE = 'methylatedBase';
const UNMETHYLATED_BASE = 'unmethylatedBase';
const BISULFITE_CONVERSION = 'bisulfiteConversion';

export class AlignmentsRenderer {
    constructor(
        viewport,
        config,
        colors,
        alignmentRowHeight,
        topMargin,
        y,
        features,
        labelsManager,
        height,
        bisulfiteModeContext
    ) {
        this._graphics = new PIXI.Graphics();
        this._container = new PIXI.Container();
        this._container.addChild(this._graphics);
        this._viewport = viewport;
        this._colors = colors;
        this._features = features;
        this._labelsManager = labelsManager;
        this._height = height;
        this._baseColor = colors.base;
        this._topMargin = topMargin;
        this._yGlobalScale = alignmentRowHeight;
        this._yElementOffset = this._yGlobalScale === 1 ? 0 : config.yElementOffset;
        this._yScale = this._yGlobalScale - this._yElementOffset;
        this._linesOffset = y * this._yGlobalScale - this._yElementOffset;
        const DEGREES_60_FACTOR = 3;
        this._figOffset = Math.floor(this._convertY(1) / DEGREES_60_FACTOR); // 60 degrees
        this._lineWidth = 1;
        this._renderOffset = this._lineWidth / 2;
        this._minimumPx = viewport.project.brushBP2pixel(Math.max(0, viewport.brush.start - viewport.actualBrushSize));
        this._maximumPx = viewport.project.brushBP2pixel(Math.min(viewport.chromosome.end, viewport.brush.end + viewport.actualBrushSize));
        this._contoured = false;
        this._hovered = false;
        const canShowLettersFactor = 10;
        this._canShowDetails = viewport.factor > CAN_SHOW_DETAILS_FACTOR;
        this._canShowLetters = viewport.factor > canShowLettersFactor;
        this.bisulfiteModeContext = bisulfiteModeContext;
    }

    startRender(hovered = false) {
        this._hovered = hovered;
        this._graphics.lineStyle(0, this._colors.base, 0);
        this._graphics.beginFill(this._colors.base, 1);
    }

    finishRender(renderer) {
        this._graphics.endFill();
        const bamTexture = renderer.generateTexture(this._container, {
            scaleMode: drawingConfiguration.scaleMode,
            resolution: drawingConfiguration.resolution
        });
        const bamSprite = new PIXI.Sprite(bamTexture);
        bamSprite.position.x = this._textureStartX;
        bamSprite.position.y = this._textureStartY;
        const mask = new PIXI.Graphics();
        mask.beginFill(0x000000, 1)
            .drawRect(0, this._topMargin, this._viewport.canvasSize, this._height - this._topMargin)
            .endFill();
        bamSprite.mask = mask;
        this._container.removeChildren();
        return bamSprite;
    }

    set line(value) {
        this._currentLine = value;
    }

    static getRenderEntryVerticalPositioning(renderEntry) {
        let localYOffset = 0;
        let localYHeight = 1;
        if (renderEntry.isPaired) {
            const pairedReadHeight = 0.5;
            if (renderEntry.isOverlaps) {
                if (renderEntry.isLeft) {
                    localYOffset = .0;
                    localYHeight = pairedReadHeight;
                }
                else if (renderEntry.isRight) {
                    localYOffset = 1.1 - pairedReadHeight;
                    localYHeight = pairedReadHeight;
                }
            }
        }
        return {localYHeight, localYOffset};
    }

    _checkTextureCoordinates({x, y}) {
        if (x !== undefined && x !== null && (!this._textureStartX || x < this._textureStartX)) {
            this._textureStartX = x;
        }
        if (y !== undefined && y !== null && (!this._textureStartY || y < this._textureStartY)) {
            this._textureStartY = y;
        }
    }

    _initReadColorModeNoColor() {
        this._setColor(this._baseColor = this._colors.base);
    }

    _initReadColorModePairOrientation(renderEntry) {
        if (!renderEntry.spec.pairSimplified ||
            this._colors.pairOrientationAndInsertSize[renderEntry.spec.pairSimplified] === undefined) {
            this._setColor(this._baseColor = this._colors.base);
        } else {
            this._setColor(this._baseColor = this._colors.pairOrientationAndInsertSize[renderEntry.spec.pairSimplified]);
        }
    }

    _initReadColorModeReadStrand(renderEntry) {
        this._setColor(this._baseColor = this._colors.strand[renderEntry.spec.strand]);
    }

    _initReadColorModeInsertSize(renderEntry) {
        this._setColor(this._baseColor = this._colors.pairOrientationAndInsertSize[renderEntry.spec.insertSize]);
    }

    _initReadColorModeInsertSizeAndPairOrientation(renderEntry) {
        if (!renderEntry.spec.insertSizeAndPairOrientation ||
            this._colors.pairOrientationAndInsertSize[renderEntry.spec.insertSizeAndPairOrientation] === undefined) {
            this._setColor(this._baseColor = this._colors.base);
        }
        else {
            this._setColor(this._baseColor = this._colors.pairOrientationAndInsertSize[renderEntry.spec.insertSizeAndPairOrientation]);
        }
    }

    _initReadColorModeFirstInPairStrand(renderEntry) {
        switch (renderEntry.spec.pair) {
            case undefined:
                this._setColor(this._baseColor = this._colors.base);
                break;
            case 'R2L1':
            case 'L1R2':
            case 'L1L2':
            case 'L2L1':
                this._setColor(this._baseColor = this._colors.strand.forward);
                break;
            case 'R1L2':
            case 'L2R1':
            case 'R1R2':
            case 'R2R1':
                this._setColor(this._baseColor = this._colors.strand.reverse);
                break;
            default:
                this._setColor(this._baseColor = this._colors.base);
                break;
        }
    }

    _initReadColorModeBisulfiteConversion(renderEntry) {
        switch (renderEntry.spec.pair) {
            case undefined:
                this._setColor(this._baseColor = this._colors.base);
                break;
            case 'R2L1':
            case 'L1R2':
            case 'L1L2':
            case 'L2L1':
                this._setColor(this._baseColor = this._colors.bisulfite.F1R2);
                break;
            case 'R1L2':
            case 'L2R1':
            case 'R1R2':
            case 'R2R1':
                this._setColor(this._baseColor = this._colors.bisulfite.F2R1);
                break;
            default:
                this._setColor(this._baseColor = this._colors.base);
                break;
        }
    }

    _initRead(renderEntry) {
        this._contoured = this._features.shadeByQuality && renderEntry.spec.lowQ;
        switch (this._features.colorMode) {
            case 'noColor':
                this._initReadColorModeNoColor();
                break;
            case 'pairOrientation':
                this._initReadColorModePairOrientation(renderEntry);
                break;
            case 'readStrand':
                this._initReadColorModeReadStrand(renderEntry);
                break;
            case 'insertSize':
                this._initReadColorModeInsertSize(renderEntry);
                break;
            case 'insertSizeAndPairOrientation':
                this._initReadColorModeInsertSizeAndPairOrientation(renderEntry);
                break;
            case 'firstInPairStrand':
                this._initReadColorModeFirstInPairStrand(renderEntry);
                break;
            case 'bisulfiteConversion':
                this._initReadColorModeBisulfiteConversion(renderEntry);
                break;
        }
    }

    _renderArrow(renderEntry, direction) {
        // direction === -1 - left, direction === 1 - right
        const {localYHeight, localYOffset} = AlignmentsRenderer.getRenderEntryVerticalPositioning(renderEntry);
        if (!this._canShowDetails || this._yScale === 1)
            return;
        this._checkTextureCoordinates({x: Math.floor(this._projectX(renderEntry.startIndex))});
        this._setColor(this._hovered ? ColorProcessor.darkenColor(this._baseColor, 0.1) : this._baseColor, this._contoured ? 0.5 : 1);
        if (!this._contoured) {
            this._graphics.lineStyle(this._lineWidth, this._hovered ? ColorProcessor.darkenColor(this._baseColor, 0.1) : this._baseColor, 1);
        }
        this._graphics.moveTo(Math.floor(this._projectX(renderEntry.startIndex)) - direction * this._lineWidth / 2.0,
            Math.floor(this._projectY(localYOffset)));
        this._graphics.lineTo(Math.floor(this._projectX(renderEntry.startIndex)) - direction * this._lineWidth / 2.0,
            Math.floor(this._projectY(localYOffset + localYHeight)) - this._lineWidth);
        this._graphics.lineStyle(0, this._baseColor, 0);
        this._graphics.drawPolygon([
            Math.floor(this._projectX(renderEntry.startIndex)) - direction * this._lineWidth / 2.0,
            Math.floor(this._projectY(localYOffset)),
            Math.floor(this._projectX(renderEntry.startIndex) + direction * this._figOffset),
            Math.floor(this._projectY(localYOffset + localYHeight / 2)),
            Math.floor(this._projectX(renderEntry.startIndex)) - direction * this._lineWidth / 2.0,
            Math.floor(this._projectY(localYOffset + localYHeight) - this._lineWidth)
        ]);

        if (this._hovered) {
            this._graphics.endFill();
            this._graphics.lineStyle(this._lineWidth, this._hovered ? ColorProcessor.darkenColor(this._baseColor) : this._baseColor, 1);
            this._graphics.moveTo(Math.floor(this._projectX(renderEntry.startIndex)) - direction * this._lineWidth / 2.0,
                Math.floor(this._projectY(localYOffset)));
            this._graphics.lineTo(Math.floor(this._projectX(renderEntry.startIndex) + direction * this._figOffset),
                Math.floor(this._projectY(localYOffset + localYHeight / 2)));
            this._graphics.moveTo(Math.floor(this._projectX(renderEntry.startIndex) + direction * this._figOffset),
                Math.floor(this._projectY(localYOffset + localYHeight / 2)));
            this._graphics.lineTo(Math.floor(this._projectX(renderEntry.startIndex)) - direction * this._lineWidth / 2.0,
                Math.floor(this._projectY(localYOffset + localYHeight)) - this._lineWidth);
            this._graphics.lineStyle(0, this._baseColor, 0);
            this._graphics.beginFill(this._currentColor, this._currentAlpha);
            this._checkTextureCoordinates({
                x: Math.min(Math.floor(this._projectX(renderEntry.startIndex) + direction * this._figOffset),
                    Math.floor(this._projectX(renderEntry.startIndex)) - direction * this._lineWidth / 2.0) - this._lineWidth / 2.0,
                y: Math.floor(this._projectY(localYOffset)) - this._lineWidth
            });
        } else {
            this._checkTextureCoordinates({
                x: Math.min(Math.floor(this._projectX(renderEntry.startIndex) + direction * this._figOffset),
                    Math.floor(this._projectX(renderEntry.startIndex)) - direction * this._lineWidth / 2.0),
                y: Math.floor(this._projectY(localYOffset)) - this._lineWidth
            });
        }
    }

    _renderMatch(renderEntry) {
        const {localYHeight, localYOffset} = AlignmentsRenderer.getRenderEntryVerticalPositioning(renderEntry);
        if (this._viewport.isShortenedIntronsMode) {
            for (let r = 0; r < this._viewport.shortenedIntronsViewport._coveredRange.ranges.length; r++) {
                const range = this._viewport.shortenedIntronsViewport._coveredRange.ranges[r];
                if ((range.startIndex >= renderEntry.startIndex && range.startIndex < renderEntry.endIndex) ||
                    (range.endIndex >= renderEntry.startIndex && range.endIndex < renderEntry.endIndex) ||
                    (range.startIndex >= renderEntry.startIndex && range.endIndex < renderEntry.endIndex) ||
                    (range.startIndex <= renderEntry.startIndex && range.endIndex >= renderEntry.endIndex)) {
                    this._renderReadBordersWithBreaks(
                        this._baseColor,
                        Math.max(renderEntry.startIndex, range.startIndex),
                        Math.min(renderEntry.endIndex, range.endIndex + 1),
                        renderEntry.startIndex < range.startIndex,
                        renderEntry.endIndex > range.endIndex,
                        this._contoured,
                        localYOffset,
                        localYHeight
                    );
                }
            }
        } else {
            this._renderReadBorders(
                this._baseColor,
                renderEntry.startIndex,
                renderEntry.endIndex,
                this._contoured,
                localYOffset,
                localYHeight
            );
        }
    }

    _renderInsertion(renderEntry) {
        if (!this._canShowDetails)
            return;
        const {localYHeight, localYOffset} = AlignmentsRenderer.getRenderEntryVerticalPositioning(renderEntry);
        this._checkTextureCoordinates({
            x: this._projectX(renderEntry.startIndex)
        });
        this._graphics.lineStyle(this._lineWidth, this._colors.ins, 1);
        const x0 = Math.floor(this._projectX(renderEntry.startIndex) >> 0);
        const x1 = Math.floor(x0 - this._figOffset - this._renderOffset);
        const x2 = Math.floor(x0 + this._figOffset + this._renderOffset);
        const y0 = Math.floor((this._projectY(localYOffset) >> 0) + this._renderOffset);
        const y1 = Math.floor((this._projectY(localYOffset + localYHeight) >> 0) - this._renderOffset);
        this._graphics.moveTo(x1, y0 + this._lineWidth / 2.0);
        this._graphics.lineTo(x2, y0 + this._lineWidth / 2.0);
        this._graphics.moveTo(x0 - this._lineWidth / 2.0, y0);
        this._graphics.lineTo(x0 - this._lineWidth / 2.0, y1);
        this._graphics.moveTo(x1, y1 + this._lineWidth / 2.0);
        this._graphics.lineTo(x2, y1 + this._lineWidth / 2.0);
        this._graphics.lineStyle(0, this._baseColor, 0);
        this._checkTextureCoordinates({x: x1, y: Math.min(y0 + this._lineWidth / 2.0, y1 + this._lineWidth / 2.0)});
    }

    _renderDeletion(renderEntry) {
        if (!this._canShowDetails)
            return;
        const {localYHeight, localYOffset} = AlignmentsRenderer.getRenderEntryVerticalPositioning(renderEntry);
        this._checkTextureCoordinates({
            x: this._projectX(renderEntry.startIndex)
        });
        this._setColor(this._colors.bg);
        this._scaledRect(renderEntry.startIndex, localYOffset, renderEntry.length, localYHeight);
        this._graphics.lineStyle(this._lineWidth, this._colors.del, 1);
        this._graphics.moveTo(Math.floor(this._projectX(renderEntry.startIndex) - this._renderOffset) + this._lineWidth / 2.0,
            Math.floor(this._projectY(localYOffset) + this._renderOffset));
        this._graphics.lineTo(Math.floor(this._projectX(renderEntry.startIndex) - this._renderOffset) + this._lineWidth / 2.0,
            Math.floor(this._projectY(localYOffset + localYHeight) - this._renderOffset));
        this._graphics.moveTo(Math.floor(this._projectX(renderEntry.startIndex) - this._renderOffset),
            Math.floor(this._projectY(localYOffset + localYHeight / 2)) + this._lineWidth / 2.0);
        this._graphics.lineTo(Math.floor(this._projectX(renderEntry.endIndex) - this._renderOffset),
            Math.floor(this._projectY(localYOffset + localYHeight / 2)) + this._lineWidth / 2.0);
        this._graphics.moveTo(Math.floor(this._projectX(renderEntry.endIndex) - this._renderOffset) + this._lineWidth / 2.0,
            Math.floor(this._projectY(localYOffset) + this._renderOffset));
        this._graphics.lineTo(Math.floor(this._projectX(renderEntry.endIndex) - this._renderOffset) + this._lineWidth / 2.0,
            Math.floor(this._projectY(localYOffset + localYHeight) - this._renderOffset));
        this._graphics.lineStyle(0, this._baseColor, 0);
        this._checkTextureCoordinates({
            x: Math.floor(this._projectX(renderEntry.startIndex) - this._renderOffset) + this._lineWidth / 2.0,
            y: this._projectY(localYOffset) + this._renderOffset
        });
    }

    _getReadBreaks(renderEntry) {
        let breakOnLeft = false;
        let breakOnRight = false;
        if (this._viewport.isShortenedIntronsMode) {
            for (let r = 0; r < this._viewport.shortenedIntronsViewport._coveredRange.ranges.length; r++) {
                const range = this._viewport.shortenedIntronsViewport._coveredRange.ranges[r];
                if (range.startIndex === renderEntry.startIndex) {
                    breakOnLeft = true;
                } else if (range.endIndex === renderEntry.startIndex) {
                    breakOnRight = true;
                }
            }
        }
        return {breakOnLeft, breakOnRight};
    }

    _getStyleForBase(renderEntry) {
        const isBisulfite = this._features.colorMode === BISULFITE_CONVERSION;
        if (!isBisulfite) {
            return {
                color: this._colors[renderEntry.base],
                style: baseLabelStyle
            };
        }
        const isCytosine = renderEntry.type === partTypes.cytosineMismatch;
        const isNoncytosine = renderEntry.type === partTypes.noncytosineMismatch;
        const letterColor = isCytosine ?
            this._colors.bisulfite.cytosineMismatch :
            (isNoncytosine ?
                this._colors.bisulfite.noncytosineMismatch :
                this._colors.bisulfite.mismatch
            );
        const baseLabelBisulfiteStyle = {...baseLabelStyle};
        baseLabelBisulfiteStyle.fill = letterColor;
        return {
            color: this._canShowLetters ? this._baseColor : letterColor,
            style: baseLabelBisulfiteStyle
        };
    }

    _renderBase(renderEntry) {
        const {localYHeight, localYOffset} = AlignmentsRenderer.getRenderEntryVerticalPositioning(renderEntry);
        const {breakOnLeft, breakOnRight} = this._getReadBreaks(renderEntry);
        const start = renderEntry.startIndex + (breakOnLeft ? BP_OFFSET : 0);
        const end = renderEntry.endIndex + (breakOnRight ? -BP_OFFSET : 0);
        const {color, style} = this._getStyleForBase(renderEntry);
        this._setColor(color);
        this._scaledRect(
            start,
            localYOffset,
            end - start,
            localYHeight,
            this._canShowLetters && this._yScale > 1 ? BP_OFFSET : 0);
        if (
            !breakOnLeft &&
            !breakOnRight &&
            this._canShowLetters &&
            this._yScale > 1 &&
            !renderEntry.isOverlaps &&
            this._labelsManager
        ) {
            const sprite = this._labelsManager.getLabel(
                renderEntry.base,
                style
            );
            sprite.x = Math.round(this._projectX(renderEntry.startIndex + BP_OFFSET) - sprite.width / 2);
            sprite.y = Math.round(this._projectY(localYOffset + localYHeight / 2) - sprite.height / 2);
            this._container.addChild(sprite);
        }
        this._setColor(this._colors.bg);
        this._checkTextureCoordinates({
            x: this._projectX(start),
            y: this._projectY(localYOffset)
        });
    }

    _renderMismatch(renderEntry) {
        if (this._features.mismatches) {
            this._renderBase(renderEntry);
        }
    }

    _renderPairedReadConnection(renderEntry) {
        if (this._yScale === 1) {
            return;
        }
        const {localYHeight, localYOffset} = AlignmentsRenderer.getRenderEntryVerticalPositioning(renderEntry);
        this._checkTextureCoordinates({
            x: Math.floor(this._projectX(renderEntry.startIndex) - this._renderOffset),
            y: Math.floor(this._projectY(localYOffset + localYHeight / 2)) + this._lineWidth / 2.0
        });
        this._graphics.lineStyle(this._lineWidth, this._colors.spliceJunctions, 1);
        this._graphics.moveTo(Math.floor(this._projectX(renderEntry.startIndex) - this._renderOffset),
            Math.floor(this._projectY(localYOffset + localYHeight / 2)) + this._lineWidth / 2.0);
        this._graphics.lineTo(Math.floor(this._projectX(renderEntry.endIndex) - this._renderOffset),
            Math.floor(this._projectY(localYOffset + localYHeight / 2)) + this._lineWidth / 2.0);
        this._graphics.lineStyle(0, this._baseColor, 0);
    }

    _renderSpliceJunction(renderEntry) {
        const {localYHeight, localYOffset} = AlignmentsRenderer.getRenderEntryVerticalPositioning(renderEntry);
        this._checkTextureCoordinates({
            x: Math.floor(this._projectX(renderEntry.startIndex) - this._renderOffset) + this._lineWidth / 2.0,
            y: Math.floor(this._projectY(localYOffset) + this._renderOffset)
        });
        const spliceJunctionColor = 0x96B8C8; //todo config
        this._setColor(this._colors.bg);
        this._scaledRect(renderEntry.startIndex, localYOffset, renderEntry.length, localYHeight);
        this._graphics.lineStyle(this._lineWidth, spliceJunctionColor, 1);
        this._graphics.moveTo(Math.floor(this._projectX(renderEntry.startIndex) - this._renderOffset) + this._lineWidth / 2.0,
            Math.floor(this._projectY(localYOffset) + this._renderOffset));
        this._graphics.lineTo(Math.floor(this._projectX(renderEntry.startIndex) - this._renderOffset) + this._lineWidth / 2.0,
            Math.floor(this._projectY(localYOffset + localYHeight) - this._renderOffset));
        this._graphics.moveTo(Math.floor(this._projectX(renderEntry.startIndex) - this._renderOffset),
            Math.floor(this._projectY(localYOffset + localYHeight / 2)) + this._lineWidth / 2.0);
        this._graphics.lineTo(Math.floor(this._projectX(renderEntry.endIndex) - this._renderOffset),
            Math.floor(this._projectY(localYOffset + localYHeight / 2)) + this._lineWidth / 2.0);
        this._graphics.moveTo(Math.floor(this._projectX(renderEntry.endIndex) - this._renderOffset) + this._lineWidth / 2.0,
            Math.floor(this._projectY(localYOffset) + this._renderOffset));
        this._graphics.lineTo(Math.floor(this._projectX(renderEntry.endIndex) - this._renderOffset) + this._lineWidth / 2.0,
            Math.floor(this._projectY(localYOffset + localYHeight) - this._renderOffset));
        this._graphics.lineStyle(0, this._baseColor, 0);
    }

    render(renderEntry) {
        switch (renderEntry.type) {
            case partTypes.initRead: {
                this._initRead(renderEntry);
            }
                break;
            case partTypes.leftArrow: {
                this._renderArrow(renderEntry, ALIGNMENT_ARROW_DIRECTION_LEFT);
            }
                break;
            case partTypes.rightArrow: {
                this._renderArrow(renderEntry, ALIGNMENT_ARROW_DIRECTION_RIGHT);
            }
                break;
            case partTypes.match: {
                this._renderMatch(renderEntry);
            }
                break;
            case partTypes.insertion: {
                this._renderInsertion(renderEntry);
            }
                break;
            case partTypes.deletion: {
                this._renderDeletion(renderEntry);
            }
                break;
            case partTypes.softClipBase: {
                this._renderBase(renderEntry);
            }
                break;
            case partTypes.base:
            case partTypes.cytosineMismatch:
            case partTypes.noncytosineMismatch: {
                this._renderMismatch(renderEntry);
            }
                break;
            case partTypes.pairedReadConnection: {
                this._renderPairedReadConnection(renderEntry);
            }
                break;
            case partTypes.spliceJunctions: {
                this._renderSpliceJunction(renderEntry);
            }
                break;
            case partTypes.methylatedBase:
            case partTypes.unmethylatedBase: {
                this._renderMethylation(renderEntry);
            }
                break;
        }
    }

    _renderMethylation(renderEntry) {
        const {localYHeight, localYOffset} =
            AlignmentsRenderer.getRenderEntryVerticalPositioning(renderEntry);
        const {breakOnLeft, breakOnRight} = this._getReadBreaks(renderEntry);
        const start = renderEntry.startIndex + (breakOnLeft ? BP_OFFSET : 0);
        const end = renderEntry.endIndex + (breakOnRight ? -BP_OFFSET : 0);
        const type = renderEntry.type === partTypes.methylatedBase ?
            METHYLATED_BASE : UNMETHYLATED_BASE;
        this._setColor(this._colors.bisulfite[type]);
        this._scaledRect(
            start,
            localYOffset,
            end - start,
            localYHeight,
            0);
    }

    _setColor(color, alpha = 1) {
        if (color !== this._currentColor || alpha !== this._currentAlpha) {
            this._graphics.endFill();
            this._currentColor = color;
            this._currentAlpha = alpha;
            this._graphics.beginFill(this._currentColor, this._currentAlpha);
        }
    }
    _convertY(y) {
        return y * this._yScale;
    }
    _projectX(x) {
        return Math.max(this._minimumPx, Math.min(this._maximumPx, this._viewport.project.brushBP2pixel(x - BP_OFFSET)));
    }
    _projectY(y) {
        return (y * this._yScale - this._linesOffset + this._currentLine * this._yGlobalScale + this._topMargin);
    }
    _scaledRect(x, y, width, height, margin = 0) {
        const x1 = Math.floor(this._projectX(x) + margin);
        const x2 = Math.floor(this._projectX(x + width) - margin);
        this._graphics.drawRect(
            x1,
            Math.floor(this._projectY(y)),
            x2 - x1,
            Math.floor(this._convertY(height) >> 0)
        );
    }
    _renderReadBorders(color, startIndex, endIndex, renderContoured, localYOffset, localYHeight) {
        this._setColor(this._hovered ? ColorProcessor.darkenColor(color, 0.1) : color, renderContoured ? 0.5 : 1);
        this._graphics.lineStyle(0, this._baseColor, 0);
        this._scaledRect(startIndex, localYOffset, endIndex - startIndex, localYHeight);

        if (this._hovered) {
            this._graphics.endFill();

            this._graphics.lineStyle(this._lineWidth, ColorProcessor.darkenColor(color), 1);

            this._graphics.moveTo(Math.floor(this._projectX(startIndex)) + this._lineWidth / 2.0,
                Math.floor(this._projectY(localYOffset)) - this._lineWidth / 2.0);

            this._graphics.lineTo(Math.floor(this._projectX(startIndex)) + this._lineWidth / 2.0,
                Math.floor(this._projectY(localYOffset + localYHeight)) - this._lineWidth / 2.0);

            this._graphics.moveTo(Math.floor(this._projectX(startIndex)),
                Math.floor(this._projectY(localYOffset + localYHeight)) - this._lineWidth);

            this._graphics.lineTo(Math.floor(this._projectX(endIndex)),
                Math.floor(this._projectY(localYOffset + localYHeight)) - this._lineWidth);

            this._graphics.moveTo(Math.floor(this._projectX(endIndex)) - this._lineWidth / 2.0,
                Math.floor(this._projectY(localYOffset + localYHeight)) - this._lineWidth / 2.0);

            this._graphics.lineTo(Math.floor(this._projectX(endIndex)) - this._lineWidth / 2.0,
                Math.floor(this._projectY(localYOffset)) - this._lineWidth / 2.0);

            this._graphics.moveTo(Math.floor(this._projectX(endIndex)),
                Math.floor(this._projectY(localYOffset)));

            this._graphics.lineTo(Math.floor(this._projectX(startIndex)),
                Math.floor(this._projectY(localYOffset)));
            this._graphics.lineStyle(0, color, 0);
            this._graphics.beginFill(this._currentColor, this._currentAlpha);
            if (!this._textureStartX || (Math.floor(this._projectX(startIndex)) - this._lineWidth / 2.0) < this._textureStartX) {
                this._textureStartX = Math.floor(this._projectX(startIndex)) - this._lineWidth / 2.0;
            }
            if (!this._textureStartY || this._projectY(localYOffset) - this._lineWidth < this._textureStartY) {
                this._textureStartY = this._projectY(localYOffset) - this._lineWidth;
            }
        } else {
            if (!this._textureStartX || (Math.floor(this._projectX(startIndex))) < this._textureStartX) {
                this._textureStartX = Math.floor(this._projectX(startIndex));
            }
            if (!this._textureStartY || this._projectY(localYOffset) < this._textureStartY) {
                this._textureStartY = this._projectY(localYOffset);
            }
        }
    }
    _renderReadBordersWithBreaks(color, startIndex, endIndex, breakStart, breakEnd, renderContoured, localYOffset, localYHeight) {
        startIndex = startIndex + (breakStart ? BP_OFFSET : 0);
        endIndex = endIndex + (breakEnd ? -BP_OFFSET : 0);
        if (!this._textureStartX || this._projectX(startIndex) < this._textureStartX) {
            this._textureStartX = this._projectX(startIndex);
        }
        this._renderReadBorders(color, startIndex, endIndex, renderContoured, localYOffset, localYHeight);
    }
}
