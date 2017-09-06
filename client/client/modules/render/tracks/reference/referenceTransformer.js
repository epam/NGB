import * as modes from './reference.modes';
import {complementNucleotidesConst, aminoAcidsConst} from '../../core';

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
                reverseItems = blocks.map(mapReverseNucleotideItemsFn);
                break;
            }
        }


        const getAminoAcidsFn = function (nucleotideItems, firstCoordinate) {
            let aminoAcids = [];
            for (let i = firstCoordinate; i < nucleotideItems.length; i = i + 3) {
                if (nucleotideItems[i] && nucleotideItems[i + 1] && nucleotideItems[i + 2]) {
                    const aminoAcidStr = nucleotideItems[i].value + nucleotideItems[i + 1].value + nucleotideItems[i + 2].value;
                    aminoAcids.push({
                        startIndex: nucleotideItems[i].startIndex,
                        endIndex: nucleotideItems[i + 2].endIndex,
                        xStart: viewport.project.brushBP2pixel(nucleotideItems[i].startIndex),
                        xEnd: viewport.project.brushBP2pixel(nucleotideItems[i + 2].endIndex),
                        value: aminoAcidsConst[aminoAcidStr.toUpperCase()]
                    })
                }
            }
            return aminoAcids;
        };

        let aminoAcidsData = [];
        let reverseAminoAcidsData = [];
        if (mode === modes.nucleotides) {
            let coordinates = [{index: 0, value: items[0].startIndex % 3}, {index: 1, value: items[1].startIndex % 3}, {index: 2, value: items[2].startIndex % 3}];
            coordinates.sort((a, b) => a.value > b.value ? 1 : -1);
            let firstCoordinate = coordinates[0];

            coordinates = [{index: 0, value: reverseItems[0].startIndex % 3}, {index: 1, value: reverseItems[1].startIndex % 3}, {index: 2, value: reverseItems[2].startIndex % 3}];
            coordinates.sort((a, b) => a.value > b.value ? 1 : -1);
            let reverseFirstCoordinate = coordinates[0];

            for (let j = 0; j < 3; j++) {
                aminoAcidsData.push(getAminoAcidsFn(items, firstCoordinate.index + j));
                reverseAminoAcidsData.push(getAminoAcidsFn(reverseItems, reverseFirstCoordinate.index + j));
            }
        }

        return {
            mode,
            items,
            reverseItems,
            aminoAcidsData,
            reverseAminoAcidsData,
            pixelsPerBp,
            viewport
        };
    }
}
