import VcfHighlightConditionService from '../../../../../../dataServices/utils/vcf-highlight-condition-service';
import {NumberFormatter, PixiTextSize, Sorting} from '../../../../utilities';
import {GeneTransformer} from '../../../gene/internal';
import VcfAnalyzer from '../../../../../../dataServices/vcf/vcf-analyzer';

const Math = window.Math;

export class VcfTransformer extends GeneTransformer {

    _collapsed = false;
    // TODO: remove before merge
    highlightProfile = {
        'is_default': true,
        'conditions': [
            {
                'highlight_color': 'ffbdbd',
                'condition': 'type == DEL'
            },
            {
                'highlight_color': 'ffff00',
                'condition': 'type == INS'
            },
            {
                'highlight_color': 'add8e6',
                'condition': 'type == SNV'
            }
        ]
    };

    constructor(config, chromosome) {
        super(config);
        this._chromosome = chromosome;
    }

    get collapsed() {
        return this._collapsed;
    }

    set collapsed(value) {
        this._collapsed = value;
    }

    isHistogramDrawingModeForViewport() {
        return false;
    }

    static getBubbleRadius(text, config) {
        const size = PixiTextSize.getTextSize(text, config);
        return Math.max(size.width, size.height) / 2;
    }

    transformCollapsedData(data, viewport) {
        if (data === null || data === undefined) {
            return {
                statisticsItems: [],
                variants: [],
                viewport: viewport
            };
        }

        let variants = [];
        let previousItem = null;
        const labelStyle = this.config.statistics.label;

        data.forEach(variant => VcfAnalyzer.analyzeVariant(variant, this._chromosome.name));
        for (let i = 0; i < data.length; i++) {
            const variant = data[i];
            variant.variationsCount = variant.variationsCount || 1;
            if (viewport.isShortenedIntronsMode && viewport.shortenedIntronsViewport.shouldSkipFeature(variant))
                continue;
            if (variant.structural && !variant.interChromosome && viewport.convert.brushBP2pixel(variant.length) > 1) {
                // we shouldn't combine this variant into bubbles
                variants.push(Object.assign({isStatistics: false}, variant));
            } else {
                if (previousItem) {
                    if (variant.startIndex > previousItem.endIndex &&

                        variant.startIndex - viewport.convert
                            .pixel2brushBP(this.config.statistics.bubble.margin) <=
                        (previousItem.endIndex + previousItem.startIndex) / 2
                        + viewport.convert.pixel2brushBP(previousItem.bubble.radius)) {

                        previousItem.isStatistics = true;
                        previousItem.variationsCount += variant.variationsCount;
                        previousItem.endIndex = variant.endIndex;
                        previousItem.bubble.radius =
                            VcfTransformer.getBubbleRadius(NumberFormatter.textWithPrefix(previousItem.variationsCount, false), labelStyle)
                            + this.config.statistics.bubble.padding;
                        continue;
                    }
                }
                const item = Object.assign({}, variant, {
                    bubble: {
                        radius: VcfTransformer.getBubbleRadius(NumberFormatter.textWithPrefix(variant.variationsCount, false), labelStyle)
                            + this.config.statistics.bubble.padding
                    },
                    isStatistics: variant.type.toLowerCase() === 'statistic',
                    startIndex: variant.startIndex + VcfAnalyzer.getVariantTranslation(variant),
                    variationsCount: variant.variationsCount
                });
                variants.push(item);
                previousItem = item;
            }
        }

        const parsedHighlightProfile = this.highlightProfile.conditions.map(item => ({
            highlightColor: item.highlight_color,
            parsedCondition: VcfHighlightConditionService.parseFullCondition(item.condition)
        }));
        variants.forEach(variant => this.addAllelesDescriptions(variant));
        variants.forEach(variant => this.addHighlight(parsedHighlightProfile, variant));
        variants = this.combineBubbles(variants, viewport);
        const hoverData = this.addVariantsLayerIndices(variants);
        return {
            hoverData: hoverData,
            variants: variants,
            viewport: viewport
        };
    }

