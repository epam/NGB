import {CachedTrackWithVerticalScroll} from '../../core';
import Menu from '../../core/menu';
import {MotifsDataService} from '../../../../dataServices';
import {linearDimensionsConflict} from '../../utilities';
import MotifsConfig from './motifsConfig';
import MotifsRenderer from './motifsRenderer';
import motifsMenuConfig from './exterior/motifsMenuConfig';

export class MOTIFSTrack extends CachedTrackWithVerticalScroll {

    renderer = null;
    motifStrand = null;

    static getTrackDefaultConfig() {
        return MotifsConfig;
    }

    get trackIsResizable() {
        return true;
    }

    get verticalScrollRenderer() {
        return this.renderer;
    }

    get nextNavigationAvailable() {
        const {
            next
        } = this.prevNextInfo || {};
        return !this.fetchingPrevNext && next && Math.abs(next) !== Infinity;
    }

    get prevNavigationAvailable() {
        const {
            previous
        } = this.prevNextInfo || {};
        return !this.fetchingPrevNext && previous && Math.abs(previous) !== Infinity;
    }

    constructor(opts) {
        super(opts);
        this.dispatcher = opts.dispatcher;
        this.motifsContext = opts.motifsContext;
        this.motifStrand = opts.state.motifStrand;
        this.motif = opts.state.motif || '';
        this._dataService = new MotifsDataService();
        this.renderer = new MotifsRenderer(
            this,
            Object.assign({}, this.trackConfig, this.config),
            opts,
            this.motifStrand
        );
        this._actions = [
            {
                enabled: function () {
                    return true;
                },
                label: 'Navigation',
                type: 'groupLinks',
                links: [{
                    label: 'Prev',
                    enabled: () => this.prevNavigationAvailable,
                    handleClick: this.prevMotif.bind(this)
                }, {
                    label: 'Next',
                    enabled: () => this.nextNavigationAvailable,
                    handleClick: this.nextMotif.bind(this)
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

    get maximumRange() {
        return this.renderer.maximumRange;
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

    getSettings() {
        if (this._menu) {
            return this._menu;
        }
        this._menu = this.constructor.Menu.attach(this, {browserId: this.browserId});
        return this._menu;
    }

    async updateCache () {
        if (this.maximumRange <= this.viewport.actualBrushSize) {
            return false;
        }
        await this.updatePrevNextInfo();
        const data = await this.motifTrack(this.cacheUpdateParameters(this.viewport));
        if (this.cache) {
            this.cache.data = this.transformData(data);
            return await super.updateCache();
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
        if (!data.blocks || data.blocks.length === 0) {
            return [];
        }
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
        const margin = Math.max(
            1,
            Math.ceil(this.viewport.convert.pixel2brushBP(MotifsConfig.matches.margin))
        );
        for (let i = 0; i < matches.length; i++) {
            const match = matches[i];
            const conflicts = matches
                .slice(0, i)
                .filter(concurrent => linearDimensionsConflict(
                    match.startIndex,
                    match.endIndex,
                    concurrent.startIndex,
                    concurrent.endIndex,
                    margin + 1
                ));
            match.levelY = Math.max(0, ...conflicts.map(conflict => conflict.levelY)) + 1;
        }
        return matches;
    }

    reload () {
        this.invalidateCache();
        this._flags.renderFeaturesChanged = true;
        this.requestRenderRefresh();
    }

    render(flags) {
        let somethingChanged = super.render(flags);
        if (flags.renderReset) {
            this.container.removeChildren();
            this.container.addChild(this.renderer.container);
            this.container.addChild(this.renderer.zoomInContainer);
        }
        this.renderer.zoomInContainer.visible = this.maximumRange < this.viewport.actualBrushSize;
        this.renderer.container.visible = this.maximumRange >= this.viewport.actualBrushSize;
        if (
            flags.brushChanged ||
            flags.widthChanged ||
            flags.heightChanged ||
            flags.renderReset ||
            flags.dataChanged
        ) {
            somethingChanged = true;
            this.renderer.height = this.height;
            this.renderer.render(flags, this.viewport, this.cache, this._showCenterLine);
        }
        return somethingChanged;
    }

    async updatePrevNextInfo() {
        if (!this.viewport) {
            return;
        }
        const {
            center: centerCache
        } = this.prevNextInfo || {};
        const request = this.setRequestToMotif();
        const {startPosition: center} = request;
        if (centerCache && centerCache === center) {
            return;
        }
        this.fetchingPrevNext = true;
        const process = (data, direction = 1) => data && data.startIndex
            ? data.startIndex
            : direction * Infinity;
        const [prevPositionData, nextPositionData] = await Promise.all(
            [
                this.dataService.getPrevMotifs(request),
                this.dataService.getNextMotifs(request)
            ]
        );
        this.prevNextInfo = {
            previous: process(prevPositionData, -1),
            next: process(nextPositionData, 1),
            center
        };
        this.fetchingPrevNext = false;
        if (typeof this.config.reloadScope === 'function') {
            this.config.reloadScope();
        }
    }

    async nextMotif() {
        const {
            next
        } = this.prevNextInfo || {};
        if (this.fetchingPrevNext || !next || Math.abs(next) === Infinity) {
            return;
        }
        await this.updatePrevNextInfo();
        this.viewport.selectPosition(next);
    }

    async prevMotif() {
        const {
            previous
        } = this.prevNextInfo || {};
        if (this.fetchingPrevNext || !previous || Math.abs(previous) === Infinity) {
            return;
        }
        await this.updatePrevNextInfo();
        this.viewport.selectPosition(previous);
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
