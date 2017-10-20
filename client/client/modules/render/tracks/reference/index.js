import * as modes from './reference.modes';
import {CachedTrack} from '../../core';
import {GenomeDataService} from '../../../../dataServices';
import ReferenceConfig from './referenceConfig';
import ReferenceRenderer from './referenceRenderer';
import ReferenceTransformer from './referenceTransformer';
import {default as menu} from './menu';
import {menu as menuUtilities} from '../../utilities';

export class REFERENCETrack extends CachedTrack {

    _referenceRenderer = new ReferenceRenderer(this.trackConfig);
    dataService = new GenomeDataService();

    constructor(opts) {
        super(opts);
        if (opts.dispatcher) {
            this.dispatcher = opts.dispatcher;
        }
        this.trackSettingsListener = (params) => {
            if(this.config.bioDataItemId === params.id) {
                const settings = params.settings;
                settings.forEach(setting => {
                    const menuItem = menuUtilities.findMenuItem(this._menu, setting.name);
                    if (menuItem.type === 'checkbox') {
                        setting.value ? menuItem.enable() : menuItem.disable();
                    }
                })
            }
        };
        const _trackSettingsListener = ::this.trackSettingsListener;
        const self = this;
        this._removeTrackSettingsListener = function() {
            self.dispatcher.removeListener('trackSettings:change', _trackSettingsListener);
        };
        this.dispatcher.on('trackSettings:change', _trackSettingsListener);
    }

    static getTrackDefaultConfig() {
        return ReferenceConfig;
    }

    get trackIsResizable() {
        return false;
    }

    get stateKeys() {
        return [
            'referenceShowTranslation',
            'referenceShowForwardStrand',
            'referenceShowReverseStrand'
        ];
    }

    getSettings() {
        if (this._menu) {
            return this._menu;
        }
        const wrapStateFn = (fn) => () => fn(this.state);
        const wrapMutatorFn = (fn) => () => {
            fn(this.state);
            this.updateHeight();
            this.updateAndRefresh();
            this.reportTrackState();
        };

        this._menu = menu.map(function processMenuList(menuEntry) {
            const result = {};
            for (const key of Object.keys(menuEntry)) {
                switch (true) {
                    case Array.isArray(menuEntry[key]):
                        result[key] = menuEntry[key].map(processMenuList);
                        break;
                    case menuEntry[key] instanceof Function: {
                        switch (true) {
                            case key.startsWith('is'):
                                result[key] = wrapStateFn(menuEntry[key]);
                                break;
                            case key.startsWith('display'):
                                result[key] = wrapStateFn(menuEntry[key]);
                                break;
                            default:
                                result[key] = wrapMutatorFn(menuEntry[key]);
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

    async updateCache() {
        const reqToken = this.__currentDataUpdateReq = {};
        const data = await this.dataService.loadReferenceTrack(this.cacheUpdateParameters(this.viewport));
        if (reqToken === this.__currentDataUpdateReq && this.cache) {
            if (data.mode) {
                switch (data.mode) {
                    case modes.gcContentNotProvided:
                        this.hideTrack();
                        break;
                    default:
                        this.showTrack();
                        break;
                }
            }
            const transformedData = ReferenceTransformer.transform(data, this.viewport, this.cache.data);
            if (!this.cache) {
                return false;
            }
            if (transformedData.mode) {
                switch (transformedData.mode) {
                    case modes.gcContentNotProvided:
                        this.hideTrack();
                        break;
                    default:
                        this.showTrack();
                        break;
                }
            }
            this.cache.data = transformedData;
            return await super.updateCache();
        }
        return false;
    }

    render(flags) {
        let somethingChanged = super.render(flags);
        this.updateHeight();

        if (flags.renderReset) {
            somethingChanged = true;
            this.container.addChild(this._referenceRenderer.container);
        }
        if (flags.brushChanged || flags.widthChanged || flags.heightChanged || flags.renderReset || flags.dataChanged) {
            somethingChanged = true;
            this._referenceRenderer.render(this.viewport, this.cache, flags.heightChanged || flags.dataChanged, null, this._showCenterLine, this.state);
        }
        return somethingChanged;
    }

    async updateAndRefresh() {
        await this.updateCache();
        this._flags.dataChanged = true;
        await this.requestRenderRefresh();
    }

    updateHeight() {
        if (this.cache.data && this.cache.data.mode === modes.gcContent) {
            this._referenceRenderer.height = this.trackConfig.minHeight;
        }
        else if (this.state.referenceShowForwardStrand && this.state.referenceShowReverseStrand && this.state.referenceShowTranslation) {
            this._referenceRenderer.height = this.trackConfig.height;
        }
        else if (this.state.referenceShowReverseStrand && this.state.referenceShowTranslation || this.state.referenceShowForwardStrand && this.state.referenceShowTranslation) {
            this._referenceRenderer.height = this.trackConfig.height / 2;
        }
        else if (this.state.referenceShowReverseStrand && this.state.referenceShowForwardStrand && !this.state.referenceShowTranslation) {
            this._referenceRenderer.height = 2 * this.trackConfig.nucleotidesHeight;
        }
        else if (this.state.referenceShowReverseStrand || this.state.referenceShowForwardStrand) {
            this._referenceRenderer.height = this.trackConfig.nucleotidesHeight;
        }
        else {
            this._referenceRenderer.height = this.trackConfig.minHeight;
        }

        this.height = this._referenceRenderer.height;
    }
}
