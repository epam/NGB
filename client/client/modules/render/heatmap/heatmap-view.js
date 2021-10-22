import * as PIXI from 'pixi.js-legacy';
import LabelsManager from '../core/labelsManager';
import {parseOffset} from '../utilities';
import HeatmapViewOptions from './heatmap-view-options';
import HeatmapInteractions from './interactions';
import HeatmapNavigation from './navigation';
import {
    HeatmapColorSchemeRenderer,
    HeatmapDataRenderer,
    HeatmapDendrogramRenderer,
    HeatmapOptionsRenderer,
    HeatmapViewportRenderer
} from './renderer';
import HeatmapEventDispatcher from './utilities/heatmap-event-dispatcher';
import makeInitializable from './utilities/make-initializable';
import HeatmapViewport from './viewport';

const ZERO_PADDING = parseOffset(0);

/**
 * @typedef {Object} HeatmapOptions
 * @property {HeatmapDataOptions} dataConfig
 * @property {DisplayOptions} displayOptions
 * @property {number} width
 * @property {number} height
 * @property {number|number[]} padding
 * @property {dispatcher} dispatcher
 * @property {projectContext} projectContext
 * @property {string} state
 * @property {boolean} [showOptionsButtons=true]
 */

class HeatmapView extends HeatmapEventDispatcher {
    /**
     *
     * @param {HeatmapOptions} options
     */
    constructor(options = {}) {
        super();
        makeInitializable(this);
        /**
         * Heatmap view options (color scheme, dendrogram) and data
         * @type {HeatmapViewOptions}
         */
        this.options = HeatmapViewOptions.parse(options.state, options.dataConfig);
        /**
         * Holds heatmap viewport info
         * @type {HeatmapViewport}
         */
        this.heatmapViewport = new HeatmapViewport();
        /**
         * Heatmap navigation manager
         * @type {HeatmapNavigation}
         */
        this.navigation = new HeatmapNavigation(options.projectContext, this.options.data);
        this.options.data.onMetadataLoaded(this.metadataLoaded.bind(this));
        this.options.colorScheme.onInitialized(this.render.bind(this, false));
        this.options.colorScheme.onChanged(this.render.bind(this, false));
        this.heatmapViewport.onViewportChanged(this.render.bind(this, false));
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

    get referenceId() {
        return this.options.data.referenceId;
    }

    set referenceId(referenceId) {
        this.options.data.referenceId = referenceId;
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
        if (this.dendrogramRenderer) {
            this.dendrogramRenderer.destroy();
            this.dendrogramRenderer = undefined;
        }
        if (this.optionsRenderer) {
            this.optionsRenderer.destroy();
            this.optionsRenderer = undefined;
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
        if (this.options) {
            this.options.destroy();
        }
        this.heatmapViewport.destroy();
        this.heatmapViewport = undefined;
        this.navigation.destroy();
        this.navigation = undefined;
    }

    /**
     *
     * @param {HeatmapData} heatmap
     * @param {boolean} [fitViewport=false]
     */
    metadataLoaded(heatmap, fitViewport = false) {
        if (heatmap && heatmap.metadataReady) {
            this.viewportRenderer.initialize(heatmap.metadata);
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
            padding,
            showOptionsButtons = true
        } = options;
        if (dispatcher) {
            this.options.colorScheme.attachDispatcher(dispatcher);
        }
        const {id} = dataConfig || {};
        const dataChanged = id && this.options.data.anotherOptions(dataConfig);
        const initialized = !dataChanged && this.initialized;
        this.updateDisplayOptions(displayOptions);
        if (dataChanged) {
            this.heatmapInteractions.clearUserInteracted();
        }
        this.resize(width, height);
        this.padding = padding;
        if (initialized !== this.initialized) {
            this.metadataLoaded(this.options.data, true);
        }
        if (dataConfig) {
            this.options.data.options = dataConfig;
        }
        if (this.optionsRenderer) {
            this.optionsRenderer.visible = showOptionsButtons;
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
            this.heatmapInteractions.onRender(this.render.bind(this, false));
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
                this.options,
                this.heatmapViewport,
                this.heatmapInteractions,
                this.labelsManager
            );
            this.viewportRenderer.onInitialized(this.render.bind(this, false));
            this.viewportRenderer.onLayout(() => {
                if (this.heatmapInteractions && !this.heatmapInteractions.userInteracted) {
                    this.render(true);
                }
            });
            this.viewportRenderer.onAxisClick((viewportRenderer, clickInfo = {}) => {
                const {
                    column: columnIndex,
                    row: rowIndex
                } = clickInfo;
                if (columnIndex !== undefined && this.options.data && this.options.data.metadata) {
                    const column = this.options.data.metadata.getColumn(columnIndex);
                    if (column && this.navigation) {
                        this.navigation.navigate(column.navigation, column.annotation || column.name);
                    }
                }
                if (rowIndex !== undefined) {
                    const row = this.options.data.metadata.getRow(rowIndex);
                    if (row && this.navigation) {
                        this.navigation.navigate(row.navigation, row.annotation || row.name);
                    }
                }
            });
        }
    }

    ensureOptionsRenderer() {
        if (!this.optionsRenderer) {
            this.ensureHeatmapInteractions();
            /**
             * Renders heatmap options (buttons)
             * @type {HeatmapOptionsRenderer}
             */
            this.optionsRenderer = new HeatmapOptionsRenderer(
                this.labelsManager,
                this.heatmapInteractions,
                this.options
            );
        }
    }

    ensureColorSchemeRenderer() {
        if (!this.colorSchemeRenderer) {
            /**
             * Colors scheme indicator renderer
             * @type {ColorSchemeRenderer}
             */
            this.colorSchemeRenderer = new HeatmapColorSchemeRenderer(
                this.options.colorScheme,
                this.labelsManager
            );
            this.colorSchemeRenderer.onLayout(() => {
                if (!this.heatmapInteractions.userInteracted) {
                // if (this.heatmapInteractions && !this.heatmapInteractions.userInteracted) {
                    this.render(true);
                }
            });
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
                this.options.colorScheme,
                this.options.data,
                this.labelsManager,
                this.pixiRenderer
            );
            this.dataRenderer.onDataItemClick((dataRenderer, item) => {
                if (item) {
                    this.navigation.navigate(this.options.data.metadata.dataNavigationType, item.annotation);
                }
            });
            this.ensureHeatmapInteractions();
            this.heatmapInteractions.registerInteractiveZone(this.dataRenderer);
        }
    }

