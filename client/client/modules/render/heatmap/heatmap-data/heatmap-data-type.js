
/**
 * Heatmap Data Types
 * @enum {number}
 */
const HeatmapDataType = {
    /**
     * Number
     */
    number: 0,
    /**
     * String
     */
    string: 1,
    /**
     * Hybrid
     */
    hybrid: 2,
    /**
     *
     * @param {string} type
     * @returns {HeatmapDataType}
     */
    parse(type) {
        switch ((type || '').toLowerCase()) {
            case 's':
            case 'string':
                return HeatmapDataType.string;
            case 'double':
            case 'integer':
            case 'number':
            case 'n':
            default:
                return HeatmapDataType.number;
        }
    },
    serialize(type, minimized = false) {
        const minimize = o => minimized ? o.slice(0, 1) : o;
        switch (type) {
            case HeatmapDataType.string:
                return minimize('string');
            case HeatmapDataType.number:
            default:
                return minimize('number');
        }
    }
};

export default HeatmapDataType;
