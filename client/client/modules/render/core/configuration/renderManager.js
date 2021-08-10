import * as PIXI from 'pixi.js-legacy';
import drawingConfiguration from './drawingConfiguration';

PIXI.settings.ROUND_PIXELS = true;
PIXI.settings.RESOLUTION = drawingConfiguration.resolution;
PIXI.settings.SCALE_MODE = PIXI.SCALE_MODES.NEAREST;

export default function getRenderer(size, opts = null) {
    const {width, height} = size;
    if (opts === null) {
        opts = {
            backgroundColor: 0xf9f9f9
        };
    }
    const {backgroundColor} = opts;
    const renderer = new PIXI.CanvasRenderer({
        width,
        height,
        antialias: drawingConfiguration.antialias,
        preserveDrawingBuffer: false,
        transparent: false
    });
    renderer.backgroundColor = backgroundColor;
    return renderer;
}
