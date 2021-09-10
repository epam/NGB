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
                    result.push(candidate);
                }
            } else if (candidate.__selected || candidate.__indeterminate) {
                result.push(candidate);
            }
        } else if (candidate.isProject && (candidate.__selected || candidate.__indeterminate)) {
            result.push(candidate);
        }
    }
    return result;
}

function processNestedProjects(projects, nesting) {
    if (projects.length === 1) {
        return projects;
    }
    if (projects.filter(p => p.nesting === nesting).length !== 1) {
        // if there are more than one project with the current nesting level, then allow summary only
        return projects;
    }
    return processNestedProjects(projects.filter(p => p.nesting !== nesting), nesting++);
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

const EDIT_RIGHT = 2;

export default class ngbProjectInfoService {
    constructor($sce, dispatcher, projectContext, projectDataService) {
        this.sce = $sce;
        this.dispatcher = dispatcher;
        this.projectContext = projectContext;
        this.projectDataService = projectDataService;
        this.currentProject = {};
        this._descriptionIsLoading = true;
        this._currentMode = undefined;
        this._previousMode = undefined;
        this._currentName = undefined;
        this._editingNote = {};
        this._newNote = {};
        this._summaryAvailable = false;
        this._descriptionAvailable = false;
        const projectChanged = this.projectChanged.bind(this);
        this.dispatcher.on('dataset:selection:change', projectChanged);
        projectChanged();
    }

    set currentMode(value) {
        if ((value === this.projectInfoModeList.DESCRIPTION && !this.descriptionAvailable)
            || (value === this.projectInfoModeList.SUMMARY && !this.summaryAvailable)) {
            return;
        }
        if (value !== undefined
            && ![this.projectInfoModeList.ADD_NOTE, this.projectInfoModeList.EDIT_NOTE].includes(this.currentMode)) {
            this._previousMode = this.currentMode;
        }
        const previousNoteName = PROJECT_INFO_MODE_NAME[this.currentMode] || this.currentNote.title;
        this._currentMode = value || this.defaultMode;
        this._currentName = value === this.projectInfoModeList.EDIT_NOTE
            ? previousNoteName
            : PROJECT_INFO_MODE_NAME[value] || this.currentNote.title;
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

    get noteList() {
        return this.currentProject.notes || [];
    }

    get previousMode() {
        return this._previousMode;
    }

    get currentNote() {
        return this.noteList.filter(note => note.id === this.currentMode)[0] || {};
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

    get canEdit() {
        return !!(this.currentProject.mask & EDIT_RIGHT);
    }

    get editingNote() {
        return this._editingNote;
    }

    get newNote() {
        return this._newNote;
    }

    get defaultMode() {
        if (this.descriptionAvailable) {
            return this.projectInfoModeList.DESCRIPTION;
        } else if (this.summaryAvailable) {
            return this.projectInfoModeList.SUMMARY;
        } else {
            return this.currentMode;
        }
    }

    static instance($sce, dispatcher, projectContext, projectDataService) {
        return new ngbProjectInfoService($sce, dispatcher, projectContext, projectDataService);
    }

    projectChanged() {
        this._summaryAvailable = this.projectContext.containsVcfFiles;
        const selectedDatasets = [
            ...(
                new Set(findSelectedDatasets(this.projectContext.datasets || []))
            )
        ];
        const projects = processNestedProjects(selectedDatasets, 0);
        const clearURLObject = () => {
            if (this.blobUrl) {
                URL.revokeObjectURL(this.blobUrl);
                this.blobUrl = undefined;
            }
        };
        if (projects.length === 1 && this.currentProject.id !== projects[0].id) {
            clearURLObject();
            this.currentProject = projects[0];
            this._newNote = {
                projectId: this.currentProject.id
            };
            this._descriptionIsLoading = true;
            this._descriptionAvailable = false;
            this.projectContext.loadDatasetDescription(this.currentProject.id).then(data => {
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
                    this._descriptionAvailable = false;
                    this.currentMode = this.summaryAvailable
                        ? this.projectInfoModeList.SUMMARY
                        : this.projectInfoModeList.ADD_NOTE;
                }
                this.dispatcher.emitSimpleEvent('project:description:url', this.blobUrl);
            });
        } else if (projects.length !== 1) {
            this.currentProject = {};
            this._newNote = {};
            this._descriptionIsLoading = false;
            this.currentMode = this.summaryAvailable
                ? this.projectInfoModeList.SUMMARY
                : undefined;
            this._descriptionAvailable = false;
            clearURLObject();
            this.dispatcher.emitSimpleEvent('project:description:url', this.blobUrl);
        }
    }

    addNote() {
        this.currentMode = this.projectInfoModeList.ADD_NOTE;
    }

    editNote(id) {
        this._editingNote = this.noteList.filter(item => item.id === id)[0] || {};
        this.currentMode = this.projectInfoModeList.EDIT_NOTE;
    }

    cancelNote() {
        this._editingNote = {};
        this.currentMode = this.previousMode;
    }

    saveNote(note) {
        const notes = [...this.noteList];
        if (note.id) {
            const index = notes.findIndex(item => item.id === note.id);
            if (~index) {
                notes[index] = note;
            } else {
                notes.push(note);
            }
        } else {
            notes.push(note);
        }
        const previousIds = (this.currentProject.notes || []).map(n => n.id);
        return this._saveProject(this.currentProject, notes)
            .then(data => {
                const newIds = (this.currentProject.notes || []).map(n => n.id);
                if (previousIds.length < newIds.length) {
                    const diff = newIds.filter(id => !previousIds.includes(id));
                    this.currentMode = diff[0] || this.previousMode;
                } else {
                    this.currentMode = this.previousMode;
                }
                return data;
            });
    }

    deleteNote(id) {
        const notes = [...this.noteList];
        const index = notes.findIndex(item => item.id === id);
        if (~index) {
            notes.splice(index, 1);
        }
        return this._saveProject(this.currentProject, notes)
            .then(data => {
                this.currentMode = this.defaultMode;
                return data;
            });
    }

    _saveProject(project, notes) {
        return this.projectDataService.saveProject(this.projectContext.convertProjectForSave({
            ...project,
            notes
        }))
            .then(data => {
                this._editingNote = {};
                this._newNote = {
                    projectId: this.currentProject.id
                };
                if (!data.error) {
                    this.currentProject.notes = this.projectContext.refreshDatasetNotes(data.notes, this.currentProject.id);
                }
                return data;
            });
    }
}
