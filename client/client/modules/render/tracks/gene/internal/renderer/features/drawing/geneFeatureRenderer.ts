import FeatureBaseRenderer from './featureBaseRenderer';
import PIXI from 'pixi.js';
import {ColorProcessor, PixiTextSize} from '../../../../../../utilities';
import TranscriptFeatureRenderer from './transcriptFeatureRenderer';
import drawStrandDirection from './strandDrawing';
import {drawingConfiguration} from '../../../../../../core';

const Math = window.Math;

export default class GeneFeatureRenderer extends FeatureBaseRenderer {

    _transcriptFeatureRenderer: TranscriptFeatureRenderer = null;

    constructor(config, registerLabel, registerDockableElement, registerFeaturePosition, transcriptRenderer) {
        super(config, registerLabel, registerDockableElement, registerFeaturePosition);
        this._transcriptFeatureRenderer = transcriptRenderer;
    }

    analyzeBoundaries(feature, viewport) {
        const boundaries = super.analyzeBoundaries(feature, viewport);
        const rect = boundaries.rect;
        const width = Math.max(1, rect.x2 - rect.x1);
        const shouldDisplayDetails = width >= this.config.gene.displayDetailsThreshold;
        let maxLabelWidth = {height: 0, width: 0};
        const gene = this.config.gene;
        if (feature.name !== null) {
            maxLabelWidth = PixiTextSize.getTextSize(feature.name, gene.label);
        }
        if (rect) {
            let transcriptsHeight = 0;
            const transcripts = feature.transcripts;
            const transcriptLength = transcripts.length;

            if (transcriptLength > 0 && shouldDisplayDetails) {
                for (let i = 0; i < transcriptLength; i++) {
                    this._transcriptFeatureRenderer.gffShowNumbersAminoacid = this._opts.gffShowNumbersAminoacid;
                    this._transcriptFeatureRenderer.collapsedMode = this._opts.collapsedMode;
                    const childBoundaries = this._transcriptFeatureRenderer.analyzeBoundaries(transcripts[i], viewport);
                    if (childBoundaries) {
                        const childRect = childBoundaries.rect;
                        const childSize = {
                            height: childRect.y2 - childRect.y1,
                            width: childRect.x2 - childRect.x1
                        };
                        maxLabelWidth.width = Math.max(maxLabelWidth.width, childSize.width);
                        transcriptsHeight += childSize.height;
                    }
                }
            }
            rect.x2 = Math.max(rect.x2, rect.x1 + maxLabelWidth.width);
            rect.y2 = gene.bar.height + maxLabelWidth.height + transcriptsHeight;
            if (transcriptLength > 0 && shouldDisplayDetails) {
                boundaries.margin = {
                    marginX: 10,
                    marginY: 10
                };
            }
        }
        return boundaries;
    }

    static hashCode(str) {
        let hash = 0;
        const length = str.length;
        const diff = 5;
        for (let i = 0; i < length; i++) {
            hash = str.charCodeAt(i) + ((hash << diff) - hash);
        }
        return hash;
    }

    static getFeatureFillColor(featureName, opts, config) {
        if (featureName !== 'gene' && opts && opts.gffColorByFeatureType) {
            const colorMask = 0x00FFFFFF;
            return GeneFeatureRenderer.hashCode(featureName) & colorMask
        }
        return config.gene.bar.fill;
    }

