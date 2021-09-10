import * as renderers from './drawing';
import {FeatureRenderer} from '../../../../../gene/internal/renderer/features';

export default class VCFFeatureRenderer extends FeatureRenderer {

    _commonFeatureRenderer = null;
    _deletionFeatureRenderer = null;
    _statisticsFeatureRenderer = null;
    _interChromosomeRenderer = null;
    _intraChromosomeRenderer = null;
    _variantAltFeatureRenderer = null;

    constructor(config, track) {
        super(config, track);
        this._commonFeatureRenderer = new renderers.CommonVariantFeatureRenderer(
            track,
            config,
            ::this.registerLabel,
            ::this.registerDockableElement,
            ::this.registerFeaturePosition,
            ::this.registerAttachedElement);
        this._variantAltFeatureRenderer = new renderers.VariantAltFeatureRenderer(
            track,
            config,
            ::this.registerLabel,
            ::this.registerDockableElement,
            ::this.registerFeaturePosition,
            ::this.registerAttachedElement
        );
        this._deletionFeatureRenderer = new renderers.DeletionRenderer(
            track,
            config,
            ::this.registerLabel,
            ::this.registerDockableElement,
            ::this.registerFeaturePosition,
            ::this.registerAttachedElement
        );
        this._statisticsFeatureRenderer = new renderers.StatisticsRenderer(
            track,
            config,
            ::this.registerLabel,
            ::this.registerDockableElement,
            ::this.registerFeaturePosition,
            ::this.registerAttachedElement
        );
        this._interChromosomeRenderer = new renderers.InterChromosomeRenderer(
            track,
            config,
            ::this.registerLabel,
            ::this.registerDockableElement,
            ::this.registerFeaturePosition,
            ::this.registerAttachedElement
        );
        this._intraChromosomeRenderer = new renderers.IntraChromosomeRenderer(
            track,
            config,
            ::this.registerLabel,
            ::this.registerDockableElement,
            ::this.registerFeaturePosition,
            ::this.registerAttachedElement
        );
    }

    getRendererForFeature(feature) {
        if (feature.isStatistics) {
            return this._statisticsFeatureRenderer;
        } else {
            switch (feature.type.toLowerCase()) {
                case 'del': return this._deletionFeatureRenderer;
                case 'ins': return this._variantAltFeatureRenderer;
                case 'snv': return this._variantAltFeatureRenderer;
                default: {
                    if (feature.structural && feature.interChromosome) {
                        return this._interChromosomeRenderer;
                    } else if (feature.structural) {
                        return this._intraChromosomeRenderer;
                    } else {
                        return this._commonFeatureRenderer;
                    }
                }
            }
        }
    }

    get renderers() {
        return [
            this._commonFeatureRenderer,
            this._variantAltFeatureRenderer,
            this._deletionFeatureRenderer,
            this._statisticsFeatureRenderer,
            this._interChromosomeRenderer,
            this._intraChromosomeRenderer
        ];
    }
}
