import PIXI from 'pixi.js';

import { drawingConfiguration } from '../../../core';
import config from '../../whole-genome-config';

export class ScaleRenderer {

    constructor({
        container,
        canvasSize,
        range
    }) {
        this._ticksNumber;
        this._ticks;
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
            tickLabels,
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
            tickLabels.push(label);
        }
    }

    appendTickGraphics(graphics, tickConfig) {
        const {
            prevPosition,
            tickType
        } = tickConfig;
        if (tickType === 'odd') {
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

            let tickLabels = [];
            let prevPosition = 0;
            let tickType = 'odd';

            for (let i = 0; i < ticks.length; i++) {

                const tick = ticks[i];
                this.renderTick(
                    tick, {
                        container,
                        ticksGraphics,
                        tickLabels,
                    }, {
                        prevPosition,
                        tickType
                    }
                );
                prevPosition += this.pixelStep + config.tick.thickness;
                tickType = (tickType === 'odd') ? 'even' : 'odd';
            }
            tickLabels = null;
        }
    }

    createLabel(label, tickConfig) {
        const {
            prevPosition,
            tickType
        } = tickConfig;

        if (tickType === 'odd') {
            const text = new PIXI.Text(label, config.tick.label);
            text.resolution = drawingConfiguration.resolution;
            text.y = prevPosition - this.pixelStep / 2 + text.height / 2 - 4 * config.axis.thickness;
            text.x = this.start.x + config.tick.offsetXOdd + config.tick.label.margin;
            return text;
        } else {
            return null;
        }
    }
}
