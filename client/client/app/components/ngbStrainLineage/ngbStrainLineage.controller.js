import baseController from '../../shared/baseController';
import CytoscapeContext from '../../shared/cytoscapeContext';

export default class ngbStrainLineageController extends baseController {
    cytoscapeContext: CytoscapeContext = undefined;

    selectedTree = null;
    currentTreeId = null;
    lineageTreeList = [];
    loading = true;
    elementDescription = null;

    events = {
        'layout:active:panel:change': this.activePanelChanged.bind(this),
        'reference:change': this.initialize.bind(this)
    };

    constructor(
        $scope,
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
        if (this.projectContext.reference) {
            this.lineageTreeList = await this.ngbStrainLineageService.loadStrainLineages(this.projectContext.reference.id);
            this.currentTreeId = this.ngbStrainLineageService.currentTreeId;
            this.loading = false;
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
        this.$scope.$apply();
    }
}
