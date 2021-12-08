import VcfAnalyzer from '../../../../../../dataServices/vcf/vcf-analyzer';
import {VcfTransformer} from './vcfTransformer';

export const STRAINS_COUNT = 200;

export class MultiSampleVcfTransformer extends VcfTransformer {
    transformData (data, viewport) {
        const coverage = new Map();
        const variants = data || [];
        const getVariantSamples = (variant) => {
            // todo: remove mock
            return (new Array(STRAINS_COUNT)).fill({}).map((o, index) => (
                {
                    sample: `Strain #${index + 1}`,
                    info: {}
                }
            ));
        };
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
        for (const variant of variants) {
            const samples = getVariantSamples(variant);
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
                variantsBySample.variants.push({
                    ...variant,
                    sampleInfo: info
                });
            }
        }
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
        const transformedData = variantsBySamples.map(variantsBySample => {
            if (!this.collapsed) {
                return {
                    sample: variantsBySample.sample,
                    data: this.transformCollapsedData(variantsBySample.variants, viewport)
                };
            }
            return {
                sample: variantsBySample.sample,
                data: this.transformExpandedData(variantsBySample.variants, viewport)
            };
        });
        return {
            data: transformedData,
            coverage: {
                items: coverageDataSorted,
                rawItems: coverageData,
                maximum: maximum * (1 + 0.25),
                minimum: 0
            }
        };
    }
}
