import * as PIXI from 'pixi.js-legacy';
import {NumberFormatter} from '../../../../../utilities';
import {VariantBaseContainer} from './baseContainer';
import {drawingConfiguration} from '../../../../../core';

const Math = window.Math;

export class StatisticsContainer extends VariantBaseContainer {

    _bubbleInfo = null;

    constructor(variant, config) {
        super(variant, config);
    }

    render(viewport, manager) {
        super.render(viewport, manager);
        this.container.x = Math.round(viewport.project.brushBP2pixel((this._variant.startIndex + this._variant.endIndex) / 2));
        if (!this._componentIsBuilt) {
            this.buildComponent(viewport, manager);
        }
    }

    buildComponent(viewport, manager) {
        this.buildBubble(manager);
        super.buildComponent(viewport, manager);
    }

    buildBubble(manager) {
        this.drawBubble(0);
        const label = new PIXI.Text(NumberFormatter.textWithPrefix(this._variant.variationsCount, false),
            this._config.statistics.label);
        label.resolution = drawingConfiguration.resolution;
        label.x = Math.round(-label.width / 2);
        label.y = Math.round(-this._config.statistics.height - this._variant.bubble.radius - label.height / 2);
        this._container.addChild(label);
        manager.submitArea('default', {
            global: {
                x: this.container.x,
                y: this.container.y
            },
            rect: {
                x1: -this._variant.bubble.radius,
                x2: this._variant.bubble.radius,
                y1: -this._config.statistics.height - 2 * this._variant.bubble.radius,
                y2: 0
            }
        });
        this._bubbleInfo = {
            radius: this._variant.bubble.radius,
            x: 0,
            y: -this._config.statistics.height - this._variant.bubble.radius
        };
    }

    drawBubble(extraSize) {
        this._graphics.clear();
        this._graphics
            .lineStyle(this._config.statistics.bubble.stroke.thickness, this._config.statistics.bubble.stroke.color, 1)
            .moveTo(-this._config.statistics.bubble.stroke.thickness / 2, 0)
            .lineTo(-this._config.statistics.bubble.stroke.thickness / 2, -this._config.statistics.height)
            .lineStyle(0, this._config.statistics.bubble.stroke.color, 0);
        this._graphics
            .beginFill(this._config.statistics.bubble.fill, 1)
            .drawCircle(0, -this._config.statistics.height - this._variant.bubble.radius,
                this._variant.bubble.radius + extraSize)
            .endFill();
    }


    isHovers(cursor) {
        if (!cursor) {
            return false;
        }
        if (super.isHovers(cursor)) {
            return true;
        }
        let {x, y} = cursor;
        x -= this.container.x;
        y -= this.container.y;
        return Math.sqrt((this._bubbleInfo.x - x) ** 2 + (this._bubbleInfo.y - y) ** 2)
            <= this._bubbleInfo.radius;
    }

    animate(time) {
        const needAnimateFade = super.animate(time);
        const needAnimateBubbleHover = (this._isHovered && this._hoverFactor < 1)
            || (!this._isHovered && this._hoverFactor > 0);
        if (needAnimateBubbleHover) {
            const oneSecond = 1000;
            const timeDelta = (time) / oneSecond;
            const hoverDelta = 1 / this._config.animation.hover.bubble.duration * timeDelta;
            this._hoverFactor =
                Math.max(0, Math.min(1, this._hoverFactor + (this._isHovered ? hoverDelta : -hoverDelta)));
            this.drawBubble(this._hoverFactor * this._config.animation.hover.bubble.extraRadius);
        }
        return needAnimateBubbleHover || needAnimateFade;
    }

}
