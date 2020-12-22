import {GeneTransformer} from '../../../gene/internal';
import {Sorting} from '../../../../utilities';

export class BEDTransformer extends GeneTransformer {

    transformData(data) {
        data.forEach(x => this.analyzeBedItem(x));
        return Sorting.quickSort(data, false, item => item.endIndex - item.startIndex);
    }

    analyzeBedItemStructure(bedItem, level = 2) {
        bedItem.name = bedItem.name || '';
        let structure = null;
        let correctedItems = null;
        switch (level) {
            case 0: {
                correctedItems = [];
                // display bed feature as rectangle
                const item = {
                    startIndex: bedItem.startIndex,
                    endIndex: bedItem.endIndex,
                    index: null,
                    strand: bedItem.strand,
                    isEmpty: false
                };
                correctedItems.push(item);
            }
                break;
            case 1: {
                // display bed feature as line & inner rectangle
                if (bedItem.thickStart !== null && bedItem.thickStart !== undefined && bedItem.thickEnd !== null && bedItem.thickEnd !== undefined) {
                    let featuresCoordinates = [bedItem.startIndex, bedItem.endIndex];
                    correctedItems = [];
                    if (featuresCoordinates.indexOf(bedItem.thickStart) === -1) {
                        featuresCoordinates.push(bedItem.thickStart);
                    }
                    if (featuresCoordinates.indexOf(bedItem.thickEnd) === -1) {
                        featuresCoordinates.push(bedItem.thickEnd);
                    }

                    featuresCoordinates = Sorting.quickSort(featuresCoordinates);

                    for (let i = 0; i < featuresCoordinates.length - 1; i++) {
                        const startIndex = featuresCoordinates[i];
                        const endIndex = featuresCoordinates[i + 1];
                        const item = {
                            startIndex: startIndex,
                            endIndex: endIndex,
                            index: null,
                            strand: bedItem.strand,
                            isEmpty: startIndex < bedItem.thickStart || endIndex > bedItem.thickEnd
                        };
                        correctedItems.push(item);
                    }
                }
            }
                break;
            default: {
                // display bed feature with blocks
                if (bedItem.blockStarts !== null && bedItem.blockStarts !== undefined && bedItem.blockStarts.length > 0) {
                    let featuresCoordinates = [bedItem.startIndex, bedItem.endIndex];
                    correctedItems = [];

                    for (let i = 0; i < bedItem.blockStarts.length; i++) {
                        if (featuresCoordinates.indexOf(bedItem.startIndex + bedItem.blockStarts[i]) === -1) {
                            featuresCoordinates.push(bedItem.startIndex + bedItem.blockStarts[i]);
                        }
                        if (featuresCoordinates.indexOf(bedItem.startIndex + bedItem.blockStarts[i] + bedItem.blockSizes[i] - 1) === -1) {
                            featuresCoordinates.push(bedItem.startIndex + bedItem.blockStarts[i] + bedItem.blockSizes[i] - 1);
                        }
                    }

                    featuresCoordinates = Sorting.quickSort(featuresCoordinates);

                    for (let i = 0; i < featuresCoordinates.length - 1; i++) {
                        const startIndex = featuresCoordinates[i];
                        const endIndex = featuresCoordinates[i + 1];
                        const item = {
                            startIndex: startIndex,
                            endIndex: endIndex,
                            index: null,
                            strand: bedItem.strand,
                            isEmpty: true
                        };
                        for (let j = 0; j < bedItem.blockStarts.length; j++) {
                            if (item.startIndex >= bedItem.startIndex + bedItem.blockStarts[j] && item.endIndex <= bedItem.startIndex + bedItem.blockStarts[j] + bedItem.blockSizes[j] - 1) {
                                item.isEmpty = false;
                                item.index = j;
                                break;
                            }
                        }
                        correctedItems.push(item);
                    }
                }
            }
                break;
        }
        if (correctedItems) {
            structure = [];
            // correction: we should combine two neighbor elements if they have equal indices.
            let i = 0;
            while (i < correctedItems.length - 1) {
                const current = correctedItems[i];
                const next = correctedItems[i + 1];
                if (current.index === next.index) {
                    current.endIndex = next.endIndex;
                    correctedItems.splice(i + 1, 1);
                }
                else {
                    i++;
                }
            }
            // creating blocks (empty / not empty)
            let block = null;
            for (let i = 0; i < correctedItems.length; i++) {
                const item = correctedItems[i];
                if (block === null) {
                    block = {
                        strand: item.strand,
                        startIndex: item.startIndex,
                        endIndex: item.endIndex,
                        items: [item],
                        isEmpty: item.isEmpty
                    };
                    continue;
                }
                if (item.isEmpty === block.isEmpty) {
                    block.items.push(item);
                    block.endIndex = item.endIndex;
                }
                else {
                    structure.push(block);
                    block = {
                        strand: item.strand,
                        startIndex: item.startIndex,
                        endIndex: item.endIndex,
                        items: [item],
                        isEmpty: item.isEmpty
                    };
                }
            }

            if (block) {
                structure.push(block);
            }

            // correction: we should increase empty block's start index by 1 bp and decrease end index by 1 bp
            for (let i = 0; i < structure.length; i++) {
                const block = structure[i];
                if (block.isEmpty) {
                    block.startIndex++;
                    block.endIndex--;
                }
            }
        }
        return structure;
    }

    getStructureNotEmptyBlocksLength(structure) {
        let result = 0;
        for (let i = 0; i < structure.length; i++) {
            const block = structure[i];
            if (!block.isEmpty) {
                result += block.endIndex - block.startIndex;
            }
        }
        return result;
    }

    analyzeBedItem(bedItem) {
        if (bedItem.rgb && bedItem.rgb.split(',').length === 3) {
            bedItem.rgb = bedItem.rgb.split(',').map(x => parseInt(x));
            if (bedItem.rgb[0] === 0 && bedItem.rgb[1] === 0 && bedItem.rgb[2] === 0) {
                bedItem.rgb = null;
            }
        }
        else {
            bedItem.rgb = null;
        }

        let detailedStructure = this.analyzeBedItemStructure(bedItem);
        let thickStrcutre = this.analyzeBedItemStructure(bedItem, 1);
        const minimizedStructure = this.analyzeBedItemStructure(bedItem, 0);

        if (!thickStrcutre) {
            thickStrcutre = minimizedStructure;
        }

        if (!detailedStructure) {
            detailedStructure = thickStrcutre;
        }

        bedItem.structures = [
            {
                length: this.getStructureNotEmptyBlocksLength(detailedStructure),
                structure: detailedStructure
            },
            {
                length: this.getStructureNotEmptyBlocksLength(thickStrcutre),
                structure: thickStrcutre
            },
            {
                length: this.getStructureNotEmptyBlocksLength(minimizedStructure),
                structure: minimizedStructure
            }];
    }


}