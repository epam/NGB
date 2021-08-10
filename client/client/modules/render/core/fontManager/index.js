import * as PIXI from 'pixi.js-legacy';


/**
 * @typedef {Object} fontStyle
 *
 * @property {boolean} dropShadow
 * @property {number} dropShadowAngle
 * @property {number} dropShadowBlur
 * @property {string | number} dropShadowColor
 * @property {number} dropShadowDistance
 * @property {string | string[] | number | number[] | CanvasGradient | CanvasPattern} fill
 * @property {number[]} fillGradientStops
 * @property {number} fillGradientType
 * @property {string | string[]} fontFamily
 * @property {number} fontSize
 * @property {string} fontVariant
 * @property {string} fontWeight
 * @property {string} lineJoin
 * @property {number} miterLimit
 * @property {number} padding
 * @property {string | number} stroke
 * @property {number} strokeThickness
 * @property {string} textBaseline
 *
 * */

export default class FontManager {

    constructor() {
    }

    /**
     * Returns font name to use with BitmapText
     * @param fontStyle {fontStyle | PIXI.TextStyle}
     *
     * @return fontName {String}
     * */
    static getFontByStyle(fontStyle) {
        if (!fontStyle) {
            return null;
        }
        const fontName = FontManager._getName(fontStyle);
        if (!FontManager._fontAvailable(fontName)) {
            FontManager._createFont(fontName, fontStyle);
        }

        return fontName;
    }

    /**
     * Returns font name to use with BitmapText
     * @param fontStyle {fontStyle | PIXI.TextStyle}
     *
     * @return fontName {String}
     * */
    static _getName(fontStyle) {
        return fontStyle.toFontString ? fontStyle.toFontString() : JSON.stringify(fontStyle);
    }

    /**
     * Checks if such font's already available
     * @param fontName {string}
     *
     * @return {boolean}
     * */
    static _fontAvailable(fontName) {
        return !!PIXI.BitmapFont.available[fontName];
    }

    /**
     * Creates BitmapFont name to use with BitmapText
     * @param fontName {String}
     * @param fontStyle {fontStyle | PIXI.TextStyle}
     *
     * @return font {BitmapFont}
     * */
    static _createFont(fontName, fontStyle) {
        return PIXI.BitmapFont.from(
            fontName,
            fontStyle,
            {
                chars: PIXI.BitmapFont.ASCII,
                resolution: PIXI.settings.RESOLUTION,
            }
        );
    }
}