    transformData(data, viewport) {
        if (this.collapsed) {
            return this.transformCollapsedData(data, viewport);
        }
        if (data === null || data === undefined) {
            return {
                statisticsItems: [],
                variants: [],
                viewport: viewport
            };
        }
        data.forEach(x => VcfAnalyzer.analyzeVariant(x, this._chromosome.name));
        let variants = [];
        let previousItem = null;
        const labelStyle = this.config.statistics.label;
        for (let i = 0; i < data.length; i++) {
            const variant = data[i];
            variant.variationsCount = variant.variationsCount || 1;
            if (viewport.isShortenedIntronsMode && viewport.shortenedIntronsViewport.shouldSkipFeature(variant))
                continue;
            if (variant.structural && !variant.interChromosome && viewport.convert.brushBP2pixel(variant.length) > 1) {
                // we shouldn't combine this variant into bubbles
                variants.push(Object.assign({isStatistics: false}, variant));
            } else {
                if (previousItem) {
                    if (variant.startIndex > previousItem.endIndex &&

                        variant.startIndex - viewport.convert
                            .pixel2brushBP(this.config.statistics.bubble.margin) <=
                        (previousItem.endIndex + previousItem.startIndex) / 2
                        + viewport.convert.pixel2brushBP(previousItem.bubble.radius)) {

                        if (!previousItem.variants) {
                            previousItem.variants = [Object.assign({}, previousItem)];
                        }
                        previousItem.variants.push(variant);
                        previousItem.isStatistics = true;
                        previousItem.variationsCount += variant.variationsCount;
                        previousItem.endIndex = variant.endIndex;
                        previousItem.bubble.radius =
                            VcfTransformer.getBubbleRadius(NumberFormatter.textWithPrefix(previousItem.variationsCount, false), labelStyle)
                            + this.config.statistics.bubble.padding;
                        continue;
                    }
                }
                const item = Object.assign({}, variant, {
                    bubble: {
                        radius: VcfTransformer.getBubbleRadius(NumberFormatter.textWithPrefix(variant.variationsCount, false), labelStyle)
                            + this.config.statistics.bubble.padding
                    },
                    isStatistics: variant.type.toLowerCase() === 'statistic',
                    startIndex: variant.startIndex + VcfAnalyzer.getVariantTranslation(variant),
                    variationsCount: variant.variationsCount
                });
                variants.push(item);
                previousItem = item;
            }
        }
        variants.forEach(variant => this.addAllelesDescriptions(variant));

        const parsedHighlightProfile = this.highlightProfile.conditions.map(item => ({
            highlightColor: item.highlight_color,
            parsedCondition: VcfHighlightConditionService.parseFullCondition(item.condition)
        }));
        variants.forEach(variant => this.addHighlight(parsedHighlightProfile, variant));
        variants = this.expandBubbles(this.combineBubbles(variants, viewport));
        data = null;
        return variants;
    }

    addAllelesDescriptions(variant) {
        if (variant.isStatistics)
            return;

        variant.allelesDescriptionsWidth = 0;
        variant.allelesDescriptionsHeight = 0;
        for (let j = 0; j < variant.alternativeAllelesInfo.length; j++) {
            const allele = variant.alternativeAllelesInfo[j];
            if (allele.displayText) {
                const size = PixiTextSize.getTextSize(allele.displayText, this.config.variant.allele.label);
                variant.allelesDescriptionsWidth = Math.max(size.width, variant.allelesDescriptionsWidth);
                variant.allelesDescriptionsHeight += size.height + this.config.variant.allele.margin;
            }
        }
    }

    addHighlight(parsedHighlightProfile, variant) {
        parsedHighlightProfile.forEach(item => {
            if (!variant.highlightColor && VcfHighlightConditionService.isHighlighted(variant, item.parsedCondition)) {
                variant.highlightColor = `0x${item.highlightColor.toUpperCase()}`;
            }
        });
    }

