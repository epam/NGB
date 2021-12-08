import * as PIXI from 'pixi.js-legacy';
import {EventVariationInfo} from '../../../../app/shared/utils/events';
import {linearDimensionsConflict} from '../../utilities';
import {
    StatisticsContainer,
    VCFCollapsedRenderer,
    VCFSampleCoverageRenderer,
    VCFSampleRenderer,
    VCFSamplesScroll,
    VariantContainer
} from './internal';
import CoordinateSystem from '../common/coordinateSystemRenderer';
import Menu from '../../core/menu';
import {MultiSampleVcfTransformer} from './internal';
import {VCFTrack} from './index';
import VcfConfig from './multi-sample-vcf-config';
import {default as menu} from './menu';
import {variantsView} from './modes';

import {STRAINS_COUNT} from './internal/data/multi-sample-vcf-transformer';

export class MultiSampleVCFTrack extends VCFTrack {
    sampleGraphics = new PIXI.Graphics();
    sampleTitlesContainer = new PIXI.Container();
    sampleScroller = new VCFSamplesScroll(VcfConfig);

    static getTrackDefaultConfig() {
        return VcfConfig;
    }

    static postStateMutatorFn = (track, key, prePayload) => {
        const {oldVariantsView} = prePayload || {};
        if (oldVariantsView !== track.state.variantsView) {
            track.height = VcfConfig.defaultHeight(track.state);
            if (track.sampleScroller) {
                track.sampleScroller.reset();
            }
        }
    };

    static Menu = Menu(
        menu,
        {
            postStateMutatorFn: [
                MultiSampleVCFTrack.postStateMutatorFn,
                VCFTrack.postStateMutatorFn
            ],
            preStateMutatorFn: VCFTrack.preStateMutatorFn
        }
    );

    get transformer () {
        if (!this._transformer) {
            this._transformer = new MultiSampleVcfTransformer(
                this.trackConfig,
                this.config.chromosome,
                this._highlightProfileConditions
            );
        }
        return this._transformer;
    }

    get totalSamplesHeight () {
        return (this.samples || []).length * VcfConfig.getSampleHeight(this.state);
    }

    constructor(opts) {
        super(opts);
        this.samples = [];
        this.renderers = [];
        this.coverageCoordinateSystemRenderer = new CoordinateSystem(this);
        this.coverageRenderer = new VCFSampleCoverageRenderer(VcfConfig, this);
        this.sampleScroller.x = this.viewport.canvasSize -
            VcfConfig.scroll.margin -
            VcfConfig.scroll.width;
        this.sampleScroller.y = VcfConfig.coverageHeight;
        this.coverageMask = new PIXI.Graphics();
        this.initializeSamples((new Array(STRAINS_COUNT)).fill({}).map((o, i) => `Strain #${i + 1}`));
    }

    initializeSamples (samples = []) {
        //todo: get samples
        this.samples = samples.slice();
        this.renderers = this.samples.map((sample) => ({
            sample,
            collapsed: new VCFSampleRenderer(VcfConfig, this),
            expanded: new VCFCollapsedRenderer(VcfConfig, this)
        }));
        this._flags.renderReset = true;
        this.requestRenderRefresh();
    }

    applyAdditionalRequestParameters(params) {
        params.collapsed = false;
    }

    updateCacheData (multiSampleVcfData) {
        const {
            data,
            coverage
        } = multiSampleVcfData;
        super.updateCacheData(data);
        if (this.cache) {
            delete this.cache.coverage;
            this.cache.coverage = coverage || [];
        }
    }

    // --- Scroller ---

    isScrollable() {
        return this.totalSamplesHeight > this.height - VcfConfig.coverageHeight;
    }

    canScroll(delta) {
        return this.isScrollable() &&
            (
                (delta > 0 && this.sampleScroller.scrollPosition > 0) ||
                (
                    delta < 0 &&
                    (
                        this.sampleScroller.scrollPosition +
                        this.height -
                        VcfConfig.coverageHeight) < this.totalSamplesHeight
                )
            );
    }

