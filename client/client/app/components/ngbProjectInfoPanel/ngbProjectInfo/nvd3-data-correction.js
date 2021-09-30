export default function nvd3dataCorrection() {
    /**
     * @typedef {Object} NVD3ChromosomeBarDataItem
     * @property {number} value
     * @property {string} color
     * @property {string} chrName
     * @property {string} label
     */
    /**
     * Appends "fake" chromosome objects to make nvd3 chart be centered
     * @param {NVD3ChromosomeBarDataItem[]} [values=[]] - real chromosome values (bars)
     * @param {number} [minimumItemsCount=0] - desired minimum bars count
     * @returns {NVD3ChromosomeBarDataItem[]}
     */
    function correct(values = [], minimumItemsCount = 0) {
        const length = values.length;
        if (length < minimumItemsCount) {
            const itemsToAddToEachSide = Math.ceil((minimumItemsCount - length) / 2.0);
            const invisibleItems = (new Array(itemsToAddToEachSide)).fill({
                value: 0,
                color: '#ffffff'
            });
            const map = name => (item, index) => ({...item, chrName: `${name} ${index}`, label: `${name} ${index}`});
            return invisibleItems.map(map('fake start'))
                .concat(values)
                .concat(invisibleItems.map(map('fake end')));
        }
        return values;
    }
    return correct;
}
