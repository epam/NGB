import { drawingConfiguration } from '../../../core';
import config from '../../whole-genome-config';

export class ScaleRenderer {

    constructor(container, canvasSize, positionInfo, drawingConfig, range){
        this._ticksNumber;
        Object.assign(this, {
            container,
            positionInfo,
            canvasSize,
            drawingConfig,
            range
        });
    }

    get start() { return this.positionInfo.start };

    get width(){ return this.canvasSize.width };
    get height(){ return this.canvasSize.height };

    get containerHeight(){ return this.height - 2 * this.topMargin};
    get topMargin(){ return this.drawingConfig.topMargin };
    get config(){ return config };
    
    get ticksNumber(){ return this._ticksNumber };
    set ticksNumber(newTicksNumber){ this._ticksNumber = newTicksNumber };
    get pixelStep(){ return Math.round(this.containerHeight/this.ticksNumber) }
  
    init(ticks) { 
        this.ticksNumber = ticks.length;
        const container = new PIXI.Container();
        this.createAxis(this.config, container);
        this.createTicks(container, ticks || [])
        this.container.addChild(container);
        return container;
    }

    createAxis(config, container){
        const axis = new PIXI.Graphics();
        axis
        .lineStyle(config.axis.thickness, config.axis.color, 1)
        .moveTo(this.start.x, this.start.y)
        .lineTo(this.start.x, this.containerHeight + (this.ticksNumber * config.axis.thickness/2))
        //.lineTo(this.start.x, this.containerHeight)
        .moveTo(this.start.x, this.start.y);
        container.addChild(axis);
    }

    renderTick(tick, graphics, tickConfig){

        const {
            container,
            ticksGraphics,
            tickLabels,
        } = graphics;

        if (!tick) return;

        const label = this.createLabel(
            this.config.tick.formatter(tick.value),
            tickConfig);

        ticksGraphics.lineStyle(
            this.config.tick.thickness,
            this.config.axis.color,
            1);

        this.appendTickGraphics(
            ticksGraphics,
            tickConfig);

        if (label) {
            container.addChild(label);
            tickLabels.push(label)
        };
    }

    appendTickGraphics(graphics, tickConfig){
        const { prevPosition, tickType} = tickConfig;
        if (tickType === 'odd'){
            graphics
                .moveTo(this.start.x, prevPosition)
                .lineTo(this.start.x + this.config.tick.offsetXOdd, prevPosition);
        } else {
            graphics
                .moveTo(this.start.x, prevPosition)
                .lineTo(this.start.x + this.config.tick.offsetXEven, prevPosition);
        }
    }

    createTicks(container, ticks){

        if (ticks.length > 1) {
            const ticksGraphics = new PIXI.Graphics();
            container.addChild(ticksGraphics);
            ticksGraphics.lineStyle(this.config.tick.thickness, this.config.axis.color, 1);

            let tickLabels = [];
            let prevPosition = this.start.y;
            let tickType = 'odd';

            for (let i = 0; i < ticks.length - 1; i++){
                const tick = ticks[i];
                this.renderTick(
                tick, 
                {
                    container,
                    ticksGraphics,
                    tickLabels,
                },
                {
                    prevPosition, 
                    tickType
                }
                );
                prevPosition += this.pixelStep;
                tickType = (tickType === 'odd') ? 'even' : 'odd';
            };
            tickLabels = null;
        };
    }

    createLabel(label, tickConfig){
        const { prevPosition, tickType } = tickConfig;

        if (tickType === 'odd') {
            const text = new PIXI.Text(label, config.tick.label)
            text.resolution = drawingConfiguration.resolution;
            text.y = prevPosition - this.pixelStep/2 + text.height/2 - 4 * config.axis.thickness;
            text.x = this.start.x + config.tick.offsetXOdd + config.tick.label.margin;
            return text;
        } else {
            return null;
        }
    };
}
