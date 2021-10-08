import {SCALE_MODES} from 'pixi.js-legacy';

export default {
    antialias: false,
    resolution: getScreenResolution(),
    scale: getScreenResolution(),
    scaleMode: SCALE_MODES.LINEAR
};

function getScreenResolution() {
    let scale = window.devicePixelRatio;
    if (scale === null || scale === undefined || scale < 1) {
        scale = 1;
    }
    return scale;
}
