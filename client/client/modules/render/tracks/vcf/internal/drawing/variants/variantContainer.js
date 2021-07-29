import * as PIXI from 'pixi.js';
import {drawDashLine, drawZygosityBar} from './internalDrawing';
import {NumberFormatter} from '../../../../../utilities';
import {VariantBaseContainer} from './baseContainer';
import VcfAnalyzer from '../../../../../../../dataServices/vcf/vcf-analyzer';
import {drawingConfiguration} from '../../../../../core';
const Math = window.Math;

export class VariantContainer extends VariantBaseContainer {
    _endContainers = [];
    _tooltipContainer = null;
    _tooltipArea = null;
    _tooltipPosition: PIXI.Point = null;
    _hasTooltip = false;

    get endContainers() {
        return this._endContainers;
    }

    constructor(variant, config, tooltipContainer) {
        super(variant, config);
        this._tooltipParentContainer = tooltipContainer;
    }

    render(viewport, manager) {
        super.render(viewport, manager);
        this.container.x = Math.round(viewport.project.brushBP2pixel(this._variant.startIndex));
        if (!this._componentIsBuilt) {
            this.buildComponent(viewport, manager);
        }
        this.manageEndLabels(viewport, manager);
        this.linesGraphics.x = this.container.x;
        this.linesGraphics.y = this.container.y;
        if (this._hasTooltip && this._tooltipPosition) {
            this._tooltipContainer.x = this._tooltipPosition.x + this.container.x;
            this._tooltipContainer.y = this._tooltipPosition.y + this.container.y;
        }
    }

    manageEndLabels(viewport, manager) {
        for (let i = 0; i < this.endContainers.length; i++) {
            const endContainer = this.endContainers[i];
            if (endContainer.isIntraChromosome) {
                if (endContainer.range.startIndex > viewport.brush.end || endContainer.range.endIndex
                    < viewport.brush.start) {
                    endContainer.visible = false;
                }
                else {
                    endContainer.visible = !(endContainer.position >= viewport.brush.start &&
                    endContainer.position <= viewport.brush.end);
                }
            }
            else {
                endContainer.visible = !(endContainer.position <= viewport.brush.start &&
                endContainer.position >= viewport.brush.end);
            }
            if (endContainer.visible) {
                const xPosition = Math.max(0, Math.min(viewport.canvasSize,
                        viewport.project.brushBP2pixel(endContainer.position))) - this.container.x;
                let containerArea = {
                    global: {
                        x: this.container.x,
                        y: this.container.y
                    },
                    rect: {
                        x1: Math.min(xPosition + endContainer.alignmentDirection * endContainer.size.width, xPosition),
                        x2: Math.max(xPosition + endContainer.alignmentDirection * endContainer.size.width, xPosition),
                        y1: endContainer.layer - endContainer.size.height / 2,
                        y2: endContainer.layer + endContainer.size.height / 2
                    }
                };
                containerArea =
                    manager.checkArea('ends_containers', containerArea,
                        {translateX: endContainer.alignmentDirection, translateY: 0});
                if (containerArea.conflicts) {
                    endContainer.visible = false;
                }
                else {
                    endContainer.container.x =
                        Math.round((containerArea.rect.x1 + containerArea.rect.x2) / 2 - endContainer.alignmentDirection
                            * endContainer.size.width / 2);
                    endContainer.area = containerArea.rect;
                    manager.submitArea('ends_containers', containerArea);
                }

                const intersects = manager.checkArea('default', containerArea, {translateX: 0, translateY: 0});
                endContainer.intersects = intersects.conflicts;
                if (endContainer.intersects) {
                    endContainer.container.alpha = 0.1;
                }
                else {
                    endContainer.container.alpha = 1;
                }
            }
            endContainer.container.visible = endContainer.visible;
        }
    }

    buildComponent(viewport, manager) {
        this.buildVariantTypeLabel(manager);
        this.buildVariantAlternativeAlleles(viewport, manager);
        const barConfig = drawZygosityBar(this._variant.zygosity, this._graphics, this._config.variant, this._bpLength);
        const globalConfig = {
            global: {
                x: this.container.x,
                y: this.container.y
            }
        };
        manager.submitArea('default', Object.assign(barConfig, globalConfig));
        if (this._hasTooltip && this._tooltipParentContainer) {
            this._tooltipParentContainer.addChild(this._tooltipContainer);
        }
        super.buildComponent(viewport, manager);
    }

    buildVariantTypeLabel(manager) {
        if (this._variant.symbol) {
            const label = new PIXI.Text(this._variant.symbol, this._config.variant.allele.defaultLabel);
            label.resolution = drawingConfiguration.resolution;
            label.y = Math.round(-this._config.variant.height - this._config.variant.allele.margin - label.height);
            label.x = -Math.round(label.width / 2);
            this._container.addChild(label);
            manager.submitArea('default', {
                global: {
                    x: this.container.x,
                    y: this.container.y
                },
                rect: {
                    x1: label.x,
                    x2: label.x + label.width,
                    y1: label.y,
                    y2: label.y + label.height
                }
            });
        }
    }

