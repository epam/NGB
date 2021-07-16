import * as GeneTypes from './geneTypes';
import {GeneRenderer, GeneTransformer} from './internal';
import {CachedTrack} from '../../core';
import GeneConfig from './geneConfig';
import {
    CachedGeneDataService,
    GenomeDataService
} from '../../../../dataServices';
import geneMenuConfig from './exterior/geneMenuConfig';
import Menu from '../../core/menu';
import {menu as menuUtilities} from '../../utilities';

export class GENETrack extends CachedTrack {

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
        this.hotKeyListenerDestructor = function() {
            self.dispatcher.removeListener('hotkeyPressed', _hotKeyListener);
        };
        this.fetchAvailableFeatureTypes();
    }

    fetchAvailableFeatureTypes () {
        const {
            id,
            project,
            referenceId
        } = this.config;
        let projectId = project ? project.id : undefined;
        if (!projectId) {
            const [someDataset] = (this.projectContext.datasets || [])
                .filter(dataset =>
                    (dataset.reference && dataset.reference.id === referenceId) ||
                    (dataset.items || dataset._lazyItems || [])
                        .filter(item => item.referenceId === referenceId)
                        .length > 0
                );
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
            console.warn(`Unknown project for gene track:`, this.name);
        }
    }

    featureTypesChanged () {
        // todo: refresh histogram cache
    }

    clearData() {
        this.hotKeyListenerDestructor();
        super.clearData();
    }

    updateAvailableFeatures(features) {
        const availableFeatures = (this.state.availableFeatures || []).concat(features || []);
        this.state.availableFeatures = [...(new Set(availableFeatures))]
            .filter(Boolean)
            .sort();
    }

    currentTrackManagesShortenedIntronsMode() {
        return this.viewport.shortenedIntronsViewport.shortenedIntronsTrackId === this.config.id;
    }

    async updateAndRefresh() {
        await this.updateCache();
        this._flags.dataChanged = true;
        await this.requestRenderRefresh();
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
            this._renderer = new GeneRenderer(this.trackConfig, this.transformer, this._pixiRenderer);
        }
        return this._renderer;
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
                this._gffColorByFeatureType,
                this._gffShowNumbersAminoacid,
                this._showCenterLine,
                this.state.geneTranscript === GeneTypes.transcriptViewTypes.collapsed,
                this.state.geneFeatures || this.state.availableFeatures
            );
            somethingChanged = true;
        }
        return somethingChanged;
    }

    onClick({x, y}) {
        super.onClick({x, y});
        const isHistogram = this.transformer.isHistogramDrawingModeForViewport(this.viewport, this.cache);
        const checkPositionResult = this.renderer.checkPosition(
            this.viewport,
            this.cache,
            {x, y},
            isHistogram,
            this.state.geneTranscript === GeneTypes.transcriptViewTypes.collapsed
        );

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
        if (this.renderer && this.renderer.hoverItem) {
            this.renderer.hoverItem(null);
            this.requestRenderRefresh();
        }
    }

    onHover({x, y}) {
        if (super.onHover({x, y})) {
            this.tooltip.hide();
            const isHistogram = this.transformer.isHistogramDrawingModeForViewport(this.viewport, this.cache);
            const checkPositionResult = this.renderer.checkPosition(this.viewport, this.cache,
                {x, y}, isHistogram);
            if (this.hoveringEffects && this.renderer.hoverItem(checkPositionResult, this.viewport, isHistogram, this.cache)) {
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

    hoverVerticalScroll() {
        return this.renderer.hoverVerticalScroll(this.viewport);
    }

    unhoverVerticalScroll() {
        return this.renderer.unhoverVerticalScroll(this.viewport);
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
                for (const attr in feature.attributes) {
                    if (feature.attributes.hasOwnProperty(attr)) {
                        info.push([attr, feature.attributes[attr]]);
                    }
                }
            }
            return info;
        }
    }

    canScroll(delta) {
        return this.renderer.canScroll(delta);
    }

    isScrollable() {
        return this.renderer.isScrollable();
    }

    scrollIndicatorBoundaries() {
        return this.renderer.scrollIndicatorBoundaries(this.viewport);
    }

    setScrollPosition(value) {
        this.renderer.setScrollPosition(this.viewport, value);
    }

    onScroll({delta}) {
        this.tooltip.hide();
        this.renderer.scroll(this.viewport, delta);
        this.updateScene();
    }

    applyAdditionalRequestParameters(params) {
        params.collapsed = this.state.geneTranscript === GeneTypes.transcriptViewTypes.collapsed;
    }

    async updateCache() {
        if (this.cache.histogramData === null || this.cache.histogramData === undefined) {
            await this.downloadHistogramFn(
                this.cacheUpdateInitialParameters(this.viewport)
            ).then((data) => {
                const downloadedData = GeneTransformer.transformFullHistogramData(data);
                if (!this.cache) {
                    return false;
                }
                this.cache.histogramData = downloadedData;
            });
        }
        if (!this.transformer.isHistogramDrawingModeForViewport(this.viewport, this.cache)) {
            const reqToken = this.__currentDataUpdateReq = {};

            const params = this.cacheUpdateParameters(this.viewport);
            this.applyAdditionalRequestParameters(params);
            if (this.trackDataLoadingStatusChanged) {
                this.trackDataLoadingStatusChanged(true);
            }
            const data = await this.downloadDataFn(params, this.config.referenceId);
            if (this.trackDataLoadingStatusChanged) {
                this.trackDataLoadingStatusChanged(false);
            }
            if (reqToken === this.__currentDataUpdateReq) {
                this.updateAvailableFeatures(this.transformer.getFeatures(data));
                const downloadedData = this.transformer.transformData(data, this.viewport);
                if (!this.cache) {
                    return false;
                }
                delete this.cache.data;
                this.cache.data = downloadedData;
                return await super.updateCache(this.viewport);
            }
            return false;
        } else {
            return await super.updateCache(this.viewport);
        }
    }
}
