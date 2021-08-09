import * as PIXI from 'pixi.js-legacy';
import {drawingConfiguration} from '../../../../../core';

const Math = window.Math;

export class PlaceholderRenderer {

    _container = null;
    _placeholder = null;

    get container() {
        return this._container;
    }

    constructor() {
        this._container = new PIXI.Container();
    }

    render() {

    }

    init(text, size) {
        const {height, width} = size;
        if (this._placeholder) {
            this._container.removeChild(this._placeholder);
        }
        this._placeholder = new PIXI.Text(text, {align: 'center'});
        this._placeholder.resolution = drawingConfiguration.resolution;
        this._placeholder.x = width / drawingConfiguration.scale / 2 - this._placeholder.width / 2;
        this._placeholder.y = height / drawingConfiguration.scale / 2 - this._placeholder.height / 2;
        this._container.addChild(this._placeholder);
    }
}
