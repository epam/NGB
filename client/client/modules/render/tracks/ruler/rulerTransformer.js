import {NumberFormatter, PixiTextSize} from '../../utilities';

const Math = window.Math;

export default class RulerTransformer {

    static _buildTicks(viewport, _config, isGlobal, ticksCount) {
        const brushSize = isGlobal ? viewport.chromosomeSize : viewport.brushSize;
        const brushStart = isGlobal ? 1 : viewport.brush.start;
        const brushEnd = isGlobal ? viewport.chromosomeSize : viewport.brush.end;
        const module = NumberFormatter.findClosestDecimalDegree(brushSize, ticksCount);
        const decimalBase = 10;
        const tickStep = Math.floor(brushSize / (ticksCount * (decimalBase ** module))) * (decimalBase ** module);
        let tickValue = Math.floor(brushStart / tickStep) * tickStep;
        while (tickValue < brushStart){
            tickValue += tickStep;
        }
        const ticks = [];
        const start = Math.floor(brushStart);
        const center = viewport.isShortenedIntronsMode ?
            viewport.shortenedIntronsViewport.brush.center :
            (viewport.brush.start + viewport.brush.end) / 2;
        const end = Math.floor(brushEnd);
        ticks.push({
            isFirst: true,
            labelStyle: _config.tick.label,
            realValue: start,
            value: start
        });
        ticks.push({
            isLast: true,
            labelStyle: _config.tick.label,
            realValue: end,
            value: end
        });
        if (!isGlobal) {
            ticks.push({
                isCenter: true,
                labelStyle: _config.centerTick.label,
                realValue: center,
                value: Math.round(center)
            });
        }

        while(tickValue < brushEnd){
            if (tickValue !== start && tickValue !== end && (isGlobal || tickValue !== Math.round(center))) {
                ticks.push({
                    labelStyle: _config.tick.label,
                    realValue: tickValue,
                    value: tickValue
                });
            }
            tickValue += tickStep;
        }
        return ticks;
    }

    static _findTicksCount(viewport, _config, isGlobal = false) {
        let ticks = RulerTransformer._buildTicks(viewport, _config, isGlobal, _config.ticks);
        let maxLabelSize = 0;
        for (let i = 0; i < ticks.length; i++) {
            maxLabelSize = Math.max(maxLabelSize, PixiTextSize.getTextSize(ticks[i].value, ticks[i].labelStyle).width);
            ticks[i].labelStyle = undefined;
        }
        ticks = null;
        const maxLabelSizeFactor = 1.5;
        return Math.min(_config.ticks, Math.max(1,
            Math.floor(viewport.canvasSize / (maxLabelSize * maxLabelSizeFactor + _config.ticksMinMargin)) - 2));
    }

    static transform(viewport, _config, isGlobal = false){
        const ticksCount = RulerTransformer._findTicksCount(viewport, _config, isGlobal);
        const ticks = RulerTransformer._buildTicks(viewport, _config, isGlobal, ticksCount);
        for (let i = 0; i < ticks.length; i++) {
            ticks[i].labelStyle = undefined;
        }
        return ticks;
    }
}