import {CachedTrack} from '../../core';
import WIGArea from './wigArea.js';
import WIGConfig from './wigConfig';
import WIGRenderer from './wigRenderer';
import WIGTransformer from './wigTransformer';
import {WigDataService} from '../../../../dataServices';

export class WIGTrack extends CachedTrack {

    static trackDefaultHeight = WIGConfig.height;

    _wigArea = new WIGArea(this.viewport, WIGConfig);
    _wigRenderer = new WIGRenderer(WIGConfig);
    _wigTransformer = new WIGTransformer(WIGConfig);
    dataService = new WigDataService();

    async updateCache() {
        const reqToken = this.__currentDataUpdateReq = {};
        const data = await this.dataService.getWigTrack(this.cacheUpdateParameters(this.viewport));
        if (reqToken === this.__currentDataUpdateReq) {
            const transformedData = this._wigTransformer.transform(data, this.viewport);
            if (!this.cache) {
                return false;
            }
            this.cache.data = transformedData;
            return await super.updateCache();
        }
        return false;
    }

    render(flags) {
        let somethingChanged = super.render(flags);
        if (flags.renderReset) {
            this.container.addChild(this._wigRenderer.container);
            this.container.addChild(this._wigArea.axis);
            somethingChanged = true;
        }
        if (flags.brushChanged || flags.widthChanged || flags.heightChanged || flags.renderReset || flags.dataChanged) {
            if (this.cache.data) {
                this._wigArea.height = this.height;
                this._wigRenderer.height = this.height;
                this.cache.coordinateSystem = this._wigTransformer.transformCoordinateSystem(this.cache.data,
                    this.viewport, this.cache.coordinateSystem);
                this._wigArea.render(this.viewport, this.cache.coordinateSystem);
                this._wigRenderer.render(this.viewport, this.cache, flags.heightChanged);
                somethingChanged = true;
            }
        }
        return somethingChanged;
    }

    onHover({x, y}) {
        super.onHover({x, y});
        if (this.shouldDisplayTooltips) {
            const hoveredItem = this._wigRenderer.onMove(this.viewport, {x, y}, this.cache.data);
            if (hoveredItem) {
                this.tooltip.setContent([['Count', Math.ceil(hoveredItem.value)]]);
                this.tooltip.move({x, y});
                this.tooltip.show({x, y});
                return;
            }
        }
        this.tooltip.hide();
    }
}