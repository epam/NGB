import GeneFeatureAnalyzer from '../../../../../../dataServices/gene/gene-feature-analyzer';
import {Sorting} from '../../../../utilities';
import {Viewport} from '../../../../core';

const Math = window.Math;

export class GeneTransformer {

    _config = null;

    constructor(config) {
        this._config = config;
    }

    get config() {
        return this._config;
    }

    registerGroupAutoScaleManager(manager, track) {
        this.groupAutoScaleManager = manager;
        this.track = track;
    }

    isHistogramDrawingModeForViewport(viewport: Viewport, data) {
        let count = 0, left = 0, right = 0;
        if (!data || !data.histogramData)
            return false;
        while (left < data.histogramData.length && data.histogramData[left].startIndex < viewport.brush.end) {
            if (data.histogramData[right].startIndex < viewport.brush.start) {
                right++;
            }
            left++;
        }
        if (left === data.histogramData.length) {
            left--;
        }
        if (right < data.histogramData.length && left < data.histogramData.length) {
            if (right < left) {
                count = data.histogramData[left].totalValue - data.histogramData[right].totalValue;
            } else {
                count = data.histogramData[left].totalValue;
            }
        }
        return (count > this.config.histogram.thresholdGenes &&
            viewport.canvasSize / (left - right) <= this.config.histogram.thresholdWidth);
    }

    static transformFullHistogramData(data) {
        const histogramData = [{startIndex: 1, totalValue: 0, value: 0}];
        let total = 0;
        for (let i = 0; i < data.length; i++) {
            if (i === 0) {
                histogramData[0].endIndex = data[i].startIndex;
            }
            total += data[i].value;
            const item = Object.assign({totalValue: total}, data[i]);
            histogramData.push(item);
        }
        return histogramData;
    }

    static transformPartialHistogramData(viewport: Viewport, data) {
        let max = 0;
        let min = 0;
        let start = 0;
        let end = data.length - 1;
        let startInitialized = false;
        for (let i = 0; i < data.length; i++) {
            const dataItem = data[i];
            if (dataItem.startIndex < viewport.brush.start || viewport.brush.end < dataItem.startIndex)
                continue;
            if (!startInitialized) {
                start = i;
                startInitialized = true;
            }
            end = i;
            max = Math.max(max, dataItem.value);
            min = Math.min(min, dataItem.value);
        }
        return {
            end: end,
            items: data,
            max: max,
            min: min,
            start: start
        };
    }

    transformData(data, viewport) {
        let genes = [];
        const unmappedFeatures = [];
        for (let i = 0; i < data.length; i++) {
            if (data[i].mapped !== undefined && data[i].mapped !== null && !data[i].mapped) {
                unmappedFeatures.push(data[i]);
            } else if (data[i].feature !== null && data[i].feature !== undefined && data[i].feature.toLowerCase() === 'gene') {
                const item = GeneTransformer.analyzeGene(data[i], viewport);
                if (item !== null) {
                    genes.push(item);
                }
            }
        }
        for (let i = 0; i < unmappedFeatures.length; i++) {
            const item = GeneFeatureAnalyzer.updateFeatureName(unmappedFeatures[i]);
            if (item !== null) {
                genes.push(item);
            }
        }
        genes = Sorting.quickSort(genes, false, item => item.endIndex - item.startIndex);
        return genes;
    }

    getFeatures(data) {
        return [...new Set((data || []).map(item => item.feature))]
            .filter(Boolean)
            .filter(feature => !/^statistic$/i.test(feature))
            .sort();
    }

    getSourcesInfo () {
        return undefined;
    }

    getCoordinateSystem () {
        return undefined;
    }

    static analyzeGene(gene) {
        if (gene.items !== null && gene.items !== undefined && gene.items.length > 0) {
            const transcripts = [];
            for (let i = 0; i < gene.items.length; i++) {
                if (gene.items[i].feature !== null && gene.items[i].feature !== undefined) {
                    const transcript = GeneTransformer.analyzeTranscript(gene.items[i]);
                    if (transcript !== null) {
                        transcripts.push(transcript);
                    }
                }
            }
            gene.items = [];
            gene.transcripts = transcripts;
        } else {
            gene.transcripts = [];
        }
        gene.name = null;
        if (gene.attributes) {
            if (gene.attributes.hasOwnProperty('gene_name')) {
                gene.name = gene.attributes.gene_name;
            } else if (gene.attributes.hasOwnProperty('gene_symbol')) {
                gene.name = gene.attributes.gene_symbol;
            } else if (gene.attributes.hasOwnProperty('gene_id')) {
                gene.name = gene.attributes.gene_id;
            }
        }
        return gene;
    }

