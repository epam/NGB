function findSelectedDatasets(candidates) {
    const result = [];
    for (let i = 0; i < candidates.length; i++) {
        const candidate = candidates[i];
        if (!candidate.isProject) {
            continue;
        }
        let added = false;
        const hasSelectedTracks = (candidate.lazyItems || candidate.items || [])
            .filter(track => !track.isProject && track.__selected)
            .length > 0;
        if (hasSelectedTracks || candidate.__selected || candidate.__indeterminate) {
            result.push(candidate);
            added = true;
        }
        if (candidate.nestedProjects && candidate.nestedProjects.length > 0) {
            const nestedSelectedDatasets = findSelectedDatasets(candidate.nestedProjects);
            if (nestedSelectedDatasets.length > 0) {
                result.push(...nestedSelectedDatasets);
                if (!added) {
                    added = true;
                    result.push(candidate);
                }
            }
        }
    }
    return result;
}

function sortDatasets (datasets = []) {
    const info = datasets.map(dataset => ({
        id: dataset.id,
        parentId: dataset.parentId || undefined
    }));
    /**
     * Returns array of [child, child, child, ..., parent] for parent dataset id
     * @param {number} datasetId - parent dataset id
     * @returns {{id: number, parentId: number}[]}
     */
    const getChildrenDatasets = datasetId => {
        const [dataset] = info.filter(i => i.id === datasetId);
        const children = info.filter(i => i.parentId === datasetId);
        return children
            .map(child => getChildrenDatasets(child.id))
            .reduce((r, c) => ([...r, ...c]), [])
            .concat(dataset)
            .filter(Boolean);
    };
    /**
     * Root datasets identifiers; "root dataset" is a dataset that is not a child for any other datasets.
     * Root datasets identifiers are sorted by decreasing of it's children count
     * @type {number[]}
     */
    const rootDatasetIds = Array.from(new Set(
        info
            .filter(child => info.filter(parent => parent.id === child.parentId).length === 0)
            .map(child => child.id)
    ))
        .sort((aId, bId) => {
            const aChildrenLength = info.filter(child => child.parentId === aId).length;
            const bChildrenLength = info.filter(child => child.parentId === bId).length;
            return bChildrenLength - aChildrenLength;
        });
    const sortedDatasetIds = rootDatasetIds
        .map(getChildrenDatasets)
        .reduce((r, c) => ([...r, ...c]), [])
        .map(o => o.id);
    const getIndex = id => {
        const index = sortedDatasetIds.indexOf(id);
        if (index === -1) {
            return Infinity;
        }
        return index;
    };
    datasets.sort((a, b) => getIndex(a.id) - getIndex(b.id));
}

const MODE = {
    SUMMARY: -1,
    CHR_SUMMARY: -2,
    DESCRIPTION: -3,
    NOTE: -4,
    ADD_NOTE: -5,
};

const MODE_NAME = {
    '-1': 'Summary',
    '-2': 'Summary',
    '-3': 'Description',
    '-4': 'Note',
    '-5': 'New note',
    '-6': 'Note'
};

const READ_PERMISSION = 1;

export default class ngbProjectInfoService {

    _descriptionIsLoading = false;
    _descriptionAvailable = false;
    selectedProjects = [];
    _currentProject = null;
    plainItems = [];
    _modeModel = [];
    previouseModeModel;
    _newNote = {};
    _editableNote = {};
    _inEditing = false;
    _isCancel = true;

    get readPermission() {
        return READ_PERMISSION;
    }
    get mode() {
        return MODE;
    }
    get modeName() {
        return MODE_NAME;
    }

    static instance($sce, $mdDialog, dispatcher, projectContext, projectDataService) {
        return new ngbProjectInfoService($sce, $mdDialog, dispatcher, projectContext, projectDataService);
    }

    constructor($sce, $mdDialog, dispatcher, projectContext, projectDataService) {
        Object.assign(this, {
            $sce, $mdDialog, dispatcher, projectContext, projectDataService
        });
        this.dispatcher.on('dataset:selection:change', this.projectChanged.bind(this));
        this.dispatcher.on('chromosome:change', this.chromosomeChanged.bind(this));
    }

