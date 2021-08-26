import PIXI from 'pixi.js';
import {drawingConfiguration} from '../../../../../core';
import ScaleRenderer from './scale-renderer';
import ensureEmptyValue from '../../../../../utilities/ensureEmptyValue';

const white = 0xffffff;

const getConverter = (coordinateSystem, height) => (value) => {
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

export default class BarChartSourceRenderer extends PIXI.Container {
    constructor(track, config, source) {
        super();
        this._source = source;
        this.track = track;
        this.config = config;
        this.dataContainer = new PIXI.Container();
        this.tooltipContainer = new PIXI.Container();
        this.graphics = new PIXI.Graphics();
        this.hoveredGraphics = new PIXI.Graphics();
        this.tooltipGraphics = new PIXI.Graphics();
        this.tooltips = new PIXI.Container();
        this.dataContainer.addChild(this.graphics);
        this.dataContainer.addChild(this.hoveredGraphics);
        this.tooltipContainer.addChild(this.tooltipGraphics);
        this.tooltipContainer.addChild(this.tooltips);
        this.scaleRenderer = new ScaleRenderer(track, config);
        this.sourceNameLabel = new PIXI.Text(source, config.barChart.title);
        this.sourceNameLabel.resolution = drawingConfiguration.resolution;
        this.background = new PIXI.Graphics();
        this.groupAutoScaleIndicator = new PIXI.Graphics();
        this.addChild(this.background);
        this.addChild(this.scaleRenderer);
        this.addChild(this.dataContainer);
        this.addChild(this.sourceNameLabel);
        this.addChild(this.tooltipContainer);
        this.addChild(this.groupAutoScaleIndicator);
        this.featurePositions = [];
    }

    destroy (destroyChildren) {
        this.groupAutoScaleManager = null;
        this.fcSourcesManager = null;
        super.destroy(destroyChildren);
    }

    registerGroupAutoScaleManager(manager) {
        this.groupAutoScaleManager = manager;
    }

    registerFCSourcesManager(manager) {
        this.fcSourcesManager = manager;
    }

    get source (): string {
        return this._source;
    }

    get dragAndDropZone () {
        if (!this.sourceNameLabel) {
            return {
                x1: 0,
                x2: 0,
                y1: 0,
                y2: 0
            };
        }
        const horizontalMargin = this.sourceNameLabel.x;
        const verticalMargin = this.sourceNameLabel.y;
        return {
            x1: 0,
            x2: 2.0 * horizontalMargin + this.sourceNameLabel.width,
            y1: 0,
            y2: 2.0 * verticalMargin + this.sourceNameLabel.height
        };
    }

    translate (drawScope){
        if (!drawScope) {
            this.dataContainer.x = 0;
            this.dataContainer.scale.x = 1;
            this.tooltipContainer.x = 0;
            this.tooltipContainer.scale.x = 1;
            return;
        }
        this.dataContainer.x = drawScope.containerTranslateFactor * drawScope.scaleFactor;
        this.dataContainer.scale.x = drawScope.scaleFactor;
        this.tooltipContainer.x = this.dataContainer.x;
        this.tooltipContainer.scale.x = this.dataContainer.scale.x;
    }

    renderGroupAutoScaleIndicator(coordinateSystem, height) {
        const {
            groupAutoScale
        } = coordinateSystem || {};
        this.groupAutoScaleIndicator.clear();
        if (this.groupAutoScaleManager && groupAutoScale) {
            const group = this.groupAutoScaleManager.getGroup(groupAutoScale);
            if (group) {
                const color = this.groupAutoScaleManager.getGroupColor(group);
                this.groupAutoScaleIndicator
                    .beginFill(color, 1.0)
                    .lineStyle(0, 0, 0)
                    .drawRect(
                        0,
                        0,
                        this.config.barChart.autoScaleGroupIndicator.width,
                        height
                    )
                    .endFill();
            }
        }
    }

    render (viewport, data, coordinateSystem, options) {
        this.featurePositions = [];
        this.translate();
        const {height = 0} = options || {};
        this.renderGroupAutoScaleIndicator(coordinateSystem, height);
        this.background
            .clear()
            .beginFill(white, 1)
            .drawRect(
                0,
                0,
                viewport.canvasSize,
                height
            )
            .endFill();
        const barChartConfig = this.config.barChart || {};
        this.sourceNameLabel.x = Math.round(
            ensureEmptyValue(barChartConfig.scale.axis.margin) +
            ensureEmptyValue(barChartConfig.title.margin)
        );
        this.sourceNameLabel.y = Math.round(
            ensureEmptyValue(this.config.barChart.margin.top) +
            ensureEmptyValue(barChartConfig.title.margin)
        );
        const drawingHeight = height -
            ensureEmptyValue(this.config.barChart.margin.top) -
            ensureEmptyValue(this.config.barChart.margin.bottom);
        const scaleDrawingHeight = height -
            ensureEmptyValue(this.config.barChart.margin.top) -
            ensureEmptyValue(this.config.barChart.margin.bottom) -
            2.0 * ensureEmptyValue(barChartConfig.title.margin) -
            this.sourceNameLabel.height;
        const convertSourceValueToPixel = getConverter(coordinateSystem, drawingHeight);
        this.scaleRenderer.render(
            viewport,
            coordinateSystem,
            scaleDrawingHeight,
            convertSourceValueToPixel
        );
        this.scaleRenderer.x = 0;
        this.scaleRenderer.y = ensureEmptyValue(barChartConfig.margin.top) +
            2.0 * ensureEmptyValue(barChartConfig.title.margin) +
            this.sourceNameLabel.height;
        this.featurePositions = this.renderFeatures(
            viewport,
            data,
            coordinateSystem,
            {...(options || {}), graphics: this.graphics}
        );
    }

    renderFeatures (viewport, data, coordinateSystem, options) {
        const {
            graphics,
            height = 0,
            features = [],
            hovered = false,
            tooltip = false,
            yPositionCorrection = (o => o)
        } = options || {};
        const color = this.fcSourcesManager
            ? this.fcSourcesManager.getColorConfiguration(this.source)
            : this.config.barChart.bar;
        const fillOpacity = hovered ? 0.5 : 0.33;
        const strokeOpacity = hovered ? 0.8 : 0.4;
        this.tooltipGraphics.clear();
        this.tooltips.removeChildren();
        if (!graphics) {
            return;
        }
        graphics.clear();
        if (!data || !coordinateSystem) {
            return;
        }
        const top = ensureEmptyValue(this.config.barChart.margin.top);
        const bottom = height - ensureEmptyValue(this.config.barChart.margin.bottom);
        const correctYPosition = o => Math.max(top, Math.min(bottom, o));
        const drawingHeight = bottom - top;
        const convertSourceValueToPixel = getConverter(coordinateSystem, drawingHeight);
        const baseLine = correctYPosition(
            ensureEmptyValue(this.config.barChart.margin.top) +
                drawingHeight -
                convertSourceValueToPixel(
                    coordinateSystem && coordinateSystem.log
                        ? -Infinity
                        : 0
                )
        );
        const featurePositions = [];
        for (let i = 0; i < (data || []).length; i++) {
            const item = data[i];
            if (
                features &&
                features.length > 0 &&
                !/^statistic$/i.test(item.feature) &&
                features.indexOf(item.feature) === -1
            ) {
                continue;
            }
            if (
                !item.attributes ||
                !item.attributes.hasOwnProperty(this.source)
            ) {
                continue;
            }
            const value = Number(item.attributes[this.source] || 0);
            if (Number.isNaN(value) || !Number(value)) {
                continue;
            }
            let x1 = viewport.project.brushBP2pixel(item.startIndex - 0.5);
            let x2 = viewport.project.brushBP2pixel(item.endIndex + 0.5);
            x1 = Math.floor(Math.max(x1, -viewport.canvasSize));
            x2 = Math.ceil(Math.min(x2, 2 * viewport.canvasSize));
            const yValue = drawingHeight - convertSourceValueToPixel(value);
            const y1 = correctYPosition(Math.min(baseLine, yValue));
            const y2 = correctYPosition(Math.max(baseLine, yValue));
            if (
                coordinateSystem &&
                coordinateSystem.log &&
                y1 >= baseLine &&
                y2 >= baseLine
            ) {
                continue;
            }
            const height = y2 - y1;
            if (height === 0) {
                continue;
            }
            graphics
                .beginFill(color, fillOpacity)
                .lineStyle(0, 0x0, 0)
                .drawRect(
                    x1,
                    y1,
                    x2 - x1,
                    height
                )
                .endFill()
                .lineStyle(1, color, strokeOpacity)
                .moveTo(x1, y2)
                .lineTo(x1, y1)
                .lineTo(x2, y1)
                .lineTo(x2, y2);
            if (tooltip) {
                const tooltipConfig = this.config.barChart.hoveredItemInfo;
                const tooltipLabel = new PIXI.Text(
                    item.name ? `${item.name}: ${value}` : `${value}`,
                    tooltipConfig.label
                );
                tooltipLabel.resolution = drawingConfiguration.resolution;
                this.tooltips.addChild(tooltipLabel);
                const tooltipX = Math.round(
                    Math.max(
                        -this.dataContainer.x +
                        ensureEmptyValue(tooltipConfig.padding),
                        Math.min(
                            viewport.canvasSize -
                            this.dataContainer.x -
                            ensureEmptyValue(tooltipConfig.padding) -
                            tooltipLabel.width,
                            (x1 + x2) / 2.0 - tooltipLabel.width / 2.0
                        )
                    )
                );
                const tooltipY2 = yPositionCorrection(
                    y1 -
                    ensureEmptyValue(tooltipConfig.padding) -
                    ensureEmptyValue(tooltipConfig.margin)
                );
                const tooltipY1 = Math.round(
                    yPositionCorrection(
                        tooltipY2 -
                        tooltipLabel.height -
                        ensureEmptyValue(tooltipConfig.padding)
                    ) + ensureEmptyValue(tooltipConfig.padding)
                );
                tooltipLabel.x = tooltipX;
                tooltipLabel.y = tooltipY1;
                this.tooltipGraphics
                    .beginFill(tooltipConfig.background.fill, tooltipConfig.background.opacity)
                    .lineStyle(1, tooltipConfig.background.stroke, 1)
                    .drawRect(
                        tooltipX - ensureEmptyValue(tooltipConfig.padding),
                        tooltipY1 - ensureEmptyValue(tooltipConfig.padding),
                        tooltipLabel.width + 2.0 * ensureEmptyValue(tooltipConfig.padding),
                        tooltipLabel.height + 2.0 * ensureEmptyValue(tooltipConfig.padding)
                    )
                    .endFill();
            }
            featurePositions.push({
                feature: item,
                source: this.source,
                sourceValue: value,
                boundaries: {
                    x1,
                    y1,
                    x2,
                    y2
                }
            });
        }
        return featurePositions;
    }

    hover (viewport, item, coordinateSystem, options) {
        this.renderFeatures(
            viewport,
            item ? [item] : [],
            coordinateSystem,
            {
                ...(options || {}),
                graphics: this.hoveredGraphics,
                tooltip: true,
                hovered: true
            }
        );
    }

    checkFeatures (position) {
        let {x} = position;
        const {y} = position;
        x += this.dataContainer.x;
        const allHits = this.featurePositions
            .filter(position => {
                const {boundaries = {}} = position;
                const {
                    x1,
                    x2
                } = boundaries;
                return x1 <= x && x <= x2;
            });
        const accurateHits = allHits
            .filter(position => {
                const {boundaries = {}} = position;
                const {
                    y1,
                    y2
                } = boundaries;
                return y1 <= y && y <= y2;
            });
        if (accurateHits.length > 0) {
            accurateHits.sort((a, b) => a.sourceValue - b.sourceValue);
            return accurateHits;
        }
        allHits.sort((a, b) => b.sourceValue - a.sourceValue);
        return allHits;
    }
}
