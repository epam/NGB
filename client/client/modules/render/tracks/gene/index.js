import * as GeneTypes from './geneTypes';
import {GeneRenderer, GeneTransformer} from './internal';
import {CachedTrackWithVerticalScroll} from '../../core';
import GeneConfig from './geneConfig';
import {
    CachedGeneDataService,
    GenomeDataService
} from '../../../../dataServices';
import geneMenuConfig from './exterior/geneMenuConfig';
import Menu from '../../core/menu';
import {menu as menuUtilities} from '../../utilities';

const PREFER_WEBGL = true;

export class GENETrack extends CachedTrackWithVerticalScroll {

    _dataService = null;
    _transformer = null;
    _renderer: GeneRenderer = null;
    _gffColorByFeatureType = true;
    features = {};
    dispatcher = null;

    static getTrackDefaultConfig() {
        return GeneConfig;
    }

    get stateKeys() {
        return ['geneTranscript', 'header', 'geneFeatures'];
    }

    get featuresFilteringEnabled () {
        return true;
    }

    static postStateMutatorFn = (track) => {
        track.updateAndRefresh();
        track.reportTrackState();
    };

    static Menu = Menu(
        geneMenuConfig,
        {
            postStateMutatorFn: GENETrack.postStateMutatorFn
        }
    );

    constructor(opts) {
        opts.preferWebGL = PREFER_WEBGL;
        super(opts);
        const self = this;
        const handleClick = async() => {
            if (self.viewport.shortenedIntronsViewport.shortenedIntronsTrackId === self.config.id) {
                self.viewport.shortenedIntronsViewport.disable();
            }
            else {
                self.viewport.shortenedIntronsViewport.enable(this);
            }
        };
        const shortenedIntronsModeIsAvailable = function() {
            return self.viewport.isShortenedIntronsMode ||
                self.viewport.brushSize < self.viewport.shortenedIntronsViewport.maximumRange;
        };
        const disabledMessageFn = function() {
            return `Shortened introns mode is available at ${self.viewport.shortenedIntronsViewport.maximumRange}bp`;
        };
        this._actions = [
            {
                disabledMessage: disabledMessageFn,
                enabled: shortenedIntronsModeIsAvailable,
                handleClick: handleClick,
                key: 'shorten_introns',
                label: 'Shorten introns',
                type: 'checkbox',
                value: ::this.currentTrackManagesShortenedIntronsMode
            }
        ];

        this._gffColorByFeatureType = opts.gffColorByFeatureType;
        this._gffShowNumbersAminoacid = opts.gffShowNumbersAminoacid;
        this._collapsed = this.state.geneTranscript === GeneTypes.transcriptViewTypes.collapsed;

        if (opts.dispatcher) {
            this.dispatcher = opts.dispatcher;
        }

        this.hotKeyListener = (event) => {
            if (event) {
                if(event === 'gene>transcript>collapsed'){
                    this.state.geneTranscript = GeneTypes.transcriptViewTypes.collapsed;
                    this.updateAndRefresh();
                }
                if(event === 'gene>transcript>expanded'){
                    this.state.geneTranscript = GeneTypes.transcriptViewTypes.expanded;
                    this.updateAndRefresh();
                }
                if(event === 'gene>showNumbersAminoacid'){
                    this._gffShowNumbersAminoacid = !this._gffShowNumbersAminoacid;
                    this.updateAndRefresh();
                }
            }
        };

        const _hotKeyListener = ::this.hotKeyListener;
        this.dispatcher.on('hotkeyPressed', _hotKeyListener);
        const featureInfoSavedCallback = (opts = {}) => {
            const {trackId} = opts;
            if (this.config.id === trackId && /^(gene|feature_counts)$/i.test(this.config.format)) {
                setTimeout(this.updateAndRefresh.bind(this, true), 0);
            }
        };
        this.dispatcher.on('feature:info:saved', featureInfoSavedCallback);
        this.removeDispatcherListeners = () => {
            this.dispatcher.removeListener('hotkeyPressed', _hotKeyListener);
            this.dispatcher.removeListener('feature:info:saved', featureInfoSavedCallback);
        };
        this.fetchAvailableFeatureTypes();
    }

    async fetchAvailableFeatureTypes () {
        if (!this.featuresFilteringEnabled) {
            return;
        }
        const {
            id,
            name,
            project,
            referenceId
        } = this.config;
        let projectId = project ? project.id : undefined;
        if (!projectId) {
            const someDataset = await this.projectContext.findAnyDatasetOfReference(referenceId);
            if (someDataset) {
                projectId = someDataset.id;
            }
        }
        if (referenceId && projectId && id) {
            const genomeService = new GenomeDataService(this.dispatcher);
            genomeService.getGeneFeatureTypes(
                referenceId,
                projectId,
                id
            )
                .then(this.updateAvailableFeatures.bind(this))
                .catch((e) => {
                    // eslint-disable-next-line
                    console.warn(`Error fetching gene feature types: ${e.message}`);
                });
        } else if (!projectId) {
            // warn user if we were unable to determine track's project
            // eslint-disable-next-line
            console.warn('Unknown project for gene track:', name);
        }
    }

