import {CommonVariantFeatureRenderer} from './commonVariantFeatureRenderer';

export default class SVFeatureRenderer extends CommonVariantFeatureRenderer {

    getFeatureDisplayText(feature) {
        let displayText = feature.symbol || feature.type;
        if (feature.alternativeAllelesInfo.length) {
            displayText = feature.alternativeAllelesInfo[0].displayText;
        }
        return displayText;
    }

}
