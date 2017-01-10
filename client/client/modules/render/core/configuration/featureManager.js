export default function extractFeaturesForTrack(features, keys) {
    const mappedFeatures = {};
    for (let i = 0; i < keys.length; i++) {
        const key = keys[i];
        if (features.hasOwnProperty(key)) {
            mappedFeatures[key] = features[key];
        }
    }
    return mappedFeatures;
}