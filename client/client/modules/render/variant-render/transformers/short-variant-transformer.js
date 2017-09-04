import {Sorting} from '../../utilities';
import { aminoacidsConst , complementNucleotidesConst } from '../../core';

export default class ShortVariantTransformer{

    static transformSequences(variant, alternativeAlleleInfo){
        const referenceSequence = [];
        const alternativeSequence = [];
        const consensusSequence = [];
        const consensusSequenceForReverseStrand = [];

        variant.reference.forEach((x) => { x.position = x.startIndex; });
        const referenceAminoacidsData = ShortVariantTransformer.getAminoacidSequences(variant.reference, null, alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts);

        switch (variant.variantInfo.type.toLowerCase()) {
            case 'ins':
                {
                    for (let i = 0; i < referenceAminoacidsData.length; i++){
                        const referenceAminoacids = referenceAminoacidsData[i].aminoacids;
                        for (let j = 0; j < referenceAminoacids.length; j++){
                            if (referenceAminoacids[j].startIndex >= variant.variantInfo.startIndexCorrected){
                                referenceAminoacids[j].startIndex += alternativeAlleleInfo.info.length || 0;
                                referenceAminoacids[j].endIndex += alternativeAlleleInfo.info.length || 0;
                            }
                            else  if (referenceAminoacids[j].startIndex < variant.variantInfo.startIndexCorrected && referenceAminoacids[j].endIndex >= variant.variantInfo.startIndexCorrected){
                                referenceAminoacids[j].endIndex += alternativeAlleleInfo.info.length || 0;
                            }
                        }
                    }
                    for (let i = 0; i < alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts.length; i++) {
                        const affectedTranscript = alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts[i];
                        if (affectedTranscript.transcript !== null){
                            affectedTranscript.transcript.value.modifiedExons = [];
                            const isNegativeStrand = affectedTranscript.gene.value.strand.toLowerCase() === 'negative';
                            for (let j = 0; j < affectedTranscript.transcript.value.exon.length; j++) {
                                const ex = Object.assign({}, affectedTranscript.transcript.value.exon[j]);
                                if (isNegativeStrand) {
                                    if (ex.end + affectedTranscript.gene.value.startIndex <= variant.variantInfo.startIndexCorrected) {
                                        ex.start -= alternativeAlleleInfo.info.length || 0;
                                        ex.end -= alternativeAlleleInfo.info.length || 0;
                                    }
                                    else if (ex.start + affectedTranscript.gene.value.startIndex < variant.variantInfo.startIndexCorrected && ex.end + affectedTranscript.gene.value.startIndex >= variant.variantInfo.startIndexCorrected) {
                                        ex.start -= alternativeAlleleInfo.info.length || 0;
                                    }
                                }
                                else {
                                    if (ex.start + affectedTranscript.gene.value.startIndex >= variant.variantInfo.startIndexCorrected) {
                                        ex.start += alternativeAlleleInfo.info.length || 0;
                                        ex.end += alternativeAlleleInfo.info.length || 0;
                                    }
                                    else if (ex.start + affectedTranscript.gene.value.startIndex < variant.variantInfo.startIndexCorrected && ex.end + affectedTranscript.gene.value.startIndex >= variant.variantInfo.startIndexCorrected) {
                                        ex.end += alternativeAlleleInfo.info.length || 0;
                                    }
                                }
                                affectedTranscript.transcript.value.modifiedExons.push(ex);
                            }
                        }
                    }
                }
                break;
            case 'del':
                for (let i = 0; i < alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts.length; i++) {
                    const affectedTranscript = alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts[i];
                    if (affectedTranscript.transcript !== null){
                        affectedTranscript.transcript.value.modifiedExons = [];
                        const isNegativeStrand = affectedTranscript.gene.value.strand.toLowerCase() === 'negative';
                        for (let j = 0; j < affectedTranscript.transcript.value.exon.length; j++) {
                            const ex = Object.assign({}, affectedTranscript.transcript.value.exon[j]);
                            if (isNegativeStrand) {
                                if (ex.end + affectedTranscript.gene.value.startIndex < variant.variantInfo.startIndexCorrected) {
                                    ex.end += (alternativeAlleleInfo.info.length || 0);
                                }
                                else if (ex.end + affectedTranscript.gene.value.startIndex < variant.variantInfo.startIndexCorrected + (alternativeAlleleInfo.info.length || 0)) {
                                    ex.end = variant.variantInfo.startIndexCorrected  + (alternativeAlleleInfo.info.length || 0) - affectedTranscript.gene.value.startIndex;
                                }
                                if (ex.start + affectedTranscript.gene.value.startIndex < variant.variantInfo.startIndexCorrected) {
                                    ex.start += (alternativeAlleleInfo.info.length || 0);
                                }
                                else if (ex.start + affectedTranscript.gene.value.startIndex < variant.variantInfo.startIndexCorrected + (alternativeAlleleInfo.info.length || 0)) {
                                    ex.start = variant.variantInfo.startIndexCorrected + (alternativeAlleleInfo.info.length || 0) - affectedTranscript.gene.value.startIndex;
                                }
                            }
                            else {
                                if (ex.end + affectedTranscript.gene.value.startIndex > variant.variantInfo.startIndexCorrected + (alternativeAlleleInfo.info.length || 0)) {
                                    ex.end -= (alternativeAlleleInfo.info.length || 0);
                                }
                                else if (ex.end + affectedTranscript.gene.value.startIndex > variant.variantInfo.startIndexCorrected) {
                                    ex.end = affectedTranscript.gene.value.startIndex - variant.variantInfo.startIndexCorrected;
                                }
                                if (ex.start + affectedTranscript.gene.value.startIndex > variant.variantInfo.startIndexCorrected + (alternativeAlleleInfo.info.length || 0)) {
                                    ex.start -= (alternativeAlleleInfo.info.length || 0);
                                }
                                else if (ex.start + affectedTranscript.gene.value.startIndex > variant.variantInfo.startIndexCorrected) {
                                    ex.start = affectedTranscript.gene.value.startIndex - variant.variantInfo.startIndexCorrected;
                                }
                            }
                            affectedTranscript.transcript.value.modifiedExons.push(ex);
                        }
                    }
                }
                break;
            default:
                for (let i = 0; i < alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts.length; i++) {
                    const affectedTranscript = alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts[i];
                    if (affectedTranscript.transcript !== null){
                        affectedTranscript.transcript.value.modifiedExons = affectedTranscript.transcript.value.exon;
                    }
                }
                break;
        }

        if (variant.variantInfo.type.toLowerCase() === 'ins') {
            for (let j = 0; j < alternativeAlleleInfo.info.sequence.length; j++) {
                alternativeSequence.push({
                    position: variant.variantInfo.startIndexCorrected + j,
                    text: alternativeAlleleInfo.info.sequence[j]
                });
                alternativeSequence.push({
                    position: variant.variantInfo.startIndexCorrected + j,
                    text: alternativeAlleleInfo.info.sequence[j]
                });
                consensusSequence.push({
                    position: variant.variantInfo.startIndexCorrected + j,
                    text: alternativeAlleleInfo.info.sequence[j]
                });
                consensusSequenceForReverseStrand.push({
                    position: variant.variantInfo.startIndexCorrected + j - alternativeAlleleInfo.info.length,
                    text: alternativeAlleleInfo.info.sequence[j]
                });
            }
        }

        for (let i = 0; i < variant.reference.length; i++) {
            const position = variant.reference[i].startIndex;
            switch (variant.variantInfo.type.toLowerCase()) {
                case 'ins':
                    {
                        referenceSequence.push({
                            position: position + (position >= variant.variantInfo.startIndexCorrected ? alternativeAlleleInfo.info.length : 0),
                            text: variant.reference[i].text
                        });
                        alternativeSequence.push({
                            position: position + (position >= variant.variantInfo.startIndexCorrected ? alternativeAlleleInfo.info.length : 0),
                            text: variant.reference[i].text
                        });
                        consensusSequence.push({
                            position: position + (position >= variant.variantInfo.startIndexCorrected ? alternativeAlleleInfo.info.length : 0),
                            text: variant.reference[i].text
                        });
                        consensusSequenceForReverseStrand.push({
                            position: position + (position < variant.variantInfo.startIndexCorrected ? - alternativeAlleleInfo.info.length : 0),
                            text: variant.reference[i].text
                        });
                    }
                    break;
                case 'del':
                    {
                        referenceSequence.push({
                            position: position,
                            text: variant.reference[i].text
                        });
                        if (position < variant.variantInfo.startIndexCorrected || position >= variant.variantInfo.startIndexCorrected + alternativeAlleleInfo.info.length) {
                            alternativeSequence.push({
                                position: position,
                                text: variant.reference[i].text
                            });
                            consensusSequence.push({
                                position: position + (position >= variant.variantInfo.startIndexCorrected ? - alternativeAlleleInfo.info.length : 0),
                                text: variant.reference[i].text
                            });
                            consensusSequenceForReverseStrand.push({
                                position: position + (position < variant.variantInfo.startIndexCorrected ? alternativeAlleleInfo.info.length : 0),
                                text: variant.reference[i].text
                            });
                        }
                    }
                    break;
                default:
                    {

                        if (position < variant.variantInfo.startIndexCorrected || position >= variant.variantInfo.startIndexCorrected + alternativeAlleleInfo.info.length) {
                            referenceSequence.push({
                                position: position,
                                text: variant.reference[i].text
                            });
                            alternativeSequence.push({
                                position: position,
                                text: variant.reference[i].text
                            });
                            consensusSequence.push({
                                position: position,
                                text: variant.reference[i].text
                            });
                            consensusSequenceForReverseStrand.push({
                                position: position,
                                text: variant.reference[i].text
                            });
                        }
                        else
                        {
                            referenceSequence.push({
                                position: position,
                                text: variant.reference[i].text
                            });
                            alternativeSequence.push({
                                position: position,
                                text: alternativeAlleleInfo.info.sequence[position - variant.variantInfo.startIndexCorrected]
                            });
                            consensusSequence.push({
                                position: position,
                                text: alternativeAlleleInfo.info.sequence[position - variant.variantInfo.startIndexCorrected]
                            });
                            consensusSequenceForReverseStrand.push({
                                position: position,
                                text: alternativeAlleleInfo.info.sequence[position - variant.variantInfo.startIndexCorrected]
                            });
                        }
                    }
                    break;
            }
        }

        const alternativeAminoacidsData = ShortVariantTransformer.getAminoacidSequences(consensusSequence, consensusSequenceForReverseStrand, alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts, false);
        if (variant.variantInfo.type.toLowerCase() === 'del'){
            for (let i = 0; i < alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts.length; i++) {
                const affectedTranscript = alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts[i];
                if (affectedTranscript.transcript !== null){
                    if (affectedTranscript.gene.value.strand.toLowerCase() === 'negative' && affectedTranscript.transcript.value.modifiedExons !== null && affectedTranscript.transcript.value.modifiedExons !== undefined) {
                        for (let j = 0; j < affectedTranscript.transcript.value.modifiedExons.length; j++) {
                            if (affectedTranscript.transcript.value.modifiedExons[j].start + affectedTranscript.gene.value.startIndex < variant.variantInfo.startIndexCorrected) {
                                affectedTranscript.transcript.value.modifiedExons[j].start -= alternativeAlleleInfo.info.length;
                            }
                            if (affectedTranscript.transcript.value.modifiedExons[j].end + affectedTranscript.gene.value.startIndex < variant.variantInfo.startIndexCorrected) {
                                affectedTranscript.transcript.value.modifiedExons[j].end -= alternativeAlleleInfo.info.length;
                            }
                        }
                    }
                    else {
                        for (let j = 0; j < affectedTranscript.transcript.value.modifiedExons.length; j++) {
                            if (affectedTranscript.transcript.value.modifiedExons[j].start + affectedTranscript.gene.value.startIndex > variant.variantInfo.startIndexCorrected) {
                                affectedTranscript.transcript.value.modifiedExons[j].start += alternativeAlleleInfo.info.length;
                            }
                            if (affectedTranscript.transcript.value.modifiedExons[j].end + affectedTranscript.gene.value.startIndex > variant.variantInfo.startIndexCorrected) {
                                affectedTranscript.transcript.value.modifiedExons[j].end += alternativeAlleleInfo.info.length;
                            }
                        }
                    }
                }
            }
            for (let i = 0; i < alternativeAminoacidsData.length; i++){
                const alternativeAminoacids = alternativeAminoacidsData[i].aminoacids;
                const isNegativeStrand = alternativeAminoacidsData[i].negativeStrand;
                for (let j = 0; j < alternativeAminoacids.length; j++){
                    if (isNegativeStrand){
                        if (alternativeAminoacids[j].endIndex <= variant.variantInfo.startIndexCorrected) {
                            alternativeAminoacids[j].endIndex -= alternativeAlleleInfo.info.length || 0;
                        }
                        if (alternativeAminoacids[j].startIndex <= variant.variantInfo.startIndexCorrected) {
                            alternativeAminoacids[j].startIndex -= alternativeAlleleInfo.info.length || 0;
                        }
                    }
                    else {
                        if (alternativeAminoacids[j].startIndex >= variant.variantInfo.startIndexCorrected) {
                            alternativeAminoacids[j].startIndex += alternativeAlleleInfo.info.length || 0;
                        }
                        if (alternativeAminoacids[j].endIndex >= variant.variantInfo.startIndexCorrected) {
                            alternativeAminoacids[j].endIndex += alternativeAlleleInfo.info.length || 0;
                        }
                    }
                }
            }
        }
        else if (variant.variantInfo.type.toLowerCase() === 'ins'){
            for (let i = 0; i < alternativeAminoacidsData.length; i++){
                const alternativeAminoacids = alternativeAminoacidsData[i].aminoacids;
                const isNegativeStrand = alternativeAminoacidsData[i].negativeStrand;
                for (let j = 0; j < alternativeAminoacids.length; j++){
                    if (isNegativeStrand){
                        alternativeAminoacids[j].endIndex += alternativeAlleleInfo.info.length || 0;
                        alternativeAminoacids[j].startIndex += alternativeAlleleInfo.info.length || 0;
                    }
                }
            }
            for (let i = 0; i < alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts.length; i++) {
                const affectedTranscript = alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts[i];
                if (affectedTranscript.transcript !== null){
                    if (affectedTranscript.gene.value.strand.toLowerCase() === 'negative' && affectedTranscript.transcript.value.modifiedExons !== null && affectedTranscript.transcript.value.modifiedExons !== undefined) {
                        for (let j = 0; j < affectedTranscript.transcript.value.modifiedExons.length; j++) {
                            affectedTranscript.transcript.value.modifiedExons[j].start += alternativeAlleleInfo.info.length;
                            affectedTranscript.transcript.value.modifiedExons[j].end += alternativeAlleleInfo.info.length;
                        }
                    }
                }
            }
        }
        
        for (let i = 0; i < alternativeAminoacidsData.length; i++){
            const alternativeAminoacids = alternativeAminoacidsData[i].aminoacids;
            if (referenceAminoacidsData.length > i){
                const referenceAminoacids = referenceAminoacidsData[i].aminoacids;
                let minModifiedPosition = null;
                let maxModifiedPosition = null;
                alternativeAminoacids.forEach(a => a.modified = false);
                referenceAminoacids.forEach(a => a.modified = false);
                for (let position = alternativeAminoacidsData[i].gene.startIndex; position < alternativeAminoacidsData[i].gene.endIndex; position++){
                    let altAminoacid = null;
                    let refAminoacid = null;
                    for (let altIndex = 0; altIndex < alternativeAminoacids.length; altIndex++){
                        if (alternativeAminoacids[altIndex].startIndex <= position && alternativeAminoacids[altIndex].endIndex >= position){
                            altAminoacid = alternativeAminoacids[altIndex];
                            break;
                        }
                    }
                    for (let refIndex = 0; refIndex < referenceAminoacids.length; refIndex++){
                        if (referenceAminoacids[refIndex].startIndex <= position && referenceAminoacids[refIndex].endIndex >= position){
                            refAminoacid = referenceAminoacids[refIndex];
                            break;
                        }
                    }
                    if (altAminoacid !== null && refAminoacid !== null){
                        if (altAminoacid.aminoacid !== refAminoacid.aminoacid) {
                            if (minModifiedPosition === null || minModifiedPosition > altAminoacid.startIndex) {
                                minModifiedPosition = altAminoacid.startIndex;
                            }
                            if (maxModifiedPosition === null || maxModifiedPosition < altAminoacid.endIndex) {
                                maxModifiedPosition = altAminoacid.endIndex;
                            }
                            altAminoacid.modified = true;
                        }
                    }
                }
                for (let position = minModifiedPosition; position <= maxModifiedPosition; position++){
                    for (let altIndex = 0; altIndex < alternativeAminoacids.length; altIndex++){
                        if (alternativeAminoacids[altIndex].startIndex <= position && alternativeAminoacids[altIndex].endIndex >= position){
                            alternativeAminoacids[altIndex].modified = true;
                            break;
                        }
                    }
                }
            }
        }
        
        return {
            reference: referenceSequence,
            alternative: alternativeSequence,
            referenceAminoacidsData: referenceAminoacidsData,
            alternativeAminoacidsData: alternativeAminoacidsData
        };
    }

