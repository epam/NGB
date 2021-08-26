import PIXI from 'pixi.js';
import {drawingConfiguration} from '../../../../../core';
import ensureEmptyValue from '../../../../../utilities/ensureEmptyValue';
import generateCoordinateSystemTicks from '../../../../../utilities/coordinateSystem';
import linearDimensionsConflict from '../../../../../utilities/linearDimensionsConflicts';

export default class ScaleRenderer extends PIXI.Container {
    constructor(track, config) {
        super();
        this.track = track;
        this.config = config;
        this.labelsContainer = new PIXI.Container();
        this.graphics = new PIXI.Graphics();
        this.logLabel = new PIXI.Text('LOG', config.barChart.scale.log.label);
        this.logLabel.resolution = drawingConfiguration.resolution;
        this.addChild(this.graphics);
        this.addChild(this.labelsContainer);
        this.addChild(this.logLabel);
    }

    getTickLabel = (text) => {
        const scaleConfig = this.config.barChart.scale || {};
        const tickStyle = scaleConfig.tick.label;
        const [label] = this.labelsContainer.children.filter(o => !o.visible);
        if (!label) {
            const newLabel = new PIXI.Text(text, tickStyle);
            newLabel.resolution = drawingConfiguration.resolution;
            this.labelsContainer.addChild(newLabel);
            return newLabel;
        }
        label.visible = true;
        label.style = tickStyle;
        label.text = text;
        return label;
    };

    render (viewport, coordinateSystem, height, converter) {
        this.graphics.clear();
        this.labelsContainer.children.forEach(child => child.visible = false);
        const scaleConfig = this.config.barChart.scale || {};
        if (coordinateSystem && converter) {
            const {
                minimum,
                maximum,
                log
            } = coordinateSystem;
            const ticksCount = height / ensureEmptyValue(scaleConfig.tick.heightPx);
            const ticks = coordinateSystem
                ? generateCoordinateSystemTicks(
                    minimum,
                    maximum,
                    log,
                    ticksCount
                )
                : [];
            this.logLabel.visible = log;
            this.logLabel.x = Math.round(
                ensureEmptyValue(scaleConfig.axis.margin) +
                ensureEmptyValue(scaleConfig.log.margin) +
                ensureEmptyValue(scaleConfig.log.padding)
            );
            this.logLabel.y = ensureEmptyValue(scaleConfig.log.padding);
            const axisTopBorderY = log
                ? (
                    this.logLabel.height +
                    ensureEmptyValue(scaleConfig.log.margin) +
                    ensureEmptyValue(scaleConfig.log.padding)
                )
                : 0;
            if (log) {
                this.graphics
                    .lineStyle(1, scaleConfig.log.stroke, 1)
                    .beginFill(
                        scaleConfig.log.fill,
                        scaleConfig.log.opacity
                    )
                    .drawRoundedRect(
                        ensureEmptyValue(scaleConfig.axis.margin) +
                        ensureEmptyValue(scaleConfig.log.margin),
                        0,
                        this.logLabel.width + 2.0 * (scaleConfig.log.padding || 0),
                        this.logLabel.height + 2.0 * (scaleConfig.log.padding || 0),
                        this.logLabel.height / 2.0
                    )
                    .endFill();
            }
            const baseLine = height - converter(log ? -Infinity : 0);
            const top = axisTopBorderY;
            const bottom = height;
            let tickPreviousLabelCoordinates;
            for (let t = 0; t < ticks.length; t++) {
                const tick = ticks[t];
                const y = Math.round(height - converter(tick.value));
                if (y >= bottom || y <= top) {
                    continue;
                }
                const tickLabel = this.getTickLabel(`${tick.display}`);
                const y1 = y - tickLabel.height / 2.0;
                const y2 = y + tickLabel.height / 2.0;
                const shouldRenderTickLabel = (y < height - tickLabel.height) &&
                    (y > axisTopBorderY) &&
                    (tick.value !== 0) &&
                    (
                        !tickPreviousLabelCoordinates ||
                        !linearDimensionsConflict(
                            y1,
                            y2,
                            tickPreviousLabelCoordinates.y1,
                            tickPreviousLabelCoordinates.y2
                        )
                    );
                if (shouldRenderTickLabel) {
                    tickLabel.y = Math.round(y - tickLabel.height / 2.0);
                    tickLabel.x = Math.round(
                        ensureEmptyValue(scaleConfig.axis.margin)
                        + ensureEmptyValue(scaleConfig.tick.margin)
                    );
                    this.labelsContainer.addChild(tickLabel);
                    tickPreviousLabelCoordinates = {y1, y2};
                } else {
                    tickLabel.visible = false;
                }
                this.graphics
                    .lineStyle(
                        1,
                        scaleConfig.tick.axis.color,
                        scaleConfig.tick.axis.opacity
                    )
                    .moveTo(
                        Math.round(
                            shouldRenderTickLabel
                                ? tickLabel.x + tickLabel.width + ensureEmptyValue(scaleConfig.axis.margin)
                                : ensureEmptyValue(scaleConfig.axis.margin)
                        ),
                        y
                    )
                    .lineTo(
                        Math.round(viewport.canvasSize - ensureEmptyValue(scaleConfig.axis.margin)),
                        y
                    );
            }
            this.graphics
                .lineStyle(1, scaleConfig.axis.color, 1)
                .moveTo(
                    ensureEmptyValue(scaleConfig.axis.margin),
                    Math.round(Math.min(height, baseLine))
                )
                .lineTo(
                    viewport.canvasSize,
                    Math.round(Math.min(height, baseLine))
                );
        }
    }
}
