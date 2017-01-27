import {StatisticsContainer} from './statisticsContainer';
import {VariantBaseContainer} from './baseContainer';
import {VariantContainer} from './variantContainer';

export function initializeContainer(variant, config, tooltipContainer): VariantBaseContainer {
    if (variant.isStatistics) {
        return new StatisticsContainer(variant, config);
    } else {
        return new VariantContainer(variant, config, tooltipContainer);
    }
}