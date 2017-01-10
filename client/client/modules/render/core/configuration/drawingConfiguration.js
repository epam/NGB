export default {
    antialias: false,
    resolution: getScreenResolution(),
    scale: getScreenResolution()
};

function getScreenResolution() {
    let scale = window.devicePixelRatio;
    if (scale === null || scale === undefined || scale < 1) {
        scale = 1;
    }
    return scale;
}
