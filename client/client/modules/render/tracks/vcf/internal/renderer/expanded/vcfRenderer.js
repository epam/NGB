import {GeneRenderer} from '../../../../gene/internal/renderer';
import {VCFFeatureRenderer} from './features';

export default class VCFRenderer extends GeneRenderer {
    constructor(config, transformer, track) {
        super(config, transformer, track);
        this._featureRenderer = new VCFFeatureRenderer(config, track);
    }
    get needConvertGraphicsToTexture() {
        return false;
    }
}
