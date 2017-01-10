const Math = window.Math;

export default class projectContext {

    static instance(dispatcher, genomeDataService, projectDataService) {
        return new projectContext(dispatcher, genomeDataService, projectDataService);
    }

    dispatcher;
    genomeDataService;
    projectDataService;

    _project = null;
    _tracks = [];
    _reference = null;
    _chromosomes = [];
    _currentChromosome = null;
    _position = null;
    _viewport = null;

    _vcfFilter = {};
    _infoFields = [];
    _vcfInfo = [];
    _filteredVariants = [];
    _isVariantsInitialized = false;
    _isVariantsLoading = true;
    _containsVcfFiles = true;

    _bookmarkVisibility = true;
    _rewriteLayout = true;
    _screenShotVisibility = true;
    _toolbarVisibility = true;
    _layout = null;
    _tracksState = null;
    _layoutPanels = {};

    _datasetsLoaded = false;
    _datasets = [];

    get project() {
        return this._project;
    }

    get projectId() {
        return this._project ? +this._project.id : this._project;
    }

    get referenceId() {
        return this._reference ? this._reference.id : null;
    }

    get chromosomes() {
        return this._chromosomes;
    }

    get currentChromosome() {
        return this._currentChromosome;
    }

    get position() {
        return this._position;
    }

    get viewport() {
        return this._viewport;
    }

    get tracks() {
        return this._tracks;
    }

    get vcfTracks() {
        return this._tracks.filter(track => track.format === 'VCF');
    }

    get geneTracks() {
        return this._tracks.filter(track => track.format === 'GENE');
    }

    get reference() {
        return this._reference;
    }

    get vcfFilter() {
        return this._vcfFilter;
    }

    get vcfInfo() {
        return this._vcfInfo;
    }

    get vcfInfoColumns() {
        return this._infoFields;
    }

    get filteredVariants() {
        return this._filteredVariants;
    }

    get isVariantsLoading() {
        return this._isVariantsLoading;
    }

    get containsVcfFiles() {
        return this._containsVcfFiles;
    }

    get rewriteLayout() {
        return this._rewriteLayout;
    }

    set rewriteLayout(value) {
        this._rewriteLayout = value;
    }

    get layout() {
        if (this._layout)
            return JSON.parse(this._layout);
        return JSON.parse(localStorage.getItem('goldenLayout'));
    }

    set layout(value) {
        this._layout = JSON.stringify(value);
        if (this.rewriteLayout === true) {
            localStorage.setItem('goldenLayout', this._layout);
        }
    }

    get tracksState() {
        if (!this.project) {
            return null;
        }
        if (this._tracksState) {
            return JSON.parse(this._tracksState);
        }
        return this.getTracksState(this.project.id);
    }

    set tracksState(value) {
        if (!this.project) {
            return;
        }
        if (value) {
            this._tracksState = JSON.stringify(value);
            if (this.rewriteLayout === true) {
                const removeStateFn = function(track) {
                    return {
                        bioDataItemId: track.bioDataItemId,
                        height: track.height,
                        hidden: track.hidden
                    };
                };
                const _tracksState = JSON.stringify(value.map(removeStateFn));
                localStorage.setItem(`projectId_${this.project.id}`, _tracksState);
            }
        } else {
            this._tracksState = localStorage.getItem(`projectId_${this.project.id}`);
        }
    }

    getTracksState(projectId) {
        return JSON.parse(localStorage.getItem(`projectId_${projectId}`));
    }

    get toolbarVisibility() {
        return this._toolbarVisibility;
    }

    set toolbarVisibility(value) {
        this._toolbarVisibility = value;
    }

    get bookmarkVisibility() {
        return this._bookmarkVisibility;
    }

    set bookmarkVisibility(value) {
        this._bookmarkVisibility = value;
    }

    get screenShotVisibility() {
        return this._screenShotVisibility;
    }

    set screenShotVisibility(value) {
        this._screenShotVisibility = value;
    }

    get datasetsLoaded() {
        return this._datasetsLoaded;
    }

    get datasets() {
        return this._datasets;
    }

    constructor(dispatcher, genomeDataService, projectDataService) {
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
        this.projectDataService = projectDataService;
        this.refreshDatasets();
    }

    _getVcfCallbacks() {
        const dispatcher = this.dispatcher;
        const self = this;
        const onSetToDefault = function() {
            dispatcher.emitGlobalEvent('ngbFilter:setDefault', {});
        };
        const onStart = function() {
            dispatcher.emit('ngbColumns:change', self.vcfInfoColumns);
            dispatcher.emitGlobalEvent('variants:loading:started', {});
        };
        const onFinish = function() {
            dispatcher.emitGlobalEvent('variants:loading:finished', {});
            dispatcher.emit('ngbColumns:change', self.vcfInfoColumns);
        };
        const onInit = function() {
            dispatcher.emit('variants:initialized', self.vcfInfo);
        };
        return {onFinish, onInit, onSetToDefault, onStart};
    }

