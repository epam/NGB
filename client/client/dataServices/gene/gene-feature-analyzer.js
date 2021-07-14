export default class GeneFeatureAnalyzer {
    static updateFeatureName(feature) {
        feature.transcripts = [];
        feature.items = [];
        feature.structure = [];
        feature.name = GeneFeatureAnalyzer.getFeatureName(feature);
        return feature;
    }

    static getFeatureName(feature, ...attributes) {
        const extractAttribute = (attributeName) => {
            if (feature.attributes.hasOwnProperty(attributeName)) {
                return feature.attributes[attributeName];
            }
            else if (feature.attributes.hasOwnProperty(attributeName.toLowerCase())) {
                return feature.attributes[attributeName.toLowerCase()];
            }
            return null;
        };
        const extractFeaturedAttribute = (postfix) =>
            extractAttribute(feature.feature + postfix) ||
            extractAttribute(feature.feature.toLowerCase() + postfix);
        const extractFeaturedAttributes = (...postfixs) => {
            for (let i = 0; i < postfixs.length; i++) {
                const attr = extractFeaturedAttribute(postfixs[i]);
                if (attr) {
                    return attr;
                }
            }
            return null;
        };
        const extractAttributes = (...attributes) => {
            for (let i = 0; i < attributes.length; i++) {
                const attr = extractAttribute(attributes[i]);
                if (attr) {
                    return attr;
                }
            }
            return null;
        };
        if (attributes && attributes.length > 0) {
            return extractAttributes(...attributes);
        }
        return extractAttributes('NAME', 'Name', 'ID', 'Id', 'ALIAS', 'Alias') ||
            extractFeaturedAttributes('_NAME', '_Name', 'NAME', 'Name', '_ID', '_Id', 'ID', 'Id', '_SYMBOL', '_Symbol') ||
            extractAttributes('TRANSCRIPT_ID', 'Transcript_Id');
    }
}
