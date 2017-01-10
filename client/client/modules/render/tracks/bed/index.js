import {BEDRenderer, BEDTransformer} from './internal';
import BEDConfig from './bedConfig';
import {BedDataService} from '../../../../dataServices';
import {GENETrack} from '../gene';
import GeneConfig from '../gene/geneConfig';

export class BEDTrack extends GENETrack {
    static trackDefaultHeight = GeneConfig.height;
    trackLocalConfig = Object.assign(GeneConfig, BEDConfig);

    constructor(opts) {
        super(opts);
        this._actions = null;
    }

    get downloadHistogramFn() {
        return ::this.dataService.getBedHistogram;
    }

    get downloadDataFn() {
        return ::this.dataService.getBedTrack;
    }

    getSettings() {
        return null;
    }

    get dataService() {
        if (!this._dataService) {
            this._dataService = new BedDataService();
        }
        return this._dataService;
    }

    get transformer() {
        if (!this._transformer) {
            this._transformer = new BEDTransformer(this.trackLocalConfig);
        }
        return this._transformer;
    }

    get renderer() {
        if (!this._renderer) {
            this._renderer = new BEDRenderer(this.trackLocalConfig, this.transformer);
        }
        return this._renderer;
    }

    getTooltipDataObject(isHistogram, item) {
        if (isHistogram) {
            return [
                ['Count', item[0].value]
            ];
        } else if (item && item.length > 0) {
            const bedItem = item[0];
            const tooltipData = [];

            tooltipData.push(['Name', bedItem.name]);
            tooltipData.push(['Start', bedItem.startIndex]);
            tooltipData.push(['End', bedItem.endIndex]);

            if (bedItem.score !== null && bedItem.score !== undefined) {
                tooltipData.push(['Score', bedItem.score]);
            }
            if (bedItem.strand) {
                tooltipData.push(['Strand', bedItem.strand]);
            }
            if (bedItem.description && bedItem.description.length < this.trackLocalConfig.bed.description.maximumDisplayLength) {
                tooltipData.push(['Description', bedItem.description]);
            }
            return tooltipData;
        }
    }
}