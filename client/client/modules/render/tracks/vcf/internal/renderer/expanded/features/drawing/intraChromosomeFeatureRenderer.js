import SVFeatureRenderer from './svFeatureRenderer';
import {ColorProcessor, PixiTextSize, NumberFormatter} from '../../../../../../../utilities';
const Math = window.Math;

export default class IntraChromosomeFeatureRenderer extends SVFeatureRenderer {

    analyzeBoundaries(feature, viewport) {
        let startIndex = feature.startIndex;
        let endIndex = feature.endIndex;
        const [alternativeAlleleInfo] = feature.alternativeAllelesInfo.filter(x => x.mate);
        if (alternativeAlleleInfo) {
            startIndex = Math.min(feature.startIndex, alternativeAlleleInfo.mate.position);
            endIndex = Math.max(feature.startIndex, alternativeAlleleInfo.mate.position);
        }
        const boundaries = super.analyzeBoundaries(feature, viewport);
        if (!boundaries) {
            return null;
        }
        boundaries.rect.x1 = Math.min(boundaries.rect.x1, viewport.project.brushBP2pixel(startIndex) - viewport.factor / 2);
        boundaries.rect.x2 = Math.max(boundaries.rect.x2, viewport.project.brushBP2pixel(endIndex) + viewport.factor / 2);
        return boundaries;
    }

    render(feature, viewport, graphics, labelContainer, dockableElementsContainer, attachedElementsContainer, position) {
        let startIndex = feature.startIndex;
        let endIndex = feature.endIndex;
        const [alternativeAlleleInfo] = feature.alternativeAllelesInfo.filter(x => x.mate);
        if (alternativeAlleleInfo) {
            startIndex = Math.min(feature.startIndex, alternativeAlleleInfo.mate.position);
            endIndex = Math.max(feature.startIndex, alternativeAlleleInfo.mate.position);
        }
        if (startIndex < endIndex) {
            const height = this.config.variant.height;
            const cX1 = Math.round(Math.max(viewport.project.brushBP2pixel(startIndex), -viewport.canvasSize) - viewport.factor / 2);
            const cX2 = Math.round(Math.min(viewport.project.brushBP2pixel(endIndex), 2 * viewport.canvasSize) + viewport.factor / 2);
            const cY = Math.round(position.y + position.height - height / 2);
            graphics.graphics
                .lineStyle(this.config.variant.multipleNucleotideVariant.thickness,
                    this.config.variant.multipleNucleotideVariant.color,
                    this.config.variant.multipleNucleotideVariant.alpha);
            graphics.hoveredGraphics
                .lineStyle(this.config.variant.multipleNucleotideVariant.thickness,
                    ColorProcessor.darkenColor(this.config.variant.multipleNucleotideVariant.color),
                    this.config.variant.multipleNucleotideVariant.alpha);
            if (feature.startIndex !== startIndex) {
                graphics.graphics
                    .moveTo(cX1 - this.config.variant.multipleNucleotideVariant.thickness / 2, cY - height / 3)
                    .lineTo(cX1 - this.config.variant.multipleNucleotideVariant.thickness / 2, cY + height / 3);
            }
            graphics.graphics
                .moveTo(cX1, cY - this.config.variant.multipleNucleotideVariant.thickness / 2)
                .lineTo(cX2, cY - this.config.variant.multipleNucleotideVariant.thickness / 2);
            if (feature.startIndex !== endIndex) {
                graphics.graphics
                    .moveTo(cX2 - this.config.variant.multipleNucleotideVariant.thickness / 2, cY - height / 3)
                    .lineTo(cX2 - this.config.variant.multipleNucleotideVariant.thickness / 2, cY + height / 3);
            }
            let style = this.config.variant.multipleNucleotideVariant.label.default;
            if (this.config.variant.multipleNucleotideVariant.label[feature.type.toLowerCase()]) {
                style = this.config.variant.multipleNucleotideVariant.label[feature.type.toLowerCase()];
            }
            this.generateAttachedElement({
                attachedAt: 'right',
                color: feature.highlightColor,
                hideOnVisible: true,
                position: position.y + position.height - height / 2,
                range: {
                    start: startIndex,
                    end: endIndex
                }
            }, `${this.getFeatureDisplayText(feature)} ${NumberFormatter.formattedText(startIndex)}`, style, attachedElementsContainer);
            this.generateAttachedElement    ({
                attachedAt: 'left',
                color: feature.highlightColor,
                hideOnVisible: true,
                position: position.y + position.height - height / 2,
                range: {
                    start: startIndex,
                    end: endIndex
                }
            }, `${this.getFeatureDisplayText(feature)} ${NumberFormatter.formattedText(endIndex)}`, style, attachedElementsContainer);
            this.updateTextureCoordinates({
                x: cX1 - this.config.variant.multipleNucleotideVariant.thickness / 2,
                y: cY - height / 3
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