    get descriptionAvailable() {
        return this._descriptionAvailable;
    }
    get descriptionIsLoading() {
        return this._descriptionIsLoading;
    }

    get currentProject () {
        return this._currentProject;
    }
    set currentProject (project) {
        this._currentProject = project;
    }

    get modeModel() {
        return this._modeModel;
    }
    set modeModel(value) {
        this._modeModel = [...value];
    }

    get currentMode() {
        return this.modeModel[0];
    }
    get currentId() {
        return this.modeModel[1];
    }

    get newNote() {
        return this._newNote;
    }
    get editableNote() {
        return this._editableNote;
    }
    get inEditing() {
        return this._inEditing;
    }

    get isAdditionMode() {
        return this.currentMode === this.mode.ADD_NOTE;
    }
    get isEditingAvailable() {
        return this.checkPermission(this._currentProject.mask);
    }

    get currentChromosome() {
        return this.projectContext.currentChromosome;
    }
    get isSingleProject() {
        return this.selectedProjects.length === 1;
    }
    get isMultipleProjects() {
        return this.selectedProjects.length > 1;
    }

    get defaultModeModel() {
        if (this.isSingleProject &&
            this._descriptionAvailable &&
            this.descriptionList.length
        ) {
            return [this.mode.DESCRIPTION, this.descriptionList[0].id];
        } else {
            return [this.mode.SUMMARY, null];
        }
    }

    get descriptionList () {
        return (this._currentProject || {}).descriptions || [];
    }
    get noteList() {
        return (this._currentProject || {}).notes || [];
    }
    get currentDescription () {
        return this.descriptionList
            .filter(description => description.id === this.currentId)[0] || {};
    }
    get currentNote() {
        return this.noteList.filter(note => note.id === this.currentId)[0] || {};
    }

    get currentName() {
        const nameValue = this._getNameValue();
        if (
            this.isMultipleProjects &&
            this.currentMode !== undefined &&
            this.currentMode !== this.mode.SUMMARY &&
            this.currentMode !== this.mode.CHR_SUMMARY
        ) {
            return `${this._currentProject.name}: ${nameValue}`;
        } else {
            return nameValue;
        }
    }

    set isCancel(value) {
        this._isCancel = value;
    }

    _getNameValue() {
        let nameValue;
        if (this.currentMode === this.mode.DESCRIPTION) {
            nameValue = this.currentDescription.name;
        }
        if (this.currentMode === this.mode.NOTE) {
            nameValue = this.currentNote.title;
        }
        return nameValue || this.modeName[this.currentMode];
    }

    chromosomeChanged() {
        if (this.currentChromosome !== null) {
            // switch to track view
            this._clearEnvironment();
            this._modeModel = [this.mode.CHR_SUMMARY, null];
        }
    }

    _getSelectedDatasets() {
        const selectedDatasets = [...(
            new Set(findSelectedDatasets(this.projectContext.datasets || []))
        )];
        if (selectedDatasets.length > 1) {
            sortDatasets(selectedDatasets);
        }
        return selectedDatasets;
    }

    _clearURLObject() {
        if (this.blobUrl) {
            URL.revokeObjectURL(this.blobUrl);
            this.blobUrl = undefined;
        }
    }

    checkPermission(mask) {
        const permission = this.readPermission;
        return (mask & permission) === permission;
    }

    projectChanged() {
        const selectedDatasets = this._getSelectedDatasets();
        this._clearURLObject();
        if (selectedDatasets.length >= 1) {
            this.selectedProjects = selectedDatasets.slice();
            this.selectedProjects.forEach(project => {
                project.canEdit = this.checkPermission(project.mask);
            });
            selectedDatasets.length === 1 ?
                this._setSingleProject() :
                this._setMultipleProjects();
        }
        if (selectedDatasets.length < 1) {
            this._setNoneProject();
        }
        this.refreshPlainList();
    }

