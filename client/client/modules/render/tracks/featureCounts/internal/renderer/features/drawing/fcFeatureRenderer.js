import FeatureBaseRenderer from '../../../../../gene/internal/renderer/features/drawing/featureBaseRenderer';
import PIXI from 'pixi.js';
import {ColorProcessor, PixiTextSize} from '../../../../../../utilities';
import drawStrandDirection, {getStrandArrowSize} from '../../../../../gene/internal/renderer/features/drawing/strandDrawing';
import {drawingConfiguration} from '../../../../../../core';

const Math = window.Math;

export default class FCFeatureRenderer extends FeatureBaseRenderer {
    constructor(track, config, registerLabel, registerDockableElement, registerFeaturePosition, registerAttachedElement) {
        super(config, registerLabel, registerDockableElement, registerFeaturePosition, registerAttachedElement);
        this.track = track;
    }

    get strandIndicatorConfig(): undefined {
        return this.config && this.config.gene && this.config.gene.strand
            ? this.config.gene.strand
            : super.strandIndicatorConfig;
    }

    getExonLayers (exons, viewport) {
        if (!exons || !exons.length) {
            return [];
        }
        const layers = [];
        const margin = 1;
        const getIntersections = (x1, x2) => layers.filter(layer =>
            layer.x2 - layer.x1 + Math.abs(x2 - x1) + margin > Math.max(layer.x2, x1, x2) - Math.min(layer.x1, x1, x2)
        );
        const getLayerIndex = (x1, x2) => {
            const intersections = new Set(
                getIntersections(x1, x2)
                    .map(intersection => intersection.layer)
                    .sort((a, b) => a - b)
            );
            const max = Math.max(...intersections, -1);
            for (let layer = 0; layer < max; layer++) {
                if (!intersections.has(layer)) {
                    return layer;
                }
            }
            return max + 1;
        };
        const result = [];
        for (let e = 0; e < exons.length; e++) {
            const exon = exons[e];
            const x1 = Math.floor(viewport.project.brushBP2pixel(exon.startIndex) - viewport.factor / 2.0);
            let x2 = Math.round(viewport.project.brushBP2pixel(exon.endIndex) + viewport.factor / 2.0);
            if (x2 - x1 < 1) {
                x2 = x1 + 1;
            }
            const layer = {
                x1,
                x2,
                layer: getLayerIndex(x1, x2)
            };
            layers.push(layer);
            result.push({
                exon,
                layer
            });
        }
        return result;
    }

    analyzeBoundaries(feature, viewport) {
        const boundaries = super.analyzeBoundaries(feature, viewport);
        let labelSize = {height: 0, width: 0};
        if (feature.name && feature.name !== '.') {
            labelSize = PixiTextSize.getTextSize(feature.name, this.config.gene.label);
        }
        if (boundaries.rect) {
            boundaries.rect.x2 = Math.max(boundaries.rect.x2, boundaries.rect.x1 + labelSize.width);
            boundaries.rect.y2 = this.config.gene.bar.height + labelSize.height;
        }
        if (feature.items && feature.items.length > 0) {
            const exons = feature.items.reduce((r, c) => ([...r, ...(c.items || [])]), []);
            const layers = this.getExonLayers(exons, viewport).map(info => info.layer);
            const maxLayer = Math.max(...(layers.map(layer => layer.layer)));
            const x1 = Math.min(...(layers.map(layer => layer.x1)));
            const x2 = Math.min(...(layers.map(layer => layer.x2)));
            boundaries.rect.x1 = Math.min(boundaries.rect.x1, x1);
            boundaries.rect.x2 = Math.max(boundaries.rect.x2, x2);
            boundaries.rect.y2 += (maxLayer + 1) * (this.config.transcript.height + this.config.transcript.marginTop);
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
            return FCFeatureRenderer.hashCode(featureName) & colorMask;
        }
        return config.gene.bar.fill;
    }

