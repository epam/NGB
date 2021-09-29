import BaseController from '../../shared/baseController';

export default class ngbProjectInfoPanelController extends BaseController {
    /**
     * @returns {string}
     */
    static get UID() {
        return 'ngbProjectInfoPanelController';
    }

    projectContext;
    newNote;
    events = {
        'project:description:url': this.refreshProjectInfo.bind(this),
    };

    /**
     * @constructor
     */
    /** @ngInject */
    constructor(projectContext, $scope, $element, $timeout, dispatcher, ngbProjectInfoService) {
        super();
        Object.assign(this, {
            projectContext, $scope, $element, $timeout, dispatcher, ngbProjectInfoService
        });
        this.initEvents();
    }

    get projectInfoModeList() {
        return this.ngbProjectInfoService.projectInfoModeList;
    }

    get currentMode() {
        return this.ngbProjectInfoService.currentMode;
    }

    get isProgressShown() {
        return this.ngbProjectInfoService.descriptionIsLoading;
    }

    get currentNote() {
        return this.ngbProjectInfoService.currentNote;
    }

    get editingNote() {
        return this.ngbProjectInfoService.editingNote;
    }

    get newNote() {
        return this.ngbProjectInfoService.newNote;
    }

    get canEdit() {
        return this.ngbProjectInfoService.canEdit;
    }

    get isEdit() {
        return this.ngbProjectInfoService.isEdit;
    }

    get heatmapId() {
        return this.heatmap ? this.heatmap.id : undefined;
    }

    get heatmapProjectId() {
        return this.heatmap && this.heatmap.project ? this.heatmap.project.id : undefined;
    }

    editNote($event) {
        this.ngbProjectInfoService.editNote(this.currentNote.id);
        $event.stopPropagation();
        $event.preventDefault();
        return false;
    }

    refreshProjectInfo() {
        if (!this.$scope.$$phase) {
            this.$scope.$apply();
            this.dispatcher.emitSimpleEvent('refresh:project:info');
        }
    }

    get containsVcfFiles() {
        return this.projectContext.containsVcfFiles && !this.projectContext.variantsGroupError;
    }
}
