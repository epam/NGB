import * as PIXI from 'pixi.js-legacy';
import AxisVectors from '../../utilities/axis-vectors';
import BinaryTreeGraphics from './binary-tree-graphics';
import InteractiveZone from '../../interactions/interactive-zone';
import cancellablePromise from '../data-renderer/utilities/cancellable-promise';
import config from './config';
import events from '../../utilities/events';
import makeInitializable from '../../utilities/make-initializable';
import splitTree from './utilities/split-tree';

const MASK_COLOR = 0xFF0000;

class AxisDendrogramRenderer extends InteractiveZone {
    /**
     *
     * @param {HeatmapInteractions} interactions
     * @param {HeatmapViewOptions} options
     * @param {HeatmapViewport} viewport
     */
    constructor(interactions, options, viewport) {
        super({
            priority: InteractiveZone.Priorities.dendrogram
        });
        makeInitializable(this);
        /**
         * Total canvas size
         * @type {number|undefined}
         */
        this._windowSize = undefined;
        this.offset = 0;
        this.localOffset = 0;
        this.extraSpace = 0;
        this.container = new PIXI.Container();
        this.dendrogramContainer = new PIXI.Container();
        this.container.addChild(this.dendrogramContainer);
        /**
         * Heatmap interactions manager
         * @type {HeatmapInteractions}
         */
        this.interactions = interactions;
        if (this.interactions) {
            this.interactions.registerInteractiveZone(this);
        }
        /**
         * Heatmap view options
         * @type {HeatmapViewOptions}
         */
        this.options = options;
        if (this.options) {
            this.dendrogramModeChangedCallback = this.rebuild.bind(this);
            this.options.onDendrogramModeChanged(this.dendrogramModeChangedCallback);
            if (this.options.data) {
                this.options.data.onClear(this.dendrogramModeChangedCallback);
                this.options.data.onTreeLoaded(this.dendrogramModeChangedCallback);
                this.options.data.onMetadataLoaded(this.dendrogramModeChangedCallback);
            }
        }
        /**
         *
         * @type {HeatmapViewport}
         */
        this.viewport = viewport;
        if (this.viewport) {
            this.viewportChangedCallback = this.render.bind(this);
            this.viewport.onViewportChanged(this.viewportChangedCallback);
        }
    }

    destroy() {
        super.destroy();
        this._destroyed = true;
        if (this.dendrogramContainer) {
            this.dendrogramContainer.removeChildren();
        }
        this.dendrogramContainer = undefined;
        if (this.container) {
            this.container.removeChildren();
        }
        this.container = undefined;
        if (this.interactions) {
            this.interactions.unregisterInteractiveZone(this);
        }
        this.interactions = undefined;
        if (this.options) {
            this.options.removeEventListeners(
                this.dendrogramModeChangedCallback
            );
            if (this.options.data) {
                this.options.data.removeEventListeners(this.dendrogramModeChangedCallback);
            }
        }
        this.options = undefined;
        if (this.viewport) {
            this.viewport.removeEventListeners(
                this.viewportChangedCallback
            );
        }
        this.viewport = undefined;
    }

    get visible() {
        const axis = this.getAxis();
        const tree = this.getDendrogramTree();
        return !!this.options && this.options.dendrogram && !!axis && !!tree;
    }

    get size() {
        const size = Number(this.dendrogramSize);
        if (!this.visible || !size || Number.isNaN(size) || !Number.isFinite(size) || size <= 0) {
            return 0;
        }
        let maxSize = config.maximumSizePx;
        if (this.windowSize) {
            maxSize = this.windowSize * config.maximumSizeRatioToCanvas;
        }
        return Math.min(
            this.dendrogramSize,
            maxSize
        );
    }

    get windowSize() {
        return this._windowSize;
    }

    set windowSize(windowSize) {
        if (
            windowSize !== this._windowSize &&
            !Number.isNaN(Number(windowSize)) &&
            Number.isFinite(Number(windowSize)) &&
            Number(windowSize) > 0
        ) {
            this._windowSize = windowSize;
            this.emit(events.render.request);
            this.emit(events.layout);
        }
    }

    /**
     * Gets corresponding axis
     * @returns {HeatmapAxis|undefined}
     */
    getAxis() {
        return undefined;
    }

    /**
     * Returns axis direction
     * @returns {Point2D}
     */
    getAxisDirection() {
        return {x: 1, y: 0};
    }

    /**
     * Returns axis normal
     * @returns {Point2D}
     */
    getAxisNormal() {
        return {x: 0, y: 1};
    }

    /**
     * Gets dendrogram tree
     * @returns {HeatmapBinaryTree|undefined}
     */
    getDendrogramTree() {
        return undefined;
    }

