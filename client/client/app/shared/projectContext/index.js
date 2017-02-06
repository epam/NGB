const Math = window.Math;

const DEFAULT_VCF_COLUMNS = ['variationType', 'chrName', 'geneNames', 'startIndex', 'info'];

export default class projectContext {

    static instance(dispatcher, genomeDataService, projectDataService) {
        return new projectContext(dispatcher, genomeDataService, projectDataService);
    }

    dispatcher;
    genomeDataService;
    projectDataService;

    _tracksStateMinificationRules = {
        bioDataItemId: 'b',
        projectId: 'p',
        height: 'h',
        state: 's',
        arrows: 'a',
        colorMode: 'c',
        coverage: 'c1',
        diffBase: 'd',
        geneTranscript: 'g',
        groupMode: 'g1',
        ins_del: 'i',
        mismatches: 'm',
        readsViewMode: 'r',
        shadeByQuality: 's1',
        softClip: 's2',
        spliceJunctions: 's3',
        variantsView: 'v',
        viewAsPairs: 'v1'
    };

    _tracksStateRevertRules = {};

    _project = null;
    _tracks = [];
    _reference = null;
    _references = null;
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
    _datasetsArePrepared = false;

    _hotkeys = null;

    get hotkeys() {
        return this._hotkeys;
    }

    set hotkeys(value) {
        this._hotkeys = value;
    }

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

    get vcfColumns() {
        if (localStorage.getItem('vcfColumns') === null || localStorage.getItem('vcfColumns') === undefined) {
            localStorage.setItem('vcfColumns', JSON.stringify(DEFAULT_VCF_COLUMNS));
        }
        return JSON.parse(localStorage.getItem('vcfColumns'));
    }

    set vcfColumns(columns) {
        localStorage.setItem('vcfColumns', JSON.stringify(columns || []));
        const oldColumns = this.vcfColumns.sort().reduce((names, name) => {
            return `${names}|${name}`;
        }, '');
        const newColumns = columns.sort().reduce((names, name) => {
            return `${names}|${name}`;
        }, '');
        if (newColumns !== oldColumns) {
            this._isVariantsInitialized = false;
        }
    }

    get tracksState() {
        if (!this.reference) {
            return null;
        }
        if (this._tracksState) {
            return JSON.parse(this._tracksState);
        }
        return [];
    }

    set tracksState(value) {
        if (!this.reference) {
            return;
        }
        if (value) {
            this._tracksState = JSON.stringify(value);
            if (this.rewriteLayout === true) {
                for (let i = 0; i < value.length; i++) {
                    this.setTrackState(value[i]);
                }
            }
        } else {
            this._tracksState = [];
        }
    }

    convertTracksStateToJson(tracksState) {
        const self = this;
        const mapFn = function(ts) {
            const converted = {};
            for (const attr in ts) {
                if (ts.hasOwnProperty(attr)) {
                    let convertedAttr = attr;
                    if (self._tracksStateMinificationRules.hasOwnProperty(attr)) {
                        convertedAttr = self._tracksStateMinificationRules[attr];
                    }
                    if (ts[attr] instanceof Object) {
                        converted[convertedAttr] = mapFn(ts[attr]);
                    } else {
                        converted[convertedAttr] = ts[attr];
                    }
                }
            }
            return converted;
        };
        const convertedState = tracksState.map(mapFn);
        return JSON.stringify(convertedState);
    }

    convertTracksStateFromJson(tracksState) {
        const self = this;
        const mapFn = function(ts) {
            const converted = {};
            for (const attr in ts) {
                if (ts.hasOwnProperty(attr)) {
                    let convertedAttr = attr;
                    if (self._tracksStateRevertRules.hasOwnProperty(attr)) {
                        convertedAttr = self._tracksStateRevertRules[attr];
                    }
                    if (ts[attr] instanceof Object) {
                        converted[convertedAttr] = mapFn(ts[attr]);
                    } else {
                        converted[convertedAttr] = ts[attr];
                    }
                }
            }
            return converted;
        };
        return JSON.parse(tracksState).map(mapFn);
    }

    getTrackState(bioDataItemId, projectId) {
        const key = `track[${bioDataItemId}][${projectId}]`;
        if (!localStorage[key]) {
            return null;
        }
        return JSON.parse(localStorage[key]);
    }

