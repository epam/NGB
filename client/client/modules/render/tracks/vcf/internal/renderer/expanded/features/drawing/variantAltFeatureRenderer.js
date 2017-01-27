import {CommonVariantFeatureRenderer} from './commonVariantFeatureRenderer';
import {PixiTextSize} from '../../../../../../../utilities';
import {drawingConfiguration} from '../../../../../../../core';
const Math = window.Math;

export class VariantAltFeatureRenderer extends CommonVariantFeatureRenderer {

    _alleleLabelHeight;

    constructor(...opts){
        super(...opts);
        this._alleleLabelHeight = PixiTextSize.getTextSize('A', this.config.variant.allele.label).height;
    }

    get alleleLabelHeight() {
        return this._alleleLabelHeight;
    }

    analyzeBoundaries(feature, viewport) {
        const boundaries = super.analyzeBoundaries(feature, viewport);
        if (!boundaries) {
            return null;
        }
        const width = boundaries.rect.x2 - boundaries.rect.x1;
        let textWidth = 0;
        for (let i = 0; i < feature.alternativeAllelesInfo.length; i++) {
            if (!feature.alternativeAllelesInfo[i].displayText || feature.alternativeAllelesInfo[i].displayText === '') {
                continue;
            }
            const displayTextSize = PixiTextSize.getTextSize(feature.alternativeAllelesInfo[i].displayText, this.config.variant.allele.label).width + 30;
            if (displayTextSize > textWidth) {
                textWidth = displayTextSize;
            }
        }

        const centerX = viewport.project.brushBP2pixel(feature.startIndex);
        boundaries.rect.x1 = centerX - textWidth / 2;
        boundaries.rect.x2 = Math.max(boundaries.rect.x1 + width, centerX + textWidth / 2);
        boundaries.rect.y2 += this._alleleLabelHeight * feature.alternativeAllelesInfo.length;
        return boundaries;
    }

    render(feature, viewport, graphics, labelContainer, dockableElementsContainer, attachedElementsContainer, position) {
        for (let i = 0; i < feature.alternativeAllelesInfo.length; i++) {
            const label = new PIXI.Text(feature.alternativeAllelesInfo[i].displayText || '', this.config.variant.allele.label);
            const labelPosition = {
                x: Math.round(position.x),
                y: Math.round(position.y + i * this._alleleLabelHeight)
            };
            label.resolution = drawingConfiguration.resolution;
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
        super.render(feature, viewport, graphics, labelContainer, dockableElementsContainer, attachedElementsContainer, position);
    }

    registerFeature(feature, viewport, position) {
        let textWidth = 0;
        for (let i = 0; i < feature.alternativeAllelesInfo.length; i++) {
            if (!feature.alternativeAllelesInfo[i].displayText) {
                continue;
            }
            const displayTextSize = PixiTextSize.getTextSize(feature.alternativeAllelesInfo[i].displayText, this.config.variant.allele.label).width + 10;
            if (displayTextSize > textWidth) {
                textWidth = displayTextSize;
            }
        }
        const centerX = viewport.project.brushBP2pixel(feature.startIndex) - viewport.factor / 2;
        const x1 = centerX - textWidth / 2;
        const x2 = centerX + textWidth / 2;
        this.registerFeaturePosition(feature, {
            x1: Math.min(x1, position.x),
            x2: Math.max(x2, position.x + position.width),
            y1: position.y,
            y2: position.y + position.height
        });
    }
}