    onLayout(callback) {
        this.addEventListener(events.layout, callback);
    }

    updatePosition(offset = 0, extraSpace = 0) {
        if (this.offset !== offset || this.extraSpace !== extraSpace) {
            this.offset = offset;
            this.extraSpace = extraSpace;
            this.emit(events.render.request);
        }
    }

    initialize() {
        this.currentPromise = cancellablePromise(
            (isCancelledFn) => new Promise((resolve) => {
                if (!isCancelledFn()) {
                    this.clearSession();
                    this.initialized = true;
                    this.emit(events.render.request);
                    this.rebuild();
                }
                resolve();
            }),
            this.currentPromise
        );
    }

    rebuild() {
        this.currentPromise = cancellablePromise(
            (isCancelledFn) => new Promise((resolve) => {
                if (!isCancelledFn() && !this._destroyed) {
                    this.dendrogramContainer.removeChildren();
                    if (this.treeGraphics) {
                        this.treeGraphics.destroy();
                    }
                    const axis = this.getAxis();
                    const tree = this.getDendrogramTree();
                    // console.log(this.constructor.name, this.options.dendrogram, !!tree, !!axis);
                    if (this.visible && tree && axis && this.options && this.options.data) {
                        // console.log('rebuilding', this.constructor.name, axis, tree);
                        /**
                         * Dendrogram maximum size
                         * @type {number}
                         */
                        this.dendrogramSize = (tree.depth + 1) * config.maximumLevelSize;
                        this.treeGraphics = new BinaryTreeGraphics(
                            splitTree(tree),
                            axis,
                            this.getAxisDirection(),
                            this.getAxisNormal()
                        );
                        this.dendrogramContainer.addChild(this.treeGraphics.container);
                        this.treeGraphics.onInitialized(() => {
                            // console.log('rebuilding', this.constructor.name, 'done', this.treeGraphics);
                            this.emit(events.layout);
                            this.clearSession();
                            this.emit(events.render.request);
                            resolve();
                        });
                        this.treeGraphics.initialize(isCancelledFn);
                    } else {
                        this.emit(events.layout);
                        resolve();
                    }
                }
            }),
            this.currentPromise
        );
    }

    getGlobalBounds(sorted = false) {
        const axis = this.getAxis();
        const direction = this.getAxisDirection();
        const normal = this.getAxisNormal();
        if (axis) {
            const margin = 5;
            const global = this.container.getGlobalPosition(undefined, true);
            const offset = this.offset + config.margin;
            const xShift = offset * normal.x;
            const yShift = offset * normal.y;
            const [x1, x2] = [
                global.x + xShift - margin * direction.x,
                global.x + xShift +
                (axis.deviceSize + margin) * direction.x +
                (this.size + this.extraSpace + offset) * normal.x
            ];
            const [y1, y2] = [
                global.y + yShift - margin * direction.y,
                global.y + yShift +
                (axis.deviceSize + margin) * direction.y +
                (this.size + this.extraSpace + offset) * normal.y
            ];
            if (sorted) {
                return {
                    x1: Math.min(x1, x2),
                    x2: Math.max(x1, x2),
                    y1: Math.min(y1, y2),
                    y2: Math.max(y1, y2)
                };
            }
            return {
                x1,
                x2,
                y1,
                y2
            };
        }
        return undefined;
    }

    test(event) {
        const bounds = this.getGlobalBounds(true);
        if (this.visible && event && bounds) {
            // this logic if for horizontal/vertical axis.
            // If SOMEHOW we need custom logic (diagonal axis, for instance), then
            // we need to use PIXI.Polygon for hit testing
            const {globalX, globalY} = event;
            const {x1, x2, y1, y2} = bounds;
            return x1 <= globalX && x2 >= globalX && y1 <= globalY && y2 >= globalY;
        }
        return super.test(event);
    }

    // eslint-disable-next-line no-unused-vars
    shouldDrag(event) {
        return this.visible && (this.size + this.extraSpace) < this.treeGraphics.totalSize;
    }

    // eslint-disable-next-line no-unused-vars
    onDragStart(event) {
        this.dragStartLocalOffset = this.localOffset;
    }

    onDrag(event) {
        const normal = this.getAxisNormal();
        if (event) {
            const normalDelta = event.xDelta * normal.x + event.yDelta * normal.y;
            this.localOffset = this.dragStartLocalOffset + normalDelta;
            this.correctLocalDelta();
            this.emit(events.render.request);
        }
        super.onDrag(event);
    }

    // eslint-disable-next-line no-unused-vars
    onDragEnd(event) {
        this.dragStartLocalOffset = this.localOffset;
    }