    setTrackState(track) {
        const key = `track[${track.bioDataItemId}][${track.projectId}]`;
        const state = {
            bioDataItemId: track.bioDataItemId,
            projectId: track.projectId,
            state: track.state,
            height: track.height
        };
        localStorage[key] = JSON.stringify(state);
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

    get datasetsArePrepared() {
        return this._datasetsArePrepared;
    }

    set datasetsArePrepared(value) {
        this._datasetsArePrepared = value;
    }

    _viewports = {};

    get viewports() {
        return this._viewports;
    }

    constructor(dispatcher, genomeDataService, projectDataService) {
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
        this.projectDataService = projectDataService;
        for (const attr in this._tracksStateMinificationRules) {
            if (this._tracksStateMinificationRules.hasOwnProperty(attr)) {
                this._tracksStateRevertRules[this._tracksStateMinificationRules[attr]] = attr;
            }
        }
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
        this._datasetsArePrepared = false;
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
            const result = await this.changeStateAsync(state, callbacks);
            const {chromosomeDidChange,
                layoutDidChange,
                positionDidChange,
                referenceDidChange,
                tracksStateDidChange,
                viewportDidChange} = result;
            let stateChanged = false;
            if (referenceDidChange) {
                emitEventFn('reference:change', this.getCurrentStateObject());
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
            if (tracksStateDidChange || referenceDidChange) {
                emitEventFn('tracks:state:change', this.getCurrentStateObject());
                stateChanged = true;
            }
            if (layoutDidChange) {
                dispatcher.emitSimpleEvent('layout:load', this.layout);
            }
            if (stateChanged) {
                emitEventFn('state:change', this.getCurrentStateObject());
            }
            dispatcher.emitGlobalEvent('route:change', this.getCurrentStateObject());
        })();
    }

    changeViewportState(viewportId, state, silent = false) {
        if (!viewportId) {
            this.changeState({viewport: state}, silent);
            return;
        }
        const dispatcher = this.dispatcher;
        const emitEventFn = function(...opts) {
            if (silent) {
                dispatcher.emitSilentEvent(...opts);
            } else {
                dispatcher.emitGlobalEvent(...opts);
            }
        };
        this._viewports[viewportId] = state;
        emitEventFn(`viewport:position:${viewportId}`, state);
    }

