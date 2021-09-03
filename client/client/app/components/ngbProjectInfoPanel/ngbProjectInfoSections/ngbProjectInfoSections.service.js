function findSelectedDatasets(candidates) {
    const result = [];
    for (let i = 0; i < candidates.length; i++) {
        const candidate = candidates[i];
        if (candidate.isProject && candidate.nestedProjects && candidate.nestedProjects.length > 0) {
            const nestedSelectedDatasets = findSelectedDatasets(candidate.nestedProjects);
            if (nestedSelectedDatasets.length > 0) {
                result.push(...nestedSelectedDatasets);
                const hasSelectedTracks = (candidate.lazyItems || candidate.items || [])
                    .filter(track => !track.isProject && track.__selected)
                    .length > 0;
                if (hasSelectedTracks) {
                    result.push(candidate.id);
                }
            } else if (candidate.__selected || candidate.__indeterminate) {
                result.push(candidate.id);
            }
        } else if (candidate.isProject && (candidate.__selected || candidate.__indeterminate)) {
            result.push(candidate.id);
        }
    }
    return result;
}

const PROJECT_INFO_MODE = {
    SUMMARY: -1,
    DESCRIPTION: -2,
    ADD_NOTE: -3,
    EDIT_NOTE: -4
};

const PROJECT_INFO_MODE_NAME = {
    '-1': 'Summary',
    '-2': 'Description',
    '-3': 'Add note'
};

export default class ngbProjectInfoSectionsService {
    static instance($sce, dispatcher, projectContext) {
        return new ngbProjectInfoSectionsService($sce, dispatcher, projectContext);
    }

    constructor($sce, dispatcher, projectContext) {
        this.sce = $sce;
        this.dispatcher = dispatcher;
        this.projectContext = projectContext;
        this.currentProject = undefined;
        this._descriptionIsLoading = true;
        this._currentMode = undefined;
        this._previousMode = undefined;
        this._currentName = undefined;
        this._editingNote = {};
        this._summaryAvailable = false;
        this._descriptionAvailable = false;
        this._noteList = [];
        const projectChanged = this.projectChanged.bind(this);
        this.dispatcher.on('tracks:state:change', projectChanged);
        projectChanged();
    }

    get projectInfoModeList() {
        return PROJECT_INFO_MODE;
    }

    get currentName() {
        return this._currentName;
    }

    get currentMode() {
        return this._currentMode;
    }

    set currentMode(value) {
        if (this.currentMode !== this.projectInfoModeList.ADD_NOTE && this.currentMode !== this.projectInfoModeList.EDIT_NOTE) {
            this._previousMode = this.currentMode;
        }
        this._currentMode = value;
        this._currentName = PROJECT_INFO_MODE_NAME[value] || this.currentNote.title;
    }

    get previousMode() {
        return this._previousMode;
    }

    get noteList() {
        return this._noteList;
    }

    get descriptionAvailable() {
        return this._descriptionAvailable;
    }

    get summaryAvailable() {
        return this._summaryAvailable;
    }

    get descriptionIsLoading() {
        return this._descriptionIsLoading;
    }

    get currentNote() {
        return this.noteList[this.currentMode];
    }

    get editingNote() {
        return this._editingNote;
    }

    projectChanged() {
        this._summaryAvailable = this.projectContext.containsVcfFiles;
        const projectIds = [
            ...(
                new Set(findSelectedDatasets(this.projectContext.datasets || []))
            )
        ];
        const clearURLObject = () => {
            if (this.blobUrl) {
                URL.revokeObjectURL(this.blobUrl);
                this.blobUrl = undefined;
            }
        };
        if (projectIds.length === 1 && this.currentProject !== projectIds[0]) {
            clearURLObject();
            this.currentProject = projectIds[0];
            this._descriptionIsLoading = true;
            this._descriptionAvailable = false;
            this.projectContext.loadDatasetDescription(this.currentProject).then(data => {
                if (data && data.byteLength) {
                    this.currentMode = this.projectInfoModeList.DESCRIPTION;
                    this._descriptionAvailable = true;
                    this._descriptionIsLoading = false;
                    this.blobUrl = this.sce.trustAsResourceUrl(
                        URL.createObjectURL(new Blob([data], {type: 'text/html'}))
                    );
                } else {
                    this._descriptionAvailable = false;
                    this._descriptionIsLoading = false;
                    this.currentMode = this.projectInfoModeList.SUMMARY;
                    this._descriptionAvailable = false;
                }
                this.dispatcher.emitSimpleEvent('project:description:url', this.blobUrl);
            });
        } else if (projectIds.length !== 1) {
            this.currentProject = undefined;
            this._descriptionIsLoading = false;
            this.currentMode = this.projectInfoModeList.SUMMARY;
            this._descriptionAvailable = false;
            clearURLObject();
            this.dispatcher.emitSimpleEvent('project:description:url', this.blobUrl);
        }
    }

    addNote() {
        this.currentMode = this.projectInfoModeList.ADD_NOTE;
    }

    editNote(id) {
        this._editingNote = this.noteList.find(item => item.id === id)[0] || {};
        this.currentMode = this.projectInfoModeList.EDIT_NOTE;
    }

    cancelNote() {
        this._editingNote = {};
        this.currentMode = this.previousMode;
    }

    saveNote(note) {
        if (note.id) {
            const index = this.noteList.findIndex(item => item.id === note.id);
            if (~index) {
                this.noteList[index] = note;
            } else {
                this.noteList.push(note);
            }
        } else {
            this.noteList.push({
                id: this.noteList.length,
                canEdit: Math.ceil(Math.random() - 0.5),
                ...note
            });
        }
        this._editingNote = {};
        return new Promise(resolve => {
            const data = {};
            this.currentMode = this.previousMode;
            resolve(data);
        });
    }

    deleteNote(id) {
        const index = this.noteList.findIndex(item => item.id === id);
        if (~index) {
            this.noteList.splice(index, 1);
        }
    }
}
