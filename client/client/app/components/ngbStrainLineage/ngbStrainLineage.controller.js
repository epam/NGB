import baseController from '../../shared/baseController';

export default class ngbStrainLineageController extends baseController {
    selectedTree = null;
    currentTreeId = null;
    lineageTreeList = [];
    loading = true;
    elementDescription = null;

    events = {
        'layout:active:panel:change': this.activePanelChanged.bind(this),
        'reference:change': this.initialize.bind(this),
        'read:show:lineage': this.initialize.bind(this)
    };

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbStrainLineageService,
        appLayout,
        projectContext
    ) {
        super();

        Object.assign(
            this,
            {
                $scope,
                $timeout,
                dispatcher,
                ngbStrainLineageService,
                appLayout,
                projectContext
            }
        );

        this.initialize();
        this.initEvents();
    }

    static get UID() {
        return 'ngbStrainLineageController';
    }

    async initialize() {
        const currentReference = this.ngbStrainLineageService.currentReferenceId
            || (this.projectContext.reference ? this.projectContext.reference.id : null);
        if (currentReference) {
            this.lineageTreeList = await this.ngbStrainLineageService.loadStrainLineages(currentReference);
            this.currentTreeId = this.ngbStrainLineageService.currentTreeId;
            this.onTreeSelect();
            this.loading = false;
            this.$timeout(() => this.$scope.$apply());
        }
    }

    activePanelChanged(o) {
        const isActive = o === this.appLayout.Panels.strainLineage.panel;
        this.dispatcher.emit('cytoscape:panel:active', isActive);
    }

    onTreeSelect() {
        this.ngbStrainLineageService.currentTreeId = this.currentTreeId;
        this.selectedTree = this.ngbStrainLineageService.getLineageTreeById(this.currentTreeId);
    }

    onElementClick(data) {
        this.elementDescription = data;
        this.$timeout(() => this.$scope.$apply());
    }
}