    correctLocalDelta() {
        if (this.treeGraphics) {
            const max = Math.max(0, this.treeGraphics.totalSize - (this.size + this.extraSpace));
            this.localOffset = Math.max(0, Math.min(max, this.localOffset));
        }
    }

    clearSession() {
        this.session = {};
    }

    /**
     * Gets current rendering session flags
     * @returns {{scaleChanged: boolean, positionChanged: boolean, offsetChanged: boolean, extraSpaceChanged: boolean}}
     */
    getSessionFlags() {
        const axis = this.getAxis();
        const flags = {
            positionChanged: false,
            scaleChanged: false,
            offsetChanged: this.session.offset !== this.offset || this.session.localOffset !== this.localOffset,
            extraSpaceChanged: this.session.extraSpace !== this.extraSpace
        };
        if (axis) {
            flags.positionChanged = this.session.start !== axis.getDevicePosition(0) ||
                this.session.end !== axis.getDevicePosition(axis.size);
            flags.scaleChanged = this.session.scale !== axis.scale.tickSize;
        }
        return flags;
    }

    updateSessionFlags() {
        const axis = this.getAxis();
        if (axis) {
            this.session.start = axis.getDevicePosition(0);
            this.session.end = axis.getDevicePosition(axis.size);
            this.session.scale = axis.scale.tickSize;
        }
        this.session.offset = this.offset;
        this.session.localOffset = this.localOffset;
        this.session.extra = this.extraSpace;
    }

    renderMask() {
        return;
        const bounds = this.getGlobalBounds();
        if (bounds) {
            const {x1, x2, y1, y2} = bounds;
            const mask = new PIXI.Graphics();
            mask.beginFill(MASK_COLOR, 1)
                .drawPolygon(
                    x1, y1,
                    x2, y1,
                    x2, y2,
                    x1, y2
                )
                .endFill();
            this.container.mask = mask;
        } else {
            this.container.mask = undefined;
        }
    }

    renderDendrogram() {
        const tree = this.getDendrogramTree();
        if (this.treeGraphics && tree) {
            const availableSpace = this.size + this.extraSpace;
            const levelSize = Math.ceil(
                Math.max(
                    config.minimumLevelSize,
                    Math.min(
                        config.maximumLevelSize,
                        availableSpace / (tree.depth + 1)
                    )
                )
            );
            return this.treeGraphics.render({levelSize});
        }
        return false;
    }

    render() {
        const axis = this.getAxis();
        if (!this.visible || !axis || axis.invalid) {
            return false;
        }
        const {
            positionChanged,
            scaleChanged,
            offsetChanged
        } = this.getSessionFlags();
        const somethingChanged = this.renderDendrogram();
        this.renderMask();
        const direction = this.getAxisDirection();
        const normal = this.getAxisNormal();
        this.correctLocalDelta();
        if (positionChanged || scaleChanged || offsetChanged) {
            this.dendrogramContainer.x = Math.floor(
                direction.x * axis.getDevicePosition(0) +
                normal.x * (this.offset + config.margin - this.localOffset)
            );
            this.dendrogramContainer.y = Math.floor(
                direction.y * axis.getDevicePosition(0) +
                normal.y * (this.offset + config.margin - this.localOffset)
            );
        }
        this.updateSessionFlags();
        return positionChanged || scaleChanged || somethingChanged;
    }
}

class ColumnsDendrogramRenderer extends AxisDendrogramRenderer {
    getAxis() {
        if (this.viewport) {
            return this.viewport.columns;
        }
        return super.getAxis();
    }

    getAxisNormal() {
        return AxisVectors.columns.normal;
    }

    getAxisDirection() {
        return AxisVectors.columns.direction;
    }

    getDendrogramTree() {
        if (
            this.options &&
            this.options.data &&
            this.options.data.columnsTree &&
            !this.options.data.columnsTree.invalid
        ) {
            return this.options.data.columnsTree;
        }
        return super.getDendrogramTree();
    }
}

class RowsDendrogramRenderer extends AxisDendrogramRenderer {
    getAxis() {
        if (this.viewport) {
            return this.viewport.rows;
        }
        return super.getAxis();
    }

    getAxisNormal() {
        return AxisVectors.rows.normal;
    }

    getAxisDirection() {
        return AxisVectors.rows.direction;
    }

    getDendrogramTree() {
        if (
            this.options &&
            this.options.data &&
            this.options.data.rowsTree &&
            !this.options.data.rowsTree.invalid
        ) {
            return this.options.data.rowsTree;
        }
        return super.getDendrogramTree();
    }
}

export {ColumnsDendrogramRenderer, RowsDendrogramRenderer};
