import {CachedTrack} from '../../core';
import Menu from '../../core/menu';
import SegConfig from './segConfig';
import {SegDataService} from '../../../../dataServices';
import SegMenu from './menu';
import SegRenderer from './segRenderer';
import SegTransformer from './segTransformer';

export class SEGTrack extends CachedTrack {

    _dataService;
    _renderer = new SegRenderer(this.trackConfig);

    static postStateMutatorFn = (track) => {
        track.reportTrackState();
    };

    static Menu = Menu(
        SegMenu,
        {
            postStateMutatorFn: SEGTrack.postStateMutatorFn
        }
    );

    constructor(opts) {
        super(opts);
        this._dataService = new SegDataService(opts.viewport);
    }

    getSettings() {
        if (this._menu) {
            return this._menu;
        }
        this._menu = this.constructor.Menu.attach(this, {browserId: this.browserId});
        return this._menu;
    }

    static getTrackDefaultConfig() {
        return SegConfig;
    }

    get dataService() {
        return this._dataService;
    }

    get segTrack() {
        return this.dataService.loadSegTrack.bind(this.dataService);
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
            this._renderer.render(this.viewport, this.cache, flags.heightChanged || flags.dataChanged, null, null, this._showCenterLine);
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