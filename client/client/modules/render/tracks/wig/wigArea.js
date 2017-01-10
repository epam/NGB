import {Viewport, drawingConfiguration} from '../../core';
import PIXI from 'pixi.js';

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
    }

    get axis(){ return this._area; }
    get height() { return this._height; }
    set height(value) { this._height = value; }

    render(viewport, coordinateSystem){
        this._viewport = viewport;
        this._changeAxises(viewport, coordinateSystem);
    }

    _changeAxises(viewport, coordinateSystem){
        if (coordinateSystem === null || coordinateSystem === undefined)
            return;
        if (this._area.children.length > 0) {
            this._area.removeChildren(0, this._area.children.length);
        }

        const dashSize = 2;
        const dashCount = Math.floor(viewport.canvasSize / (dashSize * 2));
        for (let dividerIndex = 0; dividerIndex < coordinateSystem.dividers.length; dividerIndex++) {
            const divider = coordinateSystem.dividers[dividerIndex];
            const axis = new PIXI.Graphics();
            const y = this.height * (divider.value - coordinateSystem.minimum) / (coordinateSystem.maximum - coordinateSystem.minimum);
            if (divider.value === 0){
                axis.lineStyle(1, this._config.divider.color, 1);
                axis.moveTo(0, this.height - y);
                axis.lineTo(viewport.canvasSize, this.height - y);
            }
            else {
                for (let dash = 0; dash < dashCount; dash++) {
                    axis.lineStyle(1, this._config.divider.color, 1);
                    axis.moveTo(dash * 2 * dashSize, this.height - y);
                    axis.lineTo((dash * 2 + 1) * dashSize, this.height - y);
                }
            }
            axis.endFill();
            this._area.addChild(axis);
            if (divider.value !== 0) {
                const label = new PIXI.Text(divider.value, {font: '30px'});
                label.resolution = drawingConfiguration.resolution;
                label.x = 0;
                label.y = Math.round(this.height - y - label.height);
                this._area.addChild(label);
            }
        }
    }

}
