import {GeneRenderer} from '../../../../gene/internal/renderer';
import {VCFFeatureRenderer} from './features';

export default class VCFRenderer extends GeneRenderer {
    constructor(config, transformer) {
        super(config, transformer);
        this._featureRenderer = new VCFFeatureRenderer(config);
    }
    get needConvertGraphicsToTexture() {
        return false;
    }
}