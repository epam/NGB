import baseController from '../../../shared/baseController';

export default class ngbInternalPathwaysResultController extends baseController {
    selectedTree = null;
    treeSearchParams = {
        search: null
    };
    treeSearch = null;
    loading = true;
    treeError = false;

    events = {
        'layout:active:panel:change': this.activePanelChanged.bind(this),
        'reference:change': this.initialize.bind(this),
        'reference:show:pathway': this.initialize.bind(this),
    };

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbInternalPathwaysResultService,
        ngbPathwaysService,
        appLayout,
        projectContext,
        localDataService
    ) {
        super();

        Object.assign(
            this,
            {
                $scope,
                $timeout,
                dispatcher,
                ngbInternalPathwaysResultService,
                ngbPathwaysService,
                appLayout,
                projectContext,
                localDataService
            }
        );

        this.initialize();
        this.initEvents();
    }

    static get UID() {
        return 'ngbInternalPathwaysResultController';
    }

    async initialize() {
        if (!this.ngbPathwaysService.currentInternalPathway) {
            return;
        }
        const {data, error} = await this.ngbInternalPathwaysResultService.getPathwayTree(this.ngbPathwaysService.currentInternalPathway);
        if (error) {
            this.treeError = error;
        } else {
            this.treeError = false;
            this.selectedTree = data;
        }
        this.loading = false;
        this.$timeout(() => this.$scope.$apply());
    }

    searchInTree() {
        this.treeSearchParams = {
            search: this.treeSearch
        };
    }

    activePanelChanged(o) {
        const isActive = o === this.appLayout.Panels.pathways.panel;
        this.dispatcher.emit('cytoscape:panel:active', isActive);
    }

}