    featureTypesChanged () {
        // todo: refresh histogram cache
    }

    clearData() {
        this.removeDispatcherListeners();
        super.clearData();
    }

    updateAvailableFeatures(features) {
        if (this.featuresFilteringEnabled) {
            const availableFeatures = (this.state.availableFeatures || []).concat(features || []);
            this.state.availableFeatures = [...(new Set(availableFeatures))]
                .filter(Boolean)
                .sort();
        } else {
            this.state.availableFeatures = undefined;
        }
    }

    currentTrackManagesShortenedIntronsMode() {
        return this.viewport.shortenedIntronsViewport.shortenedIntronsTrackId === this.config.id;
    }

    async updateAndRefresh(forceUpdate = false) {
        try {
            this.unsetError();
            await this.updateCache(forceUpdate);
        } catch (e) {
            this.reportError(e.message);
        } finally {
            this._flags.dataChanged = true;
            await this.requestRenderRefresh();
        }
    }

    getSettings() {
        if (this._menu) {
            return this._menu;
        }
        this._menu = this.config.sashimi ? [] : this.constructor.Menu.attach(this, {browserId: this.browserId});
        return this._menu;
    }

    get downloadHistogramFn() {
        return ::this.dataService.loadGeneHistogramTrack;
    }

    get downloadDataFn() {
        return ::this.dataService.loadGeneTrack;
    }

    get dataService() {
        if (!this._dataService) {
            this._dataService = new CachedGeneDataService(this.dispatcher);
        }
        return this._dataService;
    }

    get transformer(): GeneTransformer {
        if (!this._transformer) {
            this._transformer = new GeneTransformer(this.trackConfig);
        }
        return this._transformer;
    }

    get renderer(): GeneRenderer {
        if (!this._renderer) {
            this._renderer = new GeneRenderer(this.trackConfig, this.transformer, this);
        }
        return this._renderer;
    }

    get verticalScrollRenderer() {
        return this.renderer;
    }

    trackSettingsChanged(params) {
        if(this.config.bioDataItemId === params.id && this.config.format.toLowerCase() === 'gene') {
            const settings = params.settings;
            settings.forEach(setting => {
                if (setting.name === 'shortenIntrons') {
                    setting.value ? this.viewport.shortenedIntronsViewport.enable(this) : this.viewport.shortenedIntronsViewport.disable();
                }else {
                    const menuItem = menuUtilities.findMenuItem(this._menu, setting.name);
                    if (menuItem.type === 'checkbox') {
                        menuItem.enable();
                    }
                }
            });
        }
    }

    globalSettingsChanged(state) {
        super.globalSettingsChanged(state);
        this._gffColorByFeatureType = state.gffColorByFeatureType;
        this._gffShowNumbersAminoacid = state.gffShowNumbersAminoacid;

        this._flags.dataChanged = true;
        this.requestRenderRefresh();
    }

    render(flags) {
        let somethingChanged = super.render(flags);
        if (flags.renderReset) {
            this.container.addChild(this.renderer.container);
            somethingChanged = true;
        }
        if (flags.brushChanged || flags.widthChanged || flags.heightChanged || flags.renderReset || flags.dataChanged) {
            this.renderer.height = this.height;
            this.renderer.render(
                this.viewport,
                this.cache,
                flags.heightChanged || flags.dataChanged,
                this._showCenterLine,
                this._gffColorByFeatureType,
                this._gffShowNumbersAminoacid,
                this.state.geneTranscript === GeneTypes.transcriptViewTypes.collapsed,
                this.state.geneFeatures || this.state.availableFeatures
            );
            somethingChanged = true;
        }
        return somethingChanged;
    }

    getFeaturesByCoordinates ({x, y}) {
        const isHistogram = this.transformer.isHistogramDrawingModeForViewport(this.viewport, this.cache);
        return this.renderer.checkPosition(
            this.viewport,
            this.cache,
            {x, y},
            isHistogram,
            this.state.geneTranscript === GeneTypes.transcriptViewTypes.collapsed
        );
    }

