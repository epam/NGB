import {CachedTrack} from '../../core';
import Menu from '../../core/menu';
import {MotifsDataService} from '../../../../dataServices';
import MotifsConfig from './motifsConfig';
import motifsMenuConfig from './exterior/motifsMenuConfig';
import MotifsMatchesRenderer from './motifsRenderer';

export class MOTIFSTrack extends CachedTrack {

    renderer;
    motifStrand = null;

    constructor(opts) {
        super(opts);
        this.dispatcher = opts.dispatcher;
        this.motifsContext = opts.motifsContext;
        this.motifStrand = opts.state.motifStrand;
        this._dataService = new MotifsDataService();
        this.renderer = new MotifsMatchesRenderer(
            this,
            Object.assign({}, this.trackConfig, this.config),
            opts,
            this.motifStrand
        );
        this.dispatcher.on('motifs:results:change', this.reload.bind(this));
    }

    get stateKeys() {
        return [
            'color',
            'motifStrand'
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
        const updated = await super.updateCache();
        if (updated && this.cache) {
            const data = await this.motifTrack(this.cacheUpdateParameters(this.viewport));
            this.cache.data = (data && data.blocks) ? this.transformData(data) : [];
            return true;
        }
        return false;
    }

    cacheUpdateParameters (viewport) {
        const {referenceId} = this.config;
        const payload = super.cacheUpdateParameters(viewport);
        const motifLength = this.motifsContext.match.motif.length;
        const startIndex = Math.max(payload.startIndex - motifLength, 1);
        const endIndex = Math.min(payload.endIndex + motifLength, viewport.chromosome.end);
        return {
            id: payload.id,
            chromosomeId: payload.chromosomeId,
            startIndex,
            endIndex,
            scaleFactor: payload.scaleFactor,
            option: {},
            collapsed: false,
            projectId: referenceId,
            motif: this.motifsContext.match.motif,
            strand: this.motifStrand.toUpperCase()
        };
    }

    transformData(data) {
        const matches = data.blocks
            .map(block => {
                const start = Math.min(block.startIndex, block.endIndex);
                const end = Math.max(block.startIndex, block.endIndex);
                block.startIndex = start;
                block.endIndex = end;
                return block;
            })
            .sort((a, b) => a.start >= b.start);
        if (matches.length && matches.length === 1) {
            matches[0].levelY = 1;
        }
        for (let i = 0; i < matches.length - 1; i++) {
            matches[i].levelY = matches[i].levelY ? matches[i].levelY : 1;
            matches[i + 1].levelY = matches[i + 1].levelY ? matches[i + 1].levelY : 1;
            if (matches[i].start === matches[i + 1].start) {
                matches[i + 1].levelY = matches[i].levelY + 1;
            }  else if (matches[i].end >= matches[i + 1].start) {
                matches[i + 1].levelY = matches[i].levelY + 1;
            }
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

    destructor() {
        super.destructor();
        this.dispatcher.removeListener('motifs:results:change', this.reload);
    }
}