    static getAminoacidSequences(sequence, sequenceForReverseStrand, affectedTranscripts, isReference = true){
        const result = [];
        for (let i = 0; i < affectedTranscripts.length; i++) {
            const affectedTranscript = affectedTranscripts[i];
            if (affectedTranscript.exon !== null){
                let exonTranscriptStructureComputed = false;
                for (let j = 0; j < result.length; j++){
                    if (result[j].exonId === affectedTranscript.exon.value.id){
                        exonTranscriptStructureComputed = true;
                        break;
                    }
                }
                if (!exonTranscriptStructureComputed) {
                    let seq = sequence;
                    if (sequenceForReverseStrand && affectedTranscript.gene.value.strand.toLowerCase() === 'negative')
                        seq = sequenceForReverseStrand;
                    result.push({
                        exonId: affectedTranscript.exon.value.id,
                        negativeStrand: affectedTranscript.gene.value.strand.toLowerCase() === 'negative',
                        gene: affectedTranscript.gene.value,
                        aminoacids: ShortVariantTransformer.getAminoacidSequence(seq, affectedTranscript.gene.value, isReference ? affectedTranscript.transcript.value.exon : affectedTranscript.transcript.value.modifiedExons, affectedTranscript.gene.value.strand.toLowerCase() === 'negative')
                    });
                }
            }
        }
        return result;
    }

