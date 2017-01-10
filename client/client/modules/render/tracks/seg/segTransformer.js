export default class SegTransformer {

    static transform(data, viewport) {
        if (data === null) {
            return null;
        }
        
        const tracks = [];
        const pixelsPerBp = viewport.factor;

        for (const name in data) {
            if(data.hasOwnProperty(name)) {
                const items = data[name].map(item => ({
                    value: item.segMean,
                    startIndex: item.startIndex,
                    endIndex: item.endIndex,
                    xStart: viewport.project.brushBP2pixel(item.startIndex),
                    xEnd: viewport.project.brushBP2pixel(item.endIndex)
                }));
                if (items.length > 0) {
                    tracks.push({
                        name: name,
                        items: items
                    });
                }
            }
        }

        return {
            viewport,
            tracks,
            pixelsPerBp
        };
    }
}
