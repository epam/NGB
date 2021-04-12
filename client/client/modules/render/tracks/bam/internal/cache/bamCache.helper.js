export default function addCoverage(spliceJunctions, baseCoverage = []) {
    spliceJunctions.sort( (a,b) => {
        if (a.start < b.start) { 
            return -1;
        }
        if (a.start === b.start && a.end <= b.end) {
            return -1;
        }
        return 1;
    });
    const auxiliarySet = new Set();
    for (const item of spliceJunctions) {
        auxiliarySet.add(item.start);
        auxiliarySet.add(item.end);
    }
    const auxiliaryArray = Array.from(auxiliarySet)
        .sort((a,b) => a - b)
        .map((item, index, array) => {
            if (index < array.length - 1) {
                if (spliceJunctions.some(spliceJunction =>
                    spliceJunction.start <= item && spliceJunction.end >= array[index+1]
                )) {
                    return {
                        end: array[index + 1],
                        start: item,
                    };
                }
            }
            return undefined;})
        .filter(a => a);
    let startNumber;
    for (const section of auxiliaryArray) {
        for (let k = startNumber || 0; k < (baseCoverage || []).length; k++) {
            const item = baseCoverage[k];
            const endIndex = item.endIndex ? item.endIndex : item.startIndex;
            if (section.start <= item.startIndex && endIndex <= section.end) {
                startNumber = k;
                section.coverage = Math.max(item.value, (section.coverage || 0));
            }
            if (item.startIndex > section.end) {
                break;
            }
        }
    }
    for (const spliceJunction of spliceJunctions) {
        spliceJunction.coverage = 0;
        for (const section of auxiliaryArray) {
            if (
                spliceJunction.start <= section.start &&
                spliceJunction.end >= section.end &&
                spliceJunction.coverage < section.coverage
            ) {
                spliceJunction.coverage = section.coverage;
            }
            if (spliceJunction.end < section.start) {
                break;
            }
        }
    }
    return spliceJunctions;
}
