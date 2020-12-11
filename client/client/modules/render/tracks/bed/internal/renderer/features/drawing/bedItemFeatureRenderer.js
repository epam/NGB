import FeatureBaseRenderer from '../../../../../gene/internal/renderer/features/drawing/featureBaseRenderer';
import PIXI from 'pixi.js';
import {ColorProcessor, PixiTextSize} from '../../../../../../utilities';
import drawStrandDirection from '../../../../../gene/internal/renderer/features/drawing/strandDrawing';
import {drawingConfiguration} from '../../../../../../core';

const Math = window.Math;

export default class BedItemFeatureRenderer extends FeatureBaseRenderer {
    constructor(track, config, registerLabel, registerDockableElement, registerFeaturePosition, registerAttachedElement) {
        super(config, registerLabel, registerDockableElement, registerFeaturePosition, registerAttachedElement);
        this.track = track;
    }

    analyzeBoundaries(feature, viewport) {
        const boundaries = super.analyzeBoundaries(feature, viewport);
        const labelSize = PixiTextSize.getTextSize(feature.name, this.config.bed.label);
        let descriptionLabelSize = {height: 0, width: 0};
        if (feature.description && feature.description.length < this.config.bed.description.maximumDisplayLength) {
            descriptionLabelSize = PixiTextSize.getTextSize(feature.description, this.config.bed.description.label);
        }
        if (boundaries.rect) {
            boundaries.rect.x2 = Math.max(boundaries.rect.x2, boundaries.rect.x1 + labelSize.width);
            if (boundaries.rect.x2 - boundaries.rect.x1 < descriptionLabelSize.width) {
                descriptionLabelSize.height = 0;
            }
            boundaries.rect.y2 = 2 * this.config.bed.margin + this.config.bed.height + labelSize.height + descriptionLabelSize.height;
        }
        return boundaries;
    }

    _getFeatureColor(feature, state) {
        if (state && state.color && !feature.rgb) {
            return state.color;
        }
        let color = this.config.bed.defaultColor;
        const oneByte = 256;
        if (feature.rgb) {
            color = feature.rgb[0] * oneByte * oneByte + feature.rgb[1] * oneByte + feature.rgb[2];
        }
        return color;
    }

