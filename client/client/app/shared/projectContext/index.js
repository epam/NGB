import {utilities} from '../../../app/components/ngbDataSets/internal';

const Math = window.Math;

const DEFAULT_VCF_COLUMNS = ['variationType', 'chrName', 'geneNames', 'startIndex', 'info'];
const DEFAULT_ORDERBY_VCF_COLUMNS = {
    'variationType': 'VARIATION_TYPE',
    'chrName': 'CHROMOSOME_NAME',
    'geneNames': 'GENE_NAME',
    'startIndex': 'START_INDEX'
};
const FIRST_PAGE = 1;
const PAGE_SIZE = 50;
const FIRST_CHROMOSOME_SELECTOR = '{first-chromosome}';
const REFERENCE_TRACK_SELECTOR = '{ref}';
const GENOME_TRACKS_SELECTOR = '{ref_genes}';
const PREDEFINED_TRACKS_SELECTORS = [REFERENCE_TRACK_SELECTOR, GENOME_TRACKS_SELECTOR];

export default class projectContext {

    static instance(dispatcher, genomeDataService, projectDataService, utilsDataService) {
        return new projectContext(dispatcher, genomeDataService, projectDataService, utilsDataService);
    }

    dispatcher;
    genomeDataService;
    projectDataService;
    utilsDataService;

    _tracksStateMinificationRules = {
        bioDataItemId: 'b',
        name: 'n',
        projectId: 'p',
        height: 'h',
        format: 'f',
        index: 'i1',
        isLocal: 'l',
        state: 's',
        alignments: 'aa',
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
        viewAsPairs: 'v1',
        coverageScaleMode: 'csm',
        coverageLogScale: 'cls',
        coverageScaleFrom: 'csf',
        coverageScaleTo: 'cst'
    };

    _tracksStateRevertRules = {};

    _tracks = [];
    _reference = null;
    _referenceIsPromised = false; // should be set to 'true' if reference requested, but not loaded yet
    _references = null;
    _chromosomes = [];
    _currentChromosome = null;
    _position = null;
    _viewport = null;

    _vcfFilter = {};
    _vcfFilterIsDefault = true;
    _infoFields = [];
    _vcfInfo = [];
    _filteredVariants = [];
    _variantsDataByChromosomes = [];
    _variantsDataByType = [];
    _variantsDataByQuality = [];
    _isVariantsInitialized = false;
    _isVariantsLoading = true;
    _isVariantsGroupByChromosomesLoading = true;
    _isVariantsGroupByTypeLoading = true;
    _isVariantsGroupByQualityLoading = true;
    _variantsGroupByChromosomesError = null;
    _variantsPageLoading = false;
    _variantsGroupByTypeError = null;
    _variantsGroupByQualityError = null;
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
    _datasetsFilter = null;

    _hotkeys = null;

    _firstPageVariations = FIRST_PAGE;
    _lastPageVariations = FIRST_PAGE;
    _currentPageVariations = FIRST_PAGE;
    _hasMoreVariations = true;
    _orderByVariations = null;
    _totalPagesCountVariations = null;

    _browsingAllowed = false;

    _ngbDefaultSettings;

    _lastLocalTracks = [];
    _localDataset = null;

    get firstPageVariations() {
        return this._firstPageVariations;
    }

    set firstPageVariations(value) {
        this._firstPageVariations = value;
    }

    get lastPageVariations() {
        return this._lastPageVariations;
    }

    set lastPageVariations(value) {
        this._lastPageVariations = value;
    }

    set orderByVariations(value) {
        this._orderByVariations = value;
    }

    get orderByVariations() {
        return this._orderByVariations;
    }

    get orderByColumnsVariations() {
        return DEFAULT_ORDERBY_VCF_COLUMNS;
    }

    get totalPagesCountVariations() {
        return this._totalPagesCountVariations;
    }

    set totalPagesCountVariations(value) {
        this._totalPagesCountVariations = value;
    }
    get variationsPageSize() {
        return PAGE_SIZE;
    }

    set currentPageVariations(value) {
        this._currentPageVariations = value;
    }

    get currentPageVariations() {
        return this._currentPageVariations;
    }

    get hasMoreVariations() {
        return this._hasMoreVariations;
    }

    get hotkeys() {
        return this._hotkeys;
    }

    set hotkeys(value) {
        this._hotkeys = value;
    }

    get references() {
        return this._references || [];
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
        return this._tracks.filter(track => !track.isLocal && track.format === 'VCF');
    }

    get geneTracks() {
        return this._tracks.filter(track => !track.isLocal && track.format === 'GENE');
    }

    get lastLocalTracks() {
        return this._lastLocalTracks;
    }

    get referenceIsPromised() {
        return this._referenceIsPromised;
    }

    get reference() {
        return this._reference;
    }

