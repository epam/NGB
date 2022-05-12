import {VariantAltFeatureRenderer} from './variantAltFeatureRenderer';
import {ColorProcessor, NumberFormatter} from '../../../../../../../utilities';
const Math = window.Math;

export default class DeletionFeatureRenderer extends VariantAltFeatureRenderer {

    analyzeBoundaries(feature, viewport) {
        const boundaries = super.analyzeBoundaries(feature, viewport);
        if (!boundaries) {
            return null;
        }
        const width = Math.max(
            boundaries.rect.x2 - boundaries.rect.x1,
            viewport.project.brushBP2pixel(feature.endIndex) - viewport.project.brushBP2pixel(feature.startIndex) + 1
        );
        boundaries.rect.x2 = boundaries.rect.x1 + width;
        return boundaries;
    }

    render(feature, viewport, graphics, labelContainer, dockableElementsContainer, attachedElementsContainer, position) {
        if (feature.startIndex < feature.endIndex) {
            const alternativeAlleleLabelsHeight = feature.alternativeAllelesInfo.length * this.alleleLabelHeight;
            const height = this.config.variant.height;
            const cX1 = Math.round(Math.max(viewport.project.brushBP2pixel(feature.startIndex), -viewport.canvasSize));
            const cX2 = Math.round(Math.min(viewport.project.brushBP2pixel(feature.endIndex), 2 * viewport.canvasSize) + viewport.factor / 2);
            const cY = Math.round(position.y + alternativeAlleleLabelsHeight + this.config.variant.allele.height + height / 2);
            graphics.graphics
                .lineStyle(this.config.variant.multipleNucleotideVariant.thickness,
                    this.config.variant.multipleNucleotideVariant.color,
                    this.config.variant.multipleNucleotideVariant.alpha)
                .moveTo(cX1, cY - this.config.variant.multipleNucleotideVariant.thickness / 2)
                .lineTo(cX2, cY - this.config.variant.multipleNucleotideVariant.thickness / 2)
                .moveTo(cX2 - this.config.variant.multipleNucleotideVariant.thickness / 2, cY - height / 3)
                .lineTo(cX2 - this.config.variant.multipleNucleotideVariant.thickness / 2, cY + height / 3);

            graphics.hoveredGraphics
                .lineStyle(this.config.variant.multipleNucleotideVariant.thickness,
                    ColorProcessor.darkenColor(this.config.variant.multipleNucleotideVariant.color),
                    this.config.variant.multipleNucleotideVariant.alpha)
                .moveTo(cX1, cY - this.config.variant.multipleNucleotideVariant.thickness / 2)
                .lineTo(cX2, cY - this.config.variant.multipleNucleotideVariant.thickness / 2)
                .moveTo(cX2 - this.config.variant.multipleNucleotideVariant.thickness / 2, cY - height / 3)
                .lineTo(cX2 - this.config.variant.multipleNucleotideVariant.thickness / 2, cY + height / 3);
            this.generateAttachedElement({
                attachedAt: 'right',
                hideOnVisible: true,
                position: position.y + position.height - height / 2,
                range: {
                    start: feature.startIndex,
                    end: feature.endIndex
                }
            }, `${this.getFeatureDisplayText(feature)} ${NumberFormatter.formattedText(feature.startIndex)}`, this.config.variant.multipleNucleotideVariant.label.del, attachedElementsContainer);
            this.generateAttachedElement({
                attachedAt: 'left',
                hideOnVisible: true,
                position: position.y + position.height - height / 2,
                range: {
                    start: feature.startIndex,
                    end: feature.endIndex
                }
            }, `${this.getFeatureDisplayText(feature)} ${NumberFormatter.formattedText(feature.endIndex)}`, this.config.variant.multipleNucleotideVariant.label.del, attachedElementsContainer);
            this.updateTextureCoordinates({
                x: cX1 - this.config.variant.multipleNucleotideVariant.thickness / 2,
                y: cY - height / 3
            });
        }
        super.render(feature, viewport, graphics, labelContainer, dockableElementsContainer, attachedElementsContainer, position);
    }
}
