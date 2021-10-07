/**
 * @enum {string}
 */
const HeatmapNavigationType = {
    /**
     * Dataset
     */
    dataset: 'DATASET',
    /**
     * Reference
     */
    reference: 'REFERENCE',
    /**
     * Gene
     */
    gene: 'GENE',
    /**
     * Coordinates
     */
    coordinates: 'COORDINATE',
    missing: 'NONE',
    /**
     * Parse server navigation type
     * @param {string} type
     */
    parse(type) {
        const all = Object.values(HeatmapNavigationType);
        if (all.includes(type)) {
            return type;
        }
        return HeatmapNavigationType.missing;
    }
};

export default HeatmapNavigationType;
