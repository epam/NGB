import * as renderers from './drawing';
import {FeatureRenderer} from '../../../../../gene/internal/renderer/features';

export default class VCFFeatureRenderer extends FeatureRenderer {

    _commonFeatureRenderer = null;
    _deletionFeatureRenderer = null;
    _statisticsFeatureRenderer = null;
    _interChromosomeRenderer = null;
    _intraChromosomeRenderer = null;
    _variantAltFeatureRenderer = null;

    constructor(config) {
        super(config);
        this._commonFeatureRenderer = new renderers.CommonVariantFeatureRenderer(config,
            this.registerLabel.bind(this),
            this.registerDockableElement.bind(this),
            this.registerFeaturePosition.bind(this),
            this.registerAttachedElement.bind(this));
        this._variantAltFeatureRenderer = new renderers.VariantAltFeatureRenderer(config,
            this.registerLabel.bind(this),
            this.registerDockableElement.bind(this),
            this.registerFeaturePosition.bind(this),
            this.registerAttachedElement.bind(this));
        this._deletionFeatureRenderer = new renderers.DeletionRenderer(config,
            this.registerLabel.bind(this),
            this.registerDockableElement.bind(this),
            this.registerFeaturePosition.bind(this),
            this.registerAttachedElement.bind(this));
        this._statisticsFeatureRenderer = new renderers.StatisticsRenderer(config,
            this.registerLabel.bind(this),
            this.registerDockableElement.bind(this),
            this.registerFeaturePosition.bind(this),
            this.registerAttachedElement.bind(this));
        this._interChromosomeRenderer = new renderers.InterChromosomeRenderer(config,
            this.registerLabel.bind(this),
            this.registerDockableElement.bind(this),
            this.registerFeaturePosition.bind(this),
            this.registerAttachedElement.bind(this));
        this._intraChromosomeRenderer = new renderers.IntraChromosomeRenderer(config,
            this.registerLabel.bind(this),
            this.registerDockableElement.bind(this),
            this.registerFeaturePosition.bind(this),
            this.registerAttachedElement.bind(this));
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