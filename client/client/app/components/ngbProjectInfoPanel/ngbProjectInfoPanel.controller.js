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

    get projectInfoModeList () {
        return this.ngbProjectInfoService.projectInfoModeList;
    }

    get currentMode () {
        return this.ngbProjectInfoService.currentMode;
    }

    get descriptionMode () {
        if (this.ngbProjectInfoService.extendedMode) {
            return this.currentMode[0];
        } else {
            this.currentMode;
        }
    }

    get isProgressShown () {
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

    editNote($event) {
        this.ngbProjectInfoService.editNote(this.currentNote.id);
        $event.stopPropagation();
        $event.preventDefault();
        return false;
    }

    refreshProjectInfo() {
        this.$scope.$apply();
    }

    get containsVcfFiles() {
        return this.projectContext.containsVcfFiles && !this.projectContext.variantsGroupError;
    }
}