    async refreshDatasets() {
        this.dispatcher.emitSimpleEvent('datasets:loading:started', null);
        this._datasets = (await this.projectDataService.getProjects() || []);
        this._datasetsLoaded = true;
        this.dispatcher.emitSimpleEvent('datasets:loading:finished', null);
    }

    changeState(state, silent = false) {
        const dispatcher = this.dispatcher;
        const callbacks = this._getVcfCallbacks();
        const emitEventFn = function(...opts) {
            if (silent) {
                dispatcher.emitSilentEvent(...opts);
            } else {
                dispatcher.emitGlobalEvent(...opts);
            }
        };
        (async() => {
            const {chromosomeDidChange,
                layoutDidChange,
                positionDidChange,
                projectDidChange,
                tracksStateDidChange,
                viewportDidChange} = await this.changeStateAsync(state, callbacks);
            let stateChanged = false;
            if (projectDidChange) {
                emitEventFn('projectId:change', this.getCurrentStateObject());
                stateChanged = true;
            }
            if (chromosomeDidChange) {
                emitEventFn('chromosome:change', this.getCurrentStateObject());
                stateChanged = true;
            }
            if (positionDidChange) {
                emitEventFn('position:select', this.getCurrentStateObject());
                stateChanged = true;
            }
            if (viewportDidChange) {
                emitEventFn('viewport:position', this.getCurrentStateObject());
                stateChanged = true;
            }
            if (tracksStateDidChange) {
                emitEventFn('tracks:state:change', this.getCurrentStateObject());
                stateChanged = true;
            }
            if (layoutDidChange) {
                dispatcher.emitSimpleEvent('layout:load', this.layout);
            }
            if (stateChanged) {
                emitEventFn('state:change', this.getCurrentStateObject());
            }
        })();
    }

    applyTrackState(track) {
        const {bioDataItemId, height, hidden, state} = track;
        const __tracksState = this.tracksState || [];
        let [existedState] = __tracksState.filter(track => track.bioDataItemId === bioDataItemId);
        let index = -1;
        if (!existedState) {
            existedState = {bioDataItemId, height, hidden, state};
        } else {
            index = __tracksState.indexOf(existedState);
            Object.assign(existedState, {height, hidden, state});
        }
        if (index >= 0) {
            __tracksState[index] = existedState;
        } else {
            __tracksState.push(existedState);
        }
        this.tracksState = __tracksState;
    }

    changeVcfInfoFields(infoFields) {
        const callbacks = this._getVcfCallbacks();
        this.filter({callbacks, infoFields});
    }

    clearVcfFilter() {
        const callbacks = this._getVcfCallbacks();
        this.filter({asDefault:true, callbacks});
    }

    filterVariants() {
        const callbacks = this._getVcfCallbacks();
        this.filter({callbacks});
    }

    async changeStateAsync(state, callbacks) {
        const {project, chromosome, position, tracksState, viewport, layout} = state;
        const projectDidChange = await this._changeProject(project);
        const tracksStateDidChange = this._changeTracksState(tracksState);
        const chromosomeDidChange = this._changeChromosome(chromosome);
        let positionDidChange = false;
        let viewportDidChange = false;
        if (viewport) {
            viewportDidChange = this._changeViewport(viewport);
            if (viewportDidChange) {
                this._position = null;
            }
        } else if (position) {
            positionDidChange = this._changePosition(position);
            if (positionDidChange) {
                this._viewport = null;
            }
        }
        if (projectDidChange) {
            this.filter({asDefault: true, callbacks});
        }
        if (layout) {
            this.layout = layout;
        }
        const layoutDidChange = layout;
        return {
            chromosomeDidChange,
            layoutDidChange,
            positionDidChange,
            projectDidChange,
            tracksStateDidChange,
            viewportDidChange
        };
    }

    getCurrentStateObject() {
        const projectId = this._project ? +this._project.id : null;
        const chromosome = this.currentChromosome ? `${this.currentChromosome.name}` : null;
        const position = this._position;
        const viewport = this._viewport;
        return {
            chromosome,
            position,
            projectId,
            viewport
        };
    }

