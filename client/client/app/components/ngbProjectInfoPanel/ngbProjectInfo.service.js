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

function sortDatasets (datasets) {
    const idsList = [];
    const parentIdsList = [];
    const parentsAndChildren = {};
    const newIdsList = [];
    const objectForCheck = {};

    datasets.forEach(dataset => {
        const {id, parentId} = dataset;
        idsList.push(id);
        parentIdsList.push(parentId);
        if (parentId) {
            parentsAndChildren[parentId] = parentsAndChildren[parentId] ?
            [...parentsAndChildren[parentId], id] : [id];
        }
    });

    for (let i = 0; i < idsList.length; i++) {
        if (!objectForCheck[idsList[i]]) {
            const parent = parentIdsList[i] ?  parentIdsList[i] : idsList[i];
            const childrenList = parentsAndChildren[parent];
            if (childrenList) {
                for (let j = 0; j < childrenList.length; j++) {
                    newIdsList.push(childrenList[j]);
                    objectForCheck[childrenList[j]] = true;
                }
                newIdsList.push(parent);
                objectForCheck[parent] = true;
            } else {
                newIdsList.push(idsList[i]);
                objectForCheck[idsList[i]] = true;
            }
        }
    }

    datasets.sort((a, b) => {
        return newIdsList.indexOf(b.id) < newIdsList.indexOf(a.id) ?
            1 : -1;
    });
}

const PROJECT_INFO_MODE = {
    SUMMARY: -1,
    DESCRIPTION: -2,
    ADD_NOTE: -3,
    CHR_SUMMARY: -4
};

const PROJECT_INFO_MODE_NAME = {
    '-1': 'Summary',
    '-2': 'Description',
    '-3': 'Add note',
    '-4': 'Summary'
};

const EDIT_RIGHT = 2;

export default class ngbProjectInfoService {
    _descriptionIsLoading;
    constructor($sce, $mdDialog, dispatcher, projectContext, projectDataService) {
        Object.assign(this, {
            $sce, $mdDialog, dispatcher, projectContext, projectDataService
        });
        this.currentProject = {};
        this._currentMode = undefined;
        this._previousMode = undefined;
        this._currentName = undefined;
        this._isEdit = false;
        this._isCancel = false;
        this._editingNote = {};
        this._newNote = {};
        this.projects = [];
        this._descriptionAvailable = false;
        const projectChanged = this.projectChanged.bind(this);
        this.dispatcher.on('dataset:selection:change', projectChanged);
        this.dispatcher.on('chromosome:change', this.chromosomeChanged.bind(this));
        projectChanged();
    }

    get currentProject () {
        return this._currentProject;
    }

    set currentProject (project) {
        this._currentProject = project;
    }

    set currentMode(value) {
        const valueMode = Array.isArray(value) ? value[0] : value;
        if (valueMode === this.projectInfoModeList.CHR_SUMMARY) {
            this._clearEnvironment();
            this._currentMode = value;
            this.setCurrentName(this.currentMode);
            return;
        }
        if (this.projectContext.currentChromosome) {
            this.projectContext.changeState({chromosome: null});
        }
        if ((valueMode === this.projectInfoModeList.DESCRIPTION && !this.descriptionAvailable)
            || (value === this.projectInfoModeList.SUMMARY && !this.summaryAvailable)) {
            return;
        }
        if (value === this.currentMode && !this._isCancel) {
            return;
        }
        if (this._hasChanges()) {
            const alert = this.$mdDialog.alert()
                .title('There is an unsaved changes.')
                .textContent('Save it or cancel editing.')
                .ariaLabel('Unsaved changes')
                .ok('OK');
            this.$mdDialog.show(alert);
        } else {
            if (value !== undefined
                && ![
                    this.projectInfoModeList.ADD_NOTE,
                    this.projectInfoModeList.CHR_SUMMARY
                ].includes(this.currentMode)) {
                this._previousMode = this.currentMode;
            }
            this._clearEnvironment();
            this._currentMode = value || this.defaultMode;
            if (this.projects.length > 1 &&
                this.currentMode === this.projectInfoModeList.SUMMARY
            ) {
                this.currentProject = {};
                this._newNote = {};
            }
            this.setCurrentName(this.currentMode);
        }
    }