    static getAminoacidSequence(sequence, gene, cdsItems, reverseStrand = false){
        if (cdsItems === null || cdsItems === undefined)
            return [];
        const cdsItemsSorted = Sorting.quickSort(cdsItems, !reverseStrand, x => x.start);
        let cdsShift = 0;
        const buffer = [];
        const aminoacids = [];

        for (let i = 0; i < cdsItemsSorted.length; i++){
            const cdsItem = cdsItemsSorted[i];
            const start = Math.min(cdsItem.end, cdsItem.start);
            const end = Math.max(cdsItem.end, cdsItem.start);
            let firstAminoacidPerformed = false;
            if (reverseStrand){
                for (let j = end + cdsShift - 2; j >= start - 2; j -= 3){
                    ShortVariantTransformer.v(j, start, end, firstAminoacidPerformed, buffer, sequence, gene, aminoacids, reverseStrand);
                    firstAminoacidPerformed = true;
                }
            }
            else {
                for (let j = start - cdsShift; j <= end; j += 3){
                    ShortVariantTransformer.v(j, start, end, firstAminoacidPerformed, buffer, sequence, gene, aminoacids, reverseStrand);
                    firstAminoacidPerformed = true;
                }
            }
            // for (let j = (reverseStrand ? end + cdsShift - 2: start - cdsShift); j >= start - cdsShift - 2 && j <= end + cdsShift; reverseStrand ? j -= 3 : j += 3){
            //     let aStart = Math.min(j, j + 2);
            //     let aEnd = Math.max(j, j + 2);
            //
            //     if (firstAminoacidPerformed)
            //         buffer = [];
            //
            //     for (let s = 0; s < sequence.length; s++){
            //         if (sequence[s].position - gene.startIndex >= start && sequence[s].position - gene.startIndex <= end && sequence[s].position - gene.startIndex >= aStart && sequence[s].position - gene.startIndex <= aEnd) {
            //             buffer[sequence[s].position - gene.startIndex - aStart] = sequence[s];
            //         }
            //     }
            //     if (buffer.length === 3 && buffer[0] !== undefined && buffer[1] !== undefined && buffer[2] !== undefined){
            //         let aminoacidStr = buffer[0].text + buffer[1].text + buffer[2].text;
            //         if (reverseStrand){
            //             aminoacidStr = ShortVariantTransformer.reverseComplement(aminoacidStr);
            //         }
            //         aminoacids.push({
            //             startIndex: aStart + gene.startIndex,
            //             endIndex: aStart + 2 + gene.startIndex,
            //             aminoacid: ShortVariantTransformer.aminoacids[aminoacidStr]
            //         });
            //     }
            //     if (!firstAminoacidPerformed){
            //         firstAminoacidPerformed = true;
            //     }
            // }
            cdsShift = (cdsShift + end - start + 1) % 3;
        }
        return aminoacids;
    }

