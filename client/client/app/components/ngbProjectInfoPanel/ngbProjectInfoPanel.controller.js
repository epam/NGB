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
        const currentMode = this.ngbProjectInfoService.currentMode
        if (this.extendedMode && this.ngbProjectInfoService.projects.length) {
            return Array.isArray(currentMode) ? currentMode[0] : currentMode;
        } else {
            return currentMode;
        }
    }

    get extendedMode () {
        return this.ngbProjectInfoService.extendedMode;
    }

    get currentProject () {
        return this.ngbProjectInfoService.currentProject;
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

    editNote($event) {
        this.ngbProjectInfoService.editNote(this.currentNote.id);
        $event.stopPropagation();
        $event.preventDefault();
        return false;
    }

    refreshProjectInfo() {
        if (!this.$scope.$$phase) {
            this.$scope.$apply();
        }
    }

    get containsVcfFiles() {
        return this.projectContext.containsVcfFiles && !this.projectContext.variantsGroupError;
    }
}