    buildVariantAlternativeAlleles(viewport, manager) {
        this.buildAlternativeAllelesLabels(manager);
        this.buildMultipleNucleotideRegions(viewport, manager);
        for (let i = 0; i < this._variant.alternativeAllelesInfo.length; i++) {
            const alternativeAlleleInfo = this._variant.alternativeAllelesInfo[i];
            if (alternativeAlleleInfo.mate && !alternativeAlleleInfo.mate.intraChromosome) {
                this.buildInterChromosomeLinks(manager, alternativeAlleleInfo);
            }
        }
    }

    buildAlternativeAllelesLabels(manager) {
        const alternativeAllelesLabelsRect = {
            global: {
                x: this.container.x,
                y: this.container.y
            },
            rect: {
                x1: -this._variant.allelesDescriptionsWidth / 2
                - this._config.variant.allele.intersection.horizontalMargin,
                x2: this._variant.allelesDescriptionsWidth / 2
                + this._config.variant.allele.intersection.horizontalMargin,
                y1: -this._config.variant.height - this._variant.allelesDescriptionsHeight,
                y2: -this._config.variant.height
            }
        };
        const allelesArea = manager.checkArea('default', alternativeAllelesLabelsRect, {translateX: 0, translateY: -1});
        if (!allelesArea.conflicts) {
            manager.submitArea('default', allelesArea);
        }
        else {
            const white = 0xFFFFFF;
            this._hasTooltip = true;
            const tooltipGraphics = new PIXI.Graphics();
            tooltipGraphics.x = -this._variant.allelesDescriptionsWidth / 2;
            const borderThickness = 1;
            tooltipGraphics
                .lineStyle(borderThickness, 0x000000, 1)
                .beginFill(white, 1)
                .drawRect(
                    borderThickness / 2,
                    borderThickness / 2,
                    this._variant.allelesDescriptionsWidth - borderThickness,
                    this._variant.allelesDescriptionsHeight - borderThickness)
                .endFill();
            this._tooltipContainer = new PIXI.Container();
            this._tooltipContainer.addChild(tooltipGraphics);
        }
        const allelesContainer = allelesArea.conflicts ? this._tooltipContainer : this._container;
        let y = allelesArea.conflicts ? 0 : allelesArea.rect.y1;
        for (let i = 0; i < this._variant.alternativeAllelesInfo.length; i++) {
            if (!this._variant.alternativeAllelesInfo[i].displayText) {
                continue;
            }
            const label = new PIXI.Text(this._variant.alternativeAllelesInfo[i].displayText,
                this._config.variant.allele.label);
            label.resolution = drawingConfiguration.resolution;
            label.x = -Math.round(label.width / 2);
            label.y = Math.round(y);
            allelesContainer.addChild(label);
            y += label.height + this._config.variant.allele.margin;
        }
        const line = {
            end: {
                x: (allelesArea.rect.x1 + allelesArea.rect.x2) / 2,
                y: allelesArea.rect.y2
            },
            start: {
                x: 0,
                y: -this._config.variant.height,
            }
        };
        if (allelesArea.conflicts) {
            const tooltipZone = manager.getZoneBoundaries('tooltip', {x: this.container.x, y: this.container.y});
            if (tooltipZone) {
                const tooltipLabel = new PIXI.Text('...', this._config.variant.allele.detailsTooltipLabel);
                tooltipLabel.resolution = drawingConfiguration.resolution;
                const tooltipRect = {
                    global: {
                        x: this.container.x,
                        y: this.container.y
                    },
                    rect: {
                        x1: -tooltipLabel.width / 2,
                        x2: tooltipLabel.width / 2,
                        y1: (tooltipZone.y1 + tooltipZone.y2) / 2 - tooltipLabel.height / 2,
                        y2: (tooltipZone.y1 + tooltipZone.y2) / 2 + tooltipLabel.height / 2
                    }
                };
                this._tooltipArea = manager.checkArea('tooltip', tooltipRect, {translateX: 1, translateY: 0});
                if (!this._tooltipArea.conflicts) {
                    manager.submitArea('tooltip', this._tooltipArea);
                    tooltipLabel.x =
                        Math.round((this._tooltipArea.rect.x1 + this._tooltipArea.rect.x2) / 2 - tooltipLabel.width
                            / 2);
                    tooltipLabel.y = Math.round(this._tooltipArea.rect.y2 - tooltipLabel.height);

                    this._container.addChild(tooltipLabel);

                    line.end.x = (this._tooltipArea.rect.x1 + this._tooltipArea.rect.x2) / 2;
                    line.end.y = this._tooltipArea.rect.y2;
                    this._tooltipPosition = new PIXI.Point(tooltipZone.x1, tooltipZone.y1);
                }
            }
        }
        drawDashLine(
            this.linesGraphics, 2,
            {
                xStart: line.start.x,
                yStart: line.start.y
            },
            {
                xEnd: line.end.x,
                yEnd: line.end.y
            },
            {
                stroke: 0x000000,
                thickness: 1
            });
    }

