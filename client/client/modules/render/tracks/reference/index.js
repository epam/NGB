import * as modes from './reference.modes';
import {CachedTrack} from '../../core';
import {GenomeDataService} from '../../../../dataServices';
import ReferenceConfig from './referenceConfig';
import ReferenceRenderer from './referenceRenderer';
import ReferenceTransformer from './referenceTransformer';
import {default as menu} from './menu';

export class REFERENCETrack extends CachedTrack {

    _referenceRenderer = new ReferenceRenderer(this.trackConfig);
    dataService = new GenomeDataService();

    static getTrackDefaultConfig() {
        return ReferenceConfig;
    }

    get trackIsResizable() {
        return false;
    }

    get stateKeys() {
        return [
            'referenceTranslation',
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
                    case modes.gcContentNotProvided: this.hideTrack(); break;
                    default: this.showTrack(); break;
                }
            }
            const transformedData = ReferenceTransformer.transform(data, this.viewport, this.cache.data);
            if (!this.cache) {
                return false;
            }
            if (transformedData.mode) {
                switch (transformedData.mode) {
                    case modes.gcContentNotProvided: this.hideTrack(); break;
                    default: this.showTrack(); break;
                }
            }
            this.cache.data = transformedData;
            return await super.updateCache();
        }
        return false;
    }

    render(flags) {
        super.render(flags);
        let somethingChanged = false;

        if (flags.renderReset) {
            somethingChanged = true;
            this.container.addChild(this._referenceRenderer.container);
        }
        if (flags.brushChanged || flags.widthChanged || flags.heightChanged || flags.renderReset || flags.dataChanged) {
            somethingChanged = true;
            this._referenceRenderer.height = this.height;
            this._referenceRenderer.render(this.viewport, this.cache, flags.heightChanged || flags.dataChanged, null, this._showCenterLine, this.state);
        }
        return somethingChanged;
    }

    async updateAndRefresh() {
        await this.updateCache();
        this._flags.dataChanged = true;
        await this.requestRenderRefresh();
    }
}
