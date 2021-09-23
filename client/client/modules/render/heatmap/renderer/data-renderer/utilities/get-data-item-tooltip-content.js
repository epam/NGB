/**
 *
 * @param {HeatmapDataItem} dataItem
 */
export default function getDataItemTooltipContent(dataItem) {
    if (!dataItem) {
        return undefined;
    }
    return [
        ['Value', dataItem.value],
        dataItem.annotation ? ['Annotation', dataItem.annotation] : undefined
    ].filter(Boolean);
}
