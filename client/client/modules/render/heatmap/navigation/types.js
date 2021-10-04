/**
 * @enum {string}
 */
const HeatmapNavigationType = {
    /**
     * Dataset
     */
    dataset: 'dataset',
    /**
     * Reference
     */
    reference: 'reference',
    /**
     * Gene
     */
    gene: 'gene',
    /**
     * Coordinates
     */
    coordinates: 'coordinates',
    missing: 'missing',
    /**
     * Parse server navigation type
     * @param type
     */
    parse(type) {
        switch (type) {
            default:
                return HeatmapNavigationType.missing;
        }
    }
};

export default HeatmapNavigationType;