    render(feature, viewport, graphics, hoveredGraphics, labelContainer, dockableElementsContainer, attachedElementsContainer,  position) {
        super.render(feature, viewport, graphics, hoveredGraphics, labelContainer, dockableElementsContainer, attachedElementsContainer, position);
        const pixelsInBp = viewport.factor;
        const label = new PIXI.Text(feature.name, this.config.bed.label);
        label.resolution = drawingConfiguration.resolution;
        label.x = Math.round(position.x);
        label.y = Math.round(position.y);
        dockableElementsContainer.addChild(label);
        const yStart = position.y;
        this.registerLabel(label, position, {end: feature.endIndex, start: feature.startIndex});
        if (feature.description && feature.description.length < this.config.bed.description.maximumDisplayLength) {
            const descriptionLabelWidth = PixiTextSize.getTextSize(feature.description, this.config.bed.description.label).width;
            if (descriptionLabelWidth < position.width) {
                const descriptionLabel = new PIXI.Text(feature.description, this.config.bed.description.label);
                descriptionLabel.resolution = drawingConfiguration.resolution;
                descriptionLabel.x = Math.round(position.x);
                descriptionLabel.y = Math.round(position.y + label.height);
                dockableElementsContainer.addChild(descriptionLabel);
                this.registerLabel(descriptionLabel, {
                    x: position.x,
                    y: Math.round(position.y + label.height)
                }, {
                    end: feature.endIndex,
                    start: feature.startIndex
                });
                position.y += descriptionLabel.height;
            }
        }
        
        position.y += label.height;

        this.registerFeaturePosition(feature, {
            x1: viewport.project.brushBP2pixel(feature.startIndex) - pixelsInBp / 2,
            x2: Math.max(viewport.project.brushBP2pixel(feature.endIndex) + pixelsInBp / 2, position.x + label.width),
            y1: yStart,
            y2: position.y + this.config.bed.height
        });

        const color = this._getFeatureColor(feature, this.track ? this.track.state : undefined);
        let structureToDisplay = null;

        for (let i = 0; i < feature.structures.length; i++) {
            const width = viewport.convert.brushBP2pixel(feature.structures[i].length);
            if (width > 1) {
                structureToDisplay = feature.structures[i].structure;
                break;
            }
        }

        if (!structureToDisplay) {
            structureToDisplay = feature.structures[feature.structures.length - 1].structure; // minimized mode
        }

        const maxViewportsOnScreen = 3;

        for (let i = 0; i < structureToDisplay.length; i++) {
            const block = structureToDisplay[i];
            if (block.isEmpty) {
                graphics.beginFill(color, 0);
                graphics.lineStyle(1, color, 1);
                graphics.moveTo(Math.max(viewport.project.brushBP2pixel(block.startIndex) - pixelsInBp / 2,
                    -viewport.canvasSize), position.y + this.config.bed.margin + this.config.bed.height / 2);
                graphics.lineTo(Math.min(viewport.project.brushBP2pixel(block.endIndex) + pixelsInBp / 2,
                    2 * viewport.canvasSize), position.y + this.config.bed.margin + this.config.bed.height / 2);
                graphics.endFill();

                hoveredGraphics.beginFill(ColorProcessor.darkenColor(color), 0);
                hoveredGraphics.lineStyle(1, ColorProcessor.darkenColor(color), 1);
                hoveredGraphics.moveTo(Math.max(viewport.project.brushBP2pixel(block.startIndex) - pixelsInBp / 2,
                    -viewport.canvasSize), position.y + this.config.bed.margin + this.config.bed.height / 2);
                hoveredGraphics.lineTo(Math.min(viewport.project.brushBP2pixel(block.endIndex) + pixelsInBp / 2,
                    2 * viewport.canvasSize), position.y + this.config.bed.margin + this.config.bed.height / 2);
                hoveredGraphics.endFill();
                this.updateTextureCoordinates(
                    {
                        x: Math.max(viewport.project.brushBP2pixel(block.startIndex) - pixelsInBp / 2, -viewport.canvasSize),
                        y: position.y + this.config.bed.margin + this.config.bed.height / 2
                    });
                if (block.hasOwnProperty('strand')) {
                    drawStrandDirection(
                        block.strand,
                        {
                            centerY: position.y + this.config.bed.margin + this.config.bed.height / 2,
                            height: this.config.bed.height,
                            width: Math.min(viewport.project.brushBP2pixel(block.endIndex) + pixelsInBp / 2
                                - Math.max(viewport.project.brushBP2pixel(block.startIndex), -viewport.canvasSize),
                                maxViewportsOnScreen * viewport.canvasSize),
                            x: Math.max(viewport.project.brushBP2pixel(block.startIndex) - pixelsInBp / 2,
                                -viewport.canvasSize)
                        },
                        graphics,
                        color,
                        this.config.bed.strand.arrow,
                        1,
                        ::this.updateTextureCoordinates
                    );
                    drawStrandDirection(
                        block.strand,
                        {
                            centerY: position.y + this.config.bed.margin + this.config.bed.height / 2,
                            height: this.config.bed.height,
                            width: Math.min(viewport.project.brushBP2pixel(block.endIndex) + pixelsInBp / 2
                                - Math.max(viewport.project.brushBP2pixel(block.startIndex), -viewport.canvasSize),
                                maxViewportsOnScreen * viewport.canvasSize),
                            x: Math.max(viewport.project.brushBP2pixel(block.startIndex) - pixelsInBp / 2,
                                -viewport.canvasSize)
                        },
                        hoveredGraphics,
                        ColorProcessor.darkenColor(color),
                        this.config.bed.strand.arrow,
                        1,
                        ::this.updateTextureCoordinates
                    );
                }
            }
            else {
                graphics.beginFill(color, 1);
                graphics.lineStyle(0, color, 0);
                const start = Math.max(viewport.project.brushBP2pixel(block.startIndex) - pixelsInBp / 2, -viewport.canvasSize);
                const end = Math.min(viewport.project.brushBP2pixel(block.endIndex) + pixelsInBp / 2, 2 * viewport.canvasSize);
                graphics.drawRect(
                    start,
                    position.y + this.config.bed.margin,
                    Math.max(1, end - start),
                    this.config.bed.height
                );
                graphics.endFill();

                hoveredGraphics.beginFill(ColorProcessor.darkenColor(color), 1);
                hoveredGraphics.lineStyle(0, ColorProcessor.darkenColor(color), 0);
                hoveredGraphics.drawRect(
                    start,
                    position.y + this.config.bed.margin,
                    Math.max(1, end - start),
                    this.config.bed.height
                );
                hoveredGraphics.endFill();
                this.updateTextureCoordinates(
                    {
                        x: start,
                        y: position.y + this.config.bed.margin
                    });
                if (block.hasOwnProperty('strand')) {
                    const white = 0xFFFFFF;
                    drawStrandDirection(
                        block.strand,
                        {
                            centerY: position.y + this.config.bed.margin + this.config.bed.height / 2,
                            height: this.config.bed.height,
                            width: Math.min(viewport.project.brushBP2pixel(block.endIndex) + pixelsInBp / 2
                                - Math.max(viewport.project.brushBP2pixel(block.startIndex), -viewport.canvasSize),
                                maxViewportsOnScreen * viewport.canvasSize),
                            x: Math.max(viewport.project.brushBP2pixel(block.startIndex) - pixelsInBp / 2,
                                -viewport.canvasSize)
                        },
                        graphics,
                        white,
                        this.config.bed.strand.arrow,
                        1,
                        ::this.updateTextureCoordinates
                    );
                    drawStrandDirection(
                        block.strand,
                        {
                            centerY: position.y + this.config.bed.margin + this.config.bed.height / 2,
                            height: this.config.bed.height,
                            width: Math.min(viewport.project.brushBP2pixel(block.endIndex) + pixelsInBp / 2
                                - Math.max(viewport.project.brushBP2pixel(block.startIndex), -viewport.canvasSize),
                                maxViewportsOnScreen * viewport.canvasSize),
                            x: Math.max(viewport.project.brushBP2pixel(block.startIndex) - pixelsInBp / 2,
                                -viewport.canvasSize)
                        },
                        hoveredGraphics,
                        white,
                        this.config.bed.strand.arrow,
                        1,
                        ::this.updateTextureCoordinates
                    );
                }
            }
        }
    }
}