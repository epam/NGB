import FeatureCountRenderer from './features/featureCountRenderer';
import {GeneRenderer} from '../../../gene/internal/renderer';

export default class FCRenderer extends GeneRenderer {
    constructor(config, transformer, track) {
        super(config, transformer, track);
        this._featureRenderer = new FeatureCountRenderer(config, track);
    }
}
