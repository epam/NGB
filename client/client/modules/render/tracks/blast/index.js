import BLASTAlignmentRenderer from './renderer';
import BLASTConfig from './blastConfig';
import {BLASTResultEvents} from '../../../../app/shared/blastContext';
import {CachedTrack} from '../../core';

export class BLASTTrack extends CachedTrack {
    static getTrackDefaultConfig() {
        return BLASTConfig;
    }

    renderer: BLASTAlignmentRenderer;

    get trackIsResizable() {
        return true;
    }

    constructor(opts) {
        super(opts);
        this.blastContext = opts.blastContext;
        this.dispatcher = opts.dispatcher;
        this.renderer = new BLASTAlignmentRenderer(
            Object.assign({}, this.trackConfig, this.config),
            this._pixiRenderer,
            opts,
            opts.blastContext
        );
        this.reload = this.reload.bind(this);
        this.dispatcher.on(BLASTResultEvents.changed, this.reload);
    }

    async updateCache() {
        const updated = super.updateCache();
        if (updated && this.cache && this.blastContext) {
            const {startIndex, endIndex} = this.cache.dataViewport;
            const featureCoords = this.blastContext.featureCoords;
            const filterAlignment = alignment => {
                let {
                    sequenceStart,
                    sequenceEnd,
                    queryEnd
                } = alignment;
                if (!sequenceStart || !sequenceEnd) {
                    return false;
                }
                if (featureCoords) {
                    sequenceStart += featureCoords.start;
                    sequenceEnd = sequenceStart + queryEnd;
                }
                const start = Math.min(sequenceStart, sequenceEnd);
                const end = Math.max(sequenceStart, sequenceEnd);
                return start < endIndex && end > startIndex;
            };
            const {dbType, tool} = this.blastContext.search || {};
            this.cache.isProtein = /^protein$/i.test(dbType) || !/^blastn$/i.test(tool);
            this.cache.data = (this.blastContext.alignments || [])
                .filter(filterAlignment);
            return true;
        }
        return false;
    }

    reload () {
        this.invalidateCache();
        this._flags.renderFeaturesChanged = true;
        this.requestRenderRefresh();
    }

    hoverVerticalScroll() {
        return this.renderer.hoverVerticalScroll(this.viewport);
    }

    unhoverVerticalScroll() {
        return this.renderer.unhoverVerticalScroll(this.viewport);
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
        if (this.tooltip) {
            this.tooltip.hide();
        }
        this.renderer.scroll(this.viewport, delta);
        this.updateScene();
    }

    onHover({x, y}) {
        if (super.onHover({x, y})) {
            this.tooltip.hide();
            const checkPositionResult = this.renderer.checkPosition(this.viewport, this.cache,
                {x, y});
            if (this.hoveringEffects && this.renderer.hoverItem(checkPositionResult, this.viewport, this.cache)) {
                this.requestRenderRefresh();
            }
            if (this.shouldDisplayTooltips && checkPositionResult) {
                this.tooltip.setContent(this.getTooltipDataObject(checkPositionResult));
                this.tooltip.move({x, y});
                this.tooltip.show({x, y});
            }
            return false;
        }
        return true;
    }

    onMouseOut() {
        super.onMouseOut();
        this.tooltip.hide();
        if (
            this.hoveringEffects &&
            this.renderer.hoverItem(undefined, this.viewport, this.cache)
        ) {
            this.requestRenderRefresh();
        }
    }

    getTooltipDataObject(blastAlignment) {
        const info = [];
        if (blastAlignment && blastAlignment.alignment) {
            const {
                bitScore,
                score,
                length,
                evalue,
                numIdentity,
                gaps,
                mismatch,
                queryStart,
                queryEnd,
                queryLength,
                queryCoverageSubject
            } = blastAlignment.alignment;
            const notDefined = o => o === undefined;
            if (!notDefined(bitScore) && !notDefined(score)) {
                info.push(['Score', `${bitScore} bits (${score})`]);
            } else if (!notDefined(bitScore)) {
                info.push(['Score', `${bitScore} bits`]);
            }
            if (!notDefined(evalue)) {
                info.push(['Expect', `${evalue}`]);
            }
            const percent = o => `${Math.round(o * 100)}%`;
            if (!notDefined(numIdentity) && !notDefined(length)) {
                info.push([
                    'Identities',
                    `${numIdentity}/${length} (${percent(numIdentity / length)})`
                ]);
            }
            if (!notDefined(queryCoverageSubject)) {
                info.push([
                    'Coverage',
                    `${queryCoverageSubject}%`
                ]);
            }
            if (!notDefined(gaps) && !notDefined(length)) {
                info.push([
                    'Gaps',
                    `${gaps}/${length} (${percent(gaps / length)})`
                ]);
            }
            if (!notDefined(mismatch)) {
                info.push(['Mismatches', `${mismatch}`]);
            }
            if (!notDefined(queryStart) && queryStart > 1) {
                info.push([
                    'Unaligned 5\'',
                    `(1..${queryStart - 1}) ${queryStart - 1}`
                ]);
            }
            if (!notDefined(queryEnd) && queryEnd < queryLength) {
                info.push([
                    'Unaligned 3\'',
                    `(${queryEnd + 1}..${queryLength}) ${queryLength - queryEnd}`
                ]);
            }
        }
        return info;
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
        this.dispatcher.removeListener(BLASTResultEvents.changed, this.reload);
    }
}
