import * as PIXI from 'pixi.js-legacy';
import drawingConfiguration from './drawingConfiguration';

PIXI.settings.ROUND_PIXELS = true;
PIXI.settings.RESOLUTION = drawingConfiguration.resolution;
PIXI.settings.SCALE_MODE = drawingConfiguration.scaleMode;

export default function getRenderer(size, opts = null, forceWebGL = false) {
    const {width, height} = size;
    if (opts === null) {
        opts = {
            backgroundColor: 0xf9f9f9
        };
    }
    const rendererConstructor = forceWebGL ? PIXI.Renderer : PIXI.CanvasRenderer;
    const {backgroundColor} = opts;
    const renderer = new rendererConstructor({
        width,
        height,
        antialias: drawingConfiguration.antialias,
        preserveDrawingBuffer: forceWebGL,
        useContextAlpha: false,
    });
    renderer.backgroundColor = backgroundColor;
    return renderer;
}
