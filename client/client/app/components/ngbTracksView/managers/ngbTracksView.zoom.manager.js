import {Viewport} from '../../../../modules/render/core';

export default class ngbTracksViewZoomManager{

    _viewport: Viewport = null;

    constructor(viewport: Viewport){
        this._viewport = viewport;
    }

    get canZoom() {
        return this._viewport && this._viewport.canTransform;
    }

    zoomIn(){
        const newBrushSize = this._viewport.brushSize * (0.75);
        const brushCenter = (this._viewport.brush.start + this._viewport.brush.end) / 2;
        this._viewport.transform({
            start: brushCenter - newBrushSize / 2,
            end: brushCenter + newBrushSize / 2
        });
    }

    zoomOut(){
        const newBrushSize = this._viewport.brushSize * (1.25);
        const brushCenter = (this._viewport.brush.start + this._viewport.brush.end) / 2;
        this._viewport.transform({
            start: brushCenter - newBrushSize / 2,
            end: brushCenter + newBrushSize / 2
        });
    }
}