    combineBubbles(items, viewport) {
        const labelStyle = this.config.statistics.label;
        let previousItem = null;
        const result = [];
        for (let i = 0; i < items.length; i++) {
            const variant = items[i];
            if (!variant.isStatistics) {
                if (previousItem) {
                    result.push(previousItem);
                }
                result.push(variant);
                previousItem = null;
                continue;
            }
            if (previousItem) {
                if ((variant.startIndex + variant.endIndex) / 2 - viewport.convert.pixel2brushBP(variant.bubble.radius)
                    - viewport.convert.pixel2brushBP(this.config.statistics.bubble.margin) <=
                    (previousItem.endIndex + previousItem.startIndex) / 2
                    + viewport.convert.pixel2brushBP(previousItem.bubble.radius)) {
                    previousItem.variationsCount += variant.variationsCount;
                    previousItem.endIndex = variant.endIndex;
                    previousItem.bubble.radius =
                        VcfTransformer.getBubbleRadius(NumberFormatter.textWithPrefix(previousItem.variationsCount, false), labelStyle) +
                        this.config.statistics.bubble.padding;
                    continue;
                } else {
                    result.push(previousItem);
                }
            }
            previousItem = variant;
        }
        if (previousItem) {
            result.push(previousItem);
        }
        return result;
    }

    expandBubbles(items) {
        const result = [];
        for (let i = 0; i < items.length; i++) {
            const item = items[i];
            if (item.isStatistics && item.variationsCount < 50) {
                result.push(...item.variants);
            } else {
                result.push(item);
            }
        }
        return result;
    }

    addVariantsLayerIndices(variants) {
        const indices = [];
        variants.forEach((variant, index) => {
            variant.positioningInfos.forEach((pInfo, subIndex) => {
                pInfo.layerIndex = 0;
                pInfo.variantsUnderCount = 0;
                indices.push({
                    index: index,
                    subIndex: subIndex,
                    value: pInfo.length
                });
            });
        });
        const sortSelector = function (index) {
            return index.value;
        };
        const sorted = Sorting.quickSort(indices, false, sortSelector);

        let maxLayerIndex = 0;

        const intersects = function (var1, var2) {
            return (var1.startIndex <= var2.startIndex && var1.endIndex >= var2.startIndex) ||
                (var2.startIndex <= var1.startIndex && var2.endIndex >= var1.startIndex);
        };

        for (let i = 0; i < sorted.length; i++) {
            const testVariant = variants[sorted[i].index];
            const testVariantPositioningInfo = testVariant.positioningInfos[sorted[i].subIndex];
            testVariantPositioningInfo.maxLayerIndex = 0;
            let layerIndex = 0;
            const intersections = [];
            for (let j = 0; j < i; j++) {
                const intersectingVariantPositioningInfo = variants[sorted[j].index].positioningInfos[sorted[j].subIndex];
                if (intersects(testVariantPositioningInfo, intersectingVariantPositioningInfo)) {
                    intersections.push(sorted[j]);
                    if (intersectingVariantPositioningInfo.variantsUnderCount === undefined ||
                        intersectingVariantPositioningInfo.variantsUnderCount === null) {
                        intersectingVariantPositioningInfo.variantsUnderCount = 0;
                    }
                    if (!testVariant.isStatistics)
                        intersectingVariantPositioningInfo.variantsUnderCount++;
                    layerIndex = Math.max(layerIndex, intersectingVariantPositioningInfo.layerIndex) + 1;
                }
            }
            for (let j = 0; j < intersections.length; j++) {
                const intersectingVariant = variants[intersections[j].index];
                const intersectionVariantPositioningInfo = intersectingVariant.positioningInfos[intersections[j].subIndex];
                intersectionVariantPositioningInfo.maxLayerIndex = Math.max(intersectionVariantPositioningInfo.maxLayerIndex, layerIndex);
            }
            testVariantPositioningInfo.layerIndex = layerIndex;
            testVariantPositioningInfo.maxLayerIndex = layerIndex;
            if (maxLayerIndex < testVariantPositioningInfo.layerIndex) {
                maxLayerIndex = testVariantPositioningInfo.layerIndex;
            }
        }
        return {
            maxLayerIndex: maxLayerIndex
        };
    }
}