import SVFeatureRenderer from './svFeatureRenderer';
import {ColorProcessor, PixiTextSize, NumberFormatter} from '../../../../../../../utilities';
const Math = window.Math;

export default class InterChromosomeFeatureRenderer extends SVFeatureRenderer {

    analyzeBoundaries(feature, viewport) {
        const [alternativeAlleleInfo] = feature.alternativeAllelesInfo.filter(x => x.mate);
        const displayText = `${alternativeAlleleInfo.mate.chromosome}:${NumberFormatter.formattedText(alternativeAlleleInfo.mate.position)}`;
        let style = this.config.variant.multipleNucleotideVariant.label.default;
        if (this.config.variant.multipleNucleotideVariant.label[feature.type.toLowerCase()]) {
            style = this.config.variant.multipleNucleotideVariant.label[feature.type.toLowerCase()];
        }
        const margin = 1;
        const textWidth = PixiTextSize.getTextSize(displayText, Object.assign({}, style)).width + this.config.variant.multipleNucleotideVariant.interChromosome.margin + 2 * margin;
        const boundaries = super.analyzeBoundaries(feature, viewport);
        if (!boundaries) {
            return null;
        }
        if (alternativeAlleleInfo.mate.attachedAt === 'left') {
            boundaries.rect.x1 = Math.min(boundaries.rect.x1, viewport.project.brushBP2pixel(feature.startIndex) - viewport.factor / 2 - textWidth);
        } else {
            boundaries.rect.x2 = Math.max(boundaries.rect.x2, viewport.project.brushBP2pixel(feature.startIndex) + viewport.factor / 2 + textWidth);
        }
        return boundaries;
    }

    render(feature, viewport, graphics, labelContainer, dockableElementsContainer, attachedElementsContainer, position) {
        const [alternativeAlleleInfo] = feature.alternativeAllelesInfo.filter(x => x.mate);
        const displayText = `${alternativeAlleleInfo.mate.chromosome}:${NumberFormatter.formattedText(alternativeAlleleInfo.mate.position)}`;
        let style = this.config.variant.multipleNucleotideVariant.label.default;
        if (this.config.variant.multipleNucleotideVariant.label[feature.type.toLowerCase()]) {
            style = this.config.variant.multipleNucleotideVariant.label[feature.type.toLowerCase()];
        }
        const label = this.labelsManager
            ? this.labelsManager.getLabel(displayText, Object.assign({}, style.font))
            : undefined;
        if (label) {
            const margin = 0;
            const textSize = {
                width: label.width,
                height: label.height
            };
            const height = this.config.variant.height;
            let cX1 = Math.round(Math.max(viewport.project.brushBP2pixel(feature.startIndex), -viewport.canvasSize));
            let cX2 = cX1 + textSize.width + this.config.variant.multipleNucleotideVariant.interChromosome.margin + 2 * margin;
            let calloutX = cX1 + this.config.variant.multipleNucleotideVariant.interChromosome.margin;
            const cY = Math.round(position.y + position.height - height / 2);
            let shift = this.config.variant.multipleNucleotideVariant.interChromosome.margin + margin;
            let shiftDirection = 0;
            if (alternativeAlleleInfo.mate.attachedAt === 'left') {
                cX2 = Math.round(Math.max(viewport.project.brushBP2pixel(feature.startIndex), -viewport.canvasSize));
                cX1 = cX2 - textSize.width - this.config.variant.multipleNucleotideVariant.interChromosome.margin - 2 * margin;
                calloutX = cX1;
                shift = -this.config.variant.multipleNucleotideVariant.interChromosome.margin - margin;
                shiftDirection = -1;
            }
            graphics.graphics
                .lineStyle(this.config.variant.multipleNucleotideVariant.thickness,
                    this.config.variant.multipleNucleotideVariant.color,
                    this.config.variant.multipleNucleotideVariant.alpha)
                .moveTo(cX1, cY - this.config.variant.multipleNucleotideVariant.thickness / 2)
                .lineTo(cX2, cY - this.config.variant.multipleNucleotideVariant.thickness / 2);

            graphics.graphics
                .lineStyle(0, 0x000000, 0)
                .beginFill(style.fill, 1)
                .drawRoundedRect(calloutX, cY - textSize.height / 2 - margin, textSize.width + 2 * margin, textSize.height + 2 * margin,
                    (textSize.height + 2 * margin) / 2)
                .endFill();

            graphics.hoveredGraphics
                .lineStyle(this.config.variant.multipleNucleotideVariant.thickness,
                    ColorProcessor.darkenColor(this.config.variant.multipleNucleotideVariant.color),
                    this.config.variant.multipleNucleotideVariant.alpha)
                .moveTo(cX1, cY - this.config.variant.multipleNucleotideVariant.thickness / 2)
                .lineTo(cX2, cY - this.config.variant.multipleNucleotideVariant.thickness / 2);

            graphics.hoveredGraphics
                .lineStyle(0, 0x000000, 0)
                .beginFill(ColorProcessor.darkenColor(style.fill), 1)
                .drawRoundedRect(calloutX, cY - textSize.height / 2 - margin, textSize.width + 2 * margin, textSize.height + 2 * margin,
                    (textSize.height + 2 * margin) / 2)
                .endFill();

            const labelPosition = {
                x: cX1 + margin,
                y: cY - label.height / 2
            };
            label.x = Math.round(labelPosition.x);
            label.y = Math.round(labelPosition.y);
            labelContainer.addChild(label);

            this.registerLabel(
                label,
                labelPosition,
                {
                    end: feature.startIndex,
                    start: feature.startIndex,
                    shift,
                    shiftDirection
                },
                false,
                false);

            this.updateTextureCoordinates({
                x: cX1 - this.config.variant.multipleNucleotideVariant.thickness / 2,
                y: cY - textSize.height / 2
            });
        }
        super.render(feature, viewport, graphics, labelContainer, dockableElementsContainer, attachedElementsContainer, position);
    }

    registerFeature(feature, viewport, position) {
        const labelStyle = this.config.variant.allele.label;
        const textSize = PixiTextSize.getTextSize(this.getFeatureDisplayText(feature), labelStyle);
        const pixelsInBp = viewport.factor;
        const textX1 = Math.max(viewport.project.brushBP2pixel(feature.startIndex), -viewport.canvasSize) - pixelsInBp / 2 - textSize.width / 2;
        const textX2 = textX1 + textSize.width;
        this.registerFeaturePosition(feature, {
            x1: Math.min(position.x, textX1),
            x2: Math.max(position.x + position.width, textX2),
            y1: position.y,
            y2: position.y + position.height
        });
    }
}
