export default
class ngbCreateProjectController {
    static get UID() {
        return 'ngbProjectTabsController';
    }

    /** @ngInject */
    constructor(dispatcher, $mdDialog, ngbProjectService, projectDataService, emittedEvent, projectId) {
        this._dispatcher = dispatcher;
        this._mdDialog = $mdDialog;
        this._ngbProjectService = ngbProjectService;
        this._projectDataService = projectDataService;
        this.emittedEvent = emittedEvent;
        this.projectId = projectId;
        this.windowTitle = this.projectId ? 'Edit Project' : 'Create Project';
    }

    INIT() {

    }

    isTracksTabNotAvailable() {
        return !this._ngbProjectService.isReferenceSelected();
    }

    close() {
        this._mdDialog.hide();
    }

    isSaveDisable() {
        const project = this._ngbProjectService.getSavingProjectObject();
        if (!project.projectName
            || project.projectName === ''
            || project.projectTracks.referencesBioDataItemIdList.length === 0) {
            return true;
        }
        return false;
    }

    save() {
        const project = this._ngbProjectService.getSavingProjectObject();
        const name = project.projectName;
        const items =
            project.projectTracks.referencesBioDataItemIdList.concat(project.projectTracks.tracksBioDataItemIdList);
        const id = project.id;
        if (!name || items.length === 0) {
            return;
        }
        this._projectDataService.saveProject({name, items, id}).then(
            () => {
                this._dispatcher.emitGlobalEvent('ngbProject:projects:change', {});
                this._mdDialog.hide();
            },
            () => {
                //reject area
                //alert(reject);
            }
        );

    }

}