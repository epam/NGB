import RulerBrush from './rulerBrush';
import RulerConfig from './rulerConfig';
import RulerRenderer from './rulerRenderer';
import RulerTransformer from './rulerTransformer';
import {Track} from '../../core';
import getRulerHeight from './rulerHeightManager';

export class RulerTrack extends Track {

    renderer = new RulerRenderer(this.viewport, this.trackConfig);
    brush = new RulerBrush(::this.moveBrush, this.viewport, this.trackConfig);

    static getTrackDefaultConfig() {
        return RulerConfig;
    }

    constructor(opts){
        super(opts);
        this.height = this.getTotalHeight();
        this.brush.requestRenderRefresh = ::this.requestRenderRefresh;
        this.brush.updateScene = ::this.updateScene;
    }

    get trackIsResizable() {
        return false;
    }

    getTotalHeight() {
        return getRulerHeight(this.trackConfig.global) +
            getRulerHeight(this.trackConfig.local) +
            2 * this.trackConfig.brush.line.thickness + this.trackConfig.rulersVerticalMargin;
    }

    async getNewCache() {
    }

    render(flags) {
        if (flags.renderReset) {
            this.container.removeChildren();
            this.container.addChild(this.renderer.init(RulerTransformer.transform(this.viewport,
                this.trackConfig.global, true)));
            this.container.addChild(this.brush.container);
        }
        if (flags.widthChanged || flags.renderReset){
            this.renderer.rebuild(this.viewport,
                RulerTransformer.transform(this.viewport, this.trackConfig.global, true),
                RulerTransformer.transform(this.viewport, this.trackConfig.local));
        }
        if (flags.brushChanged || flags.widthChanged || flags.renderReset) {
            this.brush.render();
            this.renderer.render(this.viewport, RulerTransformer.transform(this.viewport, this.trackConfig.local));
        }
        return true;
    }

    clearData() {
        this.brush.clearData();
        super.clearData();
    }
}
