import angular from 'angular';
import baseController from '../../../shared/baseController';
import ngbPathwaysAnnotationAddDlgController from '../ngbPathwaysAnnotationDlg/ngbPathwaysAnnotationAddDlg.controller';

export default class ngbInternalPathwaysResultController extends baseController {
    selectedTree = null;
    treeSearchParams = {
        search: null
    };
    treeSearch = null;
    loading = true;
    treeError = false;
    annotationsPanelVisible = false;

    events = {
        'layout:active:panel:change': this.activePanelChanged.bind(this),
        'reference:change': this.initialize.bind(this),
        'reference:show:pathway': this.initialize.bind(this),
        'pathways:internalPathways:annotations:change': this.refreshAnnotationList.bind(this)
    };

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbInternalPathwaysResultService,
        ngbPathwaysService,
        ngbPathwaysAnnotationService,
        appLayout,
        $mdDialog
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
                ngbPathwaysAnnotationService,
                appLayout,
                $mdDialog
            }
        );
        this.annotationTypeList = this.ngbPathwaysAnnotationService.annotationTypeList;
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
            this.ngbInternalPathwaysResultService.selectedElementId = null;
            this.selectedTree = data;
        }
        this.refreshAnnotationList();
        this.loading = false;
        this.$timeout(() => this.$scope.$apply());
    }

    searchInTree() {
        this.treeSearchParams = {
            ...this.treeSearchParams,
            search: this.treeSearch
        };
    }

    toggleAnnotationsPanel() {
        if (
            !this.annotationsPanelVisible &&
            (!this.annotationList || this.annotationList.length === 0)) {
            this.addAnnotation();
        } else {
            this.annotationsPanelVisible = !this.annotationsPanelVisible;
        }
    }

    applyAnnotations() {
        this.treeSearchParams = {
            ...this.treeSearchParams,
            annotations: this.annotationList.filter(a => a.isActive)
        };
        this.ngbPathwaysAnnotationService.saveAnnotationList(this.annotationList);
    }

    addAnnotation() {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbPathwaysAnnotationAddDlgController,
            controllerAs: '$ctrl',
            parent: angular.element(document.body),
            template: require('../ngbPathwaysAnnotationDlg/ngbPathwaysAnnotationAddDlg.tpl.html'),
            locals: {
                annotation: null,
                pathwayId: this.ngbPathwaysService.currentInternalPathway.id
            }
        });
    }

    editAnnotation(id) {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbPathwaysAnnotationAddDlgController,
            controllerAs: '$ctrl',
            parent: angular.element(document.body),
            template: require('../ngbPathwaysAnnotationDlg/ngbPathwaysAnnotationAddDlg.tpl.html'),
            locals: {
                annotation: this.ngbPathwaysAnnotationService.getAnnotationById(id),
                pathwayId: this.ngbPathwaysService.currentInternalPathway.id
            }
        });
    }

    deleteAnnotation(id) {
        this.ngbPathwaysAnnotationService.deleteAnnotationById(id);
        if (!this.annotationList || this.annotationList.length === 0) {
            this.annotationsPanelVisible = false;
            this.treeSearchParams = {
                ...this.treeSearchParams,
                annotations: []
            };
        }
    }

    refreshAnnotationList() {
        if (this.ngbPathwaysService.currentInternalPathway) {
            this.annotationList = this.ngbPathwaysAnnotationService.getPathwayAnnotationList(this.ngbPathwaysService.currentInternalPathway.id);
            if (this.annotationList && this.annotationList.length !== 0) {
                this.applyAnnotations();
                this.annotationsPanelVisible = true;
            }
        }
    }

    activePanelChanged(o) {
        const isActive = o === this.appLayout.Panels.pathways.panel;
        this.dispatcher.emit('cytoscape:panel:active', isActive);
    }

    onElementClick(data) {
        const {id} = data || {};
        this.ngbInternalPathwaysResultService.selectedElementId = id;
        this.elementDescription = data;
        this.dispatcher.emitSimpleEvent('cytoscape:pathways:selection:change', data);
        this.$timeout(() => this.$scope.$apply());
    }

}