    ensureDendrogramRenderer() {
        if (!this.dendrogramRenderer) {
            this.ensureHeatmapInteractions();
            /**
             * Renders heatmap dendrogram
             * @type {HeatmapDendrogramRenderer}
             */
            this.dendrogramRenderer = new HeatmapDendrogramRenderer(
                this.heatmapInteractions,
                this.options,
                this.heatmapViewport
            );
            this.dendrogramRenderer.initialize();
            this.dendrogramRenderer.onInitialized(this.render.bind(this, false));
            this.dendrogramRenderer.onLayout(() => {
                if (this.heatmapInteractions && !this.heatmapInteractions.userInteracted) {
                    this.render(true);
                }
            });
        }
    }

    /**
     * Updates display options (renderer & DOM element)
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
        const changed = pixiRenderer !== this.pixiRenderer ||
            domElement !== this.domElement || force ||
            !this.container;
        if (changed && pixiRenderer && domElement) {
            if (!this.container) {
                /**
                 * Root container
                 * @type {PIXI.Container}
                 */
                this.container = new PIXI.Container();
            }
            this.destroyDisplayObjects();

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
            this.ensureDendrogramRenderer();
            this.ensureOptionsRenderer();
            this.viewportRenderer.initialize(this.options.data.metadata);
            this.colorSchemeRenderer.initialize();
            this.dataRenderer.initialize(this.pixiRenderer);
            this.container.addChild(this.dataRenderer.container);
            this.container.addChild(this.viewportRenderer.container);
            this.container.addChild(this.dendrogramRenderer.container);
            this.container.addChild(this.colorSchemeRenderer.container);
            this.container.addChild(this.optionsRenderer.container);
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
            if (this.dendrogramRenderer) {
                this.dendrogramRenderer.updateWindowSize(width, height);
            }
        }
    }

    layout(fit = false) {
        this.optionsRenderer.setPosition(this.padding.left, this.padding.top);
        if (!this.viewportRenderer) {
            return false;
        }
        const layoutInfo = this.viewportRenderer.getLayoutInfo(
            {
                width: this.width,
                height: this.height
            },
            {
                left: this.dendrogramRenderer.rows.size + this.padding.left,
                top: this.dendrogramRenderer.columns.size + this.padding.top,
                right: this.colorSchemeRenderer.width + this.padding.right,
                bottom: this.padding.bottom
            }
        );
        if (layoutInfo) {
            this.heatmapViewport.offsetX = layoutInfo.offset.column;
            this.heatmapViewport.deviceWidth = layoutInfo.size.column;
            this.heatmapViewport.offsetY = layoutInfo.offset.row;
            this.heatmapViewport.deviceHeight = layoutInfo.size.row;
            this.heatmapViewport.deviceAvailableWidth = layoutInfo.available.column;
            this.heatmapViewport.deviceAvailableHeight = layoutInfo.available.row;
            if (fit) {
                this.heatmapViewport.best(false);
            }
            /**
             * Returns array of [x, y]
             * @param {{container: PIXI.Container}} o
             */
            const getPositions = (o) => ([
                o.container.x,
                o.container.y
            ]);
            /**
             * Updates position
             * @param {{container: PIXI.Container}} o
             */
            const updatePositions = (o) => {
                o.container.x = Math.floor(layoutInfo.offset.column);
                o.container.y = Math.floor(layoutInfo.offset.row);
            };
            const currentPositions = [
                ...getPositions(this.viewportRenderer),
                ...getPositions(this.dataRenderer),
                ...getPositions(this.dendrogramRenderer)
            ];
            updatePositions(this.viewportRenderer);
            updatePositions(this.dataRenderer);
            updatePositions(this.dendrogramRenderer);
            const newPositions = [
                ...getPositions(this.viewportRenderer),
                ...getPositions(this.dataRenderer),
                ...getPositions(this.dendrogramRenderer)
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

    renderDendrogram() {
        if (!this.dendrogramRenderer) {
            return false;
        }
        if (this.viewportRenderer) {
            /**
             *
             * @param {HeatmapAxisRenderer} axisRenderer
             * @returns {{offset: number, extraSpace: number}}
             */
            const getPositionInfo = (axisRenderer) => ({
                offset: axisRenderer.actualAxisSize,
                extraSpace: Math.max(0, axisRenderer.axisSize - axisRenderer.actualAxisSize)
            });
            const columnsInfo = getPositionInfo(this.viewportRenderer.columnAxis);
            const rowsInfo = getPositionInfo(this.viewportRenderer.rowAxis);
            this.dendrogramRenderer.columns.updatePosition(columnsInfo.offset, columnsInfo.extraSpace);
            this.dendrogramRenderer.rows.updatePosition(rowsInfo.offset, rowsInfo.extraSpace);
        }
        return this.dendrogramRenderer.render();
    }

    renderHeatmap(fit = false) {
        let somethingChanged = false;
        const render = renderResult => {
            somethingChanged = renderResult || somethingChanged;
        };
        render(this.renderColorScheme());
        render(this.layout(fit));
        render(this.dataRenderer.render());
        render(this.viewportRenderer.render());
        render(this.renderDendrogram());
        render(this.optionsRenderer.render());
        return somethingChanged;
    }

    render(fit = false) {
        if (this.pixiRenderer) {
            if (this.renderHeatmap(fit)) {
                this.pixiRenderer.render(this.container);
            }
        }
    }
}

export default HeatmapView;
