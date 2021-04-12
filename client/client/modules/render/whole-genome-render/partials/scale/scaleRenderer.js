import PIXI from 'pixi.js';

import {
    drawingConfiguration
} from '../../../core';
import config from '../../whole-genome-config';

export class ScaleRenderer {

    constructor({
        container,
        canvasSize,
        range
    }) {
        this._ticksNumber;
        this._ticks;
        this._labelWidth;
        Object.assign(this, {
            container,
            canvasSize,
            range
        });
    }

    get start() {
        return config.start;
    }

    get width() {
        return this.canvasSize ? this.canvasSize.width : 0;
    }

    get height() {
        return this.canvasSize ? this.canvasSize.height : 0;
    }

    get containerHeight() {
        return this.height - 2 * this.topMargin;
    }

    get topMargin() {
        return config.start.topMargin;
    }

    get ticksNumber() {
        return this._ticksNumber;
    }

    set ticksNumber(newTicksNumber) {
        this._ticksNumber = newTicksNumber;
    }

    get ticks() {
        return this._ticks;
    }

    get realStep() {
        return this.ticks[1].value - this.ticks[0].value;
    }

    get pixelStep() {
        return this.containerHeight / (this.ticksNumber - 1) - config.tick.thickness;
    }
    set labelWidth(width) {
        this._labelWidth = width;
    }
    get labelWidth() {
        return this._labelWidth;
    }

    init(ticks) {
        this._ticks = ticks;
        this.ticksNumber = ticks.length;
        const container = new PIXI.Container();
        container.x = 0;
        container.y = this.topMargin;
        this.createAxis(config, container);
        this.createTicks(container, ticks || []);
        this.container.addChild(container);
        return container;
    }

    createAxis(config, container) {
        const axis = new PIXI.Graphics();
        axis.x = 0;
        axis.y = 0;
        axis
            .lineStyle(config.axis.thickness, config.axis.color, 1)
            .moveTo(this.start.x, 0)
            .lineTo(this.start.x, this.containerHeight);
        container.addChild(axis);
    }

    renderTick(tick, graphics, tickConfig) {
        const {
            container,
            ticksGraphics,
        } = graphics;

        if (!tick) return;

        const label = this.createLabel(
            config.tick.formatter(tick.value),
            tickConfig);

        ticksGraphics.lineStyle(
            config.tick.thickness,
            config.axis.color,
            1);

        this.appendTickGraphics(
            ticksGraphics,
            tickConfig);

        if (label) {
            container.addChild(label);
        }
    }

    appendTickGraphics(graphics, tickConfig) {
        const {
            prevPosition,
            isEven
        } = tickConfig;
        if (!isEven) {
            graphics
                .moveTo(this.start.x, prevPosition)
                .lineTo(this.start.x + config.tick.offsetXOdd, prevPosition);
        } else {
            graphics
                .moveTo(this.start.x, prevPosition)
                .lineTo(this.start.x + config.tick.offsetXEven, prevPosition);
        }
    }

    createTicks(container, ticks) {

        if (ticks.length > 1) {
            const ticksGraphics = new PIXI.Graphics();
            container.addChild(ticksGraphics);
            ticksGraphics.x = 0;
            ticksGraphics.y = 0;
            ticksGraphics.lineStyle(config.tick.thickness, config.axis.color, 1);

            let prevPosition = 0;
            for (let i = 0; i < ticks.length; i++) {

                const tick = ticks[i];
                this.renderTick(
                    tick, {
                        container,
                        ticksGraphics,
                    }, {
                        prevPosition,
                        isEven: !!(i % 2)
                    }
                );
                prevPosition += this.pixelStep + config.tick.thickness;
            }
        }
    }

    createLabel(label, tickConfig) {
        const {
            prevPosition,
            isEven
        } = tickConfig;

        if (!isEven) {
            const text = new PIXI.Text(label, config.tick.label);
            text.resolution = drawingConfiguration.resolution;
            this.labelWidth = text.width;
            text.resolution = drawingConfiguration.resolution;
            text.y = prevPosition - this.pixelStep / 2 + text.height / 2 - 4 * config.axis.thickness;
            text.x = this.start.x + config.tick.offsetXOdd + config.tick.margin;
            return text;
        } else {
            return null;
        }
    }
}