import * as PIXI from 'pixi.js-legacy';
import HeatmapColorScheme, {ColorFormats} from './color-scheme';
import HeatmapColorSchemeRenderer from './renderer/color-scheme-renderer';
import HeatmapData from './heatmap-data';
import HeatmapDataRenderer from './renderer/data-renderer';
import HeatmapEventDispatcher from './utilities/heatmap-event-dispatcher';
import HeatmapInteractions from './interactions';
import HeatmapViewOptions from './heatmap-view-options';
import HeatmapViewport from './viewport';
import HeatmapViewportRenderer from './renderer/viewport-renderer';
import LabelsManager from '../core/labelsManager';
import makeInitializable from './utilities/make-initializable';
import {parseOffset} from '../utilities';

const ZERO_PADDING = parseOffset(0);

class HeatmapView extends HeatmapEventDispatcher {
    /**
     *
     * @param {HeatmapOptions} options
     */
    constructor(options = {}) {
        super();
        makeInitializable(this);
        /**
         * Holds heatmap data & metadata
         * @type {HeatmapData}
         */
        this.heatmapData = new HeatmapData();
        /**
         * Holds heatmap viewport info
         * @type {HeatmapViewport}
         */
        this.heatmapViewport = new HeatmapViewport();
        /**
         * Heatmap color scheme configuration
         * @type {ColorScheme}
         */
        this.colorScheme = new HeatmapColorScheme();
        /**
         * Heatmap view options (color scheme, dendrogram)
         * @type {HeatmapViewOptions}
         */
        this.options = new HeatmapViewOptions(this.heatmapData, this.colorScheme);

        this.heatmapData.onMetadataLoaded(this.metadataLoaded.bind(this));
        this.heatmapViewport.onViewportChanged(this.render.bind(this));
        this.colorScheme.onInitialized(this.render.bind(this));
        this.colorScheme.onChanged(this.render.bind(this));
        const renderLoop = () => {
            if (this.needsRender) {
                this.needsRender = false;
                this.render();
            }
            this.renderLoopId = requestAnimationFrame(renderLoop);
        };
        this.stopRenderLoop = () => {
            cancelAnimationFrame(this.renderLoopId);
        };
        this.initialize(options);
    }

    /**
     *
     * @returns {LabelsManager}
     */
    get labelsManager() {
        if (!this._labelsManager && this.pixiRenderer) {
            this._labelsManager = new LabelsManager(this.pixiRenderer);
        }
        return this._labelsManager;
    }

    get width() {
        return this._width || 0;
    }

    set width(width) {
        if (this._width !== width) {
            this._width = width;
            this.needsRender = true;
        }
    }

    get height() {
        return this._height || 0;
    }

    set height(height) {
        if (this._height !== height) {
            this._height = height;
            this.needsRender = true;
        }
    }

    get padding() {
        return this._padding || ZERO_PADDING;
    }

    /**
     *
     * @param {number|number[]} padding
     */
    set padding(padding) {
        const parsed = parseOffset(padding);
        const {
            top: currentTop = 0,
            right: currentRight = 0,
            bottom: currentBottom = 0,
            left: currentLeft = 0
        } = this._padding || {};
        const {
            top = currentTop,
            right = currentRight,
            bottom = currentBottom,
            left = currentLeft
        } = parsed;
        if (
            top !== currentTop ||
            right !== currentRight ||
            bottom !== currentBottom ||
            left !== currentLeft
        ) {
            this._padding = {
                top,
                right,
                bottom,
                left
            };
            this.needsRender = true;
        }
    }

    destroyDisplayObjects() {
        if (this._labelsManager) {
            this._labelsManager.destroy();
            this._labelsManager = undefined;
        }
        if (this.heatmapInteractions) {
            this.heatmapInteractions.destroy();
            this.heatmapInteractions = undefined;
        }
        if (this.viewportRenderer) {
            this.viewportRenderer.destroy();
            this.viewportRenderer = undefined;
        }
        if (this.colorSchemeRenderer) {
            this.colorSchemeRenderer.destroy();
            this.colorSchemeRenderer = undefined;
        }
        if (this.dataRenderer) {
            this.dataRenderer.destroy();
            this.dataRenderer = undefined;
        }
    }

    destroy() {
        super.destroy();
        if (typeof this.removeDispatcherListeners === 'function') {
            this.removeDispatcherListeners();
            this.removeDispatcherListeners = undefined;
        }
        if (typeof this.stopRenderLoop === 'function') {
            this.stopRenderLoop();
            this.stopRenderLoop = undefined;
        }
        this.domElement = undefined;
        this.pixiRenderer = undefined;
        if (this.container) {
            this.container.removeChildren();
            this.container = undefined;
        }
        this.destroyDisplayObjects();
        this.heatmapData.destroy();
        this.heatmapData = undefined;
        this.colorScheme.destroy();
        this.colorScheme = undefined;
        this.heatmapViewport.destroy();
        this.heatmapViewport = undefined;
    }

