import * as PIXI from 'pixi.js-legacy';
import {CachedTrackRendererWithVerticalScroll} from '../../../../core';
import BarChartSourceRenderer from './bar-chart/bar-chart-source-renderer';
import {PlaceholderRenderer} from '../../../../utilities';

export default class FCBarChartRenderer extends CachedTrackRendererWithVerticalScroll {
    get config() {
        return this._config;
    }

    constructor(track, config, pixiRenderer) {
        super(track);
        this._track = track;
        this._config = config;
        this._pixiRenderer = pixiRenderer;
        this._showPlaceholder = false;
        this._visibleSources = [];
        this.background = new PIXI.Graphics();
        this.container.addChild(this.background);
        // to ensure z-index:
        this.container.addChild(this.dataContainer);
        // to ensure z-index:
        this.container.addChild(this.verticalScroll);
        this.placeholder = new PlaceholderRenderer(this._track);
        this.noSourcesPlaceholder = new PlaceholderRenderer(this._track);
        this.noDataPlaceholder = new PlaceholderRenderer(this._track);
        this.container.addChild(this.placeholder.container);
        this.container.addChild(this.noSourcesPlaceholder.container);
        this.container.addChild(this.noDataPlaceholder.container);
        this.sourceRenderers = [];
        this.initializeCentralLine();
    }

    registerGroupAutoScaleManager(manager) {
        this.groupAutoScaleManager = manager;
        this.sourceRenderers.forEach((renderer: BarChartSourceRenderer) => {
            renderer.registerGroupAutoScaleManager(manager);
        });
    }

    registerFCSourcesManager(manager) {
        this.fcSourcesManager = manager;
        this.sourceRenderers.forEach((renderer: BarChartSourceRenderer) => {
            renderer.registerFCSourcesManager(manager);
        });
    }

    get visibleSources () {
        return this._visibleSources;
    }

    set visibleSources (visibleSources) {
        this._visibleSources = (visibleSources || []).slice();
    }

    get showPlaceholder () {
        return this._showPlaceholder;
    }

    set showPlaceholder (showPlaceholder) {
        this._showPlaceholder = showPlaceholder;
        this.placeholder.container.visible = showPlaceholder;
        this.dataContainer.visible = !showPlaceholder;
        this.background.visible = !showPlaceholder;
    }

    get singleColors () {
        return this._singleColors;
    }

    set singleColors (singleColors) {
        this._singleColors = singleColors;
    }

    get grayScaleColors () {
        return this._grayScaleColors;
    }

    set grayScaleColors (grayScaleColors) {
        this._grayScaleColors = grayScaleColors;
    }

    get sourcesCount () {
        return this.sourceRenderers.length;
    }

    get actualHeight() {
        if (this.showPlaceholder) {
            return this.height;
        }
        const minimumHeight = this.sourcesCount * this._config.barChart.minimumHeight;
        return Math.max(this.height, minimumHeight);
    }

    get subTrackHeight () {
        if (this.sourcesCount === 0) {
            return 0;
        }
        return Math.floor(this.actualHeight / this.sourcesCount);
    }

    get movingSource () {
        return this._movingSource;
    }

    set movingSource (source) {
        this._movingSource = source;
    }

    get sourceOrders () {
        return this._sourceOrders || [];
    }

    set sourceOrders (source) {
        this._sourceOrders = source;
    }

    getVisibleSources = () => {
        const getSourceOrder = source => {
            const index = this.sourceOrders.indexOf(source);
            if (index === -1) {
                return Infinity;
            }
            return index;
        };
        const sources = this.sourceRenderers
            .map((renderer: BarChartSourceRenderer) => renderer.source)
            .filter(source => !this.sourceIsDisabled(source))
            .sort((a, b) => getSourceOrder(a) - getSourceOrder(b));
        if (this.movingSource) {
            const {
                source: movingSource,
                position,
                delta
            } = this.movingSource;
            const index = sources.indexOf(movingSource);
            if (index >= 0) {
                sources.splice(index, 1);
                const newIndex = Math.round((position - delta) / this.subTrackHeight);
                sources.splice(newIndex, 0, movingSource);
            }
        }
        return sources;
    };