    render (
        feature,
        viewport,
        graphicsObj,
        labelContainer,
        dockableElementsContainer,
        attachedElementsContainer,
        position
    ) {
        super.render(
            feature,
            viewport,
            graphicsObj,
            labelContainer,
            dockableElementsContainer,
            attachedElementsContainer,
            position
        );
        const featurePxStart = Math.max(viewport.project.brushBP2pixel(feature.startIndex), -viewport.canvasSize);
        const featurePxEnd = Math.min(viewport.project.brushBP2pixel(feature.endIndex + 1), 2 * viewport.canvasSize);
        const width = featurePxEnd - featurePxStart;
        if (width < 0) {
            return;
        }
        const pixelsInBp = viewport.factor;
        const exons = (feature.items || []).reduce((r, c) => ([...r, ...(c.items || [])]), []);
        const shouldDisplayDetails = width >= this.config.gene.displayDetailsThreshold && exons.length > 0;

        let geneNameLabelHeight = 0;
        const gene = this.config.gene;

        if (feature.name) {
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
        const fillColor = FCFeatureRenderer.getFeatureFillColor(
            feature.feature.toLowerCase(),
            this._opts,
            this.config
        );

        if (!shouldDisplayDetails) {
            const startX = Math.max(Math.round(position.x), -viewport.canvasSize);
            const endX = Math.min(Math.round(position.x + width), 2 * viewport.canvasSize);
            if (startX > endX) {
                return;
            }
            this.updateTextureCoordinates(
                {
                    x: startX,
                    y: Math.round(position.y + geneNameLabelHeight)
                });
            if (
                feature.hasOwnProperty('strand') &&
                this.shouldRenderStrandIndicatorInsteadOfGraphics(startX, endX)
            ) {
                const correctedEnd = startX +
                    getStrandArrowSize(gene.strand.arrow.height).width +
                    gene.strand.arrow.margin * 4;
                const arrowConfig = {
                    ...gene.strand.arrow,
                    mode: 'fill',
                    height: gene.strand.arrow.height + 2 * gene.strand.arrow.margin
                };
                drawStrandDirection(
                    feature.strand,
                    {
                        centerY: position.y + geneNameLabelHeight + geneBar.height / 2,
                        height: geneBar.height,
                        width: correctedEnd - startX,
                        x: startX
                    },
                    graphicsObj.graphics,
                    fillColor,
                    arrowConfig,
                    1,
                    ::this.updateTextureCoordinates
                );
                drawStrandDirection(
                    feature.strand,
                    {
                        centerY: position.y + geneNameLabelHeight + geneBar.height / 2,
                        height: geneBar.height,
                        width: correctedEnd - startX,
                        x: startX
                    },
                    graphicsObj.hoveredGraphics,
                    ColorProcessor.darkenColor(fillColor),
                    arrowConfig,
                    1,
                    ::this.updateTextureCoordinates
                );
            } else {
                graphicsObj.graphics
                    .beginFill(fillColor, 1)
                    .drawRect(
                        startX,
                        Math.round(position.y + geneNameLabelHeight),
                        endX - startX,
                        Math.round(geneBar.height)
                    )
                    .endFill();
                graphicsObj.hoveredGraphics
                    .beginFill(ColorProcessor.darkenColor(fillColor), 1)
                    .drawRect(
                        startX,
                        Math.round(position.y + geneNameLabelHeight),
                        endX - startX,
                        Math.round(geneBar.height)
                    )
                    .endFill();
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
                        graphicsObj.graphics,
                        white,
                        gene.strand.arrow,
                        1,
                        ::this.updateTextureCoordinates
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
                        graphicsObj.hoveredGraphics,
                        white,
                        gene.strand.arrow,
                        1,
                        ::this.updateTextureCoordinates
                    );
                }
            }
            this.registerFeaturePosition(feature, {
                x1: position.x,
                x2: position.x + width,
                y1: position.y + geneNameLabelHeight,
                y2: position.y + geneNameLabelHeight + geneBar.height
            });
        } else {
            // should display details
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

            const transcriptY = position.y + topMargin;
            const transcript = this.config.transcript;
            const layers = this.getExonLayers(exons, viewport);
            for (let l = 0; l < layers.length; l++) {
                const {
                    exon,
                    layer
                } = layers[l];
                const {
                    x1,
                    x2,
                    layer: layerIndex
                } = layer;
                const y1 = transcriptY +
                    layerIndex * (this.config.transcript.height + this.config.transcript.marginTop) +
                    this.config.transcript.marginTop;
                const y2 = transcriptY +
                    (layerIndex + 1) * (this.config.transcript.height + this.config.transcript.marginTop);
                const fill = this.config.transcript.features.fill.cds;
                const hoveredFill = ColorProcessor.darkenColor(fill);
                const strandFill = this.config.transcript.features.strand.fill.cds;
                graphicsObj.graphics
                    .beginFill(fill, 1)
                    .drawRect(
                        x1,
                        y1,
                        x2 - x1,
                        this.config.transcript.height
                    )
                    .endFill();
                graphicsObj.hoveredGraphics
                    .beginFill(hoveredFill, 1)
                    .drawRect(
                        x1,
                        y1,
                        x2 - x1,
                        this.config.transcript.height
                    )
                    .endFill();
                if (exon.hasOwnProperty('strand')) {
                    const maxViewportsOnScreen = 3;
                    drawStrandDirection(
                        exon.strand,
                        {
                            centerY: (y1 + y2) / 2.0,
                            height: y2 - y1,
                            width: Math.min(x2 - x1, maxViewportsOnScreen * viewport.canvasSize),
                            x: Math.max(x1, -viewport.canvasSize)
                        },
                        graphicsObj.graphics,
                        strandFill,
                        this.config.transcript.strand.arrow,
                        1,
                        this.updateTextureCoordinates.bind(this)
                    );
                    drawStrandDirection(
                        exon.strand,
                        {
                            centerY: (y1 + y2) / 2.0,
                            height: y2 - y1,
                            width: Math.min(x2 - x1, maxViewportsOnScreen * viewport.canvasSize),
                            x: Math.max(x1, -viewport.canvasSize)
                        },
                        graphicsObj.hoveredGraphics,
                        strandFill,
                        this.config.transcript.strand.arrow,
                        1,
                        this.updateTextureCoordinates.bind(this)
                    );
                }
                this.updateTextureCoordinates(
                    {
                        x: x1,
                        y: y1
                    });
                this.registerFeaturePosition(exon, {
                    x1,
                    x2,
                    y1,
                    y2
                });
            }

            // for (let i = 0; i < transcriptLength; i++) {
            //     this._transcriptFeatureRenderer.render(transcripts[i], viewport, graphics, labelContainer, dockableElementsContainer, attachedElementsContainer, {
            //         x: position.x,
            //         y: transcriptY
            //     });
            //
            //     const transcriptAminoacidsFitsViewport = this._transcriptFeatureRenderer._aminoacidFeatureRenderer.aminoacidsFitsViewport(transcripts[i], viewport);
            //
            //     if (transcripts[i].name !== null) {
            //         const labelSize = PixiTextSize.getTextSize(transcripts[i].name, transcript.label);
            //         transcriptY += labelSize.height + transcript.label.marginTop + transcript.height + transcript.marginTop + (transcriptAminoacidsFitsViewport ? this._transcriptFeatureRenderer._aminoacidFeatureRenderer._aminoacidNumberHeight : 0);
            //     } else {
            //         transcriptY += transcript.height + transcript.marginTop + (transcriptAminoacidsFitsViewport ? this._transcriptFeatureRenderer._aminoacidFeatureRenderer._aminoacidNumberHeight : 0);
            //     }
            // }

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
            });
        }
    }
}
