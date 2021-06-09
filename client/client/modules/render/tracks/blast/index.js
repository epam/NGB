import BLASTAlignmentRenderer from './renderer';
import BLASTConfig from './blastConfig';
import {BLASTResultEvents} from '../../../../app/shared/blastContext';
import {InteractiveTrack} from '../../core';

export class BLASTTrack extends InteractiveTrack {
    static getTrackDefaultConfig() {
        return BLASTConfig;
    }

    renderer: BLASTAlignmentRenderer;

    get trackIsResizable() {
        return true;
    }

    constructor(opts) {
        super(opts);
        this.blastContext = opts.blastContext;
        this.dispatcher = opts.dispatcher;
        this.renderer = new BLASTAlignmentRenderer(
            this.viewport,
            Object.assign({}, this.trackConfig, this.config),
            this._pixiRenderer,
            opts
        );
        this.reload = this.reload.bind(this);
        this.dispatcher.on(BLASTResultEvents.changed, this.reload);
    }

    reload () {
        this._flags.renderFeaturesChanged = true;
        this.requestRenderRefresh();
    }

    render(flags) {
        if (flags.renderReset) {
            this.container.removeChildren();
            this.container.addChild(this.renderer.container);
        }
        this.renderer.height = this.height;
        return this.renderer.render(
            this.blastContext.alignment,
            this.blastContext.searchResults,
            flags,
            Object.assign({}, this.state || {}, {centerLine: this._showCenterLine})
        );
    }

    destructor() {
        super.destructor();
        this.dispatcher.removeListener(BLASTResultEvents.changed, this.reload);
    }
}
