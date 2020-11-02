import {CachedTrack} from '../../core';
import WIGArea from './wigArea.js';
import WIGConfig from './wigConfig';
import WIGRenderer from './wigRenderer';
import WIGTransformer from './wigTransformer';
import {WigDataService} from '../../../../dataServices';
import {default as menu} from './menu';
import {scaleModes} from './modes';
import {menu as menuUtilities} from '../../utilities';

export class WIGTrack extends CachedTrack {

    _wigArea = new WIGArea(this.viewport, this.trackConfig);
    _wigRenderer = new WIGRenderer(this.trackConfig);
    _wigTransformer = new WIGTransformer(this.trackConfig);
    dataService = new WigDataService();

    constructor(opts) {
        super(opts);
        this.dataService = new WigDataService(opts.viewport);
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
        const getCoverageExtremum = () => {
            let max = 0;
            let min = 0;
            if (this.cache && this.cache.coordinateSystem) {
                max = this.cache.coordinateSystem.realMaximum;
                min = this.cache.coordinateSystem.realMinimum;
            } else {
                min = this.state.coverageScaleFrom;
                max = this.state.coverageScaleTo;
            }
            return {max, min};
        };
        const wrapStateFn = (fn) => () => fn(this.state);
        const wrapMutatorFn = (fn, key) => () => {
            const currentDisplayMode = this.state.coverageDisplayMode;
            const currentScaleMode = this.state.coverageScaleMode;
            const logScaleEnabled = this.state.coverageLogScale;
            fn(this.state);
            let shouldReportTrackState = true;
            if (key === 'coverage>scale>manual' && this.state.coverageScaleMode === scaleModes.manualScaleMode) {
                shouldReportTrackState = false;
                if (currentScaleMode !== this.state.coverageScaleMode) {
                    this.state.coverageScaleMode = scaleModes.defaultScaleMode;
                }
                this.config.dispatcher.emitSimpleEvent('tracks:coverage:manual:configure', {
                    source: this.config.name,
                    config: {
                        extremumFn: getCoverageExtremum,
                        isLogScale: this.state.coverageLogScale
                    }
                });
            } else if (currentScaleMode !== this.state.coverageScaleMode) {
                this._flags.dataChanged = true;
                this.state.coverageScaleFrom = undefined;
                this.state.coverageScaleTo = undefined;
            } else if (logScaleEnabled !== this.state.coverageLogScale) {
                this._flags.dataChanged = true;
            } else if (currentDisplayMode !== this.state.coverageDisplayMode) {
                this._flags.dataChanged = true;
            }
            if (shouldReportTrackState) {
                this.reportTrackState();
            }
            this.requestRenderRefresh();
        };

        this._menu = menu.map(function processMenuList(menuEntry) {
            const result = {};
            for (const key of Object.keys(menuEntry)) {
                switch (true) {
                    case Array.isArray(menuEntry[key]): {
                        result[key] = menuEntry[key].map(processMenuList);
                    }
                        break;
                    case menuEntry[key] instanceof Function: {
                        switch (true) {
                            case key.startsWith('is'): {
                                result[key] = wrapStateFn(menuEntry[key]);
                            }
                                break;
                            case key.startsWith('display'): {
                                result[key] = wrapStateFn(menuEntry[key]);
                            }
                                break;
                            default: {
                                result[key] = wrapMutatorFn(menuEntry[key], menuEntry.name);
                            }
                                break;
                        }
                    }
                        break;
                    default: {
                        result[key] = menuEntry[key];
                    }
                        break;
                }
            }

            return result;
        });

        return this._menu;
    }

    get stateKeys() {
        return [
            'coverageDisplayMode',
            'coverageLogScale',
            'coverageScaleMode',
            'coverageScaleFrom',
            'coverageScaleTo'
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
            this.container.addChild(this._wigArea.logScaleIndicator);
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
            this._wigArea.render(this.viewport, this.cache.coordinateSystem);
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
                this.tooltip.setContent([['Count', Math.ceil(dataItem.value)]]);
                this.tooltip.move({x, y});
                this.tooltip.show({x, y});
                return;
            }
        }
        this.tooltip.hide();

    }
}