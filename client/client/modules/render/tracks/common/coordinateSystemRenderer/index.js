import * as PIXI from 'pixi.js-legacy';
import {
    ensureEmptyValue,
    generateCoordinateSystemTicks,
    linearDimensionsConflict
} from '../../../utilities';
import defaultConfig from './config';

export default class CoordinateSystem extends PIXI.Container {
    static getConverter = (coordinateSystem, height) => (value) => {
        if (coordinateSystem) {
            if (coordinateSystem.log && value === -Infinity) {
                return 0;
            }
            const {
                minimum: min = 0,
                maximum: max = 1,
                log = false
            } = coordinateSystem;
            const convert = o => log ? (o > 0 ? Math.log10(o) : 0) : o;
            const minimum = convert(min);
            const maximum = convert(max);
            const range = maximum - minimum;
            if (range > 0) {
                return (convert(value) - minimum) / range * height;
            }
        }
        return 0;
    };

    static getBaseLine = (coordinateSystem, height) => {
        const {log = false} = coordinateSystem || {};
        return height - CoordinateSystem.getConverter(coordinateSystem, height)(log ? -Infinity : 0);
    };

    constructor(track, config) {
        super();
        this.track = track;
        this.config = {...(defaultConfig || {}), ...(config || {})};
        this.labelsContainer = new PIXI.Container();
        this.graphics = new PIXI.Graphics();
        this.addChild(this.graphics);
        this.addChild(this.labelsContainer);
        if (this.track && this.track.labelsManager) {
            this.logLabel = this.track.labelsManager.getLabel('log', this.config.log.label);
            if (this.logLabel) {
                this.addChild(this.logLabel);
            }
        }
    }

    getTickLabel = (text) => {
        const tickStyle = this.config.tick.label;
        return this.track.labelsManager.getLabel(text, tickStyle);
    };

    renderCoordinateSystem (viewport, coordinateSystem, height, options = {}) {
        const {
            yBoundaries = {},
            renderBaseLineAsBottomBorder = true
        } = options;
        const {
            top = -Infinity,
            bottom = Infinity
        } = yBoundaries;
        const correctPositionToFitBoundaries = position => Math.max(top, Math.min(bottom, position));
        this.graphics.clear();
        this.labelsContainer.removeChildren();
        if (coordinateSystem) {
            const converter = CoordinateSystem.getConverter(coordinateSystem, height);
            const {
                minimum = 0,
                maximum = 1,
                log = false
            } = coordinateSystem;
            const ticksCount = height / ensureEmptyValue(this.config.tick.heightPx);
            const ticks = coordinateSystem
                ? generateCoordinateSystemTicks(
                    minimum,
                    maximum,
                    log,
                    ticksCount
                )
                : [];
            if (this.logLabel) {
                this.track.labelsManager.ensureLabel(this.logLabel, 'log', this.config.log.label);
                this.logLabel.visible = log;
                this.logLabel.x = Math.round(
                    ensureEmptyValue(this.config.log.margin) +
                    ensureEmptyValue(this.config.log.padding)
                );
                this.logLabel.y = correctPositionToFitBoundaries(0) +
                    ensureEmptyValue(this.config.log.padding);
            }
            const axisTopBorderY = log
                ? (
                    correctPositionToFitBoundaries(
                        this.logLabel ? (this.logLabel.y + this.logLabel.height) : 0
                    ) + ensureEmptyValue(this.config.log.margin)
                )
                : correctPositionToFitBoundaries(0);
            if (log && this.logLabel) {
                this.graphics
                    .lineStyle(
                        this.config.log.stroke ? 1 : 0,
                        this.config.log.stroke || 0xffffff,
                        this.config.log.stroke ? 1 : 0
                    )
                    .beginFill(
                        this.config.log.fill,
                        this.config.log.opacity
                    )
                    .drawRoundedRect(
                        Math.round(this.logLabel.x - ensureEmptyValue(this.config.log.padding)),
                        Math.round(this.logLabel.y - ensureEmptyValue(this.config.log.padding)),
                        Math.round(this.logLabel.width + 2.0 * (this.config.log.padding || 0)),
                        Math.round(this.logLabel.height + 2.0 * (this.config.log.padding || 0)),
                        this.logLabel.height / 2.0
                    )
                    .endFill();
            }
            const baseLine = CoordinateSystem.getBaseLine(coordinateSystem, height);
            const minTickYPosition = correctPositionToFitBoundaries(axisTopBorderY);
            const maxTickYPosition = correctPositionToFitBoundaries(height);
            let tickPreviousLabelCoordinates;
            for (let t = 0; t < ticks.length; t++) {
                const tick = ticks[t];
                const y = Math.round(height - converter(tick.value));
                if (
                    y >= maxTickYPosition ||
                    y <= minTickYPosition ||
                    Math.abs(y - baseLine) < 1
                ) {
                    continue;
                }
                const tickLabel = this.getTickLabel(`${tick.display}`);
                let shouldRenderTickLabel = !!tickLabel;
                if (tickLabel) {
                    const y1 = y - tickLabel.height / 2.0;
                    const y2 = y + tickLabel.height / 2.0;
                    shouldRenderTickLabel = (y < height - tickLabel.height) &&
                        (y > minTickYPosition) &&
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
                        tickLabel.x = Math.round(ensureEmptyValue(this.config.tick.margin));
                        this.labelsContainer.addChild(tickLabel);
                        tickPreviousLabelCoordinates = {y1, y2};
                    }
                }
                this.graphics
                    .lineStyle(
                        1,
                        this.config.axis.color,
                        this.config.axis.opacity
                    )
                    .drawDashLine(
                        Math.round(
                            shouldRenderTickLabel && tickLabel
                                ? tickLabel.x + tickLabel.width
                                : 0
                        ),
                        y,
                        Math.round(viewport.canvasSize),
                        y,
                        this.config.axis.dash
                    );
            }
            if (height >= baseLine || renderBaseLineAsBottomBorder) {
                this.graphics
                    .lineStyle(
                        1,
                        this.config.baseAxis.color,
                        this.config.baseAxis.opacity
                    )
                    .moveTo(
                        0,
                        Math.round(Math.min(height, baseLine))
                    )
                    .lineTo(
                        viewport.canvasSize,
                        Math.round(Math.min(height, baseLine))
                    );
            }
        }
    }
}