    async _changeProject(project) {
        let projectDidChange = false;
        if (project !== undefined) {
            const projectId = (project === null || project.id === null || project.id === undefined) ? null : +project.id;
            const oldProjectId = this._project ? +this._project.id : null;
            projectDidChange = projectId !== oldProjectId;
        }
        if (projectDidChange) {
            await this._loadProject(project);
            this._isVariantsInitialized = false;
            this._currentChromosome = null;
            this._viewport = null;
            this._position = null;
        }
        return projectDidChange;
    }

    _changeTracksState(newTracksState) {
        if (!this.project || !newTracksState) {
            return false;
        }
        this.tracksState = newTracksState;
        return newTracksState !== null && newTracksState !== undefined;
    }

    async _changeReference(reference) {
        const newReferenceId = reference ? +reference.id : null;
        if (+this.referenceId !== newReferenceId) {
            this._reference = reference;
            this._chromosomes = await this._loadChromosomesForReference(this.referenceId);
        }
    }

    async _loadProject(project) {
        const projectId = (project === null || project.id === null || project.id === undefined) ? null : +project.id;
        if (projectId) {
            if (project.loaded) {
                this._project = project;
            } else {
                this._project = await this.projectDataService.getProject(projectId);
            }
        } else {
            this._project = null;
        }
        if (this._project) {
            this._tracks = this._project.items.reduce((tracks, track) => {
                if (tracks.filter(t => t.bioDataItemId === track.bioDataItemId).length === 0) {
                    return [...tracks, track];
                }
                return tracks;
            }, []);
            const [reference] = this._tracks.filter(track => track.format === 'REFERENCE');
            await this._changeReference(reference);
            this._containsVcfFiles = this.vcfTracks.length > 0;
        } else {
            this._tracks = [];
            await this._changeReference(null);
            this._containsVcfFiles = false;
            this._vcfInfo = [];
        }
    }

    async _initializeVariants(onInit) {
        if (!this._isVariantsInitialized) {
            const {infoItems} = await this.projectDataService.getProjectsFilterVcfInfo(this._project.id);
            this._vcfInfo = infoItems || [];
            this._vcfFilter = {
                additionalFilters: {},
                exons: false,
                quality: {},
                selectedGenes: [],
                selectedVcfTypes: [],
                vcfFileIds: []
            };
            if (onInit) {
                onInit();
            }
        }
        this._isVariantsInitialized = true;
    }

    async _loadChromosomesForReference(referenceId) {
        if (referenceId) {
            return await this.genomeDataService.loadAllChromosomes(referenceId);
        }
        return [];
    }

    _changeChromosome(chromosome) {
        if (chromosome === undefined) {
            return false;
        }
        const currentChromosomeId = this._currentChromosome ? +this._currentChromosome.id : null;
        this._currentChromosome = this.getChromosome(chromosome);
        const newChromosomeId = this._currentChromosome ? +this._currentChromosome.id : null;
        if (newChromosomeId !== currentChromosomeId) {
            this._viewport = null;
            this._position = null;
        }
        return newChromosomeId !== currentChromosomeId;
    }

    _changePosition(position) {
        const positionDidChange = position !== this._position;
        if (position && this._currentChromosome) {
            this._position = Math.min(Math.max(1, position), this._currentChromosome.size);
        } else {
            this._position = null;
        }
        return positionDidChange;
    }

    _changeViewport(viewport) {
        const oldViewport = this._viewport;
        if (viewport && this._currentChromosome) {
            this._viewport = {
                end: Math.min(viewport.end, this._currentChromosome.size),
                start: Math.max(viewport.start, 1)
            };
        } else if (this._currentChromosome) {
            this._viewport = {
                end: this._currentChromosome.size,
                start: 1
            };
        } else {
            this._viewport = null;
        }
        return (oldViewport === null && this._viewport !== null) ||
            (oldViewport !== null && this._viewport === null) ||
            (oldViewport !== null && this._viewport !== null && (
                (oldViewport.start !== this._viewport.start) ||
                (oldViewport.end !== this._viewport.end)
            ));
    }

    getActiveTracks() {
        if (!this.project) {
            return [];
        }
        const tracksSettings = this.tracksState;
        const referenceId = this.reference.id;
        function sortItems(file1, file2) {
            if (!tracksSettings) {
                // reference should be first;
                if (file1.id === file2.id) {
                    return 0;
                } else if (file1.id === referenceId) {
                    return -1;
                } else if (file2.id === referenceId) {
                    return 1;
                }
                return 0;
            }
            const index1 = tracksSettings.findIndex(fundIndex(file1.bioDataItemId));
            const index2 = tracksSettings.findIndex(fundIndex(file2.bioDataItemId));

            if (index1 === index2) return 0;
            return index1 > index2 ? 1 : -1;

            function fundIndex(bioDataItemId) {
                return (element) => element.bioDataItemId.toString() === bioDataItemId.toString();
            }
        }
        function filterItems(projectFiles) {
            if (!tracksSettings) return projectFiles;

            return projectFiles.filter(file => {
                const [fileSettings ]= tracksSettings.filter(m => m.bioDataItemId.toString() === file.bioDataItemId.toString());
                return fileSettings && fileSettings.hidden !== true;
            });
        }
        return filterItems(this.tracks)
            .map(m => {
                m.hidden = false;
                return m;
            })
            .sort(sortItems);
    }

