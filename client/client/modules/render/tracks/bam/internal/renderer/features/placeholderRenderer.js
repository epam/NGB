import PIXI from 'pixi.js';
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

    init(maxRequest, size) {
        const {height, width} = size;
        if (this._placeholder) {
            this._container.removeChild(this._placeholder);
        }
        const unitThreshold = 1000;
        const noReadText = {
            unit: maxRequest < unitThreshold ? 'BP' : 'kBP',
            value: maxRequest < unitThreshold ? maxRequest : Math.ceil(maxRequest / unitThreshold)
        };
        const text = `Zoom in to see reads.
Minimal zoom level is at ${noReadText.value}${noReadText.unit}`;
        this._placeholder = new PIXI.Text(text, {align: 'center'});
        this._placeholder.resolution = drawingConfiguration.resolution;
        this._placeholder.x = width / drawingConfiguration.scale / 2 - this._placeholder.width / 2;
        this._placeholder.y = height / drawingConfiguration.scale / 2 - this._placeholder.height / 2;
        this._container.addChild(this._placeholder);
    }
}