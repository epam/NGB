import {CachedTrack} from '../../core';
import Menu from '../../core/menu';
import {MotifsDataService} from '../../../../dataServices';
import {linearDimensionsConflict, PlaceholderRenderer} from '../../utilities';
import MotifsConfig from './motifsConfig';
import motifsMenuConfig from './exterior/motifsMenuConfig';
import MotifsMatchesRenderer from './motifsRenderer';

const MAXIMUM_RANGE = 500000;

export class MOTIFSTrack extends CachedTrack {

    renderer;
    motifStrand = null;
    _zoomInPlaceholderRenderer = null;

    get maximumRange () {
        return MAXIMUM_RANGE;
    }

    constructor(opts) {
        super(opts);
        this.dispatcher = opts.dispatcher;
        this.motifsContext = opts.motifsContext;
        this.motifStrand = opts.state.motifStrand;
        this.motif = opts.state.motif || '';
        this._dataService = new MotifsDataService();
        this.renderer = new MotifsMatchesRenderer(
            this,
            Object.assign({}, this.trackConfig, this.config),
            opts,
            this.motifStrand
        );
        this._zoomInPlaceholderRenderer = new PlaceholderRenderer(this);
        this._actions = [
            {
                enabled: function () {
                    return true;
                },
                label: 'Navigation',
                type: 'groupLinks',
                links: [{
                    label: 'Prev',
                    handleClick: ::this.prevMotif
                }, {
                    label: 'Next',
                    handleClick: ::this.nextMotif
                }]
            }
        ];
        this.dispatcher.on('motifs:results:change', this.reload.bind(this));
    }

    get stateKeys() {
        return [
            'color',
            'motifStrand',
            'motif'
        ];
    }

    get trackIsResizable() {
        return true;
    }

    get dataService() {
        return this._dataService;
    }

    get motifTrack() {
        return ::this.dataService.loadMotifTrack;
    }

    static Menu = Menu(
        motifsMenuConfig,
        {
            postStateMutatorFn: MOTIFSTrack.postStateMutatorFn
        }
    );

    static getTrackDefaultConfig() {
        return MotifsConfig;
    }

    getSettings() {
        if (this._menu) {
            return this._menu;
        }
        this._menu = this.constructor.Menu.attach(this, {browserId: this.browserId});
        return this._menu;
    }

    async updateCache () {
        if (MAXIMUM_RANGE <= this.viewport.actualBrushSize) {
            return false;
        }
        const updated = await super.updateCache();
        if (updated && this.cache) {
            const data = await this.motifTrack(this.cacheUpdateParameters(this.viewport));
            this.cache.data = (data && data.blocks) ? this.transformData(data) : [];
            return true;
        }
        return false;
    }

    cacheUpdateParameters (viewport) {
        const payload = super.cacheUpdateParameters(viewport);
        const {
            referenceId,
            projectId
        } = this.config;
        const motifLength = this.motif.length;
        const startIndex = Math.max(payload.startIndex - motifLength, 1);
        const endIndex = Math.min(payload.endIndex + motifLength, viewport.chromosome.end);
        const p = {
            id: referenceId,
            chromosomeId: payload.chromosomeId,
            startIndex,
            endIndex,
            scaleFactor: payload.scaleFactor,
            option: {},
            collapsed: false,
            projectId: projectId || 0,
            motif: this.motif,
            strand: this.motifStrand.toUpperCase()
        };
        return p;
    }

    transformData(data) {
        const matches = data.blocks
            .map(block => {
                const start = Math.min(block.startIndex, block.endIndex);
                const end = Math.max(block.startIndex, block.endIndex);
                block.startIndex = start;
                block.endIndex = end;
                block.length = end - start;
                return block;
            })
            .sort((a, b) => b.length - a.length)
            .sort((a, b) => a.startIndex - b.startIndex);
        const margin = this.viewport.convert.pixel2brushBP(MotifsConfig.matches.margin);
        for (let i = 0; i < matches.length; i++) {
            const match = matches[i];
            const conflicts = matches
                .slice(0, i)
                .filter(concurrent => linearDimensionsConflict(match.startIndex, match.endIndex, concurrent.startIndex, concurrent.endIndex, margin));
            match.levelY = Math.max(0, ...conflicts.map(conflict => conflict.levelY)) + 1;
        }
        return matches;
    }

    reload () {
        this.invalidateCache();
        this._flags.renderFeaturesChanged = true;
        this.requestRenderRefresh();
    }

    _getZoomInPlaceholderText() {
        const unitThreshold = 1000;
        const noReadText = {
            unit: this.maximumRange < unitThreshold ? 'BP' : 'kBP',
            value: this.maximumRange < unitThreshold
                ? this.maximumRange : Math.ceil(this.maximumRange / unitThreshold)
        };
        return `Zoom in to see motifs.
            Minimal zoom level is at ${noReadText.value}${noReadText.unit}`;
    }

    render(flags) {
        if (flags.renderReset) {
            this.container.removeChildren();
            this.container.addChild(this._zoomInPlaceholderRenderer.container);
            this._zoomInPlaceholderRenderer.init(this._getZoomInPlaceholderText(), {
                height: this._pixiRenderer.height,
                width: this._pixiRenderer.width
            });
        } else if (flags.widthChanged || flags.heightChanged) {
            this._zoomInPlaceholderRenderer.init(this._getZoomInPlaceholderText(), {
                height: this._pixiRenderer.height,
                width: this._pixiRenderer.width
            });
        }
        this._zoomInPlaceholderRenderer.container.visible = this.maximumRange < this.viewport.actualBrushSize;
        this.renderer.container.visible = this.maximumRange >= this.viewport.actualBrushSize;

        let somethingChanged = super.render(flags);

        if (flags.renderReset) {
            this.container.addChild(this.renderer.container);
            somethingChanged = true;
        }
        if (
            flags.brushChanged ||
            flags.widthChanged ||
            flags.heightChanged ||
            flags.renderReset ||
            flags.dataChanged
        ) {
            this.renderer.height = this.height;
            this.renderer.render(
                this.viewport,
                this.cache,
                flags.heightChanged || flags.dataChanged,
                this._showCenterLine
            );
            somethingChanged = true;
        }
        return somethingChanged;
    }

    nextMotif() {
        const request = this.setRequestToMotif();
        this.dataService.getNextMotifs(request)
            .then(data => {
                if (data && data.startIndex) {
                    this.viewport.selectPosition(data.startIndex);
                }
            });
    }

    prevMotif() {
        const request = this.setRequestToMotif();
        this.dataService.getPrevMotifs(request)
            .then(data => {
                if (data && data.startIndex) {
                    this.viewport.selectPosition(data.startIndex);
                }
            });
    }

    setRequestToMotif () {
        const startPosition = Math.floor((this.viewport.brush.end + this.viewport.brush.start) / 2);
        return {
            referenceId: this.config.referenceId,
            chromosomeId: this.config.chromosomeId,
            startPosition,
            motif: this.motif,
            strand: this.motifStrand,
            includeSequence: false,
        };
    }

    destructor() {
        super.destructor();
        this.dispatcher.removeListener('motifs:results:change', this.reload);
    }
}