    get projectInfoModeList() {
        return PROJECT_INFO_MODE;
    }

    get currentName() {
        return this._currentName;
    }

    setCurrentName(value) {
        const [valueMode, valueId] = Array.isArray(value) ? value : [value, null];
        if (this.projects.length > 1) {
            let nameValue = PROJECT_INFO_MODE_NAME[valueMode] || this.currentNote.title;
            if (valueMode !== undefined && (valueMode > 0 || valueId)) {
                if (valueMode === this.projectInfoModeList.DESCRIPTION) {
                    nameValue = this.currentDescription.name;
                }
                this._currentName = `${this.currentProject.name}:${nameValue}`;
            } else {
                this._currentName = nameValue;
            }
        } else {
            this._currentName = PROJECT_INFO_MODE_NAME[valueMode] || this.currentNote.title;
        }
    }

    get currentMode() {
        return this._currentMode;
    }

    get noteList() {
        return this.currentProject.notes || [];
    }

    get descriptionList () {
        return this.currentProject.descriptions || [];
    }

    get currentDescription () {
        return this.descriptionList
            .filter(description => description.id === this.currentMode[1])[0] || {};
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

    set descriptionAvailable (value) {
        this._descriptionAvailable = value;
    }

    get summaryAvailable() {
        return true;
    }

    get descriptionIsLoading() {
        return this._descriptionIsLoading;
    }

    set descriptionIsLoading (value) {
        this._descriptionIsLoading = value;
    }

    get canEdit() {
        return !!(this.currentProject.mask & EDIT_RIGHT);
    }

    get isEdit() {
        return this._isEdit;
    }

    get editingNote() {
        return this._editingNote;
    }

    get newNote() {
        return this._newNote;
    }

    get defaultMode() {
        if (this.projects.length > 1) {
            if (this.summaryAvailable) {
                return this.projectInfoModeList.SUMMARY;
            } else {
                return this.currentMode;
            }
        } else if (this.projects.length === 1) {
            if (this.descriptionAvailable) {
                return this.projectInfoModeList.DESCRIPTION;
            } else if (this.summaryAvailable) {
                return this.projectInfoModeList.SUMMARY;
            } else {
                return this.currentMode;
            }
        }
    }

    static instance($sce, $mdDialog, dispatcher, projectContext, projectDataService) {
        return new ngbProjectInfoService($sce, $mdDialog, dispatcher, projectContext, projectDataService);
    }

    setDescription (descriptionId = 0) {
        if (this.descriptionList.length) {
            descriptionId = descriptionId ? descriptionId : this.descriptionList[0].id;
            this.descriptionIsLoading = true;
            this.descriptionAvailable = false;
            this.projectContext.loadDatasetDescription(this.currentProject.id) //add description.id for case with descriptionS
                .then(data => {
                    if (data && data.byteLength) {
                        this.descriptionAvailable = true;
                        this.descriptionIsLoading = false;
                        this.blobUrl = this.$sce.trustAsResourceUrl(
                            URL.createObjectURL(new Blob([data], {type: 'text/html'}))
                        );
                        if (!this.projectContext.currentChromosome) {
                            this._isCancel = false;
                            this.currentMode = [
                                this.projectInfoModeList.DESCRIPTION,
                                descriptionId
                            ];
                        }
                    } else {
                        this.descriptionAvailable = false;
                        this.descriptionIsLoading = false;
                        if (!this.projectContext.currentChromosome) {
                            this._isCancel = true;
                            this.currentMode = this.projectInfoModeList.SUMMARY;
                        }
                    }
                    this.dispatcher.emitSimpleEvent('project:description:url', this.blobUrl);
                });
        } else {
            if (!this.projectContext.currentChromosome) {
                this.currentMode = this.projectInfoModeList.SUMMARY;
            }
        }
    }

    projectChanged() {
        const selectedDatasets = [
            ...(
                new Set(findSelectedDatasets(this.projectContext.datasets || []))
            )
        ];
        const parents = new Set(selectedDatasets.map(dataset => dataset.id));
        selectedDatasets.forEach(item => {
            if (item.parentId && !parents.has(item.parentId)) {
                selectedDatasets.add(
                    ...this.projectContext.datasets
                        .filter(dataset => dataset.id === item.parentId)
                );
                parents.add(item.parentId);
            }
        });
        if (selectedDatasets.length > 1) {
            sortDatasets(selectedDatasets);
        }
        // delete it after we get data from server
        selectedDatasets.forEach(project => {
            this.projectContext.loadDatasetDescription(project.id)
                .then(data => {
                    if (data && data.byteLength) {
                        project.descriptions = [{
                            id: project.id+1,
                            name: 'des_1'
                        }, {
                            id: project.id+2,
                            name: 'des_2'
                        }];
                    }
                });
        });
        const clearURLObject = () => {
            if (this.blobUrl) {
                URL.revokeObjectURL(this.blobUrl);
                this.blobUrl = undefined;
            }
        };
        if (selectedDatasets.length > 0) {
            clearURLObject();
            if (
                selectedDatasets.length === 1 &&
                this.currentProject.id !== selectedDatasets[0].id
            ) {
                this.projects = selectedDatasets;
                this.currentProject = selectedDatasets[0];
                this._newNote = {
                    projectId: this.currentProject.id
                };
                this.setDescription();
            } else if (selectedDatasets.length > 1) {
                this.descriptionIsLoading = false;
                this.projects = selectedDatasets.map(project => {
                    project.canEdit = !!(project.mask & EDIT_RIGHT);
                    return project;
                });
                this.currentProject = {};
                this._newNote = {};
                if (!this.projectContext.currentChromosome) {
                    this._isCancel = true;
                    this.currentMode = this.projectInfoModeList.SUMMARY;
                }
            }
        } else {
            this.projects = [];
            this.currentProject = {};
            this._newNote = {};
            this.descriptionIsLoading = false;
            this.descriptionAvailable = false;
            if (!this.projectContext.currentChromosome) {
                this._isCancel = true;
                this.currentMode = this.projectInfoModeList.SUMMARY;
            }
            clearURLObject();
            this.dispatcher.emitSimpleEvent('project:description:url', this.blobUrl);
        }
    }

    chromosomeChanged() {
        if (this.projectContext.currentChromosome !== null) {
            this.currentMode = this.projectInfoModeList.CHR_SUMMARY;
        }
    }

    addNote(project = this.currentProject) {
        if (this.projects.length > 1) {
            this.currentProject = project;
            this._newNote = {
                projectId: this.currentProject.id
            };
            this.currentMode = [this.projectInfoModeList.ADD_NOTE, project.id];
        } else {
            this.currentMode = this.projectInfoModeList.ADD_NOTE;
        }
    }

    editNote(id) {
        this.currentMode = id;
        const filteredNoteList = this.noteList.filter(item => item.id === id);
        this._editingNote = filteredNoteList.length ? {...filteredNoteList[0]} : {};
        this._isEdit = true;
        this._previousMode = this.currentMode;
    }

    cancelNote() {
        this._isCancel = true;
        if (this.projects.length > 1) {
            this.currentMode = this.defaultMode;
        } else {
            this.currentMode = this.previousMode;
        }
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
                this._finishEditing();
                this._finishCreating();
                if (!data.error) {
                    this.currentProject.notes = this.projectContext.refreshDatasetNotes(data.notes || [], this.currentProject.id);
                    this.setCurrentName(this.currentMode);
                }
                return data;
            });
    }

    _finishEditing() {
        this._isEdit = false;
        this._editingNote = {};
    }

    _finishCreating() {
        this._newNote = {
            projectId: this.currentProject.id
        };
    }

    _clearEnvironment() {
        if (this._currentMode === this.projectInfoModeList.ADD_NOTE) {
            this._finishCreating();
        } else if (this.isEdit) {
            this._finishEditing();
        }
        this._isCancel = false;
    }

    _hasChanges() {
        let hasChanges = false;
        if (!this._isCancel) {
            const currentMode = Array.isArray(this.currentMode) ?
                this.currentMode[0] : this.currentMode;
            if (this.isEdit) {
                const currentNote = this.currentNote;
                hasChanges = Object.keys(this._editingNote)
                    .some(key => this._editingNote[key] !== currentNote[key]);
            } else if (currentMode === this.projectInfoModeList.ADD_NOTE) {
                hasChanges = Object.keys(this._newNote)
                    .filter(f => f !== 'projectId')
                    .some(f => !!this.newNote[f]);
            }
        }
        return hasChanges;
    }
}
