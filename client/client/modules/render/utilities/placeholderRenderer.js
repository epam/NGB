import * as PIXI from 'pixi.js-legacy';
import {drawingConfiguration} from '../core';

const placeholderLabelStyle = {align: 'center', fontWeight: 'bold'};

export default class PlaceholderRenderer {
    _container = null;
    _placeholder = null;

    get container() {
        return this._container;
    }

    constructor(track) {
        this._container = new PIXI.Container();
        this._track = track;
    }

    render() {

    }

    init(text, size) {
        const {height, width} = size;
        if (this._placeholder) {
            this._container.removeChild(this._placeholder);
        }
        if (this._track && this._track.labelsManager) {
            this._placeholder = this._track.labelsManager.getSprite(text, placeholderLabelStyle);
            if (this._placeholder) {
                this._placeholder.x = width / drawingConfiguration.scale / 2 - this._placeholder.width / 2;
                this._placeholder.y = height / drawingConfiguration.scale / 2 - this._placeholder.height / 2;
                this._container.addChild(this._placeholder);
            }
        }
    }
}