    onClick({x, y}) {
        super.onClick({x, y});
        const isHistogram = this.transformer.isHistogramDrawingModeForViewport(this.viewport, this.cache);
        const checkPositionResult = this.getFeaturesByCoordinates({x, y});

        if (!isHistogram && checkPositionResult && checkPositionResult.length > 0) {
            if (this.dataItemClicked !== null && this.dataItemClicked !== undefined) {
                let feature = checkPositionResult[0].feature;
                if (feature.feature === 'aminoacid' || feature.feature === 'exon') {
                    [feature] = checkPositionResult.filter(x => /^(transcript|mrna)$/i.test(x.feature.feature)).map(x => x.feature);
                    if (!feature) {
                        return;
                    }
                }
                const eventInfo = {
                    endIndex: feature.endIndex,
                    feature: feature,
                    info: this.getTooltipDataObject(false, checkPositionResult),
                    startIndex: feature.startIndex
                };
                this.dataItemClicked(this, eventInfo, {name: 'feature-click', position: {x, y}});
            }

        }
    }

    onMouseOut() {
        super.onMouseOut();
        this.hoverItem(null);
        this.requestRenderRefresh();
    }

    hoverItem (items) {
        if (this.renderer && this.renderer.hoverItem) {
            const isHistogram = this.transformer.isHistogramDrawingModeForViewport(this.viewport, this.cache);
            return this.renderer.hoverItem(items, this.viewport, isHistogram, this.cache);
        }
        return false;
    }

    onHover({x, y}) {
        if (super.onHover({x, y})) {
            this.tooltip.hide();
            const isHistogram = this.transformer.isHistogramDrawingModeForViewport(this.viewport, this.cache);
            const checkPositionResult = this.getFeaturesByCoordinates({x, y});
            if (this.hoveringEffects && this.hoverItem(checkPositionResult)) {
                this.requestRenderRefresh();
            }
            if (this.shouldDisplayTooltips && checkPositionResult && checkPositionResult.length > 0) {
                this.tooltip.setContent(this.getTooltipDataObject(isHistogram, checkPositionResult));
                this.tooltip.move({x, y});
                this.tooltip.show({x, y});
            }
            return false;
        }
        return true;
    }

    getTooltipDataObject(isHistogram, geneData) {
        if (isHistogram) {
            return [
                ['Count', geneData[0].value]
            ];
        } else if (geneData.length > 0) {
            const info = [];
            for (let i = 0; i < geneData.length; i++) {
                const feature = geneData[i].feature;
                if (feature.feature && feature.feature.toLowerCase() === 'exon' && feature.exonNumber) {
                    info.push(['Exon number', feature.exonNumber]);
                } else if (feature.feature && feature.feature.toLowerCase() !== 'aminoacid') {
                    if (feature.name) {
                        info.push([feature.feature, feature.name]);
                    }
                    else {
                        info.push(['Type', feature.feature]);
                    }
                    info.push(['Start', feature.startIndex]);
                    info.push(['End', feature.endIndex]);
                    if (feature.strand) {
                        info.push(['Strand', feature.strand]);
                    }
                    if (feature.score) {
                        info.push(['Score', feature.score]);
                    }
                    if (feature.exonNumber) {
                        info.push(['Exon number', feature.exonNumber]);
                    }
                } else {
                    info.push([`${feature.name} #${feature.index}`]);
                }
                if (feature.attributes) {
                    for (const attr in feature.attributes) {
                        if (feature.attributes.hasOwnProperty(attr)) {
                            info.push([attr, feature.attributes[attr]]);
                        }
                    }
                }
            }
            return info;
        }
    }

    applyAdditionalRequestParameters(params) {
        params.collapsed = this.state.geneTranscript === GeneTypes.transcriptViewTypes.collapsed;
    }

    updateCacheData (data) {
        if (this.cache) {
            delete this.cache.data;
            this.cache.data = data;
        }
    }

    async updateCache(forceUpdate = false) {
        if (this.cache.histogramData === null || this.cache.histogramData === undefined) {
            const data = await this.downloadHistogramFn(
                this.cacheUpdateInitialParameters(this.viewport)
            );
            const downloadedData = GeneTransformer.transformFullHistogramData(data);
            if (!this.cache) {
                return false;
            }
            this.cache.histogramData = downloadedData;
        }
        if (!this.transformer.isHistogramDrawingModeForViewport(this.viewport, this.cache)) {
            const reqToken = this.__currentDataUpdateReq = {};

            const params = this.cacheUpdateParameters(this.viewport);
            this.applyAdditionalRequestParameters(params);
            if (this.trackDataLoadingStatusChanged) {
                this.trackDataLoadingStatusChanged(true);
            }
            const data = await this.downloadDataFn(params, this.config.referenceId, forceUpdate);
            if (this.trackDataLoadingStatusChanged) {
                this.trackDataLoadingStatusChanged(false);
            }
            if (reqToken === this.__currentDataUpdateReq) {
                this.updateAvailableFeatures(this.transformer.getFeatures(data));
                const downloadedData = this.transformer.transformData(data, this.viewport);
                if (!this.cache) {
                    return false;
                }
                this.updateCacheData(downloadedData);
                return await super.updateCache(this.viewport);
            }
            return false;
        } else {
            return await super.updateCache(this.viewport);
        }
    }
}
