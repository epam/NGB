import PIXI from 'pixi.js';

export default class CachedTrackRenderer{

    _labels = [];
    containerIsReady = false;
    container = new PIXI.Container();
    dataContainer = Object.assign(new PIXI.Container(), {interactiveChildren: false});
    _backgroundContainer = new PIXI.Container();
    _drawScope = {
        scaleFactor: 1,
        translateFactor: 0
    };
    _height = 0;
    _config = null;
    _centerLineGraphics = new PIXI.Graphics();

    constructor(){
        this.container.addChild(this._backgroundContainer);
        this.container.addChild(this.dataContainer);
    }

    get viewport() { return this._viewport; }

    initializeCentralLine() {
        this.container.addChild(this._centerLineGraphics);
    }

    render(viewport, cache, forceRedraw = false, _gffShowNumbersAminoacid, _showCenterLine){
        if (cache === null || cache === undefined || cache.viewport === undefined)
            return;
        const factor = viewport.factor / cache.viewport.factor;
        const factorMaximumDelta = 0.1;
        if (!forceRedraw && this.containerIsReady && viewport && cache && cache.viewport &&
            Math.abs(factor - 1) < factorMaximumDelta && !cache.isNew){
            this.translateContainer(viewport, cache);
        }
        else {
            this._viewport = cache.viewport;
            this.containerIsReady = false;
            this._labels = [];
            this.rebuildContainer(viewport, cache);
            this.containerIsReady = true;
            cache.isNew = false;
        }
        this.renderCenterLine(viewport, {
            config: this._config,
            graphics: this._centerLineGraphics,
            height: this._height,
            shouldRender: _showCenterLine
        });
    }

    translateContainer(viewport, cache){
        this.createDrawScope(viewport, cache);
        this.dataContainer.x = this._drawScope.containerTranslateFactor * this._drawScope.scaleFactor;
        this.dataContainer.scale.x = this._drawScope.scaleFactor;
        for (let i = 0; i < this._labels.length; i++){
            const label = this._labels[i];
            const labelCenter = label.x + label.width / 2;
            label.scale.x = 1 / this._drawScope.scaleFactor;
            label.x = labelCenter - label.width / 2;
        }
    }

    rebuildContainer(viewport, cache){
        this.dataContainer.x = 0;
        this.dataContainer.scale.x = 1;
        this.createDrawScope(viewport, cache);
    }

    createDrawScope(viewport, cache){
        this._drawScope = {
            containerTranslateFactor: (cache.viewport.brush.start - viewport.brush.start) * cache.viewport.factor,
            scaleFactor: viewport.factor / cache.viewport.factor,
            translateFactor: - (viewport.brush.start - cache.viewport.brush.start) * cache.viewport.factor
        };
    }

    correctedXPosition(x){
        return (x + this._drawScope.translateFactor) * this._drawScope.scaleFactor;
    }

    correctCanvasXPosition (position, viewport) {
        return Math.max(
            Math.min(position, 2.0 * viewport.canvasSize),
            -viewport.canvasSize
        );
    }

    correctedXMeasureValue(measure){
        return measure * this._drawScope.scaleFactor;
    }

    renderCenterLine(viewport, drawingConfig) {
        const {config, graphics, height, shouldRender} = drawingConfig;

        graphics.clear();
        if (shouldRender) {
            const dashesCount = height / (2 * config.centerLine.dash.length);
            const length = config.centerLine.dash.length;
            const thickness = config.centerLine.dash.thickness;
            const color = config.centerLine.dash.fill;
            const drawVerticalDashLine = (x) => {
                graphics.lineStyle(thickness, color, 1);
                for (let i = 0; i < dashesCount; i++) {
                    graphics
                        .moveTo(Math.floor(x) + thickness / 2.0, (2 * i) * length)
                        .lineTo(Math.floor(x) + thickness / 2.0, (2 * i + 1) * length);
                }
            };
            const center = Math.round(viewport.centerPosition);
            if (viewport.factor > 2) {
                drawVerticalDashLine(viewport.project.brushBP2pixel(center) - viewport.factor / 2);
                drawVerticalDashLine(viewport.project.brushBP2pixel(center) + viewport.factor / 2);
            }
            else {
                drawVerticalDashLine(viewport.project.brushBP2pixel(center));
            }
        }
    }
}
