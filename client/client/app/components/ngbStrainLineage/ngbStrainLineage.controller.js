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
            const {allData, referenceData, error} = await this.ngbStrainLineageService.loadStrainLineages(currentReference);
            if (!error) {
                this.error = false;
                if (!this.selectedTreeId) {
                    this.selectedTreeId = this.ngbStrainLineageService.selectedTreeId;
                    await this.onTreeSelect();
                }
            } else {
                this.error = error;
            }
            this.lineageTreeList = this.constructTreeList(referenceData, allData);
            this.loading = false;
        } else {
            const {allData, referenceData} = await this.ngbStrainLineageService.loadStrainLineages(null);
            this.lineageTreeList = this.constructTreeList(referenceData, allData);
            if (!this.selectedTreeId) {
                this.selectedTreeId = this.ngbStrainLineageService.selectedTreeId;
                await this.onTreeSelect();
            }
            if (this.projectContext.references.length) {
                this.loading = false;
            }
        }
        this.$timeout(() => this.$scope.$apply());
    }

    constructTreeList(referenceData, allData) {
        let result = [];
        if (referenceData.length) {
            result = referenceData;
        }
        if (result.length && allData.length) {
            result = result.concat([{divider: true}]);
        }
        if (allData.length) {
            result = result.concat(allData);
        }
        return result;
    }

    activePanelChanged(o) {
        const isActive = o === this.appLayout.Panels.strainLineage.panel;
        this.dispatcher.emit('cytoscape:panel:active', isActive);
    }

    async onTreeSelect() {
        this.treeLoading = true;
        if (this.ngbStrainLineageService.selectedTreeId !== this.selectedTreeId) {
            this.ngbStrainLineageService.selectedElementId = null;
            this.resetDataTooltip();
        }
        this.ngbStrainLineageService.selectedTreeId = this.selectedTreeId;
        const {tree, error} = await this.ngbStrainLineageService.getLineageTreeById(this.selectedTreeId);
        if (error) {
            this.treeError = error;
        } else {
            this.treeError = false;
            this.selectedTree = this.setDatasetNames(tree);
        }
        this.treeLoading = false;
        this.$timeout(() => this.$scope.$apply());
    }

    onElementClick(data) {
        const {id} = data || {};
        this.ngbStrainLineageService.selectedElementId = id;
        this.elementDescription = data;
        this.dispatcher.emitSimpleEvent('cytoscape:selection:change', data);
        this.$timeout(() => this.$scope.$apply());
    }

    resetDataTooltip() {
        this.onElementClick(undefined);
    }

    setDatasetNames(tree) {
        const datasets = this.projectContext.datasets || [];
        tree.nodes.forEach(node => {
            if (node.data.projectId && node.data.referenceId) {
                const [dataset] = datasets.filter(d => d.id === node.data.projectId);
                if (dataset) {
                    node.data.projectName = dataset.name;
                }
            }
        });
        return tree;
    }

    navigate(event, element) {
        if (element.referenceId) {
            this.navigateToReference(event, element.referenceId);
        } else if (element.projectId) {
            this.navigateToDataset(event, element.projectId);
        }
    }

    navigateToReference(event, referenceId) {
        if (!referenceId || !this.projectContext || !this.projectContext.references || !this.projectContext.references.length) {
            return;
        }
        const referenceObj = this.projectContext.references.filter(reference => reference.id === referenceId).pop();
        const payload = this.ngbStrainLineageService.getOpenReferencePayload(this.projectContext, referenceObj);
        if (payload) {
            this.projectContext.changeState(payload);
        }
    }

    navigateToDataset(event, projectId) {
        if (!projectId || !this.projectContext || !this.projectContext.datasets || !this.projectContext.datasets.length) {
            return;
        }
        const payload = this.ngbStrainLineageService.getOpenDatasetPayload(this.projectContext.datasets, projectId);
        if (payload) {
            this.projectContext.changeState(payload);
        }
    }

}
