import * as PIXI from 'pixi.js-legacy';
import {ColorProcessor, PixiTextSize} from '../../../../../../../utilities';
import FeatureBaseRenderer from '../../../../../../gene/internal/renderer/features/drawing/featureBaseRenderer';

const Math = window.Math;

export class CommonVariantFeatureRenderer extends FeatureBaseRenderer {

    analyzeBoundaries(feature, viewport) {
        const labelStyle = this.config.variant.allele.label;
        const textSize = PixiTextSize.getTextSize(this.getFeatureDisplayText(feature), labelStyle);
        const pixelsInBp = viewport.factor;
        const width = Math.max(pixelsInBp, 3);
        const height = this.config.variant.height;
        const x1 = Math.max(viewport.project.brushBP2pixel(feature.startIndex), -viewport.canvasSize) - width / 2;
        const x2 = Math.max(Math.min(viewport.project.brushBP2pixel(feature.endIndex), 2 * viewport.canvasSize) + width / 2, x1 + width);
        const textX1 = Math.max(viewport.project.brushBP2pixel(feature.startIndex), -viewport.canvasSize) - pixelsInBp / 2 - textSize.width / 2;
        const textX2 = textX1 + textSize.width;
        return {
            margin: {
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

    generateAttachedElement(attachmentInfo, text, style, dockableContainer) {
        if (!this.registerAttachedElement || !this.labelsManager) {
            return;
        }
        const element = new PIXI.Container();
        const label = this.labelsManager.getLabel(text, style.font);
        if (label) {
            const margin = 1;
            label.x = margin;
            label.y = margin;
            attachmentInfo.renderInfo = {
                width: label.width + 2 * margin,
                height: label.height + 2 * margin
            };
            dockableContainer.addChild(element);
            const background = new PIXI.Graphics();
            background
                .beginFill(attachmentInfo.color || style.fill, 1)
                .drawRoundedRect(0, 0, label.width + 2 * margin, label.height + 2 * margin,
                    (label.height + 2 * margin) / 2)
                .endFill();
            element.addChild(background);
            element.addChild(label);
            this.registerAttachedElement(element, attachmentInfo);
        }
    }

    getFeatureDisplayText(feature) {
        return feature.symbol || feature.type;
    }

    render(feature, viewport, graphics, labelContainer, dockableElementsContainer, attachedElementsContainer, position) {
        super.render(feature, viewport, graphics, labelContainer, dockableElementsContainer, attachedElementsContainer, position);
        const white = 0xFFFFFF;
        const pixelsInBp = viewport.factor;
        const labelStyle = this.config.variant.allele.label;
        const symbol = this.getFeatureDisplayText(feature);
        const width = Math.max(pixelsInBp, 3);
        const height = this.config.variant.height;
        const cX = Math.round(Math.max(viewport.project.brushBP2pixel(feature.startIndex), -viewport.canvasSize));
        const cY = Math.round(position.y + position.height - height / 2);
        if (this.labelsManager) {
            const label = this.labelsManager.getLabel(symbol, labelStyle);
            if (label) {
                const textX1 = Math.max(viewport.project.brushBP2pixel(feature.startIndex), -viewport.canvasSize) - pixelsInBp / 2 - label.width / 2;
                const labelPosition = {
                    x: Math.round(textX1),
                    y: Math.round(position.y + position.height - height - label.height)
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
        }
        graphics.graphics.lineStyle(0, white, 0);
        graphics.hoveredGraphics.lineStyle(0, white, 0);
        const zygosity = feature.zygosity;
        switch (zygosity) {
            case 1: {
                // homozygous
                graphics.graphics
                    .beginFill(this.config.variant.zygosity.homozygousColor, 1)
                    .drawRect(Math.floor(cX - width / 2), Math.floor(cY - height / 2), width, height)
                    .endFill();
                graphics.hoveredGraphics
                    .beginFill(ColorProcessor.darkenColor(this.config.variant.zygosity.homozygousColor), 1)
                    .drawRect(Math.floor(cX - width / 2), Math.floor(cY - height / 2), width, height)
                    .endFill();
            }
                break;
            case 2: {
                // heterozygous
                graphics.graphics
                    .beginFill(this.config.variant.zygosity.homozygousColor, 1)
                    .drawRect(Math.floor(cX - width / 2), Math.floor(cY - height / 2), width, height / 2)
                    .endFill();
                graphics.graphics
                    .beginFill(this.config.variant.zygosity.heterozygousColor, 1)
                    .drawRect(Math.floor(cX - width / 2), Math.floor(cY), width, height / 2)
                    .endFill();

                graphics.hoveredGraphics
                    .beginFill(ColorProcessor.darkenColor(this.config.variant.zygosity.homozygousColor), 1)
                    .drawRect(Math.floor(cX - width / 2), Math.floor(cY - height / 2), width, height / 2)
                    .endFill();
                graphics.hoveredGraphics
                    .beginFill(ColorProcessor.darkenColor(this.config.variant.zygosity.heterozygousColor), 1)
                    .drawRect(Math.floor(cX - width / 2), Math.floor(cY), width, height / 2)
                    .endFill();
            }
                break;
            default: {
                graphics.graphics
                    .beginFill(this.config.variant.zygosity.unknownColor, 1)
                    .drawRect(Math.floor(cX - width / 2), Math.floor(cY - height / 2), width, height)
                    .endFill();
                graphics.hoveredGraphics
                    .beginFill(ColorProcessor.darkenColor(this.config.variant.zygosity.unknownColor), 1)
                    .drawRect(Math.floor(cX - width / 2), Math.floor(cY - height / 2), width, height)
                    .endFill();
            }
                break;
        }
        this.registerFeature(feature, viewport, position);
        this.updateTextureCoordinates({
            x: cX - width / 2,
            y: cY - height / 2,
        });
    }

    registerFeature(feature, viewport, position) {
        const labelStyle = this.config.variant.allele.label;
        const textSize = PixiTextSize.getTextSize(feature.symbol || feature.type, labelStyle);
        const pixelsInBp = viewport.factor;
        const width = Math.max(pixelsInBp, 3);
        const x1 = Math.max(viewport.project.brushBP2pixel(feature.startIndex), -viewport.canvasSize) - width / 2;
        const x2 = Math.max(Math.min(viewport.project.brushBP2pixel(feature.endIndex), 2 * viewport.canvasSize) + width / 2, x1 + width);
        const textX1 = Math.max(viewport.project.brushBP2pixel(feature.startIndex), -viewport.canvasSize) - pixelsInBp / 2 - textSize.width / 2;
        const textX2 = textX1 + textSize.width;
        this.registerFeaturePosition(feature, {
            x1: Math.min(x1, textX1),
            x2: Math.max(x2, textX2),
            y1: position.y,
            y2: position.y + position.height
        });
    }
}
