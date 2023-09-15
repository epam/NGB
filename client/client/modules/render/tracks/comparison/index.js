import ComparisonAlignmentRenderer from './renderer';
import ComparisonConfig from './comparisonConfig';
import {TargetGenomicsResultEvents} from '../../../../app/shared/targetContext';
import {CachedTrackWithVerticalScroll} from '../../core';

export class ComparisonTrack extends CachedTrackWithVerticalScroll {
    static getTrackDefaultConfig() {
        return ComparisonConfig;
    }

    renderer: ComparisonAlignmentRenderer;

    get verticalScrollRenderer() {
        return this.renderer;
    }

    get trackIsResizable() {
        return true;
    }

    constructor(opts) {
        super(opts);
        this.targetContext = opts.targetContext;
        this.dispatcher = opts.dispatcher;
        this.renderer = new ComparisonAlignmentRenderer(
            Object.assign({}, this.trackConfig, this.config),
            opts,
            opts.targetContext,
            this
        );
        this.reload = this.reload.bind(this);
        this.dispatcher.on(TargetGenomicsResultEvents.changed, this.reload);
    }

    async updateCache() {
        const updated = super.updateCache();
        if (updated && this.cache && this.targetContext) {
            this.cache.data = this.targetContext.alignments || [];
            return true;
        }
        return false;
    }

    reload () {
        this.invalidateCache();
        this._flags.renderFeaturesChanged = true;
        this.requestRenderRefresh();
    }

    onHover({x, y}) {
        if (super.onHover({x, y})) {
            this.tooltip.hide();
            const checkPositionResult = this.renderer.checkPosition(this.viewport, this.cache,
                {x, y});
            if (this.hoveringEffects && this.renderer.hoverItem(checkPositionResult, this.viewport)) {
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
            this.renderer.hoverItem(undefined, this.viewport)
        ) {
            this.requestRenderRefresh();
        }
    }

    getTooltipDataObject(alignment) {
        const info = [];
        if (alignment && alignment.alignment) {
            const {
                length,
                numIdentity,
                gaps,
            } = alignment.alignment;
            const notDefined = o => o === undefined;
            const percent = o => `${Math.round(o * 100)}%`;
            if (!notDefined(numIdentity) && !notDefined(length)) {
                info.push([
                    'Identities',
                    `${numIdentity}/${length} (${percent(numIdentity / length)})`
                ]);
            }
            if (!notDefined(gaps) && !notDefined(length)) {
                info.push([
                    'Gaps',
                    `${gaps}/${length} (${percent(gaps / length)})`
                ]);
            }
            if (!notDefined(gaps) && !notDefined(length) && !notDefined(numIdentity)) {
                info.push(['Mismatches', `${length - numIdentity - gaps}`]);
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
        this.dispatcher.removeListener(TargetGenomicsResultEvents.changed, this.reload);
    }
}