    buildMultipleNucleotideRegions(viewport, manager) {
        if (this._variant.isDefaultPositioning) {
            this.buildMultipleNucleotideRegion(viewport, manager, this._variant.positioningInfos[0], null);
        }
        else {
            for (let i = 0; i < this._variant.alternativeAllelesInfo.length; i++) {
                const alternativeAlleleInfo = this._variant.alternativeAllelesInfo[i];
                if (
                    alternativeAlleleInfo.positioningInfoIndex !== undefined &&
                    alternativeAlleleInfo.positioningInfoIndex !== null &&
                    alternativeAlleleInfo.positioningInfoIndex >= 0 &&
                    alternativeAlleleInfo.positioningInfoIndex < this._variant.positioningInfos.length
                ) {
                    this.buildMultipleNucleotideRegion(viewport, manager,
                        this._variant.positioningInfos[alternativeAlleleInfo.positioningInfoIndex],
                        alternativeAlleleInfo);
                }
            }
        }
    }

    buildMultipleNucleotideRegion(viewport, manager, region, alternativeAllele) {
        if (region.endIndex - region.startIndex + 1 <= 1) {
            return;
        }
        const x1 = viewport.project.brushBP2pixel(region.startIndex)
            - viewport.project.brushBP2pixel(this._variant.startIndex);
        const x2 = viewport.project.brushBP2pixel(region.endIndex)
            - viewport.project.brushBP2pixel(this._variant.startIndex);
        const boundaries = manager.getZoneBoundaries('default', {x: this.container.x, y: this.container.y});
        let layerHeight = Math.abs(boundaries.y2 - boundaries.y1) / (region.maxLayerIndex + 2);
        if (region.maxLayerIndex === 0 && region.layerIndex === 0 && region.variantsUnderCount === 0) {
            layerHeight = this._config.variant.height / 2;
        }
        const y = boundaries.y2 - (region.maxLayerIndex - region.layerIndex + 1) * layerHeight;
        const offsetLength = this._config.variant.multipleNucleotideVariant.offsetLength;
        const white = 0xFFFFFF;
        this._linesGraphics
            .beginFill(white, 0)
            .lineStyle(this._config.variant.multipleNucleotideVariant.thickness,
                this._config.variant.multipleNucleotideVariant.color,
                this._config.variant.multipleNucleotideVariant.alpha)
            .moveTo(x1 - this._bpLength / 2, 0)
            .lineTo(x1 - this._bpLength / 2, y - offsetLength)
            .moveTo(x1 - this._bpLength / 2, y)
            .lineTo(x2 + this._bpLength / 2, y)
            .moveTo(x2 + this._bpLength / 2, 0)
            .lineTo(x2 + this._bpLength / 2, y - offsetLength);
        if (alternativeAllele && alternativeAllele.mate) {
            let alignment = 'right';
            let startAlignment = 'left';
            let startPosition = region.endIndex;
            if (alternativeAllele.mate.position > region.startIndex) {
                alignment = 'left';
                startAlignment = 'right';
                startPosition = region.startIndex;
            }
            this.buildVariantEndLabel({
                alignment: alignment,
                behaviour: 'end',
                chromosome: null,
                isIntraChromosome: true,
                layer: y,
                position: alternativeAllele.mate.position,
                range: region,
                symbol: null
            });
            this.buildVariantEndLabel({
                alignment: startAlignment,
                behaviour: 'start',
                chromosome: null,
                isIntraChromosome: true,
                layer: y,
                position: startPosition,
                range: region,
                symbol: this._variant.symbol || this._variant.structuralSymbol
            });
        }
        else {
            this.buildVariantEndLabel({
                alignment: 'right',
                behaviour: 'start',
                chromosome: null,
                isIntraChromosome: true,
                layer: y,
                position: region.startIndex,
                range: {endIndex: region.endIndex, startIndex: region.startIndex},
                symbol: this._variant.symbol || this._variant.structuralSymbol
            });
            this.buildVariantEndLabel({
                alignment: 'left',
                behaviour: 'end',
                chromosome: null,
                isIntraChromosome: true,
                layer: y,
                position: region.endIndex,
                range: {endIndex: region.endIndex, startIndex: region.startIndex},
                symbol: null
            });
        }
    }

