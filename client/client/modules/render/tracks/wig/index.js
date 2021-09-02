import {CachedTrack} from '../../core';
import WIGArea from './wigArea.js';
import WIGConfig from './wigConfig';
import WIGRenderer from './wigRenderer';
import WIGTransformer from './wigTransformer';
import {WigDataService} from '../../../../dataServices';
import {default as menu, scaleModesMutators} from './menu';
import {menu as menuUtilities} from '../../utilities';
import Menu from '../../core/menu';
import {scaleModes} from '../common/scaleModes';

export class WIGTrack extends CachedTrack {

    _wigArea = new WIGArea(this.viewport, this.trackConfig);
    _wigRenderer = new WIGRenderer(this.trackConfig, this.state);
    _wigTransformer = new WIGTransformer(this.trackConfig);
    dataService = new WigDataService();

    static postStateMutatorFn = (track, key, prePayload) => {
        if (scaleModesMutators.postStateMutatorFn(track, key, prePayload)) {
            track.reportTrackState();
        }
        track.requestRenderRefresh();
    }

    static Menu = Menu(
        menu,
        {
            postStateMutatorFn: WIGTrack.postStateMutatorFn,
            preStateMutatorFn: scaleModesMutators.preStateMutatorFn,
            afterStateMutatorFn: scaleModesMutators.afterStateMutatorFn
        }
    );

    constructor(opts) {
        super(opts);
        this.dataService = new WigDataService(opts.viewport);
        this._wigTransformer.registerGroupAutoScaleManager(opts.groupAutoScaleManager, this);
        this._wigArea.registerGroupAutoScaleManager(opts.groupAutoScaleManager, this);
    }

    trackSettingsChanged(params) {
        if (this.config.bioDataItemId === params.id) {
            const settings = params.settings;
            settings.forEach(setting => {
                const menuItem = menuUtilities.findMenuItem(this._menu, setting.name);
                if (menuItem.type === 'checkbox') {
                    if (setting.name === 'coverage>scale>manual') {
                        if (setting.value) {
                            this.state.coverageScaleFrom = setting.extraOptions.from;
                            this.state.coverageScaleTo = setting.extraOptions.to;
                            this.state.coverageScaleMode = scaleModes.manualScaleMode;
                        } else {
                            this.state.coverageScaleMode = scaleModes.defaultScaleMode;
                        }
                        this._flags.dataChanged = true;
                    } else {
                        setting.value ? menuItem.enable() : menuItem.disable();
                    }
                }
            });
        }
    }

    static getTrackDefaultConfig() {
        return WIGConfig;
    }

    get trackHasCoverageSubTrack() {
        return true;
    }

    getSettings() {
        if (this._menu) {
            return this._menu;
        }
        this._menu = this.constructor.Menu.attach(this, {browserId: this.browserId});
        return this._menu;
    }

    get stateKeys() {
        return [
            'coverageDisplayMode',
            'coverageLogScale',
            'coverageScaleMode',
            'coverageScaleFrom',
            'coverageScaleTo',
            'wigColors',
            'header',
            'groupAutoScale'
        ];
    }

    async updateCache() {
        const reqToken = this.__currentDataUpdateReq = {};
        if (this.trackDataLoadingStatusChanged) {
            this.trackDataLoadingStatusChanged(true);
        }
        const data = await this.dataService.getWigTrack(this.cacheUpdateParameters(this.viewport));
        if (this.trackDataLoadingStatusChanged) {
            this.trackDataLoadingStatusChanged(false);
        }
        if (reqToken === this.__currentDataUpdateReq) {
            if (!this.cache) {
                return false;
            }
            this.cache.originalData = data;
            this.cache.data = this._wigTransformer.transform(data, this.viewport);
            return await super.updateCache();
        }
        return false;
    }

    render(flags) {
        let somethingChanged = super.render(flags);
        if (flags.renderReset) {
            this.container.addChild(this._wigRenderer.container);
            this.container.addChild(this._wigArea.logScaleIndicator);
            this.container.addChild(this._wigArea.groupAutoScaleIndicator);
            this.container.addChild(this._wigArea.axis);
            somethingChanged = true;
        }
        if (flags.brushChanged || flags.widthChanged || flags.heightChanged || flags.renderReset || flags.dataChanged) {
            if (this.cache.data) {
                this._wigArea.height = this.height;
                this._wigRenderer.height = this.height;
                if (flags.dataChanged) {
                    this.cache.coordinateSystem = this._wigTransformer.transformCoordinateSystem(this.cache.data,
                        this.viewport, this.cache.coordinateSystem, this.state);
                }
            }
            this._wigArea.render(this.viewport, this.cache.coordinateSystem, this.state);
            this._wigRenderer.render(this.viewport, this.cache, flags.heightChanged || flags.dataChanged, null, this._showCenterLine);
            somethingChanged = true;

        }
        return somethingChanged;
    }

    onMouseOut() {
        super.onMouseOut();
        if (this._wigRenderer) {
            this._wigRenderer.hoverItem(null);
            this.requestRenderRefresh();
        }
    }

    onHover({x, y}) {
        super.onHover({x, y});
        if (this.shouldDisplayTooltips) {
            const hoveredItem = this._wigRenderer.onMove(this.viewport, {x, y}, this.cache.data);
            if (this.hoveringEffects) {
                this._wigRenderer.hoverItem(hoveredItem, this.viewport, this.cache.data, this.cache.coordinateSystem);
            }
            if (hoveredItem) {
                const {dataItem} = hoveredItem;
                this.tooltip.setContent([['Count', dataItem.value]]);
                this.tooltip.move({x, y});
                this.tooltip.show({x, y});
                return;
            }
        }
        this.tooltip.hide();

    }
}
