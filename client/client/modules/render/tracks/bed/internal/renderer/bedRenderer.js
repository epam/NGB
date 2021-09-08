import {BEDFeatureRenderer} from './features';
import {GeneRenderer} from '../../../gene/internal/renderer';

export default class BEDRenderer extends GeneRenderer {
    constructor(config, transformer, track) {
        super(config, transformer, track);
        this._featureRenderer = new BEDFeatureRenderer(config, track);
    }
}