    getSourcePosition = (source) => {
        const subTrackHeight = this.subTrackHeight;
        const correct = o => Math.max(0, Math.min(this.actualHeight - subTrackHeight, o));
        const sources = this.getVisibleSources();
        if (this.movingSource) {
            const {
                source: movingSource,
                position,
                delta = 0
            } = this.movingSource;
            if (movingSource === source) {
                return correct(position - delta);
            }
        }
        const index = sources.indexOf(source);
        if (index >= 0) {
            return correct(index * subTrackHeight);
        }
        return correct(sources.length * subTrackHeight);
    };

    startSourceMoving (source, position) {
        if (source) {
            this.hoverItem(null);
            const subTrackHeight = this.subTrackHeight;
            const positionCorrected = position.y - this.dataContainer.y;
            this.movingSource = {
                source,
                start: positionCorrected,
                position: positionCorrected,
                delta: positionCorrected - Math.floor(positionCorrected / subTrackHeight) * subTrackHeight
            };
        } else {
            this.movingSource = undefined;
        }
        this.rearrangeSources();
        return !!this.movingSource;
    }

    moveSource (position) {
        if (this.movingSource) {
            this.movingSource.position = position.y - this.dataContainer.y;
            this.rearrangeSources();
            return true;
        }
        return false;
    }

    finishSourceMoving (position) {
        if (this.moveSource(position)) {
            this.sourceOrders = this.getVisibleSources();
            this.movingSource = undefined;
            this.rearrangeSources();
            return true;
        }
        return false;
    }

    checkPosition(position) {
        if (this.showPlaceholder || this.movingSource) {
            return null;
        }
        let {x, y} = position;
        x -= this.dataContainer.x;
        y -= this.dataContainer.y;
        for (let r = 0; r < this.sourceRenderers.length; r++) {
            const renderer: BarChartSourceRenderer = this.sourceRenderers[r];
            if (renderer.y <= y && y <= renderer.y + this.subTrackHeight) {
                const localX = x;
                const localY = y - renderer.y;
                return renderer.checkFeatures({x: localX, y: localY});
            }
        }
        return null;
    }

    checkSourceTitle(position) {
        if (this.showPlaceholder || this.sourceRenderers.length < 2) {
            return undefined;
        }
        const [renderer] = this.sourceRenderers.filter((renderer: BarChartSourceRenderer) => {
            let {x1, x2, y1, y2} = renderer.dragAndDropZone;
            x1 += this.dataContainer.x + renderer.x;
            x2 += this.dataContainer.x + renderer.x;
            y1 += this.dataContainer.y + renderer.y;
            y2 += this.dataContainer.y + renderer.y;
            const {x, y} = position;
            return x1 <= x && x <= x2 && y1 <= y && y <= y2;
        });
        return renderer ? renderer.source : undefined;
    }

    scroll(viewport, delta) {
        this.hoverItem(null);
        super.scroll(viewport, delta);
    }

    getSourceRenderer (source, create = false) {
        const [renderer] = this.sourceRenderers
            .filter((renderer: BarChartSourceRenderer) => renderer.source === source);
        if (renderer) {
            return renderer;
        }
        if (create) {
            const newRenderer = new BarChartSourceRenderer(this._track, this._config, source);
            newRenderer.registerGroupAutoScaleManager(this.groupAutoScaleManager);
            newRenderer.registerFCSourcesManager(this.fcSourcesManager);
            this.sourceRenderers.push(newRenderer);
            this.dataContainer.addChild(newRenderer);
            return newRenderer;
        }
        return undefined;
    }

    removeSourceRenderer (source) {
        const renderer = this.getSourceRenderer(source);
        if (renderer) {
            const index = this.sourceRenderers.indexOf(renderer);
            if (index >= 0) {
                this.dataContainer.removeChild(renderer);
                renderer.destroy(true);
                this.sourceRenderers.splice(index, 1);
            }
        }
    }

    sourceIsDisabled (source) {
        return (
            this.visibleSources &&
            this.visibleSources.length > 0 &&
            !this.visibleSources.includes(source)
        );
    }

