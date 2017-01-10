import {BEDFeatureRenderer} from './features';
import {GeneRenderer} from '../../../gene/internal/renderer';

export default class BEDRenderer extends GeneRenderer {
    constructor(config, transformer) {
        super(config, transformer);
        this._featureRenderer = new BEDFeatureRenderer(config);
    }
}