    getChromosome(chromosome) {
        if (!chromosome) {
            return null;
        }
        const {id, name} = chromosome;
        const findByIdFn = function (c) {
            return c.id === +id;
        };
        const prefix = 'chr';
        const chromosomesAreEqual = (chr1, chr2) => {
            if (!chr1 || !chr2)
                return false;
            chr1 = chr1.toLowerCase();
            chr2 = chr2.toLowerCase();
            if (chr1.startsWith(prefix)) {
                chr1 = chr1.substr(prefix.length);
            }
            if (chr2.startsWith(prefix)) {
                chr2 = chr2.substr(prefix.length);
            }
            return chr1 === chr2;
        };
        const findByNameFn = function (c) {
            return chromosomesAreEqual(c.name, name);
        };
        const filterFn = id ? findByIdFn : findByNameFn;
        let [findResult] = this._chromosomes.filter(filterFn);
        if (findResult === undefined) {
            findResult = null;
        }
        return findResult;
    }

    filter(opts) {
        if (!this._project) {
            return;
        }
        this._isVariantsLoading = true;
        (async() => {
            const {infoFields, asDefault, callbacks} = opts;
            const {onError, onFinish, onInit, onSetToDefault, onStart} = callbacks;
            if (onStart) {
                onStart();
            }
            await this._initializeVariants(onInit);
            if (asDefault) {
                this._vcfFilter = {
                    additionalFilters: {},
                    exons: false,
                    quality: {},
                    selectedGenes: [],
                    selectedVcfTypes: [],
                    vcfFileIds: []
                };
            }
            if (infoFields) {
                this._infoFields = infoFields;
            } else {
                this._infoFields = [];
            }
            await this.__filterVariants(onError);
            this._isVariantsLoading = false;
            if (onFinish) {
                onFinish();
            }
            if (asDefault && onSetToDefault) {
                onSetToDefault();
            }
        })();
    }

    async __filterVariants(errorCallback) {
        try {
            if (this._project) {
                this._filteredVariants = await this._loadVariations();
            } else {
                this._filteredVariants = [];
            }
            this._isVariantsLoading = false;
        }
        catch (errorObj) {
            this._isVariantsLoading = false;
            if (errorCallback) {
                errorCallback(errorObj);
            }
        }
    }

    async _loadVariations() {
        const projectId = this._project.id;
        const vcfFileIds = this._vcfFilter ? this._vcfFilter.vcfFileIds : [];
        const quality = this._vcfFilter ? this._vcfFilter.quality.value : [];
        const exon = (this._vcfFilter && (this._vcfFilter.exons !== undefined)) ? this._vcfFilter.exons : false;
        const genes = {
            conjunction: false,
            field: this._vcfFilter ? this._vcfFilter.selectedGenes : []
        };
        const variationTypes = {
            conjunction: false,
            field: this._vcfFilter ? this._vcfFilter.selectedVcfTypes : []
        };
        const additionalFilters = this._vcfFilter ? this._vcfFilter.additionalFilters : {};
        const infoFields = this._infoFields || [];

        const filter = {
            additionalFilters,
            exon,
            genes,
            infoFields,
            projectId,
            quality,
            variationTypes,
            vcfFileIds
        };
        const data = await this.projectDataService.getVcfVariationLoad(filter);

        const infoFieldsObj = {};
        this._infoFields && this._infoFields.forEach(f => infoFieldsObj[f] = null);

        return data.map(item =>
            Object.assign({},
                {
                    chrId: item.chromosome.id,
                    chrName: item.chromosome.name,
                    chromosome: {
                        id: item.chromosome.id,
                        name: item.chromosome.name
                    },
                    endIndex: item.endIndex,
                    geneNames: item.geneNames,
                    quality: item.quality,
                    startIndex: item.startIndex,
                    variantId: item.featureId,
                    variationType: item.variationType,
                    vcfFileId: item.featureFileId
                },
                {...infoFieldsObj, ...item.info}
            )
        );
    }

    get layoutPanels() {
        return this._layoutPanels;
    }

    openPanel(panel) {
        this._layoutPanels[panel] = true;
    }

    closePanel(panel) {
        this._layoutPanels[panel] = false;
    }
}