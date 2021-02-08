import {BedItemFeatureRenderer} from './drawing';
import {FeatureRenderer} from '../../../../gene/internal/renderer/features';

export default class BEDFeatureRenderer extends FeatureRenderer {

    _bedItemFeatureRenderer = null;

    constructor(track, config) {
        super(config);
        this._bedItemFeatureRenderer = new BedItemFeatureRenderer(
            track,
            config,
            this.registerLabel.bind(this),
            this.registerDockableElement.bind(this),
            this.registerFeaturePosition.bind(this)
        );
    }

    get renderers() {
        return [this._bedItemFeatureRenderer];
    }

    get defaultRenderer() {
        return this._bedItemFeatureRenderer;
    }
}