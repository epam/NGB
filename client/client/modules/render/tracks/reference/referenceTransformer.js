import * as modes from './reference.modes';
import { complementNucleotidesConst } from '../../core';

export default class ReferenceTransformer {

    static transform(data, viewport) {
        let {blocks, mode} = data;
        const pixelsPerBp = viewport.factor;
        if (!blocks || blocks.length === 0) {
            return {
                mode,
                pixelsPerBp,
                items: [],
                viewport
            };
        }
        if (!mode) {
            mode = blocks[0].hasOwnProperty('contentGC') ? modes.gcContent : modes.nucleotides;
        }
        if (viewport.isShortenedIntronsMode) {
            blocks = viewport.shortenedIntronsViewport.transformFeaturesArray(blocks);
        }
        const mapNucleotideItemsFn = function (item) {
            return {
                endIndex: item.endIndex,
                startIndex: item.startIndex,
                value: item.text,
                xEnd: viewport.project.brushBP2pixel(item.endIndex),
                xStart: viewport.project.brushBP2pixel(item.startIndex)
            };
        };
        const mapReverseNucleotideItemsFn = function (item) {
            return {
                endIndex: item.endIndex,
                startIndex: item.startIndex,
                value: complementNucleotidesConst[item.text],
                xEnd: viewport.project.brushBP2pixel(item.endIndex),
                xStart: viewport.project.brushBP2pixel(item.startIndex)
            };
        };
        const mapGCContentItemsFn = function (item) {
            return {
                endIndex: item.endIndex,
                startIndex: item.startIndex,
                value: item.contentGC,
                xEnd: viewport.project.brushBP2pixel(item.endIndex),
                xStart: viewport.project.brushBP2pixel(item.startIndex)
            };
        };
        let items = [];
        let reverseItems = [];
        switch (mode) {
            case modes.gcContent:
                items = blocks.map(mapGCContentItemsFn);
                break;
            case modes.nucleotides: {
                items = blocks.map(mapNucleotideItemsFn);
                reverseItems =  blocks.map(mapReverseNucleotideItemsFn);
                break;
            }
        }

        return {
            mode,
            items,
            reverseItems,
            pixelsPerBp,
            viewport
        };
    }
}