    /**
     *
     * @param {HeatmapData} heatmap
     * @param {boolean} [fitViewport=false]
     */
    metadataLoaded(heatmap, fitViewport = false) {
        if (heatmap && heatmap.metadataReady) {
            this.viewportRenderer.initialize({
                columns: heatmap.metadata.columns,
                rows: heatmap.metadata.rows
            });
            this.colorScheme.assignData(heatmap.metadata);
            this.heatmapViewport.initialize({
                columns: heatmap.metadata.columns.length,
                rows: heatmap.metadata.rows.length,
                fit: fitViewport
            });
        }
    }

    /**
     * Initializes heatmap view
     * @param {HeatmapOptions} options
     */
    initialize(options = {}) {
        const {
            dataConfig,
            displayOptions,
            width,
            height,
            dispatcher,
            padding
        } = options;
        const {id} = dataConfig || {};
        if (id && dispatcher) {
            if (typeof this.removeDispatcherListeners === 'function') {
                this.removeDispatcherListeners();
            }
            const replaceColorScheme = (colorScheme) => this.colorScheme.initializeFrom(colorScheme);
            const configureColorScheme = () => dispatcher.emit(
                'heatmap:colorscheme:configure',
                {
                    config: {
                        id,
                        scheme: this.colorScheme.copy({colorFormat: ColorFormats.hex})
                    },
                }
            );
            this.removeDispatcherListeners = () => {
                dispatcher.removeListener(`heatmap:colorscheme:configure:done:${id}`, replaceColorScheme);
                this.colorScheme.removeEventListeners(configureColorScheme);
            };
            dispatcher.on(`heatmap:colorscheme:configure:done:${id}`, replaceColorScheme);
            this.colorScheme.onConfigureRequest(configureColorScheme);
        }
        const dataChanged = id && this.heatmapData.anotherOptions(dataConfig);
        const initialized = this.initialized;
        this.updateDisplayOptions(displayOptions);
        this.resize(width, height);
        this.padding = padding;
        if (initialized !== this.initialized || dataChanged) {
            this.metadataLoaded(this.heatmapData, dataChanged);
        }
        if (dataConfig) {
            this.heatmapData.options = dataConfig;
        }
        this.render();
    }

    ensureHeatmapInteractions() {
        if (!this.heatmapInteractions) {
            /**
             *
             * @type {HeatmapInteractions}
             */
            this.heatmapInteractions = new HeatmapInteractions(this.domElement, this.heatmapViewport);
            this.heatmapInteractions.onRender(this.render.bind(this));
        }
    }

    ensureViewportRenderer() {
        if (!this.viewportRenderer) {
            this.ensureHeatmapInteractions();
            /**
             * Renders heatmap scales
             * @type {HeatmapViewportRenderer}
             */
            this.viewportRenderer = new HeatmapViewportRenderer(
                this.heatmapViewport,
                this.heatmapInteractions,
                this.labelsManager
            );
            this.viewportRenderer.onInitialized(this.render.bind(this));
        }
    }

    ensureColorSchemeRenderer() {
        if (!this.colorSchemeRenderer) {
            /**
             * Colors scheme indicator renderer
             * @type {ColorSchemeRenderer}
             */
            this.colorSchemeRenderer = new HeatmapColorSchemeRenderer(this.colorScheme, this.labelsManager);
            this.ensureHeatmapInteractions();
            this.heatmapInteractions.registerInteractiveZone(this.colorSchemeRenderer);
        }
    }

    ensureDataRenderer() {
        if (!this.dataRenderer && this.pixiRenderer) {
            /**
             * Data renderer
             * @type {HeatmapDataRenderer}
             */
            this.dataRenderer = new HeatmapDataRenderer(
                this.heatmapViewport,
                this.colorScheme,
                this.heatmapData,
                this.labelsManager,
                this.pixiRenderer
            );
            this.ensureHeatmapInteractions();
            this.heatmapInteractions.registerInteractiveZone(this.dataRenderer);
        }
    }

