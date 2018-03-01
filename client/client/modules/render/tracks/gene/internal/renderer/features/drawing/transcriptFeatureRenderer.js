import FeatureBaseRenderer from './featureBaseRenderer';
import PIXI from 'pixi.js';
import {ColorProcessor, PixiTextSize} from '../../../../../../utilities';
import drawStrandDirection from './strandDrawing';
import {drawingConfiguration} from '../../../../../../core';

const Math = window.Math;

export default class TranscriptFeatureRenderer extends FeatureBaseRenderer {

    _aminoacidFeatureRenderer = null;
    gffShowNumbersAminoacid;

    constructor(config, registerLabel, registerDockableElement, registerFeaturePosition, aminoacidFeatureRenderer) {
        super(config, registerLabel, registerDockableElement, registerFeaturePosition);
        this._aminoacidFeatureRenderer = aminoacidFeatureRenderer;
    }

    analyzeBoundaries(feature, viewport) {
        const boundaries = super.analyzeBoundaries(feature, viewport);
        const rectBoundaries = boundaries.rect;
        const boundariesX1 = rectBoundaries.x1;
        const boundariesX2 = rectBoundaries.x2;

        let transcriptLabelSize = {height: 0, width: 0};
        const transcript = this.config.transcript;

        if (feature.name !== null) {
            transcriptLabelSize = PixiTextSize.getTextSize(feature.name, transcript.label);
        }

        const width = Math.max(1, boundariesX2 - boundariesX1, transcriptLabelSize.width);
        let height = transcript.height + transcriptLabelSize.height;

        this._aminoacidFeatureRenderer.gffShowNumbersAminoacid = this.gffShowNumbersAminoacid;
        const childBoundaries = this._aminoacidFeatureRenderer.analyzeBoundaries(feature, viewport);
        if (childBoundaries) {
            const childRect = childBoundaries.rect;
            const childSize = {
                height: childRect.y2 - childRect.y1,
                width: childRect.x2 - childRect.x1
            };
            height += childSize.height;
        }

        if (rectBoundaries) {
            rectBoundaries.x2 = Math.max(boundariesX2, boundariesX1 + width);
            rectBoundaries.y2 = height;
        }
        return boundaries;
    }

    _getBlockDrawingConfig(blockItem, shouldDrawAminoacid) {
        let fill = this.config.transcript.features.fill.other;
        let strandFill = this.config.transcript.features.strand.fill.other;
        let hoveredFill = this.config.transcript.features.fill.hoveredOther;
        let hoveredStrandFill = this.config.transcript.features.strand.fill.hoveredOther;
        let shouldDrawStrand = blockItem.hasOwnProperty('strand');
        let shouldFillBlock = true;
        if (blockItem.feature.toLowerCase() === 'cds') {
            fill = this.config.transcript.features.fill.cds;
            strandFill = this.config.transcript.features.strand.fill.cds;
            hoveredFill = this.config.transcript.features.fill.hoveredCds;
            hoveredStrandFill = this.config.transcript.features.strand.fill.hoveredCds;
            const aminoacidSequence = blockItem.aminoacidSequence;
            if (shouldDrawAminoacid && aminoacidSequence !== null && aminoacidSequence !== undefined && aminoacidSequence.length > 0) {
                shouldDrawStrand = false;
                shouldFillBlock = false;
            }
        }
        hoveredFill = hoveredFill !== undefined ? hoveredFill : ColorProcessor.darkenColor(fill);
        hoveredStrandFill = hoveredStrandFill !== undefined ? hoveredStrandFill : ColorProcessor.darkenColor(strandFill);
        return {
            fill,
            hoveredFill,
            shouldDrawStrand,
            shouldFillBlock,
            strandFill,
            hoveredStrandFill
        };
    }

