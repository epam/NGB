import Menu from '../../core/menu';
import {GENETrack} from '../gene';
import featureCountsMenu from './menu/index.js';

export class FeatureCountsTrack extends GENETrack {
    static Menu = Menu(
        featureCountsMenu,
        {
            postStateMutatorFn: GENETrack.postStateMutatorFn,
        }
    );

    constructor(opts) {
        super(opts);
        this._actions = null;
    }
}