    _setSingleProject() {
        const currentProjectChanged = (this._currentProject && this._currentProject.id) !== this.selectedProjects[0].id;
        if (currentProjectChanged) {
            this._currentProject = this.selectedProjects[0];
            this._newNote = {
                projectId: this._currentProject.id
            };
            this._clearEnvironment();
            this.setDescription();
        }
    }

    _setMultipleProjects() {
        const currentProjectChanged = this._currentProject && this._currentProject.id &&
            this.selectedProjects.filter(dataset => (
                dataset.id === this._currentProject.id
            )).length === 0;
        if (currentProjectChanged || !this._currentProject) {
            this._descriptionAvailable = false;
            this._descriptionIsLoading = false;
            this._newNote = {};
            this._currentProject = {};
            this.setNoCurrentChromosome();
        }
        if (!currentProjectChanged && this.isAdditionMode) {
            this.previouseModeModel = undefined;
        }
    }

    _setNoneProject() {
        this.selectedProjects = [];
        this._currentProject = {};
        this._newNote = {};
        this._descriptionAvailable = false;
        this._descriptionIsLoading = false;
        this.setNoCurrentChromosome();
        this.dispatcher.emitSimpleEvent('project:description:url', this.blobUrl);
    }

    refreshPlainList() {
        this.plainItems = this.selectedProjects
            .map(project => [
                {
                    id: `${project.id}-project-divider`,
                    isProjectDivider: true
                },
                {
                    id: `${project.id}-project-header`,
                    isHeader: true,
                    project
                },
                ...(project.descriptions || []).map(description => ({
                    id: `${project.id}-project-description-${description.id}`,
                    isDescription: true,
                    project,
                    description
                })),
                {
                    id: `${project.id}-project-description-divider`,
                    isDivider: true
                },
                ...(project.notes || []).map(note => ({
                    id: `${project.id}-project-note-${note.id}`,
                    isNote: true,
                    project,
                    note
                })),
                {
                    id: `${project.id}-project-note-add`,
                    isAddNote: true,
                    project
                }
            ])
            .reduce((r, c) => ([...r, ...c]), []);
    }

    setSummary() {
        this._clearEnvironment();
        if (this.isMultipleProjects) {
            this._currentProject = {};
            this._newNote = {};
        }
    }

    setNote(project) {
        this._clearEnvironment();
        this._currentProject = project;
    }

    async setDescription (descriptionId = 0, project = this._currentProject) {
        this._currentProject = project;
        if (this.descriptionList.length) {
            descriptionId = descriptionId ? descriptionId : this.descriptionList[0].id;
            this._descriptionIsLoading = true;
            const blob = await this.projectContext.downloadProjectDescription(descriptionId);
            if (blob && blob.byteLength) {
                this._descriptionAvailable = true;
                this._descriptionIsLoading = false;
                this.blobUrl = this.$sce.trustAsResourceUrl(
                    URL.createObjectURL(new Blob([blob], {type: 'text/html'}))
                );
                this.setNoCurrentChromosome([this.mode.DESCRIPTION, descriptionId]);
            } else {
                this._descriptionAvailable = false;
                this._descriptionIsLoading = false;
                this.setNoCurrentChromosome();
            }
            this.dispatcher.emitSimpleEvent('project:description:url', this.blobUrl);
        } else {
            this._descriptionIsLoading = false;
            this._descriptionAvailable = false;
            this.setNoCurrentChromosome();
        }
    }

    setFalseMode(falseValue) {
        if (this.isSingleProject) {
            if (falseValue[0] === this.mode.DESCRIPTION) {
                this.setDescription();
            } else if (falseValue[0] === this.mode.SUMMARY) {
                this.setSummary();
            }
        }
        if (this.isMultipleProjects) {
            this._setMultipleProjects();
        }
    }

