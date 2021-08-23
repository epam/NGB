import FCRenderer from './internal/renderer/fcRenderer';
import {FeatureCountsTransformer} from './internal/data/featureCountsTransformer';
import {GENETrack} from '../gene';
import Menu from '../../core/menu';
import featureCountsMenu from './menu/index.js';

export class FeatureCountsTrack extends GENETrack {
    static Menu = Menu(
        featureCountsMenu,
        {
            postStateMutatorFn: GENETrack.postStateMutatorFn,
        }
    );

    get transformer() {
        if (!this._transformer) {
            this._transformer = new FeatureCountsTransformer(this.trackConfig);
        }
        return this._transformer;
    }

    get renderer() {
        if (!this._renderer) {
            this._renderer = new FCRenderer(
                this.trackConfig,
                this.transformer,
                this
            );
        }
        return this._renderer;
    }

    get featuresFilteringEnabled () {
        return false;
    }

    constructor(opts) {
        super(opts);
        this._actions = null;
    }
}
