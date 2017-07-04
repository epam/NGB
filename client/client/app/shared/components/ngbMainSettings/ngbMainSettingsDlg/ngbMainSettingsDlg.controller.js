const DEFAULT_CONFIG = 7000;
export default class ngbMainSettingsDlgController {
    settings = null;

    static get UID() {
        return 'ngbMainSettingsDlgController';
    }

    showTrackHeadersIsDisabled = false;

    /* @ngInject */
    constructor(dispatcher, projectContext, localDataService, $mdDialog, ngbMainSettingsDlgService, $scope, settings) {
        this._dispatcher = dispatcher;
        this._localDataService = localDataService;
        this._mdDialog = $mdDialog;
        this.settings = settings;
        this.settings && (this.settings.maxBAMBP = this.settings.maxBAMBP || DEFAULT_CONFIG);
        this.showTrackHeadersIsDisabled = projectContext.collapsedTrackHeaders !== undefined && projectContext.collapsedTrackHeaders;
        this.settingsService = ngbMainSettingsDlgService;
        this.customizeSettings = this.settingsService.getSettings();

        this.scope = $scope;
    }

    close() {
        this._mdDialog.hide();
    }

    save() {
        this.settingsService.updateThisLocalSettingsVar(this.settings);
        this._localDataService.updateSettings(this.settings);
        this._dispatcher.emitGlobalEvent('settings:change', this.settings);
        this.close();
    }

    getterSetterMaximumBPForBam(input) {
        return arguments.length ? this.settings.maxBAMBP = input : this.settings.maxBAMBP;
    }

    setToDefaultCustomizations() {
        this.customizeSettings = this.settingsService.getDefaultSettings();
        this.scope.$broadcast('setToDefault');
    }
}