    applyTrackState(track) {
        const {bioDataItemId, height, projectId, state} = track;
        const __tracksState = this.tracksState || [];
        let [existedState] = __tracksState.filter(t => t.bioDataItemId === bioDataItemId && t.projectId === projectId);
        let index = -1;
        if (!existedState) {
            existedState = {bioDataItemId, height, projectId, state};
        } else {
            index = __tracksState.indexOf(existedState);
            Object.assign(existedState, {height, state});
        }
        if (index >= 0) {
            __tracksState[index] = existedState;
        } else {
            __tracksState.push(existedState);
        }
        this.tracksState = __tracksState;
        this.dispatcher.emitGlobalEvent('route:change', this.getCurrentStateObject());
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
        const {chromosome, position, reference, tracks, tracksState, viewport, layout, forceVariantsFilter} = state;
        const {referenceDidChange, vcfFilesChanged} = await this._changeProject(reference, tracks, tracksState);
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
        if (forceVariantsFilter || referenceDidChange || vcfFilesChanged) {
            this.filter({asDefault: true, callbacks});
        }
        if (layout) {
            this.layout = layout;
        }
        const layoutDidChange = layout ? true : false;
        return {
            chromosomeDidChange,
            layoutDidChange,
            positionDidChange,
            referenceDidChange,
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

    async _changeProject(reference, tracks, tracksState) {
        let vcfFilesChanged = false;
        let referenceDidChange = false;
        if (reference !== undefined || tracks !== undefined || tracksState !== undefined) {
            const result = await this._loadProject(reference, tracks, tracksState);
            vcfFilesChanged = result.vcfFilesChanged;
            referenceDidChange = result.referenceDidChange;
        }

        if (referenceDidChange) {
            this._currentChromosome = null;
            this._viewport = null;
            this._position = null;
        }
        if (vcfFilesChanged) {
            this._isVariantsInitialized = false;
        }

        return {referenceDidChange, vcfFilesChanged};
    }

    _changeTracksState(newTracksState) {
        if (!this.reference || !newTracksState) {
            return false;
        }
        const oldTracksStateStr = this.tracksState ? JSON.stringify(this.tracksState) : null;
        const newTracksStateStr = newTracksState ? JSON.stringify(newTracksState) : null;
        this.tracksState = newTracksState;
        return oldTracksStateStr !== newTracksStateStr;
    }

    async _changeReference(reference) {
        if (!this._references) {
            this._references = await this.genomeDataService.loadAllReference();
        }
        if (reference && reference.id === undefined && reference.name !== undefined && reference.name !== null) {
            [reference] = this._references.filter(r => r.name.toLowerCase() === reference.name.toLowerCase());
        }
        const newReferenceId = reference ? +reference.id : null;
        if (+this.referenceId !== newReferenceId) {
            this._reference = reference;
            this._chromosomes = await this._loadChromosomesForReference(this.referenceId);
            return true;
        }
        return false;
    }

    async _loadProject(reference, tracks, tracksState) {
        let referenceDidChange = false;
        const oldVcfFiles = this.vcfTracks || [];
        if (tracks || tracksState) {
            if (tracks) {
                this._tracks = tracks;
            } else if (tracksState) {
                const __tracks = [];
                const projectsIds = tracksState.reduce((ids, track) => {
                    if (ids.filter(t => t === +track.projectId).length === 0) {
                        return [...ids, +track.projectId];
                    }
                    return ids;
                }, []);
                for (let i = 0; i < projectsIds.length; i++) {
                    const _project = await this.projectDataService.getProject(projectsIds[i]);
                    if (_project) {
                        for (let j = 0; j < _project.items.length; j++) {
                            const track = _project.items[j];
                            if (tracksState.filter(t => t.bioDataItemId === track.bioDataItemId && t.projectId === projectsIds[i]).length === 1) {
                                track.projectId = projectsIds[i];
                                __tracks.push(track);
                            }
                        }
                    }
                }
                this._tracks = __tracks;
            }
            if (!reference) {
                [reference] = this._tracks.filter(t => t.format === 'REFERENCE');
            }
            referenceDidChange = await this._changeReference(reference);
            this._containsVcfFiles = this.vcfTracks.length > 0;
        } else if (tracks === null || reference === null) {
            this._tracks = [];
            referenceDidChange = await this._changeReference(null);
            this._containsVcfFiles = false;
            this._vcfInfo = [];
            this._infoFields = [];
        }
        let vcfFilesChanged = oldVcfFiles.length !== this.vcfTracks.length;
        if (!vcfFilesChanged) {
            for (let i = 0; i < oldVcfFiles.length; i++) {
                if (this.vcfTracks.filter(t => t.bioDataItemId === oldVcfFiles[i].bioDataItemId && t.projectId === oldVcfFiles[i].projectId).length === 0) {
                    vcfFilesChanged = true;
                    break;
                }
            }
        }
        return {referenceDidChange, vcfFilesChanged};
    }

    async _initializeVariants(onInit) {
        if (!this._isVariantsInitialized) {
            const {infoItems} = await this.projectDataService.getProjectsFilterVcfInfo(this.vcfTracks.map(t => t.id));
            this._vcfInfo = infoItems || [];
            this._vcfFilter = {
                additionalFilters: {},
                exons: false,
                quality: {},
                selectedGenes: [],
                selectedVcfTypes: [],
                vcfFileIds: this.vcfTracks.map(t => t.id)
            };
            this._infoFields = this.vcfColumns;
            for (let i = 0; i < DEFAULT_VCF_COLUMNS.length; i++) {
                const index = this._infoFields.indexOf(DEFAULT_VCF_COLUMNS[i]);
                if (index >= 0) {
                    this._infoFields.splice(index, 1);
                }
            }
            const notVisibleFields = this._infoFields.filter(i => this._vcfInfo.map(v => v.name).indexOf(i) === -1);
            for (let i = 0; i < notVisibleFields.length; i++) {
                const index = this._infoFields.indexOf(notVisibleFields[i]);
                if (index >= 0) {
                    this._infoFields.splice(index, 1);
                }
            }
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
        if (!this.reference) {
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
            const index1 = tracksSettings.findIndex(findIndex(file1.bioDataItemId, file1.projectId));
            const index2 = tracksSettings.findIndex(findIndex(file2.bioDataItemId, file2.projectId));

            if (index1 === index2) return 0;
            return index1 > index2 ? 1 : -1;

            function findIndex(bioDataItemId, projectId) {
                return (element) => element.bioDataItemId.toString() === bioDataItemId.toString() && element.projectId === projectId;
            }
        }
        function filterItems(projectFiles) {
            if (!tracksSettings) return projectFiles;
            return projectFiles.filter(file => {
                const [fileSettings]= tracksSettings.filter(m => m.bioDataItemId.toString() === file.bioDataItemId.toString() && m.projectId === file.projectId);
                return fileSettings !== undefined && fileSettings !== null;
            });
        }
        return filterItems(this.tracks).sort(sortItems);
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
        if (!this.reference) {
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
                    vcfFileIds: this.vcfTracks.map(t => t.id)
                };
            }
            if (infoFields) {
                this._infoFields = infoFields;
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
            if (this.reference) {
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
        const vcfFileIds = this._vcfFilter && this._vcfFilter.vcfFileIds && this._vcfFilter.vcfFileIds.length ? this._vcfFilter.vcfFileIds : this.vcfTracks.map(t => t.id);
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
            quality,
            variationTypes,
            vcfFileIds
        };
        const data = await this.projectDataService.getVcfVariationLoad(filter);

        const infoFieldsObj = {};
        this._infoFields && this._infoFields.forEach(f => infoFieldsObj[f] = null);

        const vcfTrackToProjectId = {};
        for (let i = 0; i < this.vcfTracks.length; i++) {
            const vcfTrack = this.vcfTracks[i];
            vcfTrackToProjectId[`${vcfTrack.id}`] = vcfTrack.projectId;
        }

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
                    vcfFileId: item.featureFileId,
                    projectId: vcfTrackToProjectId[`${item.featureFileId}`]
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