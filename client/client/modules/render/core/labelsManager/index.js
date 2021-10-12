import {Sprite, Text, TextStyle} from 'pixi.js-legacy';
import {drawingConfiguration} from '../configuration';

export default class LabelsManager {
    /**
     * @param {PIXI.Renderer | PIXI.CanvasRenderer} renderer
     */
    constructor(renderer) {
        /**
         *
         * @type {Map<string, PIXI.Texture>}
         */
        this.texturesCache = new Map();
        this.labelStyles = {};
        this.labelStyleKeys = new WeakMap();
        this.renderer = renderer;
        this.label = new Text('');
        this.label.resolution = drawingConfiguration.resolution;
        this.removableTextureKeys = new Set();
        this.textureKeysToRemove = new Set();
    }

    getLabelStyleKey (style = {}) {
        if (!this.labelStyleKeys.has(style)) {
            const keys = Object.keys(style).sort();
            this.labelStyleKeys.set(style, keys.map(key => `${key}:${style[key]}`).join('|'));
        }
        return this.labelStyleKeys.get(style);
    }

    /**
     * Returns sprite with text
     * @param {string} text
     * @param {object} style
     * @param {boolean} removable
     * @returns {PIXI.Text | PIXI.Sprite}
     */
    getLabel (text, style, removable = false) {
        if (this.renderer) {
            const texture = this.getTexture(text, style, removable);
            return new Sprite(texture);
        } else {
            const label = new Text(text, style);
            label.resolution = drawingConfiguration.resolution;
            return label;
        }
    }
    getTexture (text, style, removable = false) {
        const {key, styleKey} = this.getKeys(text, style);
        const textStyle = this.labelStyles[styleKey];
        if (
            !this.texturesCache.has(key) ||
            !this.texturesCache.get(key).baseTexture
        ) {
            if (this.label.style !== textStyle && textStyle) {
                this.label.style = textStyle;
            }
            this.label.text = text;
            const texture = this.renderer.generateTexture(this.label, {
                scaleMode: drawingConfiguration.scaleMode,
                resolution: drawingConfiguration.resolution
            });
            this.texturesCache.set(key, texture);
        }
        if (removable) {
            this.removableTextureKeys.add(key);
        } else if (this.removableTextureKeys.has(key)) {
            this.removableTextureKeys.delete(key);
        }
        if (removable && this.textureKeysToRemove.has(key)) {
            this.textureKeysToRemove.delete(key);
        }
        return this.texturesCache.get(key);
    }

    getKeys(text, style) {
        const styleKey = this.getLabelStyleKey(style);
        if (!this.labelStyles.hasOwnProperty(styleKey)) {
            this.labelStyles[styleKey] = new TextStyle(style);
        }
        return {
            key: `${text || ''}|${styleKey}`,
            styleKey
        };
    }

    getCacheSize() {
        return this.texturesCache.size;
    }

    startSession () {
        this.textureKeysToRemove = new Set(this.removableTextureKeys);
    }

    finishSession () {
        const remove = [];
        for (const key of this.texturesCache.keys()) {
            if (this.textureKeysToRemove.has(key)) {
                remove.push(key);
            }
        }
        remove.forEach(this.clearTextureByKey.bind(this));
    }

    clearTextureByName (name) {
        const keysToRemove = [];
        for (const key of this.texturesCache.keys()) {
            const text = key.split('|').pop();
            if (name === text) {
                keysToRemove.push(key);
            }
        }
        keysToRemove.forEach(this.clearTextureByKey.bind(this));
    }

    clearTextureByKey (key) {
        if (this.texturesCache.has(key)) {
            const texture = this.texturesCache.get(key);
            texture.destroy(true);
            this.texturesCache.delete(key);
        }
    }

    clear () {
        this.texturesCache.forEach(texture => {
            texture.destroy(true);
        });
        this.texturesCache.clear();
    }

    destroy () {
        this.clear();
        this.label.destroy({texture: true, baseTexture: true});
        this.label = null;
        this.texturesCache = null;
    }
    ensureLabel(label, text, style) {
        if (label && (!label.texture || !label.texture.baseTexture)) {
            label.texture = this.getTexture(text, style);
        }
    }
}
