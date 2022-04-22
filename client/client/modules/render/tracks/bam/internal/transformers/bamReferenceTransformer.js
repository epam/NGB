import {complementNucleotidesConst} from '../../../../core';

const modes = {
    gcContent: 'GC_CONTENT',
    nucleotides: 'NUCLEOTIDES',
};

export function transformReference(data, viewport) {
    let {blocks, mode} = data;
        
    if (!blocks || blocks.length === 0) {
        return {
            items: null,
            reverseItems: null
        };
    }
    if (!mode) {
        mode = blocks[0].hasOwnProperty('contentGC') ? modes.gcContent : modes.nucleotides;
    }
    if (viewport.isShortenedIntronsMode) {
        blocks = viewport.shortenedIntronsViewport.transformFeaturesArray(blocks);
    }
    const mapNucleotideItemsFn = function (item) {
        return [item.startIndex, item.text];
    };
    const mapReverseNucleotideItemsFn = function (item) {
        let value;
        if (complementNucleotidesConst.hasOwnProperty(item.text)) {
            value = complementNucleotidesConst[item.text];
        } else if (complementNucleotidesConst.hasOwnProperty(item.text.toUpperCase())) {
            value = complementNucleotidesConst[item.text.toUpperCase()].toLowerCase();
        } else {
            value = item.text;
        }

        return [item.startIndex, value];
    };
    const mapGCContentItemsFn = function (item) {
        return [item.startIndex, item.contentGC];
    };
    let items = {};
    let reverseItems = {};
    switch (mode) {
        case modes.gcContent:
            items = blocks.reduce((result, block) => {
                const [index, value] = mapGCContentItemsFn(block);
                result[index] = value;
                return result;
            }, {});
            break;
        case modes.nucleotides: {
            items = blocks.reduce((result, block) => {
                const [index, value] = mapNucleotideItemsFn(block);
                result[index] = value;
                return result;
            }, {});
            reverseItems = blocks.reduce((result, block) => {
                const [index, value] = mapReverseNucleotideItemsFn(block);
                result[index] = value;
                return result;
            }, {});
            break;
        }
    }
    
    return {
        items,
        reverseItems,
    };
}