    get vcfFilterIsDefault() {
        return this._vcfFilterIsDefault;
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

    get variantsDataByChromosomes() {
        return this._variantsDataByChromosomes;
    }

    get variantsDataByType() {
        return this._variantsDataByType;
    }

    get variantsDataByQuality() {
        return this._variantsDataByQuality;
    }

    get isVariantsLoading() {
        return this._isVariantsLoading;
    }

    get isVariantsGroupByChromosomesLoading() {
        return this._isVariantsGroupByChromosomesLoading;
    }

    get isVariantsGroupByTypeLoading() {
        return this._isVariantsGroupByTypeLoading;
    }

    get isVariantsGroupByQualityLoading() {
        return this._isVariantsGroupByQualityLoading;
    }

    get variantsGroupByChromosomesError() {
        return this._variantsGroupByChromosomesError;
    }

    get variantsGroupByTypeError() {
        return this._variantsGroupByTypeError;
    }

    get variantsGroupByQualityError() {
        return this._variantsGroupByQualityError;
    }

    get variantsGroupError() {
        return this.variantsGroupByChromosomesError ||
            this.variantsGroupByQualityError ||
            this.variantsGroupByTypeError;
    }

    get variantsPageLoading () {
        return this._variantsPageLoading;
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

    getTrackState(name, projectId) {
        const key = `track[${name.toLowerCase()}][${projectId.toLowerCase()}]`;
        if (!localStorage[key]) {
            return null;
        }
        return JSON.parse(localStorage[key]);
    }

    setTrackState(track) {
        const name = track.name ? track.name.toLowerCase() : track.bioDataItemId.toString().toLowerCase();
        const key = `track[${name}][${track.projectId.toLowerCase()}]`;
        const state = {
            bioDataItemId: name,
            projectId: track.projectId,
            state: track.state,
            height: track.height,
            isLocal: track.isLocal
        };
        localStorage[key] = JSON.stringify(state);
    }

    applyAnnotationTracksState() {
        if (this.reference === null) {
            return;
        }
        const stateStr = localStorage[`${this.reference.name.toLowerCase()}-annotations`];
        if (!stateStr) {
            return;
        }
        const state = JSON.parse(stateStr);
        if (!this.reference.annotationFiles) {
            this.reference.annotationFiles = [];
        } else if (this.reference.geneFile && this.reference.annotationFiles.filter(af => af.format === 'GENE' && af.name.toLowerCase() === this.reference.geneFile.name.toLowerCase()).length === 0) {
            this.reference.annotationFiles.push(this.reference.geneFile);
        }
        if (state && this.reference.annotationFiles) {
            this.reference.annotationFiles.forEach(f => {
                f.selected = state.indexOf(f.name.toLowerCase()) !== -1;
            });
            const sortFn = (file1, file2) => {
                if (file1.selected !== file2.selected) {
                    if (file1.selected) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
                const index1 = state.indexOf(file1.name.toLowerCase());
                const index2 = state.indexOf(file2.name.toLowerCase());
                if (index1 < index2) {
                    return -1;
                } else if (index1 > index2) {
                    return 1;
                }
                return 0;
            };
            this.reference.annotationFiles.sort(sortFn);
        }
    }

    saveAnnotationFilesState() {
        if (!this.reference || !this.reference.annotationFiles) {
            return;
        }
        const state = this.reference.annotationFiles.filter(f => f.selected).map(f => f.name.toLowerCase());
        localStorage[`${this.reference.name.toLowerCase()}-annotations`] = JSON.stringify(state);
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

    get browsingAllowed() {
        return this._browsingAllowed;
    }

    get ngbDefaultSettings() {
        return this._ngbDefaultSettings;
    }

    get browserHomePageUrl() {
        if (this.ngbDefaultSettings && this.ngbDefaultSettings['home'] && this.ngbDefaultSettings['home'].url) {
            return this.ngbDefaultSettings['home'].url;
        }
        return null;
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

    get datasetsFilter() {
        return this._datasetsFilter;
    }

    set datasetsFilter(value) {
        this._datasetsFilter = value;
    }

    _viewports = {};

    get viewports() {
        return this._viewports;
    }

    _collapsedTrackHeaders = undefined;

    get collapsedTrackHeaders() {
        return this._collapsedTrackHeaders;
    }

    set collapsedTrackHeaders(value) {
        if (value === null) {
            value = undefined;
        }
        if (value && typeof value === 'string') {
            value = value.toLowerCase() === 'true';
        }
        const settingChanged = this._collapsedTrackHeaders !== value;
        if (settingChanged) {
            this._collapsedTrackHeaders = value;
            this.dispatcher.emitGlobalEvent('track:headers:changed', value);
        }
    }

    _displayVariantsFilter;

    get displayVariantsFilter() {
        if (this._displayVariantsFilter !== undefined) {
            return this._displayVariantsFilter;
        } else {
            this._displayVariantsFilter = JSON.parse(localStorage.getItem('displayVariantsFilter')) || false;
            return this._displayVariantsFilter;
        }
    }

    setDisplayVariantsFilter(value, updateScope = true) {
        if (value !== this._displayVariantsFilter) {
            this._displayVariantsFilter = value;
            localStorage.setItem('displayVariantsFilter', JSON.stringify(value));
            this.dispatcher.emitSimpleEvent('display:variants:filter', updateScope);
        }
    }

    constructor(dispatcher, genomeDataService, projectDataService, utilsDataService) {
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
        this.projectDataService = projectDataService;
        this.utilsDataService = utilsDataService;
        for (const attr in this._tracksStateMinificationRules) {
            if (this._tracksStateMinificationRules.hasOwnProperty(attr)) {
                this._tracksStateRevertRules[this._tracksStateMinificationRules[attr]] = attr;
            }
        }
        (async () => {
            await this.refreshBrowsingAllowedStatus();
            await this.getDefaultTrackSettings();
            await this.refreshReferences();
        })();
        this.initEvents();
    }

    initEvents() {
        const resetVariantsFilterFn = ::this.resetVariantsFilter;
        const hotkeyPressedFn = ::this.hotkeyPressed;
        this.dispatcher.on('variants:reset:filter', resetVariantsFilterFn);
        this.dispatcher.on('hotkeyPressed', hotkeyPressedFn);
    }

    hotkeyPressed(event) {
        if (event === 'layout>filter') {
            this.setDisplayVariantsFilter(!this.displayVariantsFilter, true);
        }
    }

    resetVariantsFilter() {
        if (!this.vcfFilterIsDefault) {
            this.clearVcfFilter();
        }
    }

    async refreshBrowsingAllowedStatus() {
        this._browsingAllowed = await this.utilsDataService.getFilesAllowed();
    }

    async getDefaultTrackSettings() {
        this._ngbDefaultSettings = projectContext.analyzeTrackSettings(await this.utilsDataService.getDefaultTrackSettings());
    }

    getTrackDefaultSettings(format) {
        if (this.ngbDefaultSettings) {
            return this.ngbDefaultSettings[format.toLowerCase()];
        }
        return undefined;
    }

    static analyzeTrackSettings(settings) {
        for (const key in settings) {
            if (settings.hasOwnProperty(key) && settings[key] !== undefined) {
                switch ((typeof settings[key]).toLowerCase()) {
                    case 'object': settings[key] = projectContext.analyzeTrackSettings(settings[key]); break;
                    case 'string': {
                        if (settings[key].indexOf('#') === 0) {
                            settings[key] = parseInt(`0x${settings[key].substring(1)}`);
                        }
                    } break;
                }
            }
        }
        return settings;
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
        const groupByChromosome = {
            started: () => dispatcher.emitGlobalEvent('variants:group:chromosome:started'),
            finished: () => dispatcher.emitGlobalEvent('variants:group:chromosome:finished')
        };
        const groupByType = {
            started: () => dispatcher.emitGlobalEvent('variants:group:type:started'),
            finished: () => dispatcher.emitGlobalEvent('variants:group:type:finished')
        };
        const groupByQuality = {
            started: () => dispatcher.emitGlobalEvent('variants:group:quality:started'),
            finished: () => dispatcher.emitGlobalEvent('variants:group:quality:finished')
        };
        const groupByCallbacks = {groupByChromosome, groupByType, groupByQuality};
        return {onFinish, onInit, onSetToDefault, onStart, groupByCallbacks};
    }

    _datasetsAreLoading = false;

    get datasetsAreLoading() {
        return this._datasetsAreLoading;
    }

    refreshDatasetsPromise;

    async refreshDatasets() {
        if (this._datasetsAreLoading) {
            return this.refreshDatasetsPromise;
        }
        this._datasetsAreLoading = true;
        this._datasetsArePrepared = false;
        this.dispatcher.emitSimpleEvent('datasets:loading:started', null);
        this.refreshDatasetsPromise = new Promise((resolve) => {
            this.projectDataService.getProjects(this._datasetsFilter).then(data => {
                this._datasets = data;
                if (this._localDataset) {
                    this._datasets.push(this._localDataset);
                }
                this._datasetsLoaded = true;
                this._datasetsAreLoading = false;
                this.dispatcher.emitSimpleEvent('datasets:loading:finished', null);
                resolve();
            });
        });
        return this.refreshDatasetsPromise;
    }

    addLastLocalTrack(track) {
        if (this.lastLocalTracks.filter(t => t.name.toLowerCase() == track.name.toLowerCase()).length === 0) {
            this.lastLocalTracks.push(track);
            this._lastLocalTracksUpdated = true;
        }
    }

    refreshLocalDatasets() {
        if (this._lastLocalTracksUpdated) {
            this._lastLocalTracksUpdated = false;
            const localDataset = {
                name: 'Not registered files',
                id: 0,
                items: [],
                nestedProjects: [],
                isLocal: true
            };
            const referencesNames = this.lastLocalTracks.reduce((names, track) => {
                if (names.filter(t => t === track.projectId).length === 0) {
                    return [...names, track.projectId];
                }
                return names;
            }, []);
            localDataset.nestedProjects = [];
            for (let i = 0; i < referencesNames.length; i++) {
                const [reference] = this._references.filter(r => r.name.toLowerCase() === referencesNames[i].toLowerCase());
                if (reference) {
                    const systemTracks = [];
                    if (this.lastLocalTracks.filter(t => t.format === 'REFERENCE').length === 0) {
                        reference.isLocal = true;
                        reference.projectId = reference.name;
                        systemTracks.push(reference);
                    }
                    const nestedProject = {
                        name: referencesNames[i],
                        id: 0,
                        reference: reference,
                        items: [...systemTracks, ...this.lastLocalTracks.filter(t => t.projectId.toLowerCase() === referencesNames[i].toLowerCase())],
                        nestedProjects: null,
                        isLocal: true
                    };
                    localDataset.nestedProjects.push(nestedProject);
                }
            }
            this._localDataset = utilities.preprocessNode(localDataset);
            const [__localDataset] = this.datasets.filter(d => d.isLocal);
            if (__localDataset) {
                const index = this.datasets.indexOf(__localDataset);
                this.datasets.splice(index, 1);
            }

            this._datasets.push(this._localDataset);

            return true;
        }
        return false;
    }

    refreshReferencesPromise;
    _referencesAreLoading = false;

    async refreshReferences(forceRefresh = false) {
        if (forceRefresh || !this._references) {
            if (this._referencesAreLoading) {
                return this.refreshReferencesPromise;
            }
            this._referencesAreLoading = true;
            this.refreshReferencesPromise = new Promise((resolve) => {
                this.genomeDataService.loadAllReference(this._datasetsFilter).then(data => {
                    this._references = data;
                    this._references.forEach(r => {
                        if (!r.annotationFiles) {
                            r.annotationFiles = [];
                        }
                        if (r.geneFile && r.annotationFiles.filter(a =>
                                a.name.toLowerCase() === r.geneFile.name.toLowerCase() && a.format.toLowerCase() === r.geneFile.format.toLowerCase()
                            ).length === 0) {
                            r.geneFile.selected = true;
                            r.geneFile.isGeneFile = true;
                            r.annotationFiles.push(r.geneFile);
                        }
                    });
                    this._referencesAreLoading = false;
                    resolve();
                });
            });
            return this.refreshReferencesPromise;
        }
    }

    findDatasetByName(name) {
        if (!name || (typeof name) !== 'string') return null;
        const findFn = (items) => {
            for (let i = 0; i < items.length; i++) {
                const item = items[i];
                if (item.name && item.name.toLowerCase() === name.toLowerCase()) {
                    return item;
                } else if (item.nestedProjects && item.nestedProjects.length) {
                    const result = findFn(item.nestedProjects);
                    if (result) {
                        return result;
                    }
                }
            }
            return null;
        };
        return findFn(this._datasets);
    }

    changeState(state, silent = false) {
        const callbacks = this._getVcfCallbacks();
        const emitEventFn = (...opts) => {
            if (!silent) {
                this.dispatcher.emitGlobalEvent(...opts);
            }
        };
        (async() => {
            const result = await this.changeStateAsync(state, callbacks);
            const {
                chromosomeDidChange,
                datasetsFilterChanged,
                datasetsUpdated,
                layoutDidChange,
                positionDidChange,
                referenceDidChange,
                tracksStateDidChange,
                viewportDidChange
            } = result;
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
                this.dispatcher.emitSimpleEvent('layout:load', this.layout);
            }
            if (!datasetsUpdated && datasetsFilterChanged) {
                this.dispatcher.emitGlobalEvent('datasets:filter:changed', this.datasetsFilter);
            }
            if (datasetsUpdated) {
                this.dispatcher.emitSimpleEvent('datasets:loading:finished', null);
            }
            if (stateChanged) {
                emitEventFn('state:change', this.getCurrentStateObject());
            }
            this.dispatcher.emitGlobalEvent('route:change', this.getCurrentStateObject());
        })();
    }

    changeViewportState(viewportId, state, silent = false) {
        if (!viewportId) {
            this.changeState({viewport: state}, silent);
            return;
        }
        this._viewports[viewportId] = state;
        if (!silent) {
            this.dispatcher.emitGlobalEvent(`viewport:position:${viewportId}`, state);
        }
    }

    applyTrackState(track, silent = false) {
        const {bioDataItemId, index, isLocal, format, height, projectId, state} = track;
        const __tracksState = this.tracksState || [];
        let [existedState] = __tracksState.filter(t => t.bioDataItemId.toString().toLowerCase() === bioDataItemId.toString().toLowerCase() &&
        t.projectId.toLowerCase() === projectId.toLowerCase());
        let elementIndex = -1;
        if (!existedState) {
            existedState = {bioDataItemId, index, isLocal, height, format, projectId, state};
        } else {
            elementIndex = __tracksState.indexOf(existedState);
            Object.assign(existedState, {height, state});
        }
        if (elementIndex >= 0) {
            __tracksState[elementIndex] = existedState;
        } else {
            __tracksState.push(existedState);
        }
        this.tracksState = __tracksState;
        if (!silent) {
            this.submitTracksStates();
        }
    }

    submitTracksStates() {
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

    refreshVcfFilterEmptyStatus() {
        const additionalFiltersAreEmpty = projectContext.propertyIsEmpty(this.vcfFilter.additionalFilters);
        const selectedChromosomesIsEmpty = !this.vcfFilter.chromosomeIds || !this.vcfFilter.chromosomeIds.length;
        const selectedGenesIsEmpty = !this.vcfFilter.selectedGenes || !this.vcfFilter.selectedGenes.length;
        const selectedVcfTypesIsEmpty = !this.vcfFilter.selectedVcfTypes || !this.vcfFilter.selectedVcfTypes.length;
        const positionIsEmpty = this.vcfFilter.startIndex === undefined && this.vcfFilter.endIndex === undefined;
        this._vcfFilterIsDefault = additionalFiltersAreEmpty && selectedChromosomesIsEmpty && selectedGenesIsEmpty && selectedVcfTypesIsEmpty && positionIsEmpty;
    }

    variantsFieldIsFiltered(fieldName) {
        let result = false;
        switch (fieldName) {
            case 'variationType':
                result = this.vcfFilter.selectedVcfTypes && this.vcfFilter.selectedVcfTypes.length;
                break;
            case 'geneNames':
                result = this.vcfFilter.selectedGenes && this.vcfFilter.selectedGenes.length;
                break;
            case 'chrName':
                result = this.vcfFilter.chromosomeIds && this.vcfFilter.chromosomeIds.length;
                break;
            case 'startIndex':
                result = this.vcfFilter.startIndex !== undefined || this.vcfFilter.endIndex !== undefined;
                break;
            default: {
                result = this.vcfFilter.additionalFilters && this.vcfFilter.additionalFilters[fieldName] !== undefined;
            }
        }
        return result;
    }

    clearVariantFieldFilter(fieldName) {
        switch (fieldName) {
            case 'variationType':
                this.vcfFilter.selectedVcfTypes = [];
                break;
            case 'geneNames':
                this.vcfFilter.selectedGenes = [];
                break;
            case 'chrName':
                this.vcfFilter.chromosomeIds = [];
                break;
            case 'startIndex': {
                this.vcfFilter.startIndex = undefined;
                this.vcfFilter.endIndex = undefined;
            } break;
            default: {
                if (this.vcfFilter.additionalFilters) {
                    this.vcfFilter.additionalFilters[fieldName] = undefined;
                }
            }
        }
        this.filterVariants();
    }

    static propertyIsEmpty(property) {
        for (const key in property) {
            if (property.hasOwnProperty(key) && property[key] !== undefined) {
                return false;
            }
        }
        return true;
    }

    async changeStateAsync(state, callbacks) {
        const {
            chromosome,
            position,
            reference,
            tracks,
            tracksState,
            viewport,
            layout,
            forceVariantsFilter,
            tracksReordering,
            filterDatasets,
            shouldAddAnnotationTracks} = state;
        if (reference && !this._reference) {
            this._referenceIsPromised = true;
            this.dispatcher.emitGlobalEvent('reference:pre:change');
        }
        if (tracksState) {
            tracksState.forEach(ts => {
                if (ts.bioDataItemId === undefined) {
                    return;
                }
                if (typeof ts.bioDataItemId === 'string') {
                    ts.bioDataItemId = decodeURIComponent(ts.bioDataItemId);
                }
                if (typeof ts.index === 'string') {
                    ts.index = decodeURIComponent(ts.index);
                }
            });
        }
        const {referenceDidChange, vcfFilesChanged, recoveredTracksState} = await this._changeProject(reference, tracks, tracksState, tracksReordering, shouldAddAnnotationTracks);
        const tracksStateDidChange = await this._changeTracksState(recoveredTracksState || tracksState);
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
        const datasetsFilterChanged = this._datasetsFilter !== filterDatasets;
        const datasetsUpdated = this.refreshLocalDatasets();
        if (filterDatasets) {
            this._datasetsFilter = filterDatasets;
        }
        const layoutDidChange = layout ? true : false;
        return {
            chromosomeDidChange,
            datasetsFilterChanged,
            datasetsUpdated,
            layoutDidChange,
            positionDidChange,
            referenceDidChange,
            tracksStateDidChange,
            viewportDidChange
        };
    }

    getCurrentStateObject() {
        const referenceId = this._reference ? +this._reference.id : null;
        const chromosome = this.currentChromosome ? `${this.currentChromosome.name}` : null;
        const position = this._position;
        const viewport = this._viewport;
        return {
            chromosome,
            position,
            referenceId,
            viewport
        };
    }

    async _changeProject(reference, tracks, tracksState, tracksReordering, shouldAddAnnotationTracks) {
        let vcfFilesChanged = false;
        let referenceDidChange = false;
        let recoveredTracksState = undefined;
        if (reference !== undefined || tracks !== undefined || tracksState !== undefined) {
            const result = await this._loadProject(reference, tracks, tracksState, tracksReordering, shouldAddAnnotationTracks);
            vcfFilesChanged = result.vcfFilesChanged;
            referenceDidChange = result.referenceDidChange;
            recoveredTracksState = result.recoveredTracksState;
        }

        if (referenceDidChange) {
            this._currentChromosome = null;
            this._viewport = null;
            this._position = null;
        }
        if (vcfFilesChanged) {
            this._isVariantsInitialized = false;
        }

        return {referenceDidChange, vcfFilesChanged, recoveredTracksState};
    }

    async recoverTracksState(tracksState) {
        const projectsIds = tracksState.reduce((ids, track) => {
            if (!track.isLocal && ids.filter(t => t === track.projectId).length === 0) {
                return [...ids, track.projectId];
            }
            return ids;
        }, []);

        const recoverTracksStateFn = async () => {
            for (let i = 0; i < projectsIds.length; i++) {
                const _project = this.findDatasetByName(projectsIds[i]) || await this.projectDataService.getProject(+projectsIds[i]);
                if (_project) {
                    const _projectName = _project.name.toLowerCase();
                    const _projectId = _project.id.toString().toLowerCase();
                    const items = _project._lazyItems ? _project._lazyItems : _project.items;
                    for (let j = 0; j < items.length; j++) {
                        const track = items[j];
                        if (track.name && track.format) {
                            const [state] = tracksState.filter(t =>
                            (t.bioDataItemId.toString().toLowerCase() === track.name.toLowerCase() ||
                            t.bioDataItemId.toString().toLowerCase() === track.bioDataItemId.toString().toLowerCase()) &&
                            (t.projectId.toString().toLowerCase() === _projectName ||
                            t.projectId.toString().toLowerCase() === _projectId));
                            if (state) {
                                const index = tracksState.indexOf(state);
                                state.bioDataItemId = track.name;
                                tracksState.splice(index, 1, state);
                            }
                        }
                    }
                    tracksState.forEach(ts => {
                        if (ts.projectId === projectsIds[i]) {
                            ts.projectId = _project.name;
                        }
                    });
                }
            }
        };

        if (!this._datasets || this._datasets.length === 0) {
            await this.refreshDatasets();
        }
        await recoverTracksStateFn();
        return tracksState;
    }

    async _changeTracksState(newTracksState) {
        if (!this.reference || !newTracksState) {
            return false;
        }
        newTracksState = await this.recoverTracksState(newTracksState);
        const oldTracksStateStr = this.tracksState ? JSON.stringify(this.tracksState) : null;
        const newTracksStateStr = newTracksState ? JSON.stringify(newTracksState) : null;
        this.tracksState = newTracksState;
        return oldTracksStateStr !== newTracksStateStr;
    }

    async _changeReference(reference, shouldAddAnnotationTracks) {
        await this.refreshReferences();
        if (this._tracks) {
            // mapping reference track
            this._tracks = this._tracks.map(t => {
                if (t.format === 'REFERENCE' && t.isLocal) {
                    const [ref] = this._references.filter(r => r.name.toLowerCase() === t.name.toString().toLowerCase());
                    ref.projectId = t.projectId;
                    ref.isLocal = true;
                    return ref;
                }
                return t;
            });
        }
        if (reference && reference.id === undefined && reference.name !== undefined && reference.name !== null) {
            [reference] = this._references.filter(r => r.name.toLowerCase() === reference.name.toLowerCase());
        } else if (reference && reference.id !== undefined && reference.id !== null) {
            [reference] = this._references.filter(r => r.id === reference.id);
        }
        if (this._tracks && reference) {
            // mapping reference track
            const [systemReference] = this._references.filter(r => r.id === reference.id);
            // mapping annotation tracks
            if (systemReference && systemReference.annotationFiles) {
                for (let i = 0; i < systemReference.annotationFiles.length; i++) {
                    const annotationFile = systemReference.annotationFiles[i];
                    if (!shouldAddAnnotationTracks) {
                        annotationFile.selected = false;
                    }
                    this._tracks = this._tracks.map(t => {
                        if (t.format === annotationFile.format && t.name.toLowerCase() === annotationFile.name.toLowerCase()) {
                            annotationFile.projectId = '';
                            annotationFile.isLocal = true;
                            if (!shouldAddAnnotationTracks) {
                                annotationFile.selected = true;
                            }
                            return annotationFile;
                        }
                        return t;
                    });
                }
            }
            this.saveAnnotationFilesState();
        }
        const newReferenceId = reference ? +reference.id : null;
        if (+this.referenceId !== newReferenceId) {
            this._reference = reference;
            this.applyAnnotationTracksState();
            this._referenceIsPromised = false;
            this._chromosomes = await this._loadChromosomesForReference(this.referenceId);
            return true;
        }
        return false;
    }

    async _loadProject(reference, tracks, tracksState, tracksReordering, shouldAddAnnotationTracks) {
        let referenceDidChange = false;
        const oldVcfFiles = this.vcfTracks || [];
        if (!reference && !tracks && tracksState && tracksReordering) {
            return {referenceDidChange, vcfFilesChanged: false, recoveredTracksState: undefined};
        }
        if (tracks || tracksState) {
            if (tracks) {
                this._tracks = tracks;
            } else if (tracksState) {
                await this.refreshReferences();
                const mapOpenByUrlTracksFn = (trackState) => {
                    return {
                        id: decodeURIComponent(trackState.bioDataItemId),
                        indexPath: decodeURIComponent(trackState.index),
                        bioDataItemId: decodeURIComponent(trackState.bioDataItemId),
                        projectId: trackState.projectId,
                        name: decodeURIComponent(trackState.bioDataItemId),
                        isLocal: trackState.isLocal,
                        format: trackState.format,
                        openByUrl: true
                    };
                };
                const __tracks = tracksState.filter(ts => ts.isLocal).map(mapOpenByUrlTracksFn);
                __tracks.forEach(t => {
                    const [__reference] = this._references.filter(r => r.name.toLowerCase() === t.projectId.toLowerCase());
                    if (__reference) {
                        t.reference = __reference;
                    }
                });
                const projectsIds = tracksState.reduce((ids, track) => {
                    if (!track.isLocal && ids.filter(t => t === track.projectId).length === 0) {
                        return [...ids, track.projectId];
                    }
                    return ids;
                }, []);
                const allTracksFromProjectIds = tracksState.filter(ts => ts.bioDataItemId === undefined).reduce((ids, track) => {
                    if (!track.isLocal && ids.filter(t => t === track.projectId).length === 0) {
                        return [...ids, track.projectId];
                    }
                    return ids;
                }, []);
                const predefinedTracksFromProjectIds = tracksState.filter(ts => ts.bioDataItemId !== undefined && PREDEFINED_TRACKS_SELECTORS.indexOf(ts.bioDataItemId.toLowerCase()) >= 0).reduce((ids, track) => {
                    if (!track.isLocal && ids.filter(t => t === track.projectId).length === 0) {
                        return [...ids, track.projectId];
                    }
                    return ids;
                }, []);
                if (!this._datasets || this._datasets.length === 0) {
                    await this.refreshDatasets();
                }
                for (let i = 0; i < projectsIds.length; i++) {
                    const _project = this.findDatasetByName(projectsIds[i]) || await this.projectDataService.getProject(projectsIds[i]);
                    if (_project && (!_project.reference || !reference || (reference.name !== undefined && reference.name.toLowerCase() === _project.reference.name.toLowerCase()))) {
                        const items = _project._lazyItems ? _project._lazyItems : _project.items;
                        if (allTracksFromProjectIds.indexOf(projectsIds[i]) >= 0) {
                            const [trackState] = tracksState.filter(ts => ts.projectId === projectsIds[i]);
                            const index = tracksState.indexOf(trackState);
                            if (index >= 0) {
                                const fn = (item) => {
                                    return {
                                        bioDataItemId: item.name,
                                        projectId: _project.name
                                    };
                                };
                                tracksState.splice(index, 1, ...items.filter(item => item.format !== 'REFERENCE').map(fn));
                            }
                        }
                        if (predefinedTracksFromProjectIds.indexOf(projectsIds[i]) >= 0) {
                            const [trackState] = tracksState.filter(ts => ts.projectId === projectsIds[i]);
                            const index = tracksState.indexOf(trackState);
                            if (index >= 0) {
                                const fn = (item) => {
                                    return {
                                        bioDataItemId: item.name,
                                        projectId: _project.name
                                    };
                                };
                                let predefinedTracks = [];
                                switch (trackState.bioDataItemId.toLowerCase()) {
                                    case REFERENCE_TRACK_SELECTOR: {
                                        const [__ref] = items.filter(r => r.format === 'REFERENCE');
                                        if (__ref) {
                                            predefinedTracks.push(__ref);
                                        }
                                    }
                                        break;
                                    case GENOME_TRACKS_SELECTOR: {
                                        const [__ref] = items.filter(r => r.format === 'REFERENCE');
                                        if (__ref) {
                                            predefinedTracks.push(__ref);
                                            if (__ref.geneFile) {
                                                const [__geneTrack] = items.filter(r => r.format === 'GENE' && r.name.toLowerCase() === __ref.geneFile.name.toLowerCase());
                                                if (__geneTrack) {
                                                    predefinedTracks.push(__geneTrack);
                                                }
                                            } else {
                                                const [__predefinedRef] = this._references.filter(r => r.name.toLowerCase() === __ref.name.toLowerCase());
                                                if (__predefinedRef && __predefinedRef.geneFile) {
                                                    const [__geneTrack] = items.filter(r => r.format === 'GENE' && r.name.toLowerCase() === __predefinedRef.geneFile.name.toLowerCase());
                                                    if (__geneTrack) {
                                                        predefinedTracks.push(__geneTrack);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                        break;
                                }
                                tracksState.splice(index, 1, ...predefinedTracks.map(fn));
                            }
                        }
                        for (let j = 0; j < items.length; j++) {
                            const track = items[j];
                            if (track.name && track.format && tracksState.filter(t =>
                                t.bioDataItemId !== undefined &&
                                (t.bioDataItemId.toString().toLowerCase() === track.name.toLowerCase() ||
                                t.bioDataItemId.toString().toLowerCase() === track.bioDataItemId.toString().toLowerCase())
                                && t.projectId.toString().toLowerCase() === projectsIds[i].toString().toLowerCase()).length === 1) {
                                track.projectId = _project.name;
                                __tracks.push(track);
                            }
                        }
                    } else {
                        const wrongStates = tracksState.filter(ts => !ts.isLocal && ts.projectId === projectsIds[i]);
                        for (let i = 0; i < wrongStates.length; i++) {
                            const index = tracksState.indexOf(wrongStates[i]);
                            tracksState.splice(index, 1);
                        }
                    }
                }
                if (__tracks.filter(t => t.format === 'REFERENCE').length === 0) {
                    const [trackWithReference] = __tracks.filter(t => t.reference);
                    if (trackWithReference) {
                        trackWithReference.reference.projectId = trackWithReference.projectId;
                        __tracks.push(trackWithReference.reference);
                        tracksState.splice(0, 0, {
                            bioDataItemId: trackWithReference.reference.name,
                            projectId: trackWithReference.projectId
                        });
                    }
                }
                this._tracks = __tracks;
            }
            if (!reference) {
                const [referenceCandidate] = this._tracks.filter(t => t.format === 'REFERENCE');
                if (referenceCandidate) {
                    reference = {
                        name: referenceCandidate.name
                    };
                }
            }
            referenceDidChange = await this._changeReference(reference, shouldAddAnnotationTracks);
            if (shouldAddAnnotationTracks && this._reference && this._reference.annotationFiles) {
                const [referenceTrack] = this._tracks.filter(t => t.format === 'REFERENCE');
                let index = this._tracks.indexOf(referenceTrack);
                if (index < 0) {
                    index = this._tracks.length - 1;
                }
                for (let i = 0; i < this._reference.annotationFiles.length; i++) {
                    const annotationFile = this._reference.annotationFiles[i];
                    if (!annotationFile.selected) {
                        continue;
                    }
                    annotationFile.isLocal = true;
                    annotationFile.projectId = '';
                    if (this._tracks.filter(t => t.name.toLowerCase() === annotationFile.name.toLowerCase() && t.format.toLowerCase() === annotationFile.format.toLowerCase()).length === 0) {
                        this._tracks.push(annotationFile);
                        tracksState.splice(index + 1, 0, {
                            bioDataItemId: annotationFile.name,
                            projectId: '',
                            isLocal: true,
                            format: annotationFile.format
                        });
                        index++;
                    }
                }
            }
            this._containsVcfFiles = this.vcfTracks.length > 0;
        } else if (tracks === null || reference === null) {
            this._tracks = [];
            referenceDidChange = await this._changeReference(null);
            this._containsVcfFiles = false;
            this._vcfInfo = [];
            this._infoFields = [];
            this._totalPagesCountVariations = 0;
            this._currentPageVariations = FIRST_PAGE;
            this._lastPageVariations = FIRST_PAGE;
            this._firstPageVariations = FIRST_PAGE;
            this._hasMoreVariations = true;
            this._variantsDataByChromosomes = [];
            this._variantsDataByQuality = [];
            this._variantsDataByType = [];
        }
        let vcfFilesChanged = oldVcfFiles.length !== this.vcfTracks.length;
        if (!vcfFilesChanged) {
            for (let i = 0; i < oldVcfFiles.length; i++) {
                if (this.vcfTracks.filter(t => t.bioDataItemId.toString().toLowerCase() === oldVcfFiles[i].bioDataItemId.toString().toLowerCase()
                    && t.projectId.toLowerCase() === oldVcfFiles[i].projectId.toLowerCase()).length === 0) {
                    vcfFilesChanged = true;
                    break;
                }
            }
        }
        return {referenceDidChange, vcfFilesChanged, recoveredTracksState: tracksState};
    }

    async _initializeVariants(onInit) {
        if (!this._isVariantsInitialized) {
            const {infoItems} = await this.projectDataService.getProjectsFilterVcfInfo(this.vcfTracks.map(t => t.id));
            this._vcfInfo = infoItems || [];
            this._vcfFilter = {
                additionalFilters: {},
                chromosomeIds: [],
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
        tracksSettings.forEach(ts => ts.bioDataItemId = decodeURIComponent(ts.bioDataItemId));
        const referenceName = this.reference.name;
        function sortItems(file1, file2) {
            if (!tracksSettings) {
                // reference should be first;
                if (file1.id === file2.id) {
                    return 0;
                } else if (file1.name === referenceName) {
                    return -1;
                } else if (file2.name === referenceName) {
                    return 1;
                }
                return 0;
            }
            const index1 = tracksSettings.findIndex(findIndex(file1.name.toLowerCase(), file1.projectId.toLowerCase()));
            const index2 = tracksSettings.findIndex(findIndex(file2.name.toLowerCase(), file2.projectId.toLowerCase()));

            if (index1 === index2) return 0;
            return index1 > index2 ? 1 : -1;

            function findIndex(bioDataItemId, projectId) {
                return (element) => element.bioDataItemId.toString().toLowerCase() === bioDataItemId.toString().toLowerCase() &&
                element.projectId.toLowerCase() === projectId.toLowerCase();
            }
        }
        function filterItems(projectFiles) {
            if (!tracksSettings) return projectFiles;
            return projectFiles.filter(file => {
                const [fileSettings]= tracksSettings.filter(m =>
                m.bioDataItemId.toLowerCase() === file.name.toString().toLowerCase() &&
                m.projectId.toLowerCase() === file.projectId.toLowerCase());
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
        if (name && name.toLowerCase() === FIRST_CHROMOSOME_SELECTOR.toLowerCase()) {
            return this._chromosomes[0];
        }
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
            const {onError, onFinish, onInit, onSetToDefault, onStart, groupByCallbacks} = callbacks;
            if (onStart) {
                onStart();
            }
            await this._initializeVariants(onInit);
            if (asDefault) {
                this.currentPageVariations = FIRST_PAGE;
                this.firstPageVariations = FIRST_PAGE;
                this.lastPageVariations = FIRST_PAGE;
                this._hasMoreVariations = true;
                this._vcfFilter = {
                    additionalFilters: {},
                    chromosomeIds: [],
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
            this.refreshVcfFilterEmptyStatus();
            await this.__filterVariants(onError, groupByCallbacks);
            this._isVariantsLoading = false;
            if (onFinish) {
                onFinish();
            }
            if (asDefault && onSetToDefault) {
                onSetToDefault();
            }
        })();
    }

    async __filterVariants(errorCallback, groupByCallbacks) {
        try {
            this.loadVariationsGroupData(groupByCallbacks);
            if (this.reference) {
                this._filteredVariants = await this.loadVariations(this.currentPageVariations);
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

    loadVariationsGroupData(callbacks) {
        const {groupByChromosome, groupByType, groupByQuality} = callbacks;
        this._variantsGroupByQualityError = null;
        this._variantsGroupByTypeError = null;
        this._variantsGroupByChromosomesError = null;
        if (!this.reference) {
            this._variantsDataByChromosomes = [];
            this._variantsDataByQuality = [];
            this._variantsDataByType = [];
            this._isVariantsGroupByChromosomesLoading = false;
            this._isVariantsGroupByTypeLoading = false;
            this._isVariantsGroupByQualityLoading = false;
            if (groupByChromosome && groupByChromosome.finished) {
                groupByChromosome.finished();
            }
            if (groupByType && groupByType.finished) {
                groupByType.finished();
            }
            if (groupByQuality && groupByQuality.finished) {
                groupByQuality.finished();
            }
            return;
        }
        this._isVariantsGroupByChromosomesLoading = true;
        this._isVariantsGroupByTypeLoading = true;
        this._isVariantsGroupByQualityLoading = true;
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
        const orderBy = this._orderByVariations;
        const chromosomeIds = this._vcfFilter.chromosomeIds || [];
        const startIndex = this._vcfFilter.startIndex;
        const endIndex = this._vcfFilter.endIndex;
        const filter = {
            additionalFilters,
            chromosomeIds,
            exon,
            genes,
            infoFields,
            quality,
            variationTypes,
            vcfFileIds,
            orderBy,
            startIndex,
            endIndex
        };

        if (groupByChromosome && groupByChromosome.started) {
            groupByChromosome.started();
        }
        if (groupByType && groupByType.started) {
            groupByType.started();
        }
        if (groupByQuality && groupByQuality.started) {
            groupByQuality.started();
        }

        const errorDescription = (error) => {
            if (error.toLowerCase().startsWith('error:')) {
                error = error.substring('error:'.length);
            }
            if (error.toLowerCase().indexOf('feature index is too large to perform request') >= 0) {
                return 'VCF file too large, unable to show data';
            } else {
                return null;
            }
        };

        this.projectDataService.getVcfGroupData(filter, 'CHROMOSOME_NAME')
            .catch(({message}) => {
                this._variantsGroupByChromosomesError = errorDescription(message);
            })
            .then(data => {
                this._variantsDataByChromosomes = data || [];
                this._isVariantsGroupByChromosomesLoading = false;
                if (groupByChromosome && groupByChromosome.finished) {
                    groupByChromosome.finished();
                }
            });
        this.projectDataService.getVcfGroupData(filter, 'VARIATION_TYPE')
            .catch(({message}) => {
                this._variantsGroupByTypeError = errorDescription(message);
            })
            .then(data => {
                this._variantsDataByType = data || [];
                this._isVariantsGroupByTypeLoading = false;
                if (groupByType && groupByType.finished) {
                    groupByType.finished();
                }
            });
        this.projectDataService.getVcfGroupData(filter, 'QUALITY')
            .catch(({message}) => {
                this._variantsGroupByQualityError = errorDescription(message);
            })
            .then(data => {
                this._variantsDataByQuality = data || [];
                this._isVariantsGroupByQualityLoading = false;
                if (groupByQuality && groupByQuality.finished) {
                    groupByQuality.finished();
                }
            });
    }

    async loadVariations(page) {
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

        const pageSize = PAGE_SIZE;
        const orderBy = this._orderByVariations;

        const chromosomeIds = this._vcfFilter.chromosomeIds || [];
        const startIndex = this._vcfFilter.startIndex;
        const endIndex = this._vcfFilter.endIndex;

        const filter = {
            additionalFilters,
            chromosomeIds,
            exon,
            genes,
            infoFields,
            quality,
            variationTypes,
            vcfFileIds,
            page,
            pageSize,
            orderBy,
            startIndex,
            endIndex
        };
        this._variantsPageLoading = true;
        this.dispatcher.emit('variants:page:loading:started');
        let data = await this.projectDataService.getVcfVariationLoad(filter);
        if (data.totalPagesCount === 0) {
            data.totalPagesCount = undefined;
        }

        if (!data.entries && (data.totalPagesCount !== undefined && data.totalPagesCount < page)) {
            filter.page = data.totalPagesCount;
            this.currentPageVariations = data.totalPagesCount || FIRST_PAGE;
            this.firstPageVariations = data.totalPagesCount || FIRST_PAGE;
            this.lastPageVariations = data.totalPagesCount || FIRST_PAGE;
            if (data.totalPagesCount > 0) {
                data = await this.projectDataService.getVcfVariationLoad(filter);
                if (data.totalPagesCount === 0) {
                    data.totalPagesCount = undefined;
                }
            }
        } else if (!data.entries && data.totalPagesCount === undefined) {
            this._hasMoreVariations = false;
        }

        const infoFieldsObj = {};
        this._infoFields && this._infoFields.forEach(f => infoFieldsObj[f] = null);

        const vcfTrackToProjectId = {};
        for (let i = 0; i < this.vcfTracks.length; i++) {
            const vcfTrack = this.vcfTracks[i];
            vcfTrackToProjectId[`${vcfTrack.id}`] = vcfTrack.projectId;
        }

        this.totalPagesCountVariations = data.totalPagesCount;
        const entries = data.entries ? data.entries : [];
        this._variantsPageLoading = false;
        this.dispatcher.emit('variants:page:loading:finished');

        return entries.map(item =>
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