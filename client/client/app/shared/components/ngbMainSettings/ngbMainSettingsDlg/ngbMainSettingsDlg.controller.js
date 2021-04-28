const DEFAULT_CONFIG = 7000;
export default class ngbMainSettingsDlgController {
    settings = null;

    accessToken = '';
    tokenValidDate;
    isTokenLoading = false;

    static get UID() {
        return 'ngbMainSettingsDlgController';
    }

    isRoleModelEnabled = false;
    showTrackHeadersIsDisabled = false;

    /* @ngInject */
    constructor(dispatcher, projectContext, localDataService, $mdDialog, ngbMainSettingsDlgService, $scope, settings, moment, userDataService, utilsDataService) {
        this._dispatcher = dispatcher;
        this._localDataService = localDataService;
        this.userIsAdmin = false;
        this._mdDialog = $mdDialog;
        this.settings = settings;
        this.settings && (this.settings.maxBAMBP = this.settings.maxBAMBP || DEFAULT_CONFIG);
        this.showTrackHeadersIsDisabled = projectContext.collapsedTrackHeaders !== undefined && projectContext.collapsedTrackHeaders;
        this.settingsService = ngbMainSettingsDlgService;
        this.customizeSettings = this.settingsService.getSettings();
        this.userDataService = userDataService;
        this.moment = moment;

        this.tokenValidDate = this.moment().add(1, 'month').toDate();

        utilsDataService.isRoleModelEnabled().then(res => {
            this.isRoleModelEnabled = res;
            if (this.isRoleModelEnabled) {
                userDataService.currentUserIsAdmin().then(isAdmin => {
                    this.userIsAdmin = isAdmin;
                });
            }
        });

        this.prepareDefaultHighlightProfile();

        this.scope = $scope;
    }

    close() {
        this._mdDialog.hide();
    }

    save() {
        this.settings = this.makeConsistent(this.settings);
        this.settingsService.updateThisLocalSettingsVar(this.settings);
        this._localDataService.updateSettings(this.settings);
        this._dispatcher.emitGlobalEvent('settings:change', this.settings);
        this.close();
    }

    generateAccessKey() {
        this.isTokenLoading = true;
        this.accessToken = '';

        const validTill = this.moment(this.tokenValidDate).add(1, 'days').subtract(1, 'seconds');
        this.userDataService.getJwtToken(validTill.diff(this.moment(), 'seconds')).then(token => {
            this.accessToken = token;
            this.isTokenLoading = false;
            if (this.scope) {
                this.scope.$apply();
            }
        });
    }

    getterSetterMaximumBPForBam(input) {
        return arguments.length ? this.settings.maxBAMBP = input : this.settings.maxBAMBP;
    }

    setToDefaultCustomizations() {
        this.customizeSettings = this.settingsService.getDefaultSettings();
        this.scope.$broadcast('setToDefault');
    }

    makeConsistent(settings) {
        settings.highlightProfile = this.settings.isVariantsHighlighted
            ? settings.highlightProfile
            : undefined;
        return settings;
    }

    prepareDefaultHighlightProfile() {
        if (this.settings.isVariantsHighlighted) {
            this.settings.highlightProfile = this.settings.highlightProfile
                || Object.keys(this.settings.highlightProfileList).filter(
                    name => this.settings.highlightProfileList[name].is_default
                )[0];
        }
    }
}