    /**
     * Updates display options (size)
     * @param {DisplayOptions} options
     * @param {boolean} [force=false]
     */
    updateDisplayOptions(options = {}, force = false) {
        const {
            pixiRenderer: newPixiRenderer,
            domElement: newDOMElement
        } = options;
        const pixiRenderer = newPixiRenderer || this.pixiRenderer;
        const domElement = newDOMElement || this.domElement;
        if (pixiRenderer && domElement) {
            const invalidateAll = pixiRenderer !== this.pixiRenderer || domElement !== this.domElement || force;
            if (!this.container) {
                /**
                 * Root container
                 * @type {PIXI.Container}
                 */
                this.container = new PIXI.Container();
            }
            if (invalidateAll) {
                this.destroyDisplayObjects();
            }
            /**
             * DOM Container
             * @type {HTMLElement}
             */
            this.domElement = domElement;
            /**
             * PIXI Renderer
             * @type {PIXI.AbstractRenderer}
             */
            this.pixiRenderer = pixiRenderer;
            this.container.removeChildren();
            this.ensureHeatmapInteractions();
            this.ensureViewportRenderer();
            this.ensureColorSchemeRenderer();
            this.ensureDataRenderer();
            this.viewportRenderer.initialize({
                columns: this.heatmapData.metadata.columns,
                rows: this.heatmapData.metadata.rows
            });
            this.colorSchemeRenderer.initialize();
            this.dataRenderer.initialize(this.pixiRenderer);
            this.container.addChild(this.dataRenderer.container);
            this.container.addChild(this.viewportRenderer.container);
            this.container.addChild(this.colorSchemeRenderer.container);
        }
        this.initialized = !!pixiRenderer && !!domElement;
        this.render();
    }

    /**
     * Resizes heatmap view
     * @param {number} width
     * @param {number} height
     */
    resize(width, height) {
        if (!this.initialized) {
            return;
        }
        if (width && height) {
            this.width = width;
            this.height = height;
            this.heatmapViewport.deviceAvailableWidth = width;
            this.heatmapViewport.deviceAvailableHeight = height;
            this.heatmapViewport.offsetX = 0;
            this.heatmapViewport.deviceWidth = width;
            this.heatmapViewport.offsetY = 0;
            this.heatmapViewport.deviceHeight = height;
        }
    }

    layout() {
        if (!this.viewportRenderer) {
            return false;
        }
        const layoutInfo = this.viewportRenderer.getLayoutInfo(
            {
                width: this.width,
                height: this.height
            },
            {
                ...this.padding,
                right: this.colorSchemeRenderer.width + this.padding.right
            }
        );
        if (layoutInfo) {
            this.heatmapViewport.offsetX = layoutInfo.offset.column;
            this.heatmapViewport.deviceWidth = layoutInfo.size.column;
            this.heatmapViewport.offsetY = layoutInfo.offset.row;
            this.heatmapViewport.deviceHeight = layoutInfo.size.row;
            this.heatmapViewport.deviceAvailableWidth = layoutInfo.available.column;
            this.heatmapViewport.deviceAvailableHeight = layoutInfo.available.row;
            if (this.heatmapInteractions && !this.heatmapInteractions.userInteracted) {
                this.heatmapViewport.fit(false);
            }
            const currentPositions = [
                this.viewportRenderer.container.x,
                this.viewportRenderer.container.y,
                this.dataRenderer.container.x,
                this.dataRenderer.container.y
            ];
            this.viewportRenderer.container.x = layoutInfo.offset.column;
            this.viewportRenderer.container.y = layoutInfo.offset.row;
            this.dataRenderer.container.x = layoutInfo.offset.column;
            this.dataRenderer.container.y = layoutInfo.offset.row;
            const newPositions = [
                this.viewportRenderer.container.x,
                this.viewportRenderer.container.y,
                this.dataRenderer.container.x,
                this.dataRenderer.container.y
            ];
            return currentPositions
                .filter((p, index) => newPositions[index] !== p)
                .length > 0;
        }
        return false;
    }

    renderColorScheme() {
        if (!this.colorSchemeRenderer) {
            return false;
        }
        const currentX = this.colorSchemeRenderer.container.x;
        this.colorSchemeRenderer.container.x = this.width - this.padding.right - this.colorSchemeRenderer.width;
        const changed = this.colorSchemeRenderer.render({height: this.height});
        return currentX !== this.colorSchemeRenderer.container.x || changed;
    }

    renderHeatmap() {
        let somethingChanged = false;
        const render = renderResult => {
            somethingChanged = renderResult || somethingChanged;
        };
        render(this.renderColorScheme());
        render(this.layout());
        render(this.dataRenderer.render());
        render(this.viewportRenderer.render());
        return somethingChanged;
    }

    render() {
        if (this.pixiRenderer) {
            if (this.renderHeatmap()) {
                this.pixiRenderer.render(this.container);
            }
        }
    }
}

export default HeatmapView;
