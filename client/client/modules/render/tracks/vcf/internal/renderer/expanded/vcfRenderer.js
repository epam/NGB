import {GeneRenderer} from '../../../../gene/internal/renderer';
import {VCFFeatureRenderer} from './features';

export default class VCFRenderer extends GeneRenderer {
    constructor(config, transformer, renderer) {
        super(config, transformer, renderer);
        this._featureRenderer = new VCFFeatureRenderer(config);
    }
    get needConvertGraphicsToTexture() {
        return false;
    }
}