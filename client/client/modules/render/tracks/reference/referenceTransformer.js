export default class ReferenceTransformer {

    static transform(data, viewport) {
        if (data === null || data.length === 0) {
            return null;
        }
        const isDetailed = !data[0].hasOwnProperty('contentGC');
        const pixelsPerBp = viewport.factor;
        if (viewport.isShortenedIntronsMode) {
            data = viewport.shortenedIntronsViewport.transformFeaturesArray(data);
        }
        const mapDetailedItemFn = function(item) {
            return {
                endIndex: item.endIndex,
                startIndex: item.startIndex,
                value: item.text,
                xEnd: viewport.project.brushBP2pixel(item.endIndex),
                xStart: viewport.project.brushBP2pixel(item.startIndex)
            };
        };
        const mapNotDetailedItemFn = function(item) {
            return {
                endIndex: item.endIndex,
                startIndex: item.startIndex,
                value: item.contentGC,
                xEnd: viewport.project.brushBP2pixel(item.endIndex),
                xStart: viewport.project.brushBP2pixel(item.startIndex)
            };
        };
        const items = isDetailed
            ? data.map(mapDetailedItemFn)
            : data.map(mapNotDetailedItemFn);

        return {
            isDetailed,
            items,
            pixelsPerBp,
            viewport
        };
    }
}
