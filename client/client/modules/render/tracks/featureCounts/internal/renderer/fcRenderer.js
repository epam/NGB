import FeatureCountRenderer from './features/featureCountRenderer';
import {GeneRenderer} from '../../../gene/internal/renderer';

export default class FCRenderer extends GeneRenderer {
    constructor(track, config, transformer, renderer) {
        super(config, transformer, renderer);
        this._featureRenderer = new FeatureCountRenderer(track, config);
    }
}