    scrollIndicatorBoundaries() {
        const boundaries = this.sampleScroller.scrollIndicatorBoundaries;
        if (boundaries) {
            return {
                x: this.sampleScroller.x,
                y: this.sampleScroller.y + boundaries.y,
                width: boundaries.width,
                height: boundaries.height
            };
        }
        return undefined;
    }

    getScrollPosition() {
        const boundaries = this.scrollIndicatorBoundaries();
        if (boundaries) {
            return boundaries.y;
        }
        return 0;
    }

    correctScrollPosition () {
        const min = 0;
        const max = Math.max(0, this.totalSamplesHeight - this.height + VcfConfig.coverageHeight);
        this.sampleScroller.scrollPosition = Math.max(
            min,
            Math.min(
                max,
                this.sampleScroller.scrollPosition
            )
        );
        this.sampleTitlesContainer.y = -this.sampleScroller.scrollPosition;
        this.sampleGraphics.y = -this.sampleScroller.scrollPosition;
        this.renderers.forEach((rendererBySample, index) => {
            const renderer = this.pickRenderer(rendererBySample);
            renderer.container.y = this.getRendererY(index);
            renderer.container.visible = this.rendererVisible(index);
        });
    }

    correctScrollMask () {
        this.coverageMask
            .beginFill(0x000000, 1)
            .drawRect(
                0,
                VcfConfig.coverageHeight,
                this.viewport.canvasSize,
                this.height - VcfConfig.coverageHeight
            )
            .endFill();
        this.sampleTitlesContainer.mask = this.coverageMask;
        this.sampleGraphics.mask = this.coverageMask;
        this.renderers.forEach((rendererBySample) => {
            const renderer = this.pickRenderer(rendererBySample);
            renderer.container.mask = this.coverageMask;
        });
    }

    onScroll({delta}) {
        if (this.tooltip) {
            this.tooltip.hide();
        }
        this.sampleScroller.scrollPosition = this.sampleScroller.scrollPosition - delta;
        this.correctScrollPosition();
        this.sampleScroller.renderScroller();
        this.updateScene();
    }

    setScrollPosition(value) {
        this.sampleScroller.scrollPosition = this.sampleScroller.getWorldPosition(
            value - this.sampleScroller.y
        );
        this.correctScrollPosition();
        this.sampleScroller.renderScroller();
        this.updateScene();
    }

    hoverVerticalScroll() {
        if (!this.sampleScroller.scrollerHovered) {
            this.sampleScroller.scrollerHovered = true;
            return true;
        }
        return false;
    }

    unhoverVerticalScroll() {
        if (this.sampleScroller.scrollerHovered) {
            this.sampleScroller.scrollerHovered = false;
            return true;
        }
        return false;
    }

    _positionFitsScrollIndicatorBoundaries(position) {
        const boundaries = this.scrollIndicatorBoundaries();
        return boundaries && position.x >= boundaries.x && position.x <= boundaries.x + boundaries.width &&
            position.y >= boundaries.y && position.y <= boundaries.y + boundaries.height;
    }

    // ------

    getRendererY (index, ignoreScroll = false) {
        return VcfConfig.coverageHeight +
            index * VcfConfig.getSampleHeight(this.state) -
            (ignoreScroll ? 0 : this.sampleScroller.scrollPosition);
    }

    rendererVisible (index) {
        const y1 = this.getRendererY(index);
        const y2 = y1 + VcfConfig.getSampleHeight(this.state);
        return linearDimensionsConflict(
            y1,
            y2,
            VcfConfig.coverageHeight,
            this.height
        );
    }

    pickRenderer (renderer) {
        if (this.state.variantsView === variantsView.variantsViewExpanded) {
            return renderer.expanded;
        }
        return renderer.collapsed;
    }

    pickCache (sample) {
        const {
            data = [],
            ...rest
        } = this.cache || {};
        const [dataBySample] = data.filter(o => o.sample === sample);
        return {
            ...rest,
            data: dataBySample ? dataBySample.data : undefined
        };
    }

