import * as modes from './reference.modes';
import {CachedTrack} from '../../core';
import {GenomeDataService} from '../../../../dataServices';
import ReferenceConfig from './referenceConfig';
import ReferenceRenderer from './referenceRenderer';
import ReferenceTransformer from './referenceTransformer';

export class REFERENCETrack extends CachedTrack {

    static trackDefaultHeight = ReferenceConfig.height;

    _referenceRenderer = new ReferenceRenderer(ReferenceConfig);
    dataService = new GenomeDataService();

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
            this._referenceRenderer.render(this.viewport, this.cache, flags.heightChanged);
        }
        return somethingChanged;
    }
}