    static v(position, start, end, firstAminoacidPerformed, buffer, sequence, gene, aminoacids, reverseStrand){
        const aStart = Math.min(position, position + 2);
        const aEnd = Math.max(position, position + 2);
        /*
        let res = '';
        if (!firstAminoacidPerformed) {
            for (let i = 0; i < buffer.length; i++) {
                if (buffer[i] !== undefined)
                    res += buffer[i].text;
                else
                    res += ' ';
            }
        }
        */

        if (firstAminoacidPerformed)
            buffer = [];

        for (let s = 0; s < sequence.length; s++){
            if (sequence[s].position - gene.startIndex >= start && sequence[s].position - gene.startIndex <= end && sequence[s].position - gene.startIndex >= aStart && sequence[s].position - gene.startIndex <= aEnd) {
                buffer[sequence[s].position - gene.startIndex - aStart] = sequence[s];
            }
        }
        if (buffer.length === 3 && buffer[0] !== undefined && buffer[1] !== undefined && buffer[2] !== undefined){
            let aminoacidStr = buffer[0].text + buffer[1].text + buffer[2].text;
            if (reverseStrand){
                aminoacidStr = ShortVariantTransformer.reverseComplement(aminoacidStr);
            }
            aminoacids.push({
                startIndex: aStart + gene.startIndex,
                endIndex: aStart + 2 + gene.startIndex,
                aminoacid: aminoacidsConst[aminoacidStr.toUpperCase()]
            });
        }
    }

    static preprocessSequence(sequence){
        return sequence.toUpperCase().replace('U', 'T');
    }

    static reverseComplement(sequence){
        let result = '';
        const preprocessed = ShortVariantTransformer.preprocessSequence(sequence);
        for (let i = preprocessed.length - 1; i >= 0; i--){
            result += complementNucleotidesConst[preprocessed[i]] || preprocessed[i];
        }
        return result;
    }

}
