import {Viewport, drawingConfiguration} from '../../core';
import PIXI from 'pixi.js';

const Math = window.Math;

export default class WIGArea{

    _height = null;

    constructor(viewport, config){
        if (!(viewport instanceof Viewport)) {
            throw new TypeError('WIGArea: `viewport` is not instance of Viewport');
        }
        this._viewport = viewport;
        this._config = config;
        this._height = config.height;
        this._area = new PIXI.Container();
        this._logScaleIndicator = this._createLogScaleIndicator();
        this._logScaleIndicator.visible = false;
    }

    get axis(){ return this._area; }
    get logScaleIndicator() {return this._logScaleIndicator; }
    get height() { return this._height; }
    set height(value) { this._height = value; }

    render(viewport, coordinateSystem){
        this._viewport = viewport;
        this._changeAxises(viewport, coordinateSystem);
    }

    _createLogScaleIndicator() {
        const container = new PIXI.Container();
        container.x = this._config.logScaleIndicator.margin;
        container.y = this._config.logScaleIndicator.margin;
        const text = new PIXI.Text('log', this._config.logScaleIndicator.label);
        text.resolution = drawingConfiguration.resolution;
        text.x = Math.round(this._config.logScaleIndicator.padding);
        text.y = Math.round(this._config.logScaleIndicator.padding);

        const graphics = new PIXI.Graphics();
        graphics.beginFill(this._config.logScaleIndicator.fill, this._config.logScaleIndicator.alpha);
        graphics.drawRoundedRect(
            0,
            0,
            this._config.logScaleIndicator.padding * 2.0 + text.width,
            this._config.logScaleIndicator.padding * 2.0 + text.height,
            Math.max(this._config.logScaleIndicator.padding * 2.0 + text.width, this._config.logScaleIndicator.padding * 2.0 + text.height) / 3.0);
        graphics.endFill();

        container.addChild(graphics);
        container.addChild(text);

        return container;
    }

    _changeAxises(viewport, coordinateSystem){
        if (coordinateSystem === null || coordinateSystem === undefined)
            return;
        this._logScaleIndicator.visible = coordinateSystem.isLogScale;
        if (this._area.children.length > 0) {
            this._area.removeChildren(0, this._area.children.length);
        }

        const dashSize = 2;
        const spaceSize = 2;
        const dashCount = Math.floor(viewport.canvasSize / (dashSize + spaceSize));
        for (let dividerIndex = 0; dividerIndex < coordinateSystem.dividers.length; dividerIndex++) {
            const divider = coordinateSystem.dividers[dividerIndex];
            const axis = new PIXI.Graphics();
            const y = this.height * (divider.value - coordinateSystem.minimum) / (coordinateSystem.maximum - coordinateSystem.minimum);
            if (divider.value === 0){
                axis.lineStyle(1, this._config.divider.color, 0.5);
                axis.moveTo(0, Math.floor(this.height - y) - 0.5);
                axis.lineTo(viewport.canvasSize, Math.floor(this.height - y) - 0.5);
            }
            else {
                for (let dash = 0; dash < dashCount; dash++) {
                    axis.lineStyle(1, this._config.divider.color, 0.5);
                    axis.moveTo(dash * (dashSize + spaceSize), Math.floor(this.height - y) - 0.5);
                    axis.lineTo(dash * (dashSize + spaceSize) + dashSize, Math.floor(this.height - y) - 0.5);
                }
            }
            axis.endFill();
            this._area.addChild(axis);
            if (divider.value !== 0) {
                let value = coordinateSystem.isLogScale ? Math.pow(10, divider.value)  : divider.value;
                const label = new PIXI.Text(value, {font: '30px'});
                label.resolution = drawingConfiguration.resolution;
                label.x = 0;
                label.y = Math.round(this.height - y - label.height);
                this._area.addChild(label);
            }
        }
    }

}
