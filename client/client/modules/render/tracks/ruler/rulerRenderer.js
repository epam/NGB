import * as PIXI from 'pixi.js-legacy';
import {drawingConfiguration} from '../../core';
import getRulerHeight from './rulerHeightManager';

const Math = window.Math;

export default class RulerRenderer {

    viewport;

    _globalRuler: PIXI.Container = null;
    _localRuler: PIXI.Container = null;

    _globalRulerBody: PIXI.Graphics = null;
    _localRulerBody: PIXI.Graphics = null;

    _globalRulerTicks: PIXI.Container = null;
    _localRulerTicks: PIXI.Container = null;
    _localRulerBlatRegion = null;

    _globalRulerTickLabels: PIXI.Container = null;
    _localRulerTickLabels: PIXI.Container = null;

    tickConfigs = new WeakMap();
    /**
     * @type {Map<object, Array<PIXI.Text>>}
     */
    tickLabels = new Map();

    constructor(viewport, config) {
        this.viewport = viewport;
        this._config = config;
    }

    clearContainers () {
        if (this._globalRuler) {
            this._globalRuler.removeChildren();
            this._globalRuler = null;
        }
        if (this._localRuler) {
            this._localRuler.removeChildren();
            this._localRuler = null;
        }
    }

    clearLabels () {
        this.tickLabels.forEach(labelsArray => {
            (labelsArray || []).forEach(label => {
                label.destroy();
            });
        });
        this.tickLabels.clear();
    }

    destroy () {
        this.clearContainers();
        this.clearLabels();
        this.tickLabels = null;
    }

    get blatRegionConfig() {
        return {
            fillColor: 0x92AEE7,
            fillAlpha: 0.7,
            lineColor: 0x000000,
            thinLineThickness: 0.5,
            boldLineThickness: 1.5,
            coordinatesAdd: 0.5,
            regionHeight: this._config.local.body.height - 1,
        };
    }

    init(ticks) {
        this.clearContainers();
        this.clearLabels();
        const container = new PIXI.Container();
        const _globalRulerOffsetY = this._config.brush.line.thickness;
        const _localRulerOffsetY = _globalRulerOffsetY +
            getRulerHeight(this._config.global) +
            this._config.rulersVerticalMargin + this._config.brush.line.thickness;

        const globalRulerContainers = this.createRuler(this._config.global, _globalRulerOffsetY);
        this._globalRuler = globalRulerContainers.ruler;
        this._globalRulerBody = globalRulerContainers.body;
        this._globalRulerTicks = globalRulerContainers.ticksArea;
        this._globalRulerTickLabels = globalRulerContainers.tickLabelsArea;

        const localRulerContainers = this.createRuler(this._config.local, _localRulerOffsetY, true);
        this._localRuler = localRulerContainers.ruler;
        this._localRulerBody = localRulerContainers.body;
        this._localRulerTicks = localRulerContainers.ticksArea;
        this._localRulerTickLabels = localRulerContainers.tickLabelsArea;
        this._localRulerBlatRegion = localRulerContainers.blatRegion;

        container.addChild(this._globalRuler);
        container.addChild(this._localRuler);

        this.changeDividers(
            this._globalRulerTicks,
            this._globalRulerTickLabels,
            ticks || [],
            this.viewport,
            true
        );

        return container;
    }

    createRuler(_config, rulerYOffset, isLocal = false) {
        const ruler = new PIXI.Container();
        ruler.y = rulerYOffset;

        const body = this.createRulerBody(_config);
        ruler.addChild(body);

        const ticksArea = new PIXI.Container();
        ticksArea.y = _config.tickArea.margin;
        ruler.addChild(ticksArea);

        const tickLabelsArea = new PIXI.Container();
        tickLabelsArea.y = _config.tickArea.margin;
        ruler.addChild(tickLabelsArea);

        let blatRegion = null;
        if (isLocal) {
            blatRegion = new PIXI.Container();
            ruler.addChild(blatRegion);
        }

        this.tickConfigs.set(ticksArea, _config);

        const res =  {
            body,
            ruler,
            ticksArea,
            tickLabelsArea
        };

        if (blatRegion) {
            res.blatRegion = blatRegion;
        }

        return res;
    }

    render(viewport, localTicks) {
        this.viewport = viewport;
        this.changeDividers(
            this._localRulerTicks,
            this._localRulerTickLabels,
            localTicks || [],
            viewport
        );
        this.changeBlatRegion(this._localRulerBlatRegion, viewport);
    }

    rebuild(viewport, globalTicks, localTicks) {
        this.changeRulerBody(this._globalRulerBody, this._config.global);
        this.changeRulerBody(this._localRulerBody, this._config.local);
        this.changeDividers(
            this._globalRulerTicks,
            this._globalRulerTickLabels,
            globalTicks || [],
            viewport,
            true
        );
        this.changeDividers(
            this._localRulerTicks,
            this._localRulerTickLabels,
            localTicks || [],
            viewport
        );
        this.changeBlatRegion(this._localRulerBlatRegion, viewport);
    }

    createRulerBody(_config) {
        const body = new PIXI.Graphics();
        this.changeRulerBody(body, _config);
        return body;
    }

