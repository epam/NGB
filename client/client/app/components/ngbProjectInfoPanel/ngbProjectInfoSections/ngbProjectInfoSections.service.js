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
        this._summaryAvailable = false;
        this._descriptionAvailable = false;
        this._showDescription = false;
        const projectChanged = this.projectChanged.bind(this);
        this.dispatcher.on('tracks:state:change', projectChanged);
        projectChanged();
    }

    get showDescription () {
        return this._showDescription;
    }

    set showDescription (value) {
        this._showDescription = value;
    }

    get showSummary () {
        return !this._showDescription;
    }

    set showSummary (value) {
        this._showDescription = !value;
    }

    get descriptionAvailable () {
        return this._descriptionAvailable;
    }

    get summaryAvailable () {
        return this._summaryAvailable;
    }

    get descriptionIsLoading () {
        return this._descriptionIsLoading;
    }

    projectChanged () {
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
                    this._showDescription = true;
                    this._descriptionAvailable = true;
                    this._descriptionIsLoading = false;
                    this.blobUrl = this.sce.trustAsResourceUrl(
                        URL.createObjectURL(new Blob([data], {type: 'text/html'}))
                    );
                } else {
                    this._descriptionAvailable = false;
                    this._descriptionIsLoading = false;
                    this._showDescription = false;
                    this._descriptionAvailable = false;
                }
                this.dispatcher.emitSimpleEvent('project:description:url', this.blobUrl);
            });
        } else if (projectIds.length !== 1) {
            this.currentProject = undefined;
            this._descriptionIsLoading = false;
            this._showDescription = false;
            this._descriptionAvailable = false;
            clearURLObject();
            this.dispatcher.emitSimpleEvent('project:description:url', this.blobUrl);
        }
    }
}
