import baseController from '../../../shared/baseController';

export default class ngbProjectInfoSectionsController extends baseController {
    static get UID() {
        return 'ngbProjectInfoSectionsController';
    }

    constructor($scope, dispatcher, ngbProjectInfoService, projectContext) {
        super(dispatcher);
        this.ngbProjectInfoService = ngbProjectInfoService;
        this.projectContext = projectContext;
    }

    onButtonClick($mdOpenMenu, $event) {
        if (this.projectContext.currentChromosome) {
            this.ngbProjectInfoService.currentMode = this.ngbProjectInfoService.projectInfoModeList.SUMMARY;
        } else {
            this.openMenu($mdOpenMenu, $event);
        }
    }

    get descriptionAvailable () {
        return this.ngbProjectInfoService.descriptionAvailable;
    }

    get projects () {
        return this.ngbProjectInfoService.projects;
    }

    setDescription (project, descriptionId) {
        const [
            currentMode,
            currentDescriptionId
        ] = Array.isArray(this.ngbProjectInfoService.currentMode) ?
                this.ngbProjectInfoService.currentMode :
                [this.ngbProjectInfoService.currentMode, null];
        const currentProjectId = this.ngbProjectInfoService.currentProject.id;
        if (currentMode === this.ngbProjectInfoService.projectInfoModeList.DESCRIPTION &&
            currentProjectId === project.id &&
            currentDescriptionId === descriptionId
        ) {
            return;
        }
        this.setCurrentProject(project);
        this.ngbProjectInfoService.setDescription(descriptionId);
    }

    setCurrentProject (project) {
        this.ngbProjectInfoService.currentProject = project;
    }

    openMenu($mdOpenMenu, $event) {
        $event.stopPropagation();
        $mdOpenMenu($event);
    }

    summaryFalseValue () {
        if (this.projects.length === 1 && this.descriptionList.length) {
            return [
                this.ngbProjectInfoService.projectInfoModeList.DESCRIPTION,
                this.descriptionList[0].id
            ];
        }
        return this.ngbProjectInfoService.projectInfoModeList.SUMMARY;
    }
}
