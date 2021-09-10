import {BEDRenderer, BEDTransformer} from './internal';
import BEDConfig from './bedConfig';
import {BedDataService} from '../../../../dataServices';
import BedMenu from './menu';
import {GENETrack} from '../gene';
import GeneConfig from '../gene/geneConfig';
import Menu from '../../core/menu';

export class BEDTrack extends GENETrack {

    static getTrackDefaultConfig() {
        return Object.assign({}, GeneConfig, BEDConfig);
    }

    static Menu = Menu(
        BedMenu,
        {
            postStateMutatorFn: GENETrack.postStateMutatorFn,
        }
    );

    get stateKeys() {
        return [
            'header',
            'color'
        ];
    }

    get featuresFilteringEnabled () {
        return false;
    }

    constructor(opts) {
        super(opts);
        this._actions = null;
    }

    cacheUpdateParameters(viewport) {
        const params = super.cacheUpdateParameters(viewport);
        let extraFactor = 1;
        const kb = 1000;
        const mb = 1000 * kb;
        if (viewport.brushSize > 20 * mb) {
            extraFactor = viewport.brushSize / mb;
        } else if (viewport.brushSize > 500 * kb) {
            extraFactor = 20;
        } else if (viewport.brushSize > 5 * kb) {
            extraFactor = 10;
        }
        const mapParams = p => ({
            ...p,
            scaleFactor: p.scaleFactor / extraFactor
        });
        if (Array.isArray(params)) {
            return params.map(mapParams);
        }
        return mapParams(params);
    }

    getSettings() {
        if (this._menu) {
            return this._menu;
        }
        this._menu = this.config.sashimi ? [] : this.constructor.Menu.attach(this, {browserId: this.browserId});
        return this._menu;
    }

    get downloadHistogramFn() {
        return ::this.dataService.getBedHistogram;
    }

    get downloadDataFn() {
        return ::this.dataService.getBedTrack;
    }

    get dataService() {
        if (!this._dataService) {
            this._dataService = new BedDataService(this.dispatcher);
        }
        return this._dataService;
    }

    get transformer() {
        if (!this._transformer) {
            this._transformer = new BEDTransformer(this.trackConfig);
        }
        return this._transformer;
    }

    get renderer() {
        if (!this._renderer) {
            this._renderer = new BEDRenderer(
                this.trackConfig,
                this.transformer,
                this
            );
        }
        return this._renderer;
    }

    applyAdditionalRequestParameters() {
    }

    getTooltipDataObject(isHistogram, item) {
        if (isHistogram) {
            return [
                ['Count', item[0].value]
            ];
        } else if (item && item.length > 0) {
            const bedItem = item[0].feature;
            const tooltipData = [];
            if (bedItem.name && bedItem.name.length) {
                tooltipData.push(['Name', bedItem.name]);
            }
            tooltipData.push(['Start', bedItem.startIndex]);
            tooltipData.push(['End', bedItem.endIndex]);

            if (bedItem.score !== null && bedItem.score !== undefined) {
                tooltipData.push(['Score', bedItem.score]);
            }
            if (bedItem.strand) {
                tooltipData.push(['Strand', bedItem.strand]);
            }
            if (bedItem.description && bedItem.description.length < this.trackConfig.bed.description.maximumDisplayLength) {
                tooltipData.push(['Description', bedItem.description]);
            }
            if (bedItem.additional) {
                const keys = Object
                    .keys(bedItem.additional)
                    .filter(key => ['name', 'startIndex', 'endIndex', 'description', 'strand', 'score'].indexOf(key) === -1);
                for (let i = 0; i < keys.length; i++) {
                    const key = keys[i];
                    if (bedItem.additional.hasOwnProperty(key)) {
                        tooltipData.push([key, bedItem.additional[key]]);
                    }
                }
            }
            return tooltipData;
        }
    }
}
