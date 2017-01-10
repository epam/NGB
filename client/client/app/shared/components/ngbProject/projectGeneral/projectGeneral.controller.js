
export default
class projectGeneralController {

    static get UID() {
        return 'projectGeneralController';
    }


    /** @ngInject */
    constructor(dispatcher, genomeDataService, ngbProjectService, projectDataService, $scope) {
        this._dispatcher = dispatcher;
        this._genomeDataService = genomeDataService;
        this._ngbProjectService = ngbProjectService;
        this._projectDataService = projectDataService;

        const onProjectCreate = () => {
            this.INIT();
            this._ngbProjectService.setDefault();
        };

        const onProjectEdit = () => {
            const projectId = this.projectId;
            this._projectDataService.getProject(projectId)
                .then((project)=> {
                    this.referenceList = project.items.filter(item => item.format === 'REFERENCE');
                    this.projectName = project.name;
                    this._ngbProjectService.setProjectId(project.id);
                    this.changeProjectName(this.projectName);
                    this.emitReferenceChangeEvent();
                });
        };

        this._dispatcher.on('ngbProject:create:new', onProjectCreate);
        this._dispatcher.on('ngbProject:edit', onProjectEdit);

        $scope.$on('$destroy', () => {
            this._dispatcher.removeListener('ngbProject:create:new', onProjectCreate);
            this._dispatcher.removeListener('ngbProject:edit', onProjectEdit);
        });
    }

    isEdit() {
        if (this.emittedEvent === 'ngbProject:edit') {
            return true;
        }
        return false;
    }

    INIT() {
        this.projectName = '';
        this.referenceList = [];
        this._ngbProjectService.setProjectId('');
    }

    getReferences() {
        return this._ngbProjectService.getReferences();
    }

    changeProjectName(name) {
        this._ngbProjectService.setProjectName(name);
    }

    selectedReferenceListContains(item) {
        if (!this.referenceList) {
            return -1;
        }
        for (let i = 0; i < this.referenceList.length; i++) {
            if (this.referenceList[i].bioDataItemId === item.bioDataItemId) {
                return i;
            }
        }
        return -1;
    }

    toggleReference(item) {
        if (this.isEdit()) {
            return;
        }
        const idx = this.selectedReferenceListContains(item);

        if (idx > -1) {
            this.referenceList.splice(idx, 1);
        } else {
            this.referenceList = [item];
        }
        this.emitReferenceChangeEvent();
    }

    existsReference(item) {
        const idx = this.selectedReferenceListContains(item);
        return idx !== -1
            ? true
            : false;
    }

    emitReferenceChangeEvent() {
        this._dispatcher.emitGlobalEvent('ngbProject:reference:change', {referenceList: this.referenceList});
        this._ngbProjectService.referenceChange({referenceList: this.referenceList});
    }

}