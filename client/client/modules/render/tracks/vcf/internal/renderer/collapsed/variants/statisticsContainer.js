import {NumberFormatter} from '../../../../../../utilities';
import {VariantBaseContainer} from './baseContainer';
import {drawingConfiguration} from '../../../../../../core';

const Math = window.Math;

export class StatisticsContainer extends VariantBaseContainer {
    _bubbleInfo = null;

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
        const label = this.labelsManager
            ? this.labelsManager.getLabel(
                NumberFormatter.textWithPrefix(this._variant.variationsCount, false),
                this._config.statistics.label
            )
            : undefined;
        if (label) {
            label.resolution = drawingConfiguration.resolution;
            label.x = Math.floor(-label.width / 2);
            label.y = Math.floor(-this._config.statistics.height - this._variant.bubble.radius - label.height / 2);
            this._container.addChild(label);
        }
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
        const white = 0xFFFFFF,
            cx = 0,
            cy = Math.floor(-this._config.statistics.height - this._variant.bubble.radius),
            r = Math.round(this._variant.bubble.radius + extraSize);
        const arcStart = -Math.PI / 2;
        this._graphics.clear();
        this._graphics
            .lineStyle(this._config.statistics.bubble.stroke.thickness, this._config.statistics.bubble.stroke.color, 1)
            .moveTo(-this._config.statistics.bubble.stroke.thickness / 2, 0)
            .lineTo(-this._config.statistics.bubble.stroke.thickness / 2, -this._config.statistics.height)
            .lineStyle(0, this._config.statistics.bubble.stroke.color, 0);
        this._graphics
            .beginFill(white, 0)
            .moveTo(cx, cy)
            .arc(cx, cy, r, arcStart, arcStart + 2 * Math.PI)
            .lineTo(cx, cy)
            .endFill();
        this._graphics
            .lineStyle(1, this._config.statistics.bubble.stroke.color, 1)
            .drawCircle(0, Math.floor(-this._config.statistics.height - this._variant.bubble.radius),
                Math.round(this._variant.bubble.radius + extraSize));
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
