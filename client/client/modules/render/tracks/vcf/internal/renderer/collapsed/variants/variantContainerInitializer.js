import {StatisticsContainer} from './statisticsContainer';
import {VariantBaseContainer} from './baseContainer';
import {VariantContainer} from './variantContainer';

export function initializeContainer(variant, config, track, tooltipContainer): VariantBaseContainer {
    if (variant.isStatistics) {
        return new StatisticsContainer(variant, config, track);
    } else {
        return new VariantContainer(variant, config, track, tooltipContainer);
    }
}