    _renderNonEmptyBlock(opts) {
        const {viewport, graphics, hoveredGraphics, block, centeredPositionY, feature} = opts;
        const shouldDrawAminoacid = this._aminoacidFeatureRenderer !== null ? this._aminoacidFeatureRenderer.shouldDrawAminoacids(viewport) : false;
        const pixelsInBp = viewport.factor;
        const blockPxStart = Math.max(viewport.project.brushBP2pixel(block.startIndex), -viewport.canvasSize) - pixelsInBp / 2;
        const blockPxEnd = Math.min(viewport.project.brushBP2pixel(block.endIndex), 2 * viewport.canvasSize) + pixelsInBp / 2;
        const white = 0xFFFFFF;
        const height = this.config.transcript.height;
        graphics.lineStyle(0, white, 0);
        hoveredGraphics.lineStyle(0, white, 0);
        for (let j = 0; j < block.items.length; j++) {
            const blockItem = block.items[j];
            const {
                fill,
                hoveredFill,
                shouldDrawStrand,
                strandFill,
                hoveredStrandFill,
                shouldFillBlock
            } = this._getBlockDrawingConfig(blockItem, shouldDrawAminoacid);
            const blockItemPxStart = Math.max(viewport.project.brushBP2pixel(blockItem.startIndex), -viewport.canvasSize) - pixelsInBp / 2;
            const blockItemPxEnd = Math.min(viewport.project.brushBP2pixel(blockItem.endIndex), 2 * viewport.canvasSize) + pixelsInBp / 2;
            if (blockItemPxStart > blockItemPxEnd) {
                continue;
            }
            if (shouldFillBlock) {
                graphics
                    .beginFill(fill, 1)
                    .drawRect(
                        blockItemPxStart,
                        centeredPositionY - height / 2,
                        blockItemPxEnd - blockItemPxStart,
                        height)
                    .endFill();
                hoveredGraphics
                    .beginFill(hoveredFill, 1)
                    .drawRect(
                        blockItemPxStart,
                        centeredPositionY - height / 2,
                        blockItemPxEnd - blockItemPxStart,
                        height)
                    .endFill();
                this.updateTextureCoordinates(
                    {
                        x: blockItemPxStart,
                        y: centeredPositionY - height / 2
                    });
            }
            if (shouldDrawStrand) {
                drawStrandDirection(
                    block.strand,
                    {
                        centerY: centeredPositionY,
                        height: height,
                        width: blockItemPxEnd - blockItemPxStart,
                        x: blockItemPxStart
                    },
                    graphics,
                    strandFill,
                    this.config.transcript.features.strand.arrow,
                    1,
                    ::this.updateTextureCoordinates
                );
                drawStrandDirection(
                    block.strand,
                    {
                        centerY: centeredPositionY,
                        height: height,
                        width: blockItemPxEnd - blockItemPxStart,
                        x: blockItemPxStart
                    },
                    hoveredGraphics,
                    hoveredStrandFill,
                    this.config.transcript.features.strand.arrow,
                    1,
                    ::this.updateTextureCoordinates
                );
            }
            this.registerFeaturePosition(
                Object.assign({exonNumber: blockItem.attributes.exon_number}, feature),
                {
                    x1: blockItemPxStart,
                    x2: blockItemPxEnd,
                    y1: centeredPositionY - height / 2,
                    y2: centeredPositionY + height / 2
                });
        }
        graphics
            .lineStyle(this.config.transcript.features.border.thickness, this.config.transcript.features.border.color, 1)
            .drawRect(
                blockPxStart,
                centeredPositionY - height / 2,
                blockPxEnd - blockPxStart,
                height
            );
        hoveredGraphics
            .lineStyle(this.config.transcript.features.border.thickness, ColorProcessor.darkenColor(this.config.transcript.features.border.color), 1)
            .drawRect(
                blockPxStart,
                centeredPositionY - height / 2,
                blockPxEnd - blockPxStart,
                height
            );
        this.updateTextureCoordinates(
            {
                x: blockPxStart,
                y: centeredPositionY - height / 2
            });
    }

    _renderEmptyBlock(opts) {
        const {viewport, graphics, hoveredGraphics, block, centeredPositionY} = opts;
        const pixelsInBp = viewport.factor;
        const blockPxStart = Math.max(viewport.project.brushBP2pixel(block.startIndex), -viewport.canvasSize) - pixelsInBp / 2;
        const blockPxEnd = Math.min(viewport.project.brushBP2pixel(block.endIndex), 2 * viewport.canvasSize) + pixelsInBp / 2;
        const height = this.config.transcript.height;
        const white = 0xFFFFFF;
        graphics
            .beginFill(white, 0)
            .lineStyle(this.config.transcript.thickness, this.config.transcript.fill, 1)
            .moveTo(blockPxStart, centeredPositionY)
            .lineTo(blockPxEnd, centeredPositionY)
            .endFill();
        hoveredGraphics
            .beginFill(white, 0)
            .lineStyle(this.config.transcript.thickness, ColorProcessor.darkenColor(this.config.transcript.fill), 1)
            .moveTo(blockPxStart, centeredPositionY)
            .lineTo(blockPxEnd, centeredPositionY)
            .endFill();
        this.updateTextureCoordinates(
            {
                x: blockPxStart,
                y: centeredPositionY - this.config.transcript.thickness / 2
            });
        if (block.hasOwnProperty('strand')) {
            drawStrandDirection(
                block.strand,
                {
                    centerY: centeredPositionY,
                    height: height,
                    width: blockPxEnd - blockPxStart,
                    x: blockPxStart
                },
                graphics,
                this.config.transcript.strand.fill,
                this.config.transcript.strand.arrow,
                1,
                ::this.updateTextureCoordinates
            );
            drawStrandDirection(
                block.strand,
                {
                    centerY: centeredPositionY,
                    height: height,
                    width: blockPxEnd - blockPxStart,
                    x: blockPxStart
                },
                hoveredGraphics,
                ColorProcessor.darkenColor(this.config.transcript.strand.fill),
                this.config.transcript.strand.arrow,
                1,
                ::this.updateTextureCoordinates
            );
        }
    }

