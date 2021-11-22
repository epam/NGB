import baseController from '../../shared/baseController';

export default class ngbStrainLineageController extends baseController {
    selectedTree = null;
    selectedTreeId = null;
    lineageTreeList = [];
    loading = true;
    treeLoading = false;
    error = false;
    treeError = false;
    elementDescription = null;

    events = {
        'layout:active:panel:change': this.activePanelChanged.bind(this),
        'reference:change': this.initialize.bind(this),
        'reference:show:lineage': this.initialize.bind(this)
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
            if (!this.selectedTreeId) {
                this.loading = true;
            }
            const {data, error} = await this.ngbStrainLineageService.loadStrainLineages(currentReference);
            if (!error) {
                this.error = false;
                this.lineageTreeList = data;
                if (!this.selectedTreeId) {
                    this.selectedTreeId = this.ngbStrainLineageService.selectedTreeId;
                    await this.onTreeSelect();
                }
            } else {
                this.error = error;
            }
            this.loading = false;
        } else {
            if (this.lineageTreeList.length && this.selectedTreeId) {
                this.ngbStrainLineageService.setCurrentStrainLineageAsList();
                this.lineageTreeList = this.ngbStrainLineageService.getCurrentStrainLineageAsList();
            }
            if (this.projectContext.references.length) {
                this.loading = false;
            }
        }
        this.$timeout(() => this.$scope.$apply());
    }

    activePanelChanged(o) {
        const isActive = o === this.appLayout.Panels.strainLineage.panel;
        this.dispatcher.emit('cytoscape:panel:active', isActive);
    }

    async onTreeSelect() {
        this.treeLoading = true;
        if (this.ngbStrainLineageService.selectedTreeId !== this.selectedTreeId) {
            this.resetDataTooltip();
        }
        this.ngbStrainLineageService.selectedTreeId = this.selectedTreeId;
        const {tree, error} = await this.ngbStrainLineageService.getLineageTreeById(this.selectedTreeId);
        if (error) {
            this.treeError = error;
        } else {
            this.treeError = false;
            this.selectedTree = tree;
        }
        this.treeLoading = false;
        this.$timeout(() => this.$scope.$apply());
    }

    onElementClick(data) {
        this.elementDescription = data;
        this.$timeout(() => this.$scope.$apply());
    }

    resetDataTooltip() {
        this.onElementClick(undefined);
    }
}
