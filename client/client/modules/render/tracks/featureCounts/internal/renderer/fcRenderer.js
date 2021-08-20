import FeatureCountRenderer from './features/featureCountRenderer';
import {GeneRenderer} from '../../../gene/internal/renderer';

export default class FCRenderer extends GeneRenderer {
    constructor(config, transformer, renderer, track) {
        super(config, transformer, renderer, track);
        this._featureRenderer = new FeatureCountRenderer(config, track);
    }
}