    renderSampleGraphics () {
        if (!this.sampleGraphics) {
            return;
        }
        this.sampleGraphics.clear();
        if (this.state.variantsView === variantsView.variantsViewExpanded) {
            this.sampleGraphics.lineStyle(
                1,
                VcfConfig.sample.divider.stroke,
                1
            );
            this.samples.forEach((sample, index) => {
                const y = this.getRendererY(index, true);
                this.sampleGraphics
                    .moveTo(0, y)
                    .lineTo(this.viewport.canvasSize, y);
            });
        }
    }

    renderSampleTitles () {
        if (!this.sampleTitlesContainer) {
            return;
        }
        this.sampleTitlesContainer.removeChildren();
        const backgroundGraphics = new PIXI.Graphics();
        this.sampleTitlesContainer.addChild(backgroundGraphics);
        this.samples.forEach((sample, index) => {
            const y = this.getRendererY(index, true);
            const title = this.labelsManager.getLabel(
                sample,
                VcfConfig.sample.label.font
            );
            title.x = VcfConfig.sample.label.margin;
            if (this.state.variantsView === variantsView.variantsViewExpanded) {
                title.y = Math.round(
                    y + VcfConfig.sample.label.margin
                );
            } else {
                title.y = Math.round(
                    y + VcfConfig.getSampleHeight(this.state) / 2.0 - title.height / 2.0
                );
            }
            this.sampleTitlesContainer.addChild(title);
            backgroundGraphics
                .beginFill(0xffffff, 0.75)
                .drawRect(
                    title.x - 1,
                    title.y,
                    title.width + 2,
                    title.height
                )
                .endFill();
        });
    }

    render(flags) {
        let somethingChanged = false;
        if (flags.renderReset) {
            this.height = this.height;// force track to resize
            this.container.removeChildren();
            this.container.addChild(this.coverageCoordinateSystemRenderer);
            this.container.addChild(this.coverageRenderer.container);
            this.container.addChild(this._zoomInRenderer.container);
            this._zoomInRenderer.init(this._getZoomInPlaceholderText(), {
                height: this._pixiRenderer.height,
                width: this._pixiRenderer.width
            });
            somethingChanged = true;
            this.container.addChild(this.sampleGraphics);
            this.renderSampleGraphics();
            this.renderSampleTitles();
            this.renderers.forEach((rendererBySample, index) => {
                rendererBySample.collapsed.clear();
                const renderer = this.pickRenderer(rendererBySample);
                renderer.container.y = this.getRendererY(index);
                renderer.height = VcfConfig.getSampleHeight(this.state);
                this.container.addChild(renderer.container);
            });
            this.container.addChild(this.sampleTitlesContainer);
            this.container.addChild(this.sampleScroller);
            this.correctScrollPosition();
        } else if (flags.widthChanged || flags.heightChanged) {
            this.renderSampleGraphics();
            this._zoomInRenderer.init(this._getZoomInPlaceholderText(), {
                height: this._pixiRenderer.height,
                width: this._pixiRenderer.width
            });
        }
        const zoomInPlaceholderVisible = this._variantsMaximumRange < this.viewport.actualBrushSize;
        this._zoomInRenderer.container.visible = zoomInPlaceholderVisible;
        if (this.coverageCoordinateSystemRenderer) {
            this.coverageCoordinateSystemRenderer.visible = !zoomInPlaceholderVisible;
        }
        if (this.coverageRenderer) {
            this.coverageRenderer.container.visible = !zoomInPlaceholderVisible;
        }
        if (this.sampleGraphics) {
            this.sampleGraphics.visible = !zoomInPlaceholderVisible;
        }
        if (this.sampleTitlesContainer) {
            this.sampleTitlesContainer.visible = !zoomInPlaceholderVisible;
        }
        if (this.sampleScroller) {
            this.sampleScroller.visible = !zoomInPlaceholderVisible;
        }
        if (zoomInPlaceholderVisible) {
            this.sampleScroller.totalHeight = 0;
        }
        if (flags.heightChanged) {
            this.correctScrollMask();
        }
        if (
            flags.dataChanged
        ) {
            this.coverageCoordinateSystemRenderer.renderCoordinateSystem(
                this.viewport,
                this.cache.coverage,
                VcfConfig.coverageHeight,
                {
                    renderBaseLineAsBottomBorder: false
                }
            );
        }
        if (
            flags.brushChanged ||
            flags.widthChanged ||
            flags.heightChanged ||
            flags.renderReset ||
            flags.dataChanged
        ) {
            this.correctScrollPosition();
            if (!zoomInPlaceholderVisible) {
                this.sampleScroller.x = this.viewport.canvasSize -
                    VcfConfig.scroll.margin -
                    VcfConfig.scroll.width;
                this.sampleScroller.y = VcfConfig.coverageHeight;
                this.sampleScroller.totalHeight = this.totalSamplesHeight;
                this.sampleScroller.displayedHeight = this.height - VcfConfig.coverageHeight;
                this.sampleScroller.renderScroller(false);
            }
            this.coverageRenderer.height = VcfConfig.coverageHeight;
            this.coverageRenderer.render(
                this.viewport,
                this.cache,
                flags.heightChanged || flags.dataChanged,
                this._showCenterLine
            );
            this.renderers.forEach((rendererBySample, index) => {
                const renderer = this.pickRenderer(rendererBySample);
                renderer.height = VcfConfig.getSampleHeight(this.state);
                renderer.container.visible = this.rendererVisible(index) &&
                    this._variantsMaximumRange >= this.viewport.actualBrushSize;
                renderer.render(
                    this.viewport,
                    this.pickCache(rendererBySample.sample),
                    flags.heightChanged || flags.dataChanged || flags.renderReset,
                    this._showCenterLine
                );
            });
            somethingChanged = true;
        }
        return somethingChanged;
    }