    render(viewport, ...opts) {
        super.render(viewport, ...opts);
        if (this.showPlaceholder) {
            this.placeholder.init(
                'Zoom in to see features',
                {
                    height: this._pixiRenderer.height,
                    width: this._pixiRenderer.width
                });
        }
        this.background.clear();
        this.background
            .beginFill(this.config.barChart.background, 1)
            .drawRect(0, 0, viewport.canvasSize, this.height)
            .endFill();
    }

    translateContainer(viewport, cache){
        this.createDrawScope(viewport, cache);
        this.sourceRenderers.forEach((sourceRenderer: BarChartSourceRenderer) => {
            sourceRenderer.translate(this._drawScope);
        });
    }

    rebuildContainer(viewport, cache){
        if (this.showPlaceholder) {
            this.noDataPlaceholder.container.visible = false;
            this.noSourcesPlaceholder.container.visible = false;
            return;
        }
        this.createDrawScope(viewport, cache);
        this.noSourcesPlaceholder.init(
            'No sources found',
            {
                height: this._pixiRenderer.height,
                width: this._pixiRenderer.width
            });
        this.noDataPlaceholder.init(
            'No data found',
            {
                height: this._pixiRenderer.height,
                width: this._pixiRenderer.width
            });
        const {
            coordinateSystem,
            sources: sourcesData = {},
            data: items = []
        } = cache || {};
        const {
            sources = [],
            values: data = {}
        } = sourcesData;
        this.noSourcesPlaceholder.container.visible = cache &&
            cache.sources &&
            cache.data &&
            cache.data.length > 0 &&
            sources.length === 0;
        // console.log('this.noSourcesPlaceholder.container.visible', this.noSourcesPlaceholder.container.visible);
        this.noDataPlaceholder.container.visible = cache && (!cache.data || cache.data.length === 0);
        this.dataContainer.visible = sources.length > 0;
        this.background.visible = sources.length > 0;
        // removing unused source renderers:
        const sourceRenderersToRemove = this.sourceRenderers.filter((sourceRenderer: BarChartSourceRenderer) => {
            const {disabled = true} = data[sourceRenderer.source] || {};
            return this.sourceIsDisabled(sourceRenderer.source) ||
                !sources.includes(sourceRenderer.source) ||
                disabled;
        })
            .map((renderer: BarChartSourceRenderer) => renderer.source);
        sourceRenderersToRemove.forEach(this.removeSourceRenderer.bind(this));
        // attaching new source renderers:
        sources.forEach((source: string) => {
            const {disabled = false} = data[source] || {};
            if (!disabled && !this.sourceIsDisabled(source)) {
                this.getSourceRenderer(source, true);
            }
        });
        // rendering sources data:
        sources.forEach((source: string) => {
            const {disabled = false} = data[source] || {};
            if (!disabled && !this.sourceIsDisabled(source)) {
                const renderer = this.getSourceRenderer(source, true);
                renderer.renderData(
                    viewport,
                    items,
                    coordinateSystem,
                    {
                        height: this.subTrackHeight,
                        singleColors: this.singleColors,
                        grayScaleColors: this.grayScaleColors,
                        renderBottomBorder: sources.length > 1
                    }
                );
            }
        });
        this.scroll(viewport, 0);
        this.rearrangeSources();
    }

    rearrangeSources () {
        this.sourceRenderers.forEach((renderer: BarChartSourceRenderer) => {
            if (this.movingSource && this.movingSource.source === renderer.source) {
                // ensure z-index:
                this.dataContainer.addChild(renderer);
            }
            renderer.y = this.getSourcePosition(renderer.source);
        });
    }

    hoverItem (items, viewport, cache) {
        const [hoveredItem] = items || [];
        const changed = this.hoveredItem !== hoveredItem;
        if (changed) {
            const hover = (source, item) => {
                const renderer = this.getSourceRenderer(source);
                if (renderer) {
                    renderer.hover(
                        viewport,
                        item ? item.feature : undefined,
                        cache ? cache.coordinateSystem : undefined,
                        {
                            height: this.subTrackHeight,
                            singleColors: this.singleColors,
                            grayScaleColors: this.grayScaleColors
                        }
                    );
                }
            };
            if (this.hoveredItem) {
                hover(this.hoveredItem.source, undefined);
            }
            if (hoveredItem) {
                hover(hoveredItem.source, hoveredItem);
            }
            this.hoveredItem = hoveredItem;
        }
        return changed;
    }
}