    _renderAminoacid(opts) {
        const {block, viewport, graphics, hoveredGraphics, labelContainer, dockableElementsContainer, attachedElementsContainer,  position, centeredPositionY} = opts;
        const shouldDrawAminoacid = this._aminoacidFeatureRenderer !== null ?
            this._aminoacidFeatureRenderer.shouldDrawAminoacids(viewport) : false;
        if (shouldDrawAminoacid) {
            for (let j = 0; j < block.items.length; j++) {
                const blockItem = block.items[j];
                if (blockItem.feature.toLowerCase() === 'cds') {
                    const aminoacidSequence = blockItem.aminoacidSequence;
                    if (blockItem.feature.toLowerCase() === 'cds' &&
                        this._aminoacidFeatureRenderer && aminoacidSequence !== null &&
                        aminoacidSequence !== undefined && aminoacidSequence.length > 0) {
                        this._aminoacidFeatureRenderer.gffShowNumbersAminoacid = this.gffShowNumbersAminoacid;
                        this._aminoacidFeatureRenderer.render(
                            blockItem,
                            viewport,
                            graphics,
                            hoveredGraphics,
                            labelContainer,
                            dockableElementsContainer,
                            attachedElementsContainer,
                            {
                                x: position.x,
                                y: centeredPositionY
                            });
                    }
                }
            }
        }
    }

    render(feature, viewport, graphics, hoveredGraphics, labelContainer, dockableElementsContainer, attachedElementsContainer, position) {
        super.render(feature, viewport, graphics, hoveredGraphics, labelContainer, dockableElementsContainer, attachedElementsContainer, position);
        const pixelsInBp = viewport.factor;
        const transcriptConfig = this.config.transcript;
        const aminoacidsFitsViewport = this._aminoacidFeatureRenderer.aminoacidsFitsViewport(feature, viewport);
        let center = transcriptConfig.height / 2 + transcriptConfig.marginTop + (aminoacidsFitsViewport ? this._aminoacidFeatureRenderer._aminoacidNumberHeight : 0);
        const project = viewport.project;
        if (feature.name !== null &&
            (feature.feature.toLowerCase() === 'transcript' || feature.feature.toLowerCase() === 'mrna') && !feature.canonical) {
            const label = new PIXI.Text(feature.name, transcriptConfig.label);
            let labelStart = project.brushBP2pixel(feature.startIndex) - pixelsInBp / 2;
            label.resolution = drawingConfiguration.resolution;
            labelStart = Math.max(Math.min(labelStart, project.brushBP2pixel(feature.endIndex) - label.width), position.x);
            label.x = Math.round(labelStart);
            label.y = Math.round(position.y + transcriptConfig.label.marginTop);
            center = label.height + transcriptConfig.label.marginTop + transcriptConfig.height / 2 + transcriptConfig.marginTop + (aminoacidsFitsViewport ? this._aminoacidFeatureRenderer._aminoacidNumberHeight : 0);
            labelContainer.addChild(label);
            this.registerLabel(label, {x: labelStart, y: position.y + transcriptConfig.label.marginTop}, {
                end: feature.endIndex,
                start: feature.startIndex
            });
        }

        if (feature.structure !== null && feature.structure !== undefined) {
            const height = transcriptConfig.height;
            const centeredPositionY = position.y + center;
            for (let i = 0; i < feature.structure.length; i++) {
                const block = feature.structure[i];
                if (viewport.isShortenedIntronsMode && viewport.shortenedIntronsViewport.shouldSkipFeature(block))
                    continue;
                const blockPxStart = Math.max(project.brushBP2pixel(block.startIndex), -viewport.canvasSize) - pixelsInBp / 2;
                const blockPxEnd = Math.min(project.brushBP2pixel(block.endIndex), 2 * viewport.canvasSize) + pixelsInBp / 2;
                if (blockPxStart > blockPxEnd) {
                    continue;
                }
                if (!block.isEmpty) {
                    this._renderNonEmptyBlock({
                        block,
                        centeredPositionY,
                        graphics,
                        hoveredGraphics,
                        viewport,
                        feature
                    });
                    this._renderAminoacid({
                        block,
                        centeredPositionY,
                        dockableElementsContainer,
                        attachedElementsContainer,
                        graphics,
                        hoveredGraphics,
                        labelContainer,
                        position,
                        viewport,
                    });
                }
                else {
                    this._renderEmptyBlock({
                        block,
                        centeredPositionY,
                        graphics,
                        hoveredGraphics,
                        viewport,
                    });
                }
            }
            this.registerFeaturePosition(feature, {
                x1: project.brushBP2pixel(feature.startIndex) - pixelsInBp / 2,
                x2: project.brushBP2pixel(feature.endIndex) + pixelsInBp / 2,
                y1: centeredPositionY - height / 2,
                y2: centeredPositionY + height / 2
            });

        }
    }
}
