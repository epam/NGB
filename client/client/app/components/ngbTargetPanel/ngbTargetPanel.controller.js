const TARGET_TAB = {
    TARGETS: 'TARGETS',
    IDENTIFICATIONS: 'IDENTIFICATIONS',
    DISEASES: 'DISEASES'
};

export default class ngbTargetPanelController {

    get targetTab() {
        return TARGET_TAB;
    }

    tabSelected;
    reportLoading = false;

    get identificationTabIsShown() {
        const {identificationTarget, identificationData} = this.ngbTargetPanelService;
        return identificationTarget && identificationData;
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
        targetContext
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbTargetPanelService,
            ngbTargetsTabService,
            ngbDiseasesTabService,
            targetContext
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

    addTarget () {
        if (this.ngbTargetsTabService) {
            this.ngbTargetsTabService.setAddMode();
        }
    }

    downloadReport () {
        this.reportLoading = true;
        this.ngbTargetPanelService.exportResults()
            .then(data => {
                const linkElement = document.createElement('a');
                try {
                    const blob = new Blob([data], {type: 'application/xls'});
                    const url = window.URL.createObjectURL(blob);
                    const geneChips = this.ngbTargetPanelService.allChips;

                    linkElement.setAttribute('href', url);
                    linkElement.setAttribute('download',
                        `${geneChips.join('_')}-report.xls`);

                    const clickEvent = new MouseEvent('click', {
                        'view': window,
                        'bubbles': true,
                        'cancelable': false
                    });
                    linkElement.dispatchEvent(clickEvent);
                    this.reportLoading = false;
                } catch (ex) {
                    // eslint-disable-next-line no-console
                    this.reportLoading = false;
                    console.error(ex);
                }
            });
    }

    changeTab(tab) {
        if (this.targetTab.hasOwnProperty(tab)) {
            this.tabSelected = this.targetTab[tab];
        }
        this.$timeout(() => this.$scope.$apply());
    }

    async changeState(state) {
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
        this.$timeout(() => this.$scope.$apply());
    }

    showIdentificationTab() {
        this.tabSelected = this.targetTab.IDENTIFICATIONS;
        this.$timeout(() => this.$scope.$apply());
    }

    createTargetFromHomologs() {
        this.changeTab(this.targetTab.TARGETS);
    }

    showDiseasesTab(disease) {
        this.tabSelected = this.targetTab.DISEASES;
        this.$timeout(() => {
            this.$scope.$apply();
            this.ngbDiseasesTabService.viewDiseaseFromTable(disease);
        });
    }

    showTargetsTab() {
        this.tabSelected = this.targetTab.TARGETS;
        this.$timeout(() => this.$scope.$apply());
    }
}
