import HeatmapCanvasGraphics from './canvas-graphics';
import HeatmapGraphicsBase from './base';
import HeatmapWebGlGraphics from './webgl-graphics';

/**
 *
 * @param {HeatmapGraphicsOptions} options
 */
HeatmapGraphicsBase.createInstance = function (options) {
    const {
        CANVAS_RENDERER = true
    } = options || {};
    if (CANVAS_RENDERER) {
        return new HeatmapCanvasGraphics(options);
    }
    return new HeatmapWebGlGraphics(options);
};

export default HeatmapGraphicsBase;
