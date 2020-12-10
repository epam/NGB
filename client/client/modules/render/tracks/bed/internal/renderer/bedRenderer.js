import {BEDFeatureRenderer} from './features';
import {GeneRenderer} from '../../../gene/internal/renderer';

export default class BEDRenderer extends GeneRenderer {
    constructor(track, config, transformer, renderer) {
        super(config, transformer, renderer);
        this._featureRenderer = new BEDFeatureRenderer(track, config);
    }
}
