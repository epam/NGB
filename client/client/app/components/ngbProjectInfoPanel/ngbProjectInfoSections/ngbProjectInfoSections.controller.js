import baseController from '../../../shared/baseController';

export default class ngbProjectInfoSectionsController extends baseController {
    static get UID() {
        return 'ngbProjectInfoSectionsController';
    }

    constructor($scope, dispatcher, projectContext, ngbProjectInfoService) {
        super(dispatcher);
        Object.assign(this, {$scope, dispatcher, projectContext, ngbProjectInfoService});
    }

    handleOpenMenu($mdOpenMenu, $event) {
        if (this.projectContext.currentChromosome) {
            // switch from track view
            this.modeModel = [this.mode.SUMMARY, null];
            this.projectContext.changeState({chromosome: null});
        } else {
            this.openMenu($mdOpenMenu, $event);
        }
    }

    openMenu($mdOpenMenu, $event) {
        $event.stopPropagation();
        $mdOpenMenu($event);
    }

    get currentName() {
        return this.ngbProjectInfoService.currentName;
    }

    get isSingleProject() {
        return  this.ngbProjectInfoService.isSingleProject;
    }
    get isMultipleProjects() {
        return  this.ngbProjectInfoService.isMultipleProjects;
    }

    get plainItems() {
        return this.ngbProjectInfoService.plainItems;
    }

    get currentProject() {
        return this.ngbProjectInfoService.currentProject;
    }

    get modeModel() {
        return this.ngbProjectInfoService.modeModel;
    }
    set modeModel(value) {
        this.ngbProjectInfoService.modeModel = value;
    }

    get defaultModeModel() {
        return this.ngbProjectInfoService.defaultModeModel;
    }

    get mode() {
        return this.ngbProjectInfoService.mode;
    }
    get currentMode() {
        return this.ngbProjectInfoService.currentMode;
    }
    get currentId() {
        return this.ngbProjectInfoService.currentId;
    }

    get isAdditionMode() {
        return this.ngbProjectInfoService.isAdditionMode;
    }

    get summaryFalseValue() {
        return this.isSingleProject ? [...this.defaultModeModel] : [this.mode.SUMMARY, null];
    }
    get descriptionFalseValue() {
        if (this.isSingleProject &&
            this.defaultModeModel[0] === this.currentMode &&
            this.defaultModeModel[1] === this.currentId
        ) {
            return [this.mode.SUMMARY, null];
        } else {
            return [...this.defaultModeModel];
        }
    }
    get noteFalseValue() {
        return [...this.defaultModeModel];
    }

    hasChanges() {
        return this.ngbProjectInfoService.hasChanges();
    }

    showUnsavedChangesDialog() {
        this.ngbProjectInfoService.showUnsavedChangesDialog();
    }

    handleChangeSummary($event) {
        if (this.currentMode === this.mode.SUMMARY) {
            this.ngbProjectInfoService.setFalseMode(this.summaryFalseValue);
            return;
        }
        if (this.hasChanges()) {
            this.showUnsavedChangesDialog();
            $event.stopImmediatePropagation();
        } else {
            this.ngbProjectInfoService.setSummary();
        }
    }

    handleChangeDescription ($event, project, descriptionId) {
        if (this.currentMode === this.mode.DESCRIPTION &&
            this.currentProject &&
            this.currentProject.id === project.id &&
            this.currentId === descriptionId
        ) {
            this.ngbProjectInfoService.setFalseMode(this.descriptionFalseValue);
            return;
        }
        if (this.hasChanges()) {
            this.showUnsavedChangesDialog();
            $event.stopImmediatePropagation();
        } else {
            this.ngbProjectInfoService.setDescription(descriptionId, project);
        }
    }

    handleChangeNote ($event, project, noteId) {
        if (this.currentMode === this.mode.NOTE &&
            this.currentProject &&
            this.currentProject.id === project.id &&
            this.currentId  === noteId
        ) {
            this.ngbProjectInfoService.setFalseMode(this.noteFalseValue);
        }
        if (this.hasChanges()) {
            this.showUnsavedChangesDialog();
            $event.stopImmediatePropagation();
        } else {
            this.ngbProjectInfoService.setNote(project, noteId);
        }
    }

    handleClickAddNote($event, project) {
        if (this.isAdditionMode &&
            this.currentProject &&
            this.currentProject.id === project.id
        ) {
            return;
        }
        this.ngbProjectInfoService.isCancel = false;
        if (this.hasChanges()) {
            this.showUnsavedChangesDialog();
            $event.stopImmediatePropagation();
        } else {
            this.ngbProjectInfoService.addNote(project);
        }
    }
}