    getMousePositionOverSample ({x, y}) {
        if (y < VcfConfig.coverageHeight) {
            return {
                x,
                y,
                coverage: true
            };
        }
        for (let index = 0; index < this.samples.length; index += 1) {
            const sample = this.samples[index];
            const sampleY1 = this.getRendererY(index);
            const sampleY2 = this.getRendererY(index + 1);
            if (sampleY1 <= y && y <= sampleY2) {
                return {
                    x,
                    y: y - sampleY1,
                    sample
                };
            }
        }
        return {x, y};
    }

    onVariantClicked (variant, position) {
        const variantRequest = new EventVariationInfo(
            {
                chromosome: {
                    id: this.config.chromosome.id,
                    name: this.config.chromosome.name
                },
                id: variant.identifier,
                position: variant.serverStartIndex,
                type: variant.type,
                vcfFileId: this.dataConfig.id,
                projectId: this.config.projectId,
                projectIdNumber: this.config.project.id
            }
        );
        if (this.dataItemClicked !== null && this.dataItemClicked !== undefined) {
            this.dataItemClicked(this, variantRequest, {name: 'variant-request', position});
        }
    }

    onClick(originalPoint) {
        const {
            x,
            y,
            coverage,
            sample
        } = this.getMousePositionOverSample(originalPoint);
        if (!coverage && sample) {
            const [rendererBySample] = this.renderers.filter(o => o.sample === sample);
            if (rendererBySample) {
                const renderer = this.pickRenderer(rendererBySample);
                const clickResult = renderer.onClick({x, y});
                if (clickResult && clickResult instanceof StatisticsContainer) {
                    this.onStatisticsClicked(clickResult);
                } else if (clickResult && clickResult instanceof VariantContainer) {
                    this.onVariantContainerClicked(clickResult, originalPoint);
                } else if (clickResult) {
                    this.onVariantClicked(clickResult, originalPoint);
                }
            }
        }
    }

