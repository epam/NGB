const TARGET_TAB = {
    TARGETS: 'TARGETS',
    IDENTIFICATIONS: 'IDENTIFICATIONS',
    DISEASES: 'DISEASES'
};

const DOWNLOAD_OPTIONS = {
    EXCEL: 'Excel',
    HTML: 'HTML',
};
export default class ngbTargetPanelController {

    get targetTab() {
        return TARGET_TAB;
    }

    get downloadOptions () {
        return DOWNLOAD_OPTIONS;
    }

    tabSelected;
    reportLoading = false;
    _nameModel;
    _saveStatus;
    saveLoading = false;
    saveFailed = false;
    errorMessageList = null;
    downloadModel;

    get identificationTabIsShown() {
        const {identificationTarget, identificationData} = this.ngbTargetPanelService;
        return identificationTarget && identificationData;
    }

    get nameModel () {
        return this._nameModel;
    }
    set nameModel(value) {
        this._nameModel = value;
    }
    get saveStatus() {
        return this.ngbTargetPanelService.saveStatus;
    }
    set saveStatus(value) {
        this.ngbTargetPanelService.saveStatus = value;
    }
    get statusOptions() {
        return this.ngbTargetPanelService.statusOptions;
    }

    static get UID() {
        return 'ngbTargetPanelController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbTargetPanelService,
        ngbTargetsTabService,
        ngbDiseasesTabService,
        targetContext,
        $mdDialog,
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbTargetPanelService,
            ngbTargetsTabService,
            ngbDiseasesTabService,
            targetContext,
            $mdDialog,
        });
        this.tabSelected = this.targetTab.TARGETS;
        dispatcher.on('target:show:identification:tab', this.showIdentificationTab.bind(this));
        dispatcher.on('homologs:create:target', this.createTargetFromHomologs.bind(this));
        dispatcher.on('target:identification:show:diseases:tab', this.showDiseasesTab.bind(this));
        dispatcher.on('target:diseases:show:targets:tab', this.showTargetsTab.bind(this));
        dispatcher.on('load:target', this.changeState.bind(this));
        this.changeState(this.targetContext.currentState);
    }

    get isTableMode() {
        return this.ngbTargetsTabService &&
            this.ngbTargetsTabService.isTableMode &&
            this.tabSelected === this.targetTab.TARGETS;
    }

    get isIdentificationsMode() {
        return this.tabSelected === this.targetTab.IDENTIFICATIONS;
    }

    get format() {
        return this.ngbTargetPanelService.format;
    }

    addTarget () {
        if (this.ngbTargetsTabService) {
            this.ngbTargetsTabService.setAddMode();
        }
    }

    onChangeDownload() {
        const {EXCEL, HTML} = this.downloadOptions;
        if (this.downloadModel === EXCEL) {
            this.downloadModel = undefined;
            this.donwloadReport(this.format.XLSX);
        }
        if (this.downloadModel === HTML) {
            this.downloadModel = undefined;
            this.donwloadReport(this.format.HTML);
        }
    }

    donwloadReport(format) {
        this.reportLoading = true;
        this.ngbTargetPanelService.exportResults(format)
            .then(data => {
                const linkElement = document.createElement('a');
                try {
                    const blob = new Blob([data], {type: `application/${format}`});
                    const url = window.URL.createObjectURL(blob);
                    const geneChips = this.ngbTargetPanelService.allChips;

                    linkElement.setAttribute('href', url);
                    linkElement.setAttribute('download',
                        `${geneChips.join('_')}-report.${format}`);

                    const clickEvent = new MouseEvent('click', {
                        'view': window,
                        'bubbles': true,
                        'cancelable': false
                    });
                    linkElement.dispatchEvent(clickEvent);
                    this.reportLoading = false;
                    this.$timeout(() => this.$scope.$apply());
                } catch (ex) {
                    // eslint-disable-next-line no-console
                    this.reportLoading = false;
                    console.error(ex);
                    this.$timeout(() => this.$scope.$apply());
                }
            });
    }

    changeTab(tab) {
        if (this.targetTab.hasOwnProperty(tab)) {
            this.tabSelected = this.targetTab[tab];
            this.targetContext.setCurrentTab(this.targetTab[tab]);
        }
        this.$timeout(() => this.$scope.$apply());
    }

    async changeState(state) {
        await this.changeIdentificationState(state);
        await this.changeDiseaseState(state);
        this.changeTabState(state);
        this.$timeout(() => this.$scope.$apply());
    }

    changeTabState(state) {
        const {tab} = state;
        if (!tab) return;
        this.changeTab(tab);
    }

    async changeIdentificationState(state) {
        const {targetId, targetName, genesOfInterest, translationalGenes} = state || {};
        if (!targetId || !genesOfInterest || !translationalGenes) return;
        const target = {
            id: targetId,
            name: targetName,
        };
        const params = {
            targetId: target.id,
            genesOfInterest: genesOfInterest.map(s => s.geneId),
            translationalGenes: translationalGenes.map(s => s.geneId)
        };
        const info = {
            target: target,
            interest: genesOfInterest,
            translational: translationalGenes
        };
        await this.ngbTargetsTabService.getIdentificationData(params, info);
    }

    async changeDiseaseState(state) {
        const {diseaseId: id, diseaseName: name} = state || {};
        if (!id || !name) return;
        const disease = { id, name };
        await this.ngbDiseasesTabService.viewDiseaseFromTable(disease);
    }

    showIdentificationTab() {
        this.changeTab(this.targetTab.IDENTIFICATIONS)
    }

    createTargetFromHomologs() {
        this.changeTab(this.targetTab.TARGETS);
    }

    showDiseasesTab(disease) {
        this.changeTab(this.targetTab.DISEASES)
        this.$timeout(() => {
            this.ngbDiseasesTabService.viewDiseaseFromTable(disease);
        });
    }

    showTargetsTab() {
        this.changeTab(this.targetTab.TARGETS)
    }

    async saveIdentification(name) {
        this.saveLoading = true;
        await this.ngbTargetPanelService.saveIdentification(name)
            .then(data => {
                if (data) {
                    this.saveStatus = this.statusOptions.SAVED;
                }
                this.saveFailed = false;
                this.errorMessageList = null;
                this.saveLoading = false;
            })
            .catch(error => {
                this.saveFailed = true;
                this.errorMessageList = [error.message];
                this.saveLoading = false;
            });
        this.$timeout(() => this.$scope.$apply());
    }

    onClickSave() {
        const self = this.$scope.$ctrl;
        this.$mdDialog.show({
            template: require('./ngbTargetSavingDialog.tpl.html'),
            controller: function($scope, $mdDialog) {
                $scope.nameModel;
                $scope.save = function () {
                    self.saveIdentification($scope.nameModel);
                    $mdDialog.hide();
                    $mdDialog.hide();
                };
                $scope.cancel = function () {
                    $mdDialog.hide();
                };
            },
            preserveScope: true,
            autoWrap: true,
            skipHide: true,
        });
    }
}
