import buildMainInfoBlocks from './utilities/build-main-info-blocks';

export default class ngbIdentificationsTabController {

    static get UID() {
        return 'ngbIdentificationsTabController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbIdentificationsTabService,
        ngbBibliographyPanelService,
        ngbTargetPanelService
    ) {
        Object.assign(
            this,
            {
                $scope,
                $timeout,
                dispatcher,
                ngbIdentificationsTabService,
                ngbBibliographyPanelService,
                ngbTargetPanelService
            });
        this.dispatcher.on('target:identification:changed', this.identificationChanged.bind(this));
        this.dispatcher.on('target:identification:publications:loading', this.refreshInfoBlocks.bind(this));
        this.dispatcher.on('target:identification:publications:loaded', this.refreshInfoBlocks.bind(this));
        this._identificationTarget = undefined;
        this._interest = [];
        this._translational = [];
        this._mainInfoBlocks = [];
    }

    get descriptionCollapsed() {
        return !this.openedPanels.description;
    }

    get openedPanels () {
        return this.ngbIdentificationsTabService.openedPanels || {};
    }
    set openedPanels(value) {
        this.ngbIdentificationsTabService.openedPanels = value;
    }

    get identificationData () {
        return this.ngbTargetPanelService.identificationData;
    }

    get identificationTarget () {
        return this._identificationTarget;
    }

    get targetName() {
        const target = this.identificationTarget;
        return target && target.target.name;
    }

    get interest() {
        return this._interest;
    }

    get translational() {
        return this._translational;
    }

    get descriptions() {
        return this.ngbTargetPanelService.descriptions;
    }

    get mainInfo () {
        return this._mainInfoBlocks;
    }

    $onInit() {
        this.updateData(this.ngbTargetPanelService.identificationTarget);
    }

    identificationChanged(identificationTarget) {
        this.ngbIdentificationsTabService.closeAll();
        this.updateData(identificationTarget);
    }

    updateData(identificationTarget) {
        this._identificationTarget = identificationTarget;
        if (this._identificationTarget) {
            this._interest = (this._identificationTarget.interest || []).map((o) => o.chip);
            this._translational = (this._identificationTarget.translational || []).map((o) => o.chip);
        } else {
            this._interest = [];
            this._translational = [];
        }
        this.refreshInfoBlocks();
    }

    refreshInfoBlocks() {
        this._mainInfoBlocks = buildMainInfoBlocks(
            this.identificationData,
            this.ngbBibliographyPanelService
        );
    }

    toggleDescriptionCollapsed () {
        this.openedPanels.description = !this.openedPanels.description;
    }
}
