import VcfAnalyzer from '../../../../../../dataServices/vcf/vcf-analyzer';
import {VcfTransformer} from './vcfTransformer';

function mergeVariantInfo (variantInfo = {}, ...genotypeInfos) {
    const result = {
        ...variantInfo
    };
    for (const genotypeInfo of genotypeInfos) {
        for (const [key, value] of Object.entries(genotypeInfo || {})) {
            if (result.hasOwnProperty(key)) {
                if (typeof result[key] === 'object') {
                    result[key].value = value;
                } else {
                    result[key] = {value};
                }
            } else {
                result[key] = {value};
            }
        }
    }
    return result;
}

export class MultiSampleVcfTransformer extends VcfTransformer {
    _collapseSamples = true;
    get collapseSamples () {
        return this._collapseSamples;
    }
    set collapseSamples (value) {
        this._collapseSamples = value;
    }

    transformData (data, viewport) {
        const coverage = new Map();
        const variants = data || [];
        const getVariantSamples = (variant) => Object
            .keys(variant.genotypeData || {})
            .map((sample) => ({
                sample,
                info: variant.genotypeData[sample]
            }));
        const variantsBySamples = [];
        const expandCoverageItem = (startIndex, type, count) => {
            const prev = coverage.has(startIndex)
                ? (coverage.get(startIndex) || {})
                : {};
            coverage.set(
                startIndex,
                {
                    ...prev,
                    [type]: count + (prev[type] || 0)
                }
            );
        };
        const allSamples = [];
        const allVariantsBySample = [];
        for (const variant of variants) {
            const samples = getVariantSamples(variant);
            allSamples.push(...samples.map(o => o.sample));
            let {startIndex} = variant;
            startIndex += VcfAnalyzer.getVariantTranslation(variant);
            expandCoverageItem(startIndex, variant.type.toUpperCase(), samples.length);
            expandCoverageItem(startIndex, 'total', samples.length);
            if (
                /^(snp|snv)$/i.test(variant.type) &&
                variant.alternativeAlleles &&
                variant.alternativeAlleles.length > 0
            ) {
                variant.alternativeAlleles
                    .filter(o => !!o)
                    .forEach(allele => {
                        expandCoverageItem(startIndex, allele, samples.length);
                    });
            }
            for (const {sample, info} of samples) {
                let [variantsBySample] = variantsBySamples
                    .filter(o => o.sample === sample);
                if (!variantsBySample) {
                    variantsBySample = {
                        sample,
                        variants: []
                    };
                    variantsBySamples.push(variantsBySample);
                }
                const sampledVariant = {
                    ...variant,
                    genotypeData: info,
                    info: mergeVariantInfo(
                        variant.info,
                        info.info,
                        info.extendedAttributes
                    )
                };
                variantsBySample.variants.push(sampledVariant);
                allVariantsBySample.push(sampledVariant);
            }
        }
        const uniqueSamples = [...(new Set(allSamples))];
        const coverageData = [];
        let maximum = 0;
        for (const [position, coverageValue] of coverage) {
            const {total = 0} = coverageValue;
            if (maximum < total) {
                maximum = total;
            }
            coverageData.push({
                startIndex: position,
                endIndex: position,
                coverage: coverageValue
            });
        }
        const separateItem = item => item.SNV > 0 || item.SNP > 0;
        const coverageDataSorted = coverageData
            .sort((a, b) => a.startIndex - b.startIndex)
            .reduce((result, item) => {
                if (result.length === 0) {
                    return [{...item}];
                }
                const last = result[result.length - 1];
                const lastStart = viewport.project.brushBP2pixel(last.startIndex);
                const currentStart = viewport.project.brushBP2pixel(item.startIndex);
                if (
                    currentStart - lastStart > 1 ||
                    separateItem(last) ||
                    separateItem(item)
                ) {
                    return [...result, {...item}];
                }
                last.coverage = {
                    total: Math.max(last.coverage.total || 0, item.coverage.total || 0)
                };
                last.endIndex = Math.max(last.endIndex, item.endIndex);
                return result;
            }, []);
        const transformedData = variantsBySamples
            .map(variantsBySample => ({
                sample: variantsBySample.sample,
                data: this.transformCollapsedData(variantsBySample.variants, viewport)
            }))
            .map(info => ({
                ...info,
                hasStatistics: info.data && info.data.variants && info.data.variants.some(o => o.isStatistics)
            }));
        const hasStatistics = transformedData.some(o => o.hasStatistics);
        let collapsedSamplesInfo;
        if (hasStatistics) {
            collapsedSamplesInfo = this.transformCollapsedData(allVariantsBySample, viewport);
        }
        return {
            collapsedSamplesInfo,
            data: transformedData,
            samples: uniqueSamples,
            coverage: {
                items: coverageDataSorted,
                rawItems: coverageData,
                maximum: maximum * (1 + 0.25),
                minimum: 0
            }
        };
    }
}
