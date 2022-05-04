import {complementNucleotidesConst} from '../../../../core';

const CONTENT_GC = 'contentGC';
const NUCLEOTIDE_MODE = 'NUCLEOTIDES';
const GC_CONTENT_MODE = 'GC_CONTENT';

export function transformReference(data, viewport) {
    let {blocks, mode} = data;
    const reference = {
        items: {},
        reverseItems: {}
    };
    if (!blocks || blocks.length === 0) {
        return reference;
    }
    if (!mode) {
        mode = blocks[0].hasOwnProperty(CONTENT_GC) ? GC_CONTENT_MODE : NUCLEOTIDE_MODE;
    }
    if (mode === GC_CONTENT_MODE) {
        return reference;
    }
    if (viewport.isShortenedIntronsMode) {
        blocks = viewport.shortenedIntronsViewport.transformFeaturesArray(blocks);
    }

    reference.items = blocks.reduce((result, block) => {
        result[block.startIndex] = block.text;
        return result;
    }, {});
    reference.reverseItems = blocks.reduce((result, block) => {
        let value;
        if (complementNucleotidesConst.hasOwnProperty(block.text)) {
            value = complementNucleotidesConst[block.text];
        } else if (complementNucleotidesConst.hasOwnProperty(block.text.toUpperCase())) {
            value = complementNucleotidesConst[block.text.toUpperCase()].toLowerCase();
        } else {
            value = block.text;
        }

        result[block.startIndex] = value;
        return result;
    }, {});

    return reference;
}