    changeRulerBody(body, _config) {
        body.clear();
        body.beginFill(_config.body.fill, 1)
            .drawRect(0, 0, this.viewport.canvasSize, _config.body.height)
            .endFill()
            .lineStyle(_config.body.stroke.thickness, _config.body.stroke.color, 1)
            .moveTo(_config.body.stroke.thickness / 2, _config.body.stroke.thickness / 2)
            .lineTo(this.viewport.canvasSize - _config.body.stroke.thickness / 2, _config.body.stroke.thickness / 2)
            .lineTo(this.viewport.canvasSize - _config.body.stroke.thickness / 2, _config.body.height - _config.body.stroke.thickness / 2)
            .lineTo(_config.body.stroke.thickness / 2, _config.body.height - _config.body.stroke.thickness / 2)
            .lineTo(_config.body.stroke.thickness / 2, _config.body.stroke.thickness / 2);
    }

    static _labelsIntersects(labelA, labelB, labelsMinMargin) {
        return ((labelA.x + labelA.width >= labelB.x - labelsMinMargin) && (labelA.x + labelA.width <= labelB.x + labelB.width + labelsMinMargin) ||
        (labelB.x + labelB.width >= labelA.x - labelsMinMargin) && (labelB.x + labelB.width <= labelA.x + labelA.width + labelsMinMargin));
    }

    static _canPutLabel(label, tickLabels, ticksMinMargin) {
        let intersects = false;
        for (let i = 0; i < tickLabels.length; i++) {
            intersects = intersects || RulerRenderer._labelsIntersects(label, tickLabels[i], ticksMinMargin);
            if (intersects)
                break;
        }
        return !intersects;
    }

    renderTick(tick, graphics, configs, viewport, isGlobal) {
        const {tickLabels, labelsContainer, dividersGraphics} = graphics;
        const {mainConfig, tickConfig} = configs;
        if (!tick) {
            return;
        }
        const label = this.createText(
            tickConfig.formatter(tick.value),
            isGlobal ?
                viewport.project.chromoBP2pixel(tick.realValue) :
                viewport.project.brushBP2pixel(tick.realValue),
            viewport,
            labelsContainer,
            tickConfig
        );
        if (tick.isCenter) {
            dividersGraphics.lineStyle(
                tickConfig.thickness,
                this.viewport.isShortenedIntronsMode ?
                    this._config.brushColor.shortenedIntrons :
                    this._config.brushColor.normal,
                tickConfig.background.alpha
            );
        } else {
            dividersGraphics.lineStyle(tickConfig.thickness, mainConfig.body.stroke.color, 1);
        }
        RulerRenderer.appendTickGraphics(
            dividersGraphics,
            isGlobal ?
                viewport.project.chromoBP2pixel(tick.value) :
                viewport.project.brushBP2pixel(tick.value),
            mainConfig,
            viewport
        );
        if (RulerRenderer._canPutLabel(label, tickLabels, mainConfig.ticksMinMargin)) {
            label.visible = true;
            tickLabels.push(label);
        } else {
            label.visible = false;
        }
        if (tick.isCenter) {
            const white = 0xFFFFFF;
            dividersGraphics.lineStyle(0, white, 0);
            dividersGraphics
                .beginFill(
                    this.viewport.isShortenedIntronsMode ?
                    this._config.brushColor.shortenedIntrons :
                    this._config.brushColor.normal,
                    tickConfig.background.alpha
                )
                .drawRoundedRect(label.x - tickConfig.background.padding.x,
                    label.y - tickConfig.background.padding.y,
                    label.width + 2 * tickConfig.background.padding.x,
                    label.height + 2 * tickConfig.background.padding.y,
                    label.height / 2 + tickConfig.background.padding.y)
                .endFill();
            dividersGraphics.lineStyle(tickConfig.thickness, mainConfig.body.stroke.color, 1);
        }
    }

    /**
     *
     * @param container {PIXI.Container}
     * @param labelsContainer {PIXI.Container}
     * @param ticks {Array}
     * @param viewport {Viewport}
     * @param isGlobal {boolean}
     */
    changeDividers(container, labelsContainer, ticks, viewport, isGlobal = false) {
        const _config = this.tickConfigs.get(container);
        container.removeChildren();
        labelsContainer.children.forEach(child => {
            child.visible = false;
        });
        if (ticks.length > 1) {
            const dividersGraphics = new PIXI.Graphics();
            container.addChild(dividersGraphics);
            dividersGraphics.lineStyle(_config.tick.thickness, _config.body.stroke.color, 1);
            let tickLabels = [];
            const firstTick = ticks.filter(x => x.isFirst)[0];
            const centerTick = ticks.filter(x => x.isCenter)[0];
            const lastTick = ticks.filter(x => x.isLast)[0];
            this.renderTick(
                centerTick,
                {container, labelsContainer, dividersGraphics, tickLabels},
                {mainConfig: _config, tickConfig: _config.centerTick},
                viewport,
                isGlobal
            );
            this.renderTick(
                firstTick,
                {container, labelsContainer, dividersGraphics, tickLabels},
                {mainConfig: _config, tickConfig: _config.tick},
                viewport,
                isGlobal
            );
            this.renderTick(
                lastTick,
                {container, labelsContainer, dividersGraphics, tickLabels},
                {mainConfig: _config, tickConfig: _config.tick},
                viewport,
                isGlobal
            );
            if (isGlobal || !this.viewport.shortenedIntronsViewport.shortenedIntronsMode) {
                for (let i = 0, len = ticks.length; i < len; i++) {
                    const tick = ticks[i];
                    if (tick.isFirst || tick.isLast || tick.isCenter)
                        continue;
                    this.renderTick(
                        tick,
                        {container, labelsContainer, dividersGraphics, tickLabels},
                        {mainConfig: _config, tickConfig: _config.tick},
                        viewport,
                        isGlobal
                    );
                }
            }
            tickLabels = null;
        }
    }

