import angular from 'angular';
import baseController from '../../../shared/baseController';
import ngbPathwaysAnnotationAddDlgController from '../ngbPathwaysAnnotation/ngbPathwaysAnnotationAddDlg.controller';

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
        'pathways:internalPathways:annotations:change': this.refreshAnnotationList.bind(this)
    };

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbInternalPathwaysResultService,
        ngbPathwaysService,
        appLayout,
        projectContext,
        localDataService,
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
                appLayout,
                projectContext,
                localDataService,
                $mdDialog
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

    applyAnnotations() {
        this.treeSearchParams = {
            ...this.treeSearchParams,
            annotations: this.annotationList.filter(a => a.isActive)
        };
        this.ngbPathwaysService.saveAnnotationList(this.annotationList);
    }

    // FIXME: separate annotation component
    addAnnotation() {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbPathwaysAnnotationAddDlgController,
            controllerAs: '$ctrl',
            parent: angular.element(document.body),
            template: require('../ngbPathwaysAnnotation/ngbPathwaysAnnotationAddDlg.tpl.html'),
            locals: {
                annotation: null
            }
        });
    }

    editAnnotation(id) {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbPathwaysAnnotationAddDlgController,
            controllerAs: '$ctrl',
            parent: angular.element(document.body),
            template: require('../ngbPathwaysAnnotation/ngbPathwaysAnnotationAddDlg.tpl.html'),
            locals: {
                annotation: this.ngbPathwaysService.getAnnotationById(id)
            }
        });
    }

    deleteAnnotation(id) {
        this.ngbPathwaysService.deleteAnnotationById(id);
    }

    refreshAnnotationList() {
        this.annotationList = this.ngbPathwaysService.getAnnotationList();
        this.applyAnnotations();
    }

    activePanelChanged(o) {
        const isActive = o === this.appLayout.Panels.pathways.panel;
        this.dispatcher.emit('cytoscape:panel:active', isActive);
    }

}
