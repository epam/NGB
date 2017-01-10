import PIXI from 'pixi.js';

export default class PixiTextSize {

    static _label = null;

    static get label() {
        if (PixiTextSize._label === null) {
            PixiTextSize._label = new PIXI.Text('', {});
        }
        return PixiTextSize._label;
    }

    static getTextSize(text, _config) {
        PixiTextSize.label.text = text;
        PixiTextSize.label.style = _config;
        return {
            height: PixiTextSize.label.height,
            width: PixiTextSize.label.width
        };
    }
}