    render(feature, viewport, graphics, hoveredGraphics, labelContainer, dockableElementsContainer, attachedElementsContainer, position) {
        super.render(feature, viewport, graphics, hoveredGraphics, labelContainer, dockableElementsContainer, attachedElementsContainer, position);
        const featurePxStart = Math.max(viewport.project.brushBP2pixel(feature.startIndex), - viewport.canvasSize);
        const featurePxEnd = Math.min(viewport.project.brushBP2pixel(feature.endIndex + 1), 2 * viewport.canvasSize);
        const width = featurePxEnd - featurePxStart;
        if (width < 0) {
            return;
        }
        const pixelsInBp = viewport.factor;
        const shouldDisplayDetails = width >= this.config.gene.displayDetailsThreshold;

        let geneNameLabelHeight = 0;
        const gene = this.config.gene;

        if (feature.name !== null) {
            const label = new PIXI.Text(feature.name, this.config.gene.label);
            label.resolution = drawingConfiguration.resolution;
            label.x = Math.round(position.x);
            label.y = Math.round(position.y);
            dockableElementsContainer.addChild(label);
            geneNameLabelHeight = label.height;
            this.registerLabel(label, position, {
                end: feature.endIndex,
                height: position.height - this.config.transcript.height - this.config.transcript.marginTop,
                start: feature.startIndex
            }, true);
        }

        const geneBar = gene.bar;
        const fillColor = GeneFeatureRenderer.getFeatureFillColor(feature.feature.toLowerCase(), this._opts, this.config);

        const transcripts = feature.transcripts;
        const transcriptLength = transcripts.length;
        if (transcriptLength === 0 || !shouldDisplayDetails) {
            const startX = Math.max(Math.round(position.x), -viewport.canvasSize);
            const endX = Math.min(Math.round(position.x + width), 2 * viewport.canvasSize);
            if (startX > endX) {
                return;
            }
            graphics
                .beginFill(fillColor, 1)
                .drawRect(
                    startX,
                    Math.round(position.y + geneNameLabelHeight),
                    endX - startX,
                    Math.round(geneBar.height)
                )
                .endFill();
            hoveredGraphics
                .beginFill(ColorProcessor.darkenColor(fillColor), 1)
                .drawRect(
                    startX,
                    Math.round(position.y + geneNameLabelHeight),
                    endX - startX,
                    Math.round(geneBar.height)
                )
                .endFill();
            this.updateTextureCoordinates(
                {
                    x: startX,
                    y: Math.round(position.y + geneNameLabelHeight)
                });
            if (feature.hasOwnProperty('strand')) {
                const project = viewport.project;
                const white = 0xFFFFFF;
                const maxViewportsOnScreen = 3;
                drawStrandDirection(
                    feature.strand,
                    {
                        centerY: position.y + geneNameLabelHeight + geneBar.height / 2,
                        height: geneBar.height,
                        width: Math.min(project.brushBP2pixel(feature.endIndex) + pixelsInBp / 2
                            - Math.max(project.brushBP2pixel(feature.startIndex), -viewport.canvasSize), maxViewportsOnScreen * viewport.canvasSize),
                        x: Math.max(project.brushBP2pixel(feature.startIndex) - pixelsInBp / 2, -viewport.canvasSize)
                    },
                    graphics,
                    white,
                    gene.strand.arrow,
                    1,
                    this.updateTextureCoordinates.bind(this)
                );
                drawStrandDirection(
                    feature.strand,
                    {
                        centerY: position.y + geneNameLabelHeight + geneBar.height / 2,
                        height: geneBar.height,
                        width: Math.min(project.brushBP2pixel(feature.endIndex) + pixelsInBp / 2
                            - Math.max(project.brushBP2pixel(feature.startIndex), -viewport.canvasSize), maxViewportsOnScreen * viewport.canvasSize),
                        x: Math.max(project.brushBP2pixel(feature.startIndex) - pixelsInBp / 2, -viewport.canvasSize)
                    },
                    hoveredGraphics,
                    white,
                    gene.strand.arrow,
                    1,
                    this.updateTextureCoordinates.bind(this)
                );
            }
            this.registerFeaturePosition(feature, {
                x1: position.x,
                x2: position.x + width,
                y1: position.y + geneNameLabelHeight,
                y2: position.y + geneNameLabelHeight + geneBar.height
            });
        }
        else {
            const dockableGraphics = new PIXI.Graphics();
            dockableGraphics
                .lineStyle(1, geneBar.callout, 1)
                .moveTo(0, Math.round(geneBar.height / 2))
                .lineTo(0, 0)
                .lineTo(Math.round(width), 0)
                .lineTo(Math.round(width), Math.round(geneBar.height / 2));
            dockableGraphics.lineStyle(0, geneBar.fill, 0);
            dockableElementsContainer.addChild(dockableGraphics);
            dockableGraphics.y = Math.round(position.y + geneNameLabelHeight);
            dockableGraphics.x = Math.round(position.x);

            const topMargin = geneNameLabelHeight + geneBar.height / 2;

            let transcriptY = position.y + topMargin;
            const transcript = this.config.transcript;

            for (let i = 0; i < transcriptLength; i++) {
                this._transcriptFeatureRenderer.render(transcripts[i], viewport, graphics, hoveredGraphics, labelContainer, dockableElementsContainer, attachedElementsContainer, {
                    x: position.x,
                    y: transcriptY
                });

                const transcriptAminoacidsFitsViewport = this._transcriptFeatureRenderer._aminoacidFeatureRenderer.aminoacidsFitsViewport(transcripts[i], viewport);

                if (transcripts[i].name !== null) {
                    const labelSize = PixiTextSize.getTextSize(transcripts[i].name, transcript.label);
                    transcriptY += labelSize.height + transcript.label.marginTop + transcript.height + transcript.marginTop + (transcriptAminoacidsFitsViewport ? this._transcriptFeatureRenderer._aminoacidFeatureRenderer._aminoacidNumberHeight : 0);
                }
                else {
                    transcriptY += transcript.height + transcript.marginTop + (transcriptAminoacidsFitsViewport ? this._transcriptFeatureRenderer._aminoacidFeatureRenderer._aminoacidNumberHeight : 0);
                }
            }

            this.registerDockableElement(dockableGraphics, {
                topMargin: geneNameLabelHeight,
                y1: Math.round(position.y + geneNameLabelHeight),
                y2: Math.round(position.y + position.height - transcript.height - transcript.marginTop)
            });

            this.registerFeaturePosition(feature, {
                x1: position.x,
                x2: position.x + width,
                y1: position.y + geneNameLabelHeight,
                y2: transcriptY
            }, {ignore: true});
        }
    }
}
