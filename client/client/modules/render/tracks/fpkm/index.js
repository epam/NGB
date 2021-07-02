import {GENETrack} from '../gene';
import FpkmMenu from './fpkmMenu';
import Menu from '../../core/menu';

export class FPKMTrack extends GENETrack {

    static Menu = Menu(
        FpkmMenu,
        {
            postStateMutatorFn: GENETrack.postStateMutatorFn,
        }
    );

    constructor(opts) {
        super(opts);
        this._actions = null;
    }
}
