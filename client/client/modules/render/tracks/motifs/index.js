import {CachedTrack} from '../../core';
import Menu from '../../core/menu';
import MotifsMatchesRenderer from './motifsRenderer';
import motifsMenuConfig from './exterior/motifsMenuConfig';
import MotifsConfig from './motifsConfig';

export class MOTIFSTrack extends CachedTrack {

    renderer;
    motifStrand = null;

    constructor(opts) {
        super(opts);
        this.opts = opts;
        this.dispatcher = opts.dispatcher;
        this.motifsContext = opts.motifsContext;
        this.motifStrand = opts.state.motifStrand;
        this.setCache();
        this.renderer = new MotifsMatchesRenderer(
            this,
            Object.assign({}, this.trackConfig, this.config),
            this._pixiRenderer,
            opts,
            opts.motifsContext,
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

    static Menu = Menu(
        motifsMenuConfig,
        {
            postStateMutatorFn: MOTIFSTrack.postStateMutatorFn
        }
    );

    static getTrackDefaultConfig() {
        return MotifsConfig;
    }

    get trackIsResizable() {
        return true;
    }

    getSettings() {
        if (this._menu) {
            return this._menu;
        }
        this._menu = this.constructor.Menu.attach(this, {browserId: this.browserId});
        return this._menu;
    }

    updateCache () {
        const updated = super.updateCache();
        if (updated) {
            return this.setCache();
        }
        return false;
    }

    setCache() {
        if (this.cache && this.motifsContext) {
            const matches = this.motifsContext.matches;
            const setStrandMatches = (allMatches, strand) => {
                const strandMatches = allMatches
                    .filter(match => match.chromosome === this.opts.chromosome.name &&
                        match.strand.toLowerCase() === strand.toLowerCase())
                    .map(match => {
                        const start = Math.min(match.start, match.end);
                        const end = Math.max(match.start, match.end);
                        match.start = start;
                        match.end = end;
                        return match;
                    })
                    .sort((a, b) => a.start >= b.start);
                if (strandMatches.length && strandMatches.length === 1) {
                    strandMatches[0].levelY = 1;
                }
                for (let i = 0; i < strandMatches.length - 1; i++) {
                    strandMatches[i].levelY = strandMatches[i].levelY ? strandMatches[i].levelY : 1;
                    strandMatches[i + 1].levelY = strandMatches[i + 1].levelY ? strandMatches[i + 1].levelY : 1;
                    if (strandMatches[i].start === strandMatches[i + 1].start) {
                        strandMatches[i + 1].levelY = strandMatches[i].levelY + 1;
                    }  else if (strandMatches[i].end >= strandMatches[i + 1].start) {
                        strandMatches[i + 1].levelY = strandMatches[i].levelY + 1;
                    }
                }
                return strandMatches;
            };
            const positiveMatches = setStrandMatches(matches, 'positive');
            const negativeMatches = setStrandMatches(matches, 'negative');

            this.cache.data = {
                positive: positiveMatches,
                negative: negativeMatches
            };
            return true;
        }
        return false;
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
                flags.heightChanged ||
                flags.dataChanged,
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
