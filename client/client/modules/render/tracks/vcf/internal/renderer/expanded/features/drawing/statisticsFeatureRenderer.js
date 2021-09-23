import {ColorProcessor, PixiTextSize} from '../../../../../../../utilities';
import FeatureBaseRenderer from '../../../../../../gene/internal/renderer/features/drawing/featureBaseRenderer';
const Math = window.Math;

export default class StatisticsFeatureRenderer extends FeatureBaseRenderer {

    static getStatisticsText(feature) {
        return `${feature.variationsCount} variants`;
    }

    analyzeBoundaries(feature, viewport) {
        const labelStyle = this.config.variant.allele.label;
        const text = StatisticsFeatureRenderer.getStatisticsText(feature);
        const textSize = PixiTextSize.getTextSize(text, labelStyle);
        const pixelsInBp = viewport.factor;
        const width = Math.max(pixelsInBp, 3);
        const height = this.config.variant.height;
        const x1 = Math.max(viewport.project.brushBP2pixel(feature.startIndex), -viewport.canvasSize) - width / 2;
        const x2 = Math.max(Math.min(viewport.project.brushBP2pixel(feature.endIndex), 2 * viewport.canvasSize) + width / 2, x1 + width);
        const textX1 = Math.max(viewport.project.brushBP2pixel(feature.startIndex), -viewport.canvasSize) - pixelsInBp / 2 - textSize.width / 2;
        const textX2 = textX1 + textSize.width;
        return {
            margin:{
                marginX: 2,
                marginY: 2
            },
            rect: {
                x1: Math.min(x1, textX1),
                x2: Math.max(x2, textX2),
                y1: 0,
                y2: height + this.config.variant.allele.height
            }
        };
    }

    render(feature, viewport, graphics, labelContainer, dockableElementsContainer, attachedElementsContainer,  position) {
        super.render(feature, viewport, graphics, labelContainer, dockableElementsContainer, attachedElementsContainer, position);
        const pixelsInBp = viewport.factor;
        const labelStyle = this.config.variant.allele.label;
        const symbol =  StatisticsFeatureRenderer.getStatisticsText(feature);
        const label = this.labelsManager ? this.labelsManager.getLabel(symbol, labelStyle) : undefined;
        const width = Math.max(pixelsInBp, 3);
        const height = this.config.variant.height;
        const cX = Math.round(Math.max(viewport.project.brushBP2pixel(feature.startIndex), -viewport.canvasSize));
        const cY = Math.round(position.y + position.height - height / 2);
        if (label) {
            const textX1 = Math.max(viewport.project.brushBP2pixel(feature.startIndex), -viewport.canvasSize) - pixelsInBp / 2 - label.width / 2;
            const labelPosition = {
                x: Math.round(textX1),
                y: Math.round(position.y + position.height - height - this.config.variant.allele.height / 2 - label.height / 2)
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
                },
                false,
                true);
        }
        const white = 0xFFFFFF;
        graphics.graphics.lineStyle(0, white, 0);
        graphics.graphics
            .beginFill(this.config.variant.zygosity.unknownColor, 1)
            .drawRect(cX - width / 2, cY - height / 2, width, height)
            .endFill();
        graphics.hoveredGraphics.lineStyle(0, white, 0);
        graphics.hoveredGraphics
            .beginFill(ColorProcessor.darkenColor(this.config.variant.zygosity.unknownColor), 1)
            .drawRect(cX - width / 2, cY - height / 2, width, height)
            .endFill();
        this.registerFeature(feature, viewport, position);
        this.updateTextureCoordinates({
            x: cX - width / 2,
            y: cY - height / 2,
        });
    }

    registerFeature() {
        // we should not register this type of feature
    }
}
