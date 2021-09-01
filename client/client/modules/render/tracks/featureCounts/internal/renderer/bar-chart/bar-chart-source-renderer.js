import {ColorProcessor} from '../../../../../utilities';
import CoordinateSystem from '../../../../common/coordinateSystemRenderer';
import PIXI from 'pixi.js';
import {drawingConfiguration} from '../../../../../core';
import ensureEmptyValue from '../../../../../utilities/ensureEmptyValue';

const white = 0xffffff;

export default class BarChartSourceRenderer extends PIXI.Container {
    constructor(track, config, source) {
        super();
        this._source = source;
        this.track = track;
        this.config = config;
        this.dataContainer = new PIXI.Container();
        this.graphics = new PIXI.Graphics();
        this.lineGraphics = new PIXI.Graphics();
        this.hoveredGraphics = new PIXI.Graphics();
        this.hoveredLineGraphics = new PIXI.Graphics();
        this.dataContainer.addChild(this.graphics);
        this.dataContainer.addChild(this.hoveredGraphics);
        this.dataContainer.addChild(this.lineGraphics);
        this.dataContainer.addChild(this.hoveredLineGraphics);
        this.coordinateSystemRenderer = new CoordinateSystem(track);
        this.sourceNameLabel = new PIXI.Text(source, config.barChart.title);
        this.sourceNameLabel.resolution = drawingConfiguration.resolution;
        this.background = new PIXI.Graphics();
        this.groupAutoScaleIndicator = new PIXI.Graphics();
        this.addChild(this.background);
        this.addChild(this.dataContainer);
        this.addChild(this.coordinateSystemRenderer);
        this.addChild(this.sourceNameLabel);
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
            return;
        }
        this.dataContainer.x = drawScope.containerTranslateFactor * drawScope.scaleFactor;
        this.dataContainer.scale.x = drawScope.scaleFactor;
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

    getDrawingArea = (height) => {
        const top = ensureEmptyValue(this.config.barChart.margin.top);
        const bottom = height - ensureEmptyValue(this.config.barChart.margin.bottom);
        return {
            top,
            bottom,
            height: bottom - top
        };
    };

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
        const {top, bottom, height: drawingHeight} = this.getDrawingArea(height);
        this.sourceNameLabel.x = Math.round(ensureEmptyValue(barChartConfig.title.margin));
        this.sourceNameLabel.y = Math.round(top + ensureEmptyValue(barChartConfig.title.margin));
        this.coordinateSystemRenderer.render(
            viewport,
            coordinateSystem,
            drawingHeight,
            {
                yBoundaries: {
                    top: this.sourceNameLabel.y +
                        this.sourceNameLabel.height +
                        ensureEmptyValue(barChartConfig.title.margin) -
                        top
                }
            }
        );
        this.coordinateSystemRenderer.x = 0;
        this.coordinateSystemRenderer.y = bottom - drawingHeight;
        this.featurePositions = this.renderFeatures(
            viewport,
            data,
            coordinateSystem,
            {
                ...(options || {}),
                graphics: this.graphics,
                lineGraphics: this.lineGraphics
            }
        );
    }

    renderFeatures (viewport, data, coordinateSystem, options) {
        const {
            graphics,
            lineGraphics,
            height = 0,
            features = [],
            hovered = false,
            singleColors = false,
            grayScaleColors = false
        } = options || {};
        const colorOptions = {
            hovered,
            singleColor: singleColors,
            grayScale: grayScaleColors
        };
        const color = this.fcSourcesManager
            ? this.fcSourcesManager.getColorConfiguration(this.source, colorOptions)
            : this.config.barChart.bar;
        const borderColor = this.fcSourcesManager
            ? this.fcSourcesManager.getColorConfiguration(this.source, {...colorOptions, border: true})
            : ColorProcessor.darkenColor(this.config.barChart.bar);
        if (!graphics) {
            return;
        }
        graphics.clear();
        lineGraphics.clear();
        if (!data || !coordinateSystem) {
            return;
        }
        const top = ensureEmptyValue(this.config.barChart.margin.top);
        const bottom = height - ensureEmptyValue(this.config.barChart.margin.bottom);
        const correctYPosition = o => Math.max(top, Math.min(bottom, o));
        const drawingHeight = bottom - top;
        const convertSourceValueToPixel = CoordinateSystem.getConverter(coordinateSystem, drawingHeight);
        const baseLine = top + CoordinateSystem.getBaseLine(coordinateSystem, drawingHeight);
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
            const yValue = bottom - convertSourceValueToPixel(value);
            if (Math.abs(baseLine - yValue) === 0) {
                continue;
            }
            if (
                coordinateSystem &&
                coordinateSystem.log &&
                yValue >= baseLine
            ) {
                continue;
            }
            let y1, y2;
            if (yValue < baseLine) {
                y1 = Math.floor(correctYPosition(Math.min(yValue, baseLine - 1))); // ensure 1px height
                y2 = Math.floor(baseLine);
            } else {
                y1 = Math.ceil(baseLine);
                y2 = Math.ceil(correctYPosition(Math.max(yValue, baseLine + 1))); // ensure 1px height
            }
            const height = y2 - y1;
            graphics
                .beginFill(color, 1)
                .lineStyle(0, 0x0, 0)
                .drawRect(
                    x1,
                    y1,
                    x2 - x1,
                    height
                )
                .endFill();
            lineGraphics
                .lineStyle(1, borderColor, 1)
                .moveTo(x1, y2)
                .lineTo(x1, y1)
                .lineTo(x2, y1)
                .lineTo(x2, y2);
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
                lineGraphics: this.hoveredLineGraphics,
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
