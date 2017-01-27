export default class VcfAnalyzer{

    static getVariantTranslation(variant){
        if (variant.type === null || variant.type === undefined)
            return 0;
        if (variant.type.toLowerCase() === 'ins'){
            return +1;
        }
        else if (variant.type.toLowerCase() === 'del'){
            return +1;
        }
        else if (variant.type.toLowerCase() === 'snv'){
            return 0;
        }
        else {
            return 0;
        }
    }

    static analyzeVariant(variant, currentChromosome){
        if (variant.analyzed) {
            return variant;
        }
        let zygosity = 0; // unknown

        if (variant.genotypeData && variant.genotypeData.organismType) {
            switch (variant.genotypeData.organismType.toLowerCase()){
                case 'homozygous': zygosity = 1; break;
                case 'heterozygous': zygosity = 2; break;
            }
        }
        const result = {
            analyzed: true,
            alternativeAllelesInfo: [],
            symbol: null,
            name: null,
            length: variant.endIndex - variant.startIndex + 1,
            interChromosome: false,
            isDefaultPositioning: true,
            startIndexCorrected: variant.startIndex + VcfAnalyzer.getVariantTranslation(variant),
            serverStartIndex: variant.startIndex,
            positioningInfos:[{
                startIndex: variant.startIndex + VcfAnalyzer.getVariantTranslation(variant),
                endIndex: variant.endIndex,
                length: variant.endIndex - variant.startIndex + 1
            }],
            zygosity
        };
        variant = Object.assign(variant, result);
        if (variant.type !== undefined && variant.type !== null && variant.type.toLowerCase() === 'statistic') {
            return variant;
        }
        if (variant.type.toLowerCase() === 'ins') {
            variant.endIndex = variant.startIndex;
        }
        const alternativeAllelesInfo = VcfAnalyzer.analyzeAlternativeAlleles(variant);
        let name = null;
        let symbol = null;
        let structuralSymbol = null;
        if (variant.type === null || variant.type === undefined){
            for (let i = 0; i < alternativeAllelesInfo.length; i++){
                if (alternativeAllelesInfo[i].info && alternativeAllelesInfo[i].info.type){
                    variant.type = alternativeAllelesInfo[i].info.type;
                    break;
                }
            }
        }
        if (variant.type === null || variant.type === undefined){
            variant.type = '';
        }
        switch (variant.type.toLowerCase()){
            case 'ins':
                {
                    symbol = structuralSymbol = name = '+';
                }
                break;
            case 'del':
                {
                    symbol = structuralSymbol = name = '\u2014';
                }
                break;
            case 'snv':
                {
                    symbol = structuralSymbol = name = '\u2192';
                }
                break;
        }
        if (name === null){
            symbol = structuralSymbol = name = variant.type;
        }
        if (variant.structural) {
            symbol = '';
        }
        let isInterChromosome = false;
        let currentChromosomeWithPrefix = currentChromosome;
        const currentChromosomeWithBrackets = `<${  currentChromosome  }>`;
        if (currentChromosomeWithPrefix.toLowerCase().indexOf('chr') === -1){
            currentChromosomeWithPrefix = `chr${  currentChromosomeWithPrefix}`;
        }
        alternativeAllelesInfo.forEach(alt => {
            if (alt.mate){
                alt.mate.intraChromosome = VcfAnalyzer.isCurrentChromosome(
                    {
                        chromosome: currentChromosome,
                        chromosomeWithPrefix: currentChromosomeWithPrefix,
                        chromosomeWithBrackets: currentChromosomeWithBrackets
                    }, alt.mate.chromosome);
                if (alt.mate.intraChromosome && alt.mate.position !== null && alt.mate.position !== undefined){
                    alt.mate.boundaries = {
                        startIndex: Math.min(variant.startIndex, alt.mate.position),
                        endIndex: Math.max(variant.startIndex, alt.mate.position)
                    };
                }
                isInterChromosome = isInterChromosome || !alt.mate.intraChromosome;
            }

        });

        const variantPositioningInfos = [];
        for (let i = 0; i < alternativeAllelesInfo.length; i++){
            const alt = alternativeAllelesInfo[i];
            if (alt.mate && alt.mate.boundaries){
                alt.positioningInfoIndex = variantPositioningInfos.length;
                variantPositioningInfos.push({
                    startIndex: alt.mate.boundaries.startIndex + VcfAnalyzer.getVariantTranslation(variant),
                    endIndex: alt.mate.boundaries.endIndex,
                    length: alt.mate.boundaries.endIndex - alt.mate.boundaries.startIndex + 1
                });
            }
        }

        if (variantPositioningInfos.length === 0){
            variant.isDefaultPositioning = true;
            variantPositioningInfos.push({
                startIndex: variant.startIndex + VcfAnalyzer.getVariantTranslation(variant),
                endIndex: variant.endIndex,
                length: variant.endIndex - variant.startIndex + 1
            });
        }
        else {
            variant.isDefaultPositioning = false;
        }
        const buildDisplayTextFn = function(alternativeAlleleInfo) {
            VcfAnalyzer.buildAlternativeAlleleDisplayText(variant, alternativeAlleleInfo);
        };
        alternativeAllelesInfo.forEach(buildDisplayTextFn);
        variant.analyzed = true;
        variant.alternativeAllelesInfo = alternativeAllelesInfo;
        variant.positioningInfos = variantPositioningInfos;
        variant.symbol = symbol;
        variant.name = name;
        variant.structuralSymbol = structuralSymbol;
        variant.length = variant.endIndex - variant.startIndex + 1;
        variant.interChromosome = isInterChromosome;
        variant.zygosity = zygosity;
        return variant;
    }

    static isCurrentChromosome(current, test){
        if (test === null || test === undefined)
            return true;
        const {chromosome, chromosomeWithPrefix, chromosomeWithBrackets} = current;
        if (test.toLowerCase() === chromosome.toLowerCase())
            return true;
        if (test.toLowerCase() === chromosomeWithBrackets.toLowerCase())
            return true;
        if (test.toLowerCase() === chromosomeWithPrefix.toLowerCase())
            return true;
        if ((`<${  test  }>`).toLowerCase() === chromosomeWithBrackets.toLowerCase())
            return true;
        if ((`chr${  test}`).toLowerCase() === chromosomeWithPrefix.toLowerCase())
            return true;
        return false;
    }

    static buildAlternativeAlleleDisplayText(variant, alternativeAlleleInfo) {
        let displayText = null;
        if (alternativeAlleleInfo.info && alternativeAlleleInfo.info.sequence){
            if (!alternativeAlleleInfo.info.type || alternativeAlleleInfo.info.type === 'snv'){
                displayText = `${variant.referenceAllele}${'\u2192'}${alternativeAlleleInfo.info.sequence}`;
            }
            else{
                displayText = VcfAnalyzer.concatenateAlternativeAlleleDescription(alternativeAlleleInfo.info.sequence);
            }
        }
        if (!displayText && variant.structural && !variant.interChromosome) {
            switch (variant.type.toLowerCase()) {
                case 'inv' :
                    displayText = '\u2194';
                    break;
                case 'ins':
                    displayText = '+';
                    break;
                case 'del':
                    displayText = '\u2014';
                    break;
                case 'bnd':
                    displayText = 'BND';
                    break;
                default:
                    if (alternativeAlleleInfo.source)
                        displayText = alternativeAlleleInfo.source;
                    break;
            }
        }
        alternativeAlleleInfo.displayText = displayText;
    }

    static analyzeAlternativeAlleles(variant){
        const alternateAllelesInfos = [];
        const referenceAllele = variant.referenceAllele;
        for (let i = 0; i < variant.alternativeAlleles.length; i++){
            const alternativeAllele = variant.alternativeAlleles[i];

            if (alternativeAllele.indexOf('[') >= 0 || alternativeAllele.indexOf(']') >= 0){
                // we should analyze breakend
                let directionSymbol = '[';
                if (alternativeAllele.indexOf(directionSymbol) === -1){
                    directionSymbol = ']';
                }
                const directionSymbolFirstAppearance = alternativeAllele.indexOf(directionSymbol);
                const directionSymbolLastAppearance = directionSymbolFirstAppearance + 1 + alternativeAllele.substring(directionSymbolFirstAppearance + 1).indexOf(directionSymbol);
                const mate = VcfAnalyzer.analyzePosition(alternativeAllele.substring(directionSymbolFirstAppearance + 1, directionSymbolLastAppearance));
                mate.attachedAt = directionSymbolFirstAppearance === 0 ? 'left' : 'right';
                mate.reverseComp = directionSymbol === ']';

                let temp = null;

                if (directionSymbolFirstAppearance === 0){
                    temp = alternativeAllele.substring(directionSymbolLastAppearance + 1);
                }
                else {
                    temp = alternativeAllele.substring(0, directionSymbolFirstAppearance);
                }

                const alternativeAlleleInfo = {
                    mate: mate,
                    info: VcfAnalyzer.analyzeSequence(referenceAllele, temp, mate.attachedAt === 'left'),
                    source: alternativeAllele
                };
                alternateAllelesInfos.push(alternativeAlleleInfo);
            }
            else if (alternativeAllele[0] === '<' && alternativeAllele[alternativeAllele.length - 1] === '>' ) {
                alternateAllelesInfos.push({
                    mate: null,
                    info:{
                        type: alternativeAllele.substring(1, alternativeAllele.length - 1),
                        sequence: null,
                        length: null
                    },
                    source: alternativeAllele
                });
            }
            else {
                const alternativeAlleleInfo = {
                    mate: null,
                    info: VcfAnalyzer.analyzeSequence(referenceAllele, alternativeAllele),
                    source: alternativeAllele,
                };
                alternateAllelesInfos.push(alternativeAlleleInfo);
            }
        }
        return alternateAllelesInfos;
    }

    static analyzeSequence(referenceAllele, alternateAllele, refAttachedAtLeft = true){
        let type = null;
        let sequence = null;
        if (referenceAllele.length > alternateAllele.length){
            // deletion
            type = 'del';
            if (referenceAllele.substring(refAttachedAtLeft ? 0 : referenceAllele.length - alternateAllele.length, refAttachedAtLeft ? alternateAllele.length : alternateAllele.length + referenceAllele.length) === alternateAllele){
                sequence = referenceAllele.substring(refAttachedAtLeft ? alternateAllele.length : 0, refAttachedAtLeft ? (referenceAllele.length) : (referenceAllele.length - alternateAllele.length));
            }
        }
        else if (referenceAllele.length < alternateAllele.length){
            // insertion
            type = 'ins';
            if (alternateAllele.substring(refAttachedAtLeft ? 0 : alternateAllele.length - referenceAllele.length, refAttachedAtLeft ? referenceAllele.length : alternateAllele.length + referenceAllele.length) === referenceAllele){
                sequence = alternateAllele.substring(refAttachedAtLeft ? referenceAllele.length : 0, refAttachedAtLeft ? (alternateAllele.length) : (alternateAllele.length - referenceAllele.length));
            }
        }
        else if (referenceAllele !== alternateAllele){
            // snv
            type = 'snv';
            sequence = alternateAllele;
        }
        return {
            type: type,
            sequence: sequence,
            length: sequence !== null ? sequence.length : null
        };
    }

    static analyzePosition(position){
        if (position.indexOf(':') > 0){
            const contigOrChromosomeName = position.substring(0, position.indexOf(':'));
            const contigOrChromosomePosition = position.substring(position.indexOf(':') + 1);
            return {
                position: parseInt(contigOrChromosomePosition),
                chromosome: contigOrChromosomeName
            };
        }
        return {
            position: parseInt(position),
            chromosome: null
        };
    }

    static concatenateAlternativeAlleleDescription(description){
        if (!description)
            return description;
        const maxDescriptionLength = 10;
        if (description.length <= maxDescriptionLength + 1) // no need to replace 1 letter with '...'
            return description;
        return `${description.substring(0, Math.floor(maxDescriptionLength / 2))  }...${  description.substring(description.length - Math.floor(maxDescriptionLength / 2))}`;
    }

}
