const NCBI_SOURCE = 'NCBI';
const GOOGLE_PATENTS_SOURCE = 'GOOGLE_PATENTS';

const SOURCE_OPTIONS = {
    NCBI: {
        displayName: 'NCBI',
        name: NCBI_SOURCE
    },
    GOOGLE_PATENTS: {
        displayName: 'Google Patents',
        name: GOOGLE_PATENTS_SOURCE
    }
};

export { NCBI_SOURCE, GOOGLE_PATENTS_SOURCE };

export default class ngbPatentsPanelService {
    _sourceModel = SOURCE_OPTIONS.NCBI;
    _targetSettingsPromise;
    static instance (dispatcher, utilsDataService) {
        return new ngbPatentsPanelService(dispatcher, utilsDataService);
    }

    constructor(dispatcher, utilsDataService) {
        Object.assign(this, {dispatcher, utilsDataService});
        this._sourceModel = this.sourceOptions.NCBI;
        this._targetSettingsPromise = undefined;
    }

    get sourceOptions () {
        return SOURCE_OPTIONS;
    }

    get sourceModel() {
        return this._sourceModel;
    }
    set sourceModel(value) {
        this._sourceModel = value;
        this.dispatcher.emit('target:identification:patents:source:changed');
    }
    resetData() {
        this._sourceModel = this.sourceOptions.OPEN_TARGETS;
    }

    async getTargetSettings() {
        if (!this._targetSettingsPromise) {
            this._targetSettingsPromise = new Promise(async (resolve) => {
                try {
                    const {target_settings: targetSettings = {}} = await this.utilsDataService.getDefaultTrackSettings();
                    resolve(targetSettings);
                } catch (e) {
                    console.log(e);
                    resolve({});
                }
            });
        }
        return this._targetSettingsPromise;
    }
}
