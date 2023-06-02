import BaseController from '../../shared/baseController';

export default class ngbProjectInfoPanelController extends BaseController {
    /**
     * @returns {string}
     */
    static get UID() {
        return 'ngbProjectInfoPanelController';
    }

    events = {
        'project:description:url': this.refreshProjectInfo.bind(this),
    };

    /**
     * @constructor
     */
    /** @ngInject */
    constructor($scope, $element, $timeout, dispatcher, projectContext, ngbProjectInfoService) {
        super();
        Object.assign(this, {
            $scope, $element, $timeout, dispatcher, projectContext, ngbProjectInfoService
        });
        this.initEvents();
    }

    get mode() {
        return this.ngbProjectInfoService.mode;
    }
    get currentMode() {
        return this.ngbProjectInfoService.currentMode;
    }
    get isProgressShown() {
        return this.ngbProjectInfoService.descriptionIsLoading;
    }
    get blobUrl() {
        return this.ngbProjectInfoService.blobUrl;
    }
    get currentNote() {
        return this.ngbProjectInfoService.currentNote;
    }
    get editableNote() {
        return this.ngbProjectInfoService.editableNote;
    }
    get newNote() {
        return this.ngbProjectInfoService.newNote;
    }
    get isAdditionMode() {
        return this.ngbProjectInfoService.isAdditionMode;
    }
    get isEditingAvailable() {
        return this.ngbProjectInfoService.isEditingAvailable;
    }
    get inEditing() {
        return this.ngbProjectInfoService.inEditing;
    }

    get heatmapId() {
        return this.heatmap ? this.heatmap.id : undefined;
    }
    get heatmapProjectId() {
        return this.heatmap && this.heatmap.project ? this.heatmap.project.id : undefined;
    }
    get referenceId() {
        return this.projectContext && this.projectContext.reference
            ? this.projectContext.reference.id
            : undefined;
    }

    get containsVcfFiles() {
        return this.projectContext.containsVcfFiles && !this.projectContext.variantsGroupError;
    }

    handleClickEditNote($event) {
        this.ngbProjectInfoService.editNote(this.currentNote.id);
        $event.stopPropagation();
        $event.preventDefault();
        return false;
    }

    refreshProjectInfo() {
        this.$timeout(() => this.$scope.$apply());
        this.dispatcher.emitSimpleEvent('refresh:project:info');
    }
}
