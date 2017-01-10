import {CachedTrack} from '../../core';
import SegConfig from './segConfig';
import {SegDataService} from '../../../../dataServices';
import SegRenderer from './segRenderer';
import SegTransformer from './segTransformer';

export class SEGTrack extends CachedTrack {

    static trackDefaultHeight = SegConfig.height;

    _dataService = new SegDataService();
    _renderer = new SegRenderer(SegConfig);

    get dataService() {
        return this._dataService;
    }

    get segTrack() {
        return ::this.dataService.loadSegTrack;
    }

    async updateCache() {
        const data = await this.segTrack(this.cacheUpdateParameters(this.viewport));
        const transformedData = SegTransformer.transform(data, this.viewport, this.cache.data);
        if (!this.cache) {
            return false;
        }
        this.cache.data = transformedData;
        this._renderer.height = this.height;
        return await super.updateCache();
    }

    render(flags) {
        let somethingChanged = super.render(flags);
        if (flags.renderReset) {
            this.container.addChild(this._renderer.container);
            somethingChanged = true;
        }
        if (flags.brushChanged || flags.widthChanged || flags.heightChanged || flags.renderReset || flags.dataChanged) {
            flags.heightChanged && (this._renderer.height = this.height);
            this._renderer.render(this.viewport, this.cache, flags.heightChanged);
            somethingChanged = true;
        }
        return somethingChanged;
    }

    hoverVerticalScroll() {
        return this._renderer.hoverVerticalScroll(this.viewport);
    }

    unhoverVerticalScroll() {
        return this._renderer.unhoverVerticalScroll(this.viewport);
    }

    canScroll(delta) {
        return this._renderer.canScroll(delta);
    }

    isScrollable() {
        return this._renderer.isScrollable();
    }

    scrollIndicatorBoundaries() {
        return this._renderer.scrollIndicatorBoundaries(this.viewport);
    }

    setScrollPosition(value) {
        this._renderer.setScrollPosition(this.viewport, value);
    }

    onScroll({delta}) {
        this._renderer.scroll(this.viewport, delta);
        this.tooltip.hide();
        this.updateScene();
    }

    onHover({x,y}) {
        if (super.onHover({x,y})) {
            if (this.shouldDisplayTooltips) {
                const data = SEGTrack.getTooltipDataObject(this._renderer.checkPosition({x, y}));
                this.tooltip.setContent(data);
                this.tooltip.move({x, y});
                this.tooltip.show({x, y});
            } else {
                this.tooltip.hide();
            }
            return false;
        }
        return true;
    }

    static getTooltipDataObject(segData) {
        const info = [];
        for (const property in segData) {
            if (segData.hasOwnProperty(property)) {
                info.push([property, segData[property]]);
            }
        }
        return info;
    }

}