    buildInterChromosomeLinks(manager, alternativeAlleleInfo) {
        const {mate, info} = alternativeAlleleInfo;
        const {chromosome, position, attachedAt} = mate;
        const {type, sequence} = info;
        const boundaries = manager.getZoneBoundaries('default', {x: this.container.x, y: this.container.y});
        const y = boundaries.y1 + this._config.variant.multipleNucleotideVariant.interChromosome.line.margin;
        let prefix = null;
        let postfix = null;
        if (sequence) {
            let extraSequence = VcfAnalyzer.concatenateAlternativeAlleleDescription(sequence);
            if (type.toLowerCase() === 'ins') {
                extraSequence = `+${extraSequence}`;
            }
            else if (type.toLowerCase() === 'del') {
                extraSequence = `${'\u2014'}${extraSequence}`;
            }
            if (attachedAt === 'right') {
                prefix = extraSequence;
            }
            else {
                postfix = extraSequence;
            }
        }
        const white = 0xFFFFFF;
        this._linesGraphics
            .beginFill(white, 0)
            .lineStyle(1, this._config.variant.multipleNucleotideVariant.interChromosome.line.color, 1)
            .moveTo(0, -this._config.variant.height)
            .lineTo(0, y);
        this.buildVariantEndLabel({
            alignment: attachedAt,
            behaviour: 'end',
            chromosome: chromosome,
            displayPosition: position,
            isIntraChromosome: false,
            layer: y,
            position: this._variant.startIndex,
            postfix: postfix,
            prefix: prefix,
            symbol: null
        });
    }

    buildVariantEndLabel(info) {
        const {layer, alignment, chromosome, position, symbol, displayPosition, prefix, postfix} = info;
        const container = new PIXI.Container();

        const positionText = (displayPosition !== null && displayPosition !== undefined)
            ? NumberFormatter.formattedText(parseInt(displayPosition))
            : NumberFormatter.formattedText(parseInt(position));
        const chromosomeText = chromosome ? `${chromosome}:` : '';
        const text = `${prefix || ''} ${symbol || ''} ${chromosomeText}${positionText} ${postfix || ''}`.trim();
        let variantStyle = this._config.variant.multipleNucleotideVariant.label[this._variant.type.toLowerCase()];
        if (!variantStyle) {
            variantStyle = this._config.variant.multipleNucleotideVariant.label.default;
        }
        const label = new PIXI.Text(text, variantStyle.font);
        label.resolution = drawingConfiguration.resolution;
        let dX = 0;
        if (alignment === 'left') {
            dX = -label.width;
        }
        const background = new PIXI.Graphics();
        const margin = 1;
        background
            .beginFill(variantStyle.fill, 1)
            .drawRoundedRect(dX, -label.height / 2, label.width + 2 * margin, label.height + 2 * margin,
                (label.height + 2 * margin) / 2)
            .endFill();
        container.addChild(background);
        container.addChild(label);
        label.x = Math.round(margin + dX);
        label.y = Math.round(-label.height / 2);
        container.y = Math.round(layer);
        const endContainer = Object.assign(
            {
                alignmentDirection: alignment === 'left' ? -1 : 1,
                conflicts: false,
                container: container,
                size: {
                    height: label.height,
                    width: label.width
                },
                visible: false
            },
            info);
        this._container.addChild(container);
        this.endContainers.push(endContainer);
    }

    unhover() {
        super.unhover();
        for (let i = 0; i < this._endContainers.length; i++) {
            const endContainer = this._endContainers[i];
            if (endContainer.visible) {
                const intersectsAlpha = 0.1;
                endContainer.container.alpha = endContainer.intersects ? intersectsAlpha : 1;
            }
        }
    }

    hover() {
        super.hover();
        for (let i = 0; i < this._endContainers.length; i++) {
            const endContainer = this._endContainers[i];
            if (endContainer.visible) {
                endContainer.container.alpha = 1;
            }
        }
    }

    isHovers(cursor) {
        if (!cursor) {
            return false;
        }
        if (super.isHovers(cursor)) {
            return true;
        }
        const {x, y} = cursor;
        const containerBounds = this.container.getBounds();
        const graphicsBounds = this._graphics.getBounds();
        const linesGraphicsBounds = this.linesGraphics.getBounds();
        return containerBounds.contains(x, y) || graphicsBounds.contains(x, y) || linesGraphicsBounds.contains(x, y);
    }

    animate(time) {
        const needAnimateFade = super.animate(time);
        if (this._tooltipContainer) {
            this._tooltipContainer.visible = this._isHovered && this._hasTooltip;
            return needAnimateFade || (this._isHovered && this._hasTooltip !== this._tooltipContainer.visible);
        }
        return needAnimateFade;
    }
}