    static analyzeTranscript(transcript) {
        transcript.name = null;
        if (transcript.attributes) {
            if (transcript.attributes.hasOwnProperty('transcript_name')) {
                transcript.name = transcript.attributes.transcript_name;
            } else if (transcript.attributes.hasOwnProperty('transcript_symbol')) {
                transcript.name = transcript.attributes.transcript_symbol;
            }
        }
        if (transcript.items === null || transcript.items === undefined) {
            transcript.items = [];
        }

        transcript.structure = GeneTransformer.analyzeTranscriptExonStructure(transcript);
        GeneTransformer.analyzeTranscriptAminoacidStructure(transcript);
        return transcript;
    }

    // eslint-disable-next-line complexity
    static analyzeTranscriptExonStructure(transcript) {
        const structure = [];
        const correctedItems = [];
        let featuresCoordinates = [transcript.startIndex, transcript.endIndex];
        for (let i = 0; i < transcript.items.length; i++) {
            if (featuresCoordinates.indexOf(transcript.items[i].startIndex) === -1)
                featuresCoordinates.push(transcript.items[i].startIndex);
            if (featuresCoordinates.indexOf(transcript.items[i].endIndex) === -1)
                featuresCoordinates.push(transcript.items[i].endIndex);
        }
        featuresCoordinates = Sorting.quickSort(featuresCoordinates);

        for (let i = 0; i < featuresCoordinates.length - 1; i++) {
            const startIndex = featuresCoordinates[i];
            const endIndex = featuresCoordinates[i + 1];
            let item = {
                endIndex: endIndex,
                feature: null,
                index: null,
                isEmpty: true,
                startIndex: startIndex,
                strand: transcript.strand
            };
            for (let j = 0; j < transcript.items.length; j++) {
                const target = transcript.items[j];
                if (!GeneTransformer.shouldReplaceFeature(item, target)) {
                    continue;
                }
                if (item.startIndex >= target.startIndex && item.endIndex <= target.endIndex) {
                    item = Object.assign(item, target, {
                        endIndex: item.endIndex,
                        index: j,
                        isEmpty: false,
                        startIndex: item.startIndex
                    });
                }
            }
            correctedItems.push(item);
        }

        // correction: we should combine two neighbor elements if they have equal indices.
        let i = 0;
        while (i < correctedItems.length - 1) {
            const current = correctedItems[i];
            const next = correctedItems[i + 1];
            if (current.index === next.index) {
                current.endIndex = next.endIndex;
                correctedItems.splice(i + 1, 1);
            } else {
                i++;
            }
        }
        // creating blocks (empty / not empty)
        let block = null;
        for (let i = 0; i < correctedItems.length; i++) {
            const item = correctedItems[i];
            if (block === null) {
                block = {
                    endIndex: item.endIndex,
                    isEmpty: item.isEmpty,
                    items: [item],
                    startIndex: item.startIndex,
                    strand: item.strand
                };
                continue;
            }
            if (item.isEmpty === block.isEmpty) {
                block.items.push(item);
                block.endIndex = item.endIndex;
            } else {
                structure.push(block);
                block = {
                    endIndex: item.endIndex,
                    isEmpty: item.isEmpty,
                    items: [item],
                    startIndex: item.startIndex,
                    strand: item.strand
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

        return structure;
    }

    static analyzeTranscriptAminoacidStructure(transcript) {
        if (transcript.hasOwnProperty('psList')) {
            for (let i = 0; i < transcript.structure.length; i++) {
                const block = transcript.structure[i];
                if (block.isEmpty)
                    continue;
                for (let j = 0; j < block.items.length; j++) {
                    const item = block.items[j];
                    item.aminoacidSequence = [];
                    if (item.feature.toLowerCase() === 'cds') {
                        for (let s = 0; s < transcript.psList.length; s++) {
                            if (transcript.psList[s].startIndex > transcript.psList[s].endIndex ||
                                transcript.psList[s].endIndex - transcript.psList[s].startIndex > 2) {
                                continue;
                            }
                            if (item.startIndex <= transcript.psList[s].startIndex &&
                                item.endIndex >= transcript.psList[s].endIndex) {
                                item.aminoacidSequence.push(transcript.psList[s]);
                            }
                        }
                    }
                }
            }
        }
    }

    // Function is designed while response could contain overlapping
    // features.
    // Function compares source & target features: if source feature is coding sequence,
    // then we shouldn't replace it with target.
    // Function returns true if we should replace source feature with target one,
    // false otherwise.
    static shouldReplaceFeature(source, target) {
        if (!source.feature)
            return true;
        if (!target.feature)
            return false;
        if (source.feature.toLowerCase() === 'cds')
            return false;
        // todo: display 3UTR, 5UTR, start- and stop- codons overlapping exon or coding sequence.
        // For now we shouldn't display 3utr, 5utr, start- or stop- codons as separate elements while they're overlaps exon or coding sequence.
        return (target.feature.toLowerCase() === 'exon' || target.feature.toLowerCase() === 'cds');
    }
}