    addNote(project = this._currentProject) {
        if (this.isMultipleProjects) {
            if ([this.mode.DESCRIPTION, this.mode.NOTE].includes(this.currentMode) &&
                project.id === this._currentProject.id
            ) {
                this.previouseModeModel = [...this.modeModel];
            }
            this._currentProject = project;
            this._newNote = {
                projectId: this._currentProject.id
            };
            this._modeModel = [this.mode.ADD_NOTE, project.id];
        } else {
            if ([this.mode.SUMMARY, this.mode.DESCRIPTION, this.mode.NOTE]
                .includes(this.currentMode)
            ) {
                this.previouseModeModel = [...this.modeModel];
            }
            this._modeModel = [this.mode.ADD_NOTE, project.id];
        }
    }

    editNote(id) {
        this._clearEnvironment();
        this._isCancel = false;
        this._modeModel = [this.mode.NOTE, id];
        const filteredNoteList = this.noteList.filter(item => item.id === id);
        this._editableNote = filteredNoteList.length ? {...filteredNoteList[0]} : {};
        this._inEditing = true;
        this.previouseModeModel = [...this.modeModel];
    }

    cancelNote() {
        this._clearEnvironment();
        this.modeModel = this.previouseModeModel || this.defaultModeModel;
        this.previouseModeModel = undefined;
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
        const previousIds = this.noteList.map(n => n.id);
        this._isCancel = true;
        return this._saveProject(this._currentProject, notes)
            .then(data => {
                const newIds = this.noteList.map(n => n.id);
                if (previousIds.length < newIds.length) {
                    const newNoteId = newIds.filter(id => !previousIds.includes(id))[0];
                    this._modeModel = [this.mode.NOTE, newNoteId];
                } else {
                    this.modeModel = this.previouseModeModel;
                }
                this.previouseModeModel = undefined;
                this.refreshPlainList();
                return data;
            });
    }

    deleteNote(id) {
        const notes = [...this.noteList];
        const index = notes.findIndex(item => item.id === id);
        if (~index) {
            notes.splice(index, 1);
        }
        return this._saveProject(this._currentProject, notes)
            .then(data => {
                this.modeModel = this.defaultModeModel;
                this.refreshPlainList();
                return data;
            });
    }

    _saveProject(project, notes) {
        return this.projectDataService.saveProject(
            this.projectContext.convertProjectForSave({
                ...project,
                notes
            }))
            .then(data => {
                this._finishEditing();
                this._finishCreating();
                if (!data.error) {
                    this._currentProject.notes = this.projectContext.refreshDatasetNotes(
                        data.notes || [], this._currentProject.id
                    );
                    this.refreshPlainList();
                } else {
                    this.modeModel = this.defaultModeModel;
                }
                return data;
            });
    }

    _finishEditing() {
        this._inEditing = false;
        this._editableNote = {};
    }

    _finishCreating() {
        this._newNote = {
            projectId: this._currentProject.id
        };
    }

    _clearEnvironment() {
        if (this.isAdditionMode) {
            this._finishCreating();
        } else if (this.inEditing) {
            this._finishEditing();
        }
        this._isCancel = true;
    }

    showUnsavedChangesDialog() {
        const alert = this.$mdDialog.alert()
            .title('There is an unsaved changes.')
            .textContent('Save it or cancel editing.')
            .ariaLabel('Unsaved changes')
            .ok('OK');
        this.$mdDialog.show(alert);
    }

    hasChanges() {
        let hasChanges = false;
        if (!this._isCancel) {
            if (this.inEditing) {
                hasChanges = Object.keys(this._editableNote)
                    .some(key => (
                        this._editableNote[key] !== this.currentNote[key]
                    ));
            } else if (this.isAdditionMode) {
                hasChanges = Object.keys(this._newNote)
                    .filter(f => f !== 'projectId')
                    .some(f => !!this.newNote[f]);
            }
        }
        return hasChanges;
    }

    setNoCurrentChromosome (mode) {
        const modeModel = mode ? [...mode] : [this.mode.SUMMARY, null];
        if (!this.currentChromosome) {
            this._isCancel = true;
            this.modeModel = modeModel;
        }
    }
}
