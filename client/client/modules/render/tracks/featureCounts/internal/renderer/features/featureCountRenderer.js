import FCFeatureRenderer from './drawing/fcFeatureRenderer';
import {FeatureRenderer} from '../../../../gene/internal/renderer/features';

export default class FeatureCountRenderer extends FeatureRenderer {

    _fcFeatureRenderer = null;

    constructor(track, config) {
        super(config);
        this._fcFeatureRenderer = new FCFeatureRenderer(
            track,
            config,
            ::this.registerLabel,
            ::this.registerDockableElement,
            ::this.registerFeaturePosition
        );
    }

    get renderers() {
        return [this._fcFeatureRenderer];
    }

    get defaultRenderer() {
        return this._fcFeatureRenderer;
    }
}