    changeBlatRegion(container, viewport) {
        container.removeChildren();
        if (viewport.blatRegion && viewport.blatRegion.chromosomeName === viewport.projectContext.currentChromosome.name) {
            const blatRegionGraphics = new PIXI.Graphics();
            container.addChild(blatRegionGraphics);

            this.renderBlatRegion(blatRegionGraphics, viewport.blatRegion);
        }
    }

    renderBlatRegion(blatRegionGraphics, region) {
        const { fillColor, fillAlpha, lineColor, thinLineThickness, boldLineThickness, coordinatesAdd, regionHeight } = this.blatRegionConfig;
        const BP2Pixel = this.viewport.project.brushBP2pixel;

        const renderHatchFn = (start, boundaries) => {
            const { x1, x2 } = boundaries;
            const pointA = { x: start, y: regionHeight };
            const pointB = { x: start + regionHeight, y: 0 };
            if (pointA.x < x1) {
                pointA.x = x1;
                pointA.y = pointB.x - x1;
            }
            if (pointB.x > x2) {
                pointB.y = pointB.x - x2;
                pointB.x = x2;
            }
            blatRegionGraphics
                .moveTo(pointA.x, pointA.y)
                .lineTo(pointB.x, pointB.y);
        };
        const renderHatchingFn = (item) => {
            const hatchAlpha = 0.5;
            const hatchStep = 8;

            const startPx = Math.max(- this.viewport.canvasSize, BP2Pixel(item.start - coordinatesAdd));
            const endPx = Math.min(2 * this.viewport.canvasSize, BP2Pixel(item.end + coordinatesAdd));

            blatRegionGraphics.lineStyle(thinLineThickness, lineColor, hatchAlpha);

            for (let i = startPx - regionHeight; i < endPx; i += hatchStep) {
                renderHatchFn(i, { x1: startPx, x2: endPx });
            }
        };

        blatRegionGraphics
            .lineStyle(0, lineColor, 1)
            .beginFill(fillColor, fillAlpha)
            .drawRect(
                BP2Pixel(region.start - coordinatesAdd),
                0,
                BP2Pixel(region.end + coordinatesAdd) - BP2Pixel(region.start - coordinatesAdd),
                regionHeight
            )
            .endFill()
            .lineStyle(boldLineThickness, lineColor, 1)
            .moveTo(BP2Pixel(region.start - coordinatesAdd), 0)
            .lineTo(BP2Pixel(region.start - coordinatesAdd), regionHeight)
            .moveTo(BP2Pixel(region.end + coordinatesAdd), 0)
            .lineTo(BP2Pixel(region.end + coordinatesAdd), regionHeight);

        renderHatchingFn(region);
    }

    static appendTickGraphics(graphics, x, _config, viewport) {
        graphics
            .moveTo(Math.min(Math.max(x, _config.tick.thickness / 2), viewport.canvasSize - _config.tick.thickness / 2),
                0)
            .lineTo(Math.min(Math.max(x, _config.tick.thickness / 2), viewport.canvasSize - _config.tick.thickness / 2),
                _config.tick.height);
    }

    static ticks = 0;

    /**
     * @param config {PIXI.TextStyle}
     * @param container {PIXI.Container}
     * @returns {PIXI.Text|unknown}
     */
    getInvisibleLabelFromCache (config, container) {
        if (!this.tickLabels.has(config)) {
            this.tickLabels.set(config, []);
        }
        const labels = this.tickLabels.get(config) || [];
        const [invisibleLabel] = labels.filter(label => label.parent === container && !label.visible);
        if (invisibleLabel) {
            invisibleLabel.visible = true;
            invisibleLabel.style = config;
            return invisibleLabel;
        }
        const label = new PIXI.Text('', config);
        label.resolution = drawingConfiguration.resolution;
        label.visible = true;
        container.addChild(label);
        labels.push(label);
        this.tickLabels.set(config, labels);
        return label;
    }

    createText(label, x, viewport, container, _config) {
        const text = this.getInvisibleLabelFromCache(_config.label, container);
        text.text = label;
        text.y = _config.height + _config.margin;
        text.x = Math.max(0, Math.min(viewport.canvasSize - text.width, x - (text.width / 2)));
        text.x = Math.round(text.x);
        return text;
    }
}
