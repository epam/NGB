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

const READ_PERMISSION = 1;
const EDIT_PERMISSION = 1 << 1;
const checkPermission = (mask, permission) => (mask & permission) === permission;

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
        this.plainItems = [];
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
            // switch to track view
            this._clearEnvironment();
            this._currentMode = value;
            this.setCurrentName(this.currentMode);
            return;
        }
        if (this.projectContext.currentChromosome) {
            // switch from track view
            this.projectContext.changeState({chromosome: null});
        }
        if (valueMode === this.projectInfoModeList.DESCRIPTION && !this.descriptionAvailable) {
            // prevent switching to non existing state
            return;
        }
        if (value === this.currentMode && !this._isCancel) {
            // prevent previous state rewriting
            return;
        }
        if (this._hasChanges()) {
            // prevent from losing changes in edit or create modes
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
        let nameValue = PROJECT_INFO_MODE_NAME[valueMode] || this.currentNote.title;
        if (valueMode === this.projectInfoModeList.DESCRIPTION) {
            nameValue = this.currentDescription.name;
        }
        if (this.projects.length > 1) {
            if (valueMode !== undefined && (valueMode > 0 || valueId)) {
                this._currentName = `${this.currentProject.name}:${nameValue}`;
            } else {
                this._currentName = nameValue;
            }
        } else {
            this._currentName = nameValue;
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

    get descriptionIsLoading() {
        return this._descriptionIsLoading;
    }

    set descriptionIsLoading (value) {
        this._descriptionIsLoading = value;
    }

    get canEdit() {
        return checkPermission(this.currentProject.mask, READ_PERMISSION);
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
        if (
            this.projects.length === 1 &&
            this.descriptionAvailable &&
            this.descriptionList.length
        ) {
            return [
                this.projectInfoModeList.DESCRIPTION,
                this.descriptionList[0].id
            ];
        } else {
            return this.projectInfoModeList.SUMMARY;
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
            this.projectContext.downloadProjectDescription(descriptionId)
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
            this.descriptionAvailable = false;
            if (!this.projectContext.currentChromosome) {
                this.currentMode = this.projectInfoModeList.SUMMARY;
            }
        }
    }

    projectChanged() {
        const selectedDatasets = [...(new Set(findSelectedDatasets(this.projectContext.datasets || [])))];
        if (selectedDatasets.length > 1) {
            sortDatasets(selectedDatasets);
        }
        const clearURLObject = () => {
            if (this.blobUrl) {
                URL.revokeObjectURL(this.blobUrl);
                this.blobUrl = undefined;
            }
        };
        if (selectedDatasets.length > 0) {
            // project list had been (possibly) changed
            clearURLObject();
            this.projects = selectedDatasets.slice();
            this.projects.forEach(project => {
                project.canEdit = checkPermission(project.mask, READ_PERMISSION);
            });
            const currentProjectChanged = (this.currentProject && this.currentProject.id ||
                selectedDatasets.filter(dataset => dataset.id === this.currentProject.id).length === 0);
            if (currentProjectChanged) {
                if (selectedDatasets.length === 1) {
                    this.currentProject = selectedDatasets[0];
                    this._newNote = {
                        projectId: this.currentProject.id
                    };
                    this.setDescription();
                } else {
                    this.descriptionIsLoading = false;
                    this.currentProject = {};
                    this._newNote = {};
                    if (!this.projectContext.currentChromosome) {
                        this._isCancel = true;
                        this.currentMode = this.projectInfoModeList.SUMMARY;
                    }
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
        this.refreshPlainList();
    }

    refreshPlainList() {
        this.plainItems = this.projects
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
        return this._saveProject(this.currentProject, notes)
            .then(data => {
                this.currentMode = this.defaultMode;
                this.refreshPlainList();
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
                    this.refreshPlainList();
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