    onHover(originalPoint) {
        if (this._positionFitsScrollIndicatorBoundaries(originalPoint)) {
            if (this.hoverVerticalScroll()) {
                this.updateScene();
            }
            this.tooltip.hide();
            this._lastHovered = undefined;
            this.requestAnimation();
            return false;
        }
        else if (this.unhoverVerticalScroll()) {
            this.updateScene();
        }
        const {
            x,
            y,
            coverage,
            sample
        } = this.getMousePositionOverSample(originalPoint);
        let hoveredItem;
        let tooltipShown = false;
        if (coverage) {
            hoveredItem = this.coverageRenderer.onMove({x, y});
            if (hoveredItem) {
                const chromosomePrefix = this.config &&
                    this.config.chromosome &&
                    this.config.chromosome.name
                    ? this.config.chromosome.name.concat(': ')
                    : '';
                const tooltip = [
                    ['Count', hoveredItem.coverage.total],
                    this.viewport.factor > 1
                        ? undefined
                        : ['Position', `${chromosomePrefix}${hoveredItem.startIndex}`],
                    ...(
                        Object
                            .keys(hoveredItem.coverage)
                            .filter(o => o !== 'total')
                            .sort((a, b) => b.length - a.length)
                            .map(key => [key, hoveredItem.coverage[key]])
                    )
                ].filter(Boolean);
                this.tooltip.setContent(tooltip);
                this.tooltip.move(originalPoint);
                this.tooltip.show(originalPoint);
                tooltipShown = true;
            }
            this.renderers
                .forEach(rendererBySample => {
                    const renderer = this.pickRenderer(rendererBySample);
                    renderer.onMove();
                });
        } else if (sample) {
            const [rendererBySample] = this.renderers.filter(o => o.sample === sample);
            this.coverageRenderer.onMove();
            this.renderers
                .filter(o => o.sample !== sample)
                .forEach(rendererBySample => {
                    const renderer = this.pickRenderer(rendererBySample);
                    renderer.onMove();
                });
            if (rendererBySample) {
                const renderer = this.pickRenderer(rendererBySample);
                hoveredItem = renderer.onMove({x, y});
                if (
                    hoveredItem &&
                    !(hoveredItem instanceof StatisticsContainer) &&
                    this.shouldDisplayTooltips
                ) {
                    const tooltip = [
                        ['Sample', sample],
                        ['Chromosome', this.config.chromosome.name],
                        ['Start', hoveredItem.variant.startIndex],
                        ['End', hoveredItem.variant.endIndex],
                        ['ID', hoveredItem.variant.identifier],
                        ['Type', hoveredItem.variant.type],
                        ['REF', hoveredItem.variant.referenceAllele],
                        ['ALT', hoveredItem.variant.alternativeAlleles.join(', ')],
                        ['Quality', hoveredItem.variant.quality && hoveredItem.variant.quality.toFixed(2)]
                    ];
                    const filters = hoveredItem.variant.failedFilters || [];
                    if (filters.length > 0) {
                        tooltip.push(['Filter', filters.map(filter => filter.value).join(', ')]);
                    }
                    this.tooltip.setContent(tooltip);
                    this.tooltip.move(originalPoint);
                    this.tooltip.show(originalPoint);
                    tooltipShown = true;
                }
            }
        }
        if (!tooltipShown) {
            this.tooltip.hide();
        }
        if (hoveredItem !== this._lastHovered) {
            this._lastHovered = hoveredItem;
            this.requestAnimation();
            return false;
        }
        return true;
    }

    unhoverRenderer () {
        this._lastHovered = null;
        this.coverageRenderer.onMove();
        this.renderers.forEach(rendererBySample => {
            const renderer = this.pickRenderer(rendererBySample);
            renderer.onMove();
        });
    }

    animate(time) {
        let animate = this.coverageRenderer.animate();
        this.renderers.forEach(rendererBySample => {
            const renderer = this.pickRenderer(rendererBySample);
            animate = renderer.animate(time) || animate;
        });
        return animate;
    }
}
