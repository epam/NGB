import * as PIXI from 'pixi.js-legacy';

export default class PixiTextSize {

    static _label = null;

    static get label() {
        if (PixiTextSize._label === null) {
            PixiTextSize._label = new PIXI.Text('', {});
        }
        return PixiTextSize._label;
    }

    static getTextSize(text, _config, isBitmapText = false) {
        PixiTextSize.label.text = text;
        PixiTextSize.label.style = {
            ...isBitmapText ? {letterSpacing: 0.5} : {},
            ..._config
        };
        return {
            height: PixiTextSize.label.height,
            width: PixiTextSize.label.width
        };
    }

    static trimTextToFitWidth(text, width, _config) {
        if (!text || PixiTextSize.getTextSize(text, _config).width <= width) {
            return text;
        }
        let result = text.substr(0, text.length - 3);
        while (result.length && PixiTextSize.getTextSize(`${result}...`, _config).width > width) {
            result = result.substr(0, result.length - 1);
        }
        return `${result}...`;
    }
}
