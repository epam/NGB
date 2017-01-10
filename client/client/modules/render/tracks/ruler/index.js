import RulerBrush from './rulerBrush';
import RulerConfig from './rulerConfig';
import RulerRenderer from './rulerRenderer';
import RulerTransformer from './rulerTransformer';
import {Track} from '../../core';
import getRulerHeight from './rulerHeightManager';

export class RulerTrack extends Track {

    renderer = new RulerRenderer(this.viewport, RulerConfig);
    brush = new RulerBrush(::this.moveBrush, this.viewport, RulerConfig);

    constructor(opts){
        super(opts);
        this.height = RulerTrack.getTotalHeight();
        this.brush.requestRenderRefresh = ::this.requestRenderRefresh;
        this.brush.updateScene = ::this.updateScene;
    }

    static getTotalHeight() {
        return getRulerHeight(RulerConfig.global) +
            getRulerHeight(RulerConfig.local) +
            2 * RulerConfig.brush.line.thickness + RulerConfig.rulersVerticalMargin;
    }

    async getNewCache() {
    }

    render(flags) {
        if (flags.renderReset) {
            this.container.removeChildren();
            this.container.addChild(this.renderer.init(RulerTransformer.transform(this.viewport,
                RulerConfig.global, true)));
            this.container.addChild(this.brush.container);
        }
        if (flags.widthChanged || flags.renderReset){
            this.renderer.rebuild(this.viewport,
                RulerTransformer.transform(this.viewport, RulerConfig.global, true),
                RulerTransformer.transform(this.viewport, RulerConfig.local));
        }
        if (flags.brushChanged || flags.widthChanged || flags.renderReset) {
            this.brush.render();
            this.renderer.render(this.viewport, RulerTransformer.transform(this.viewport, RulerConfig.local));
        }
        return true;
    }

    clearData() {
        this.brush.clearData();
        super.clearData();
    }
}
