import * as PIXI from 'pixi.js-legacy';
import {SubTreeNodeDisplayModes, getNodeDisplayMode} from './utilities/get-node-display-mode';
import HeatmapEventDispatcher from '../../utilities/heatmap-event-dispatcher';
import config from './config';
import getNodeIsCollapsed from './utilities/get-node-is-collapsed';
import getNodeSubTrees from './utilities/get-node-sub-trees';
import makeInitializable from '../../utilities/make-initializable';

class BinaryTreeGraphics extends HeatmapEventDispatcher {
    /**
     *
     * @param {SubTreeNode} tree
     * @param {HeatmapAxis} axis
     * @param {Vector} direction
     * @param {Vector} normal
     */
    constructor(tree, axis, direction, normal) {
        super();
        makeInitializable(this);
        this.visible = true;
        /**
         * Axis direction
         * @type {Vector}
         */
        this.direction = direction;
        /**
         * Axis normal
         * @type {Vector}
         */
        this.normal = normal;
        /**
         * Sub tree
         * @type {SubTreeNode}
         */
        this.tree = tree;
        /**
         * Heatmap axis
         * @type {HeatmapAxis}
         */
        this.axis = axis;
        /**
         *
         * @type {Map<SubTreeNode, BinaryTreeGraphics>}
         */
        this.children = new Map();
        this.container = new PIXI.Container();
    }

    get totalSize() {
        if (
            this.session &&
            Number.isFinite(Number(this.session.levelSize)) &&
            this.tree
        ) {
            return this.tree.level * this.session.levelSize;
        }
        return 0;
    }

    initialize(isCancelledFn) {
        this.destroyChildren();
        this.container.removeChildren();
        this.graphics = new PIXI.Graphics();
        this.container.addChild(this.graphics);
        this.clearSession();
        if (!isCancelledFn() && this.tree && this.axis) {
            const onInitialized = () => {
                let initialized = true;
                for (const child of this.children.values()) {
                    initialized = initialized && child.initialized;
                }
                if (initialized) {
                    this.hide();
                }
                this.initialized = initialized;
            };
            /**
             *
             * @param {SubTreeNode} node
             */
            const iterate = (node) => {
                if (!isCancelledFn()) {
                    if (node.leftSubTree) {
                        const leftSubTreeGraphics = new BinaryTreeGraphics(
                            node.leftSubTree,
                            this.axis,
                            this.direction,
                            this.normal
                        );
                        this.container.addChild(leftSubTreeGraphics.container);
                        this.children.set(node.leftSubTree, leftSubTreeGraphics);
                    }
                    if (node.rightSubTree) {
                        const rightSubTreeGraphics = new BinaryTreeGraphics(
                            node.rightSubTree,
                            this.axis,
                            this.direction,
                            this.normal
                        );
                        this.container.addChild(rightSubTreeGraphics.container);
                        this.children.set(node.rightSubTree, rightSubTreeGraphics);
                    }
                    if (node.left) {
                        iterate(node.left);
                    }
                    if (node.right) {
                        iterate(node.right);
                    }
                }
            };
            iterate(this.tree);
            onInitialized();
            for (const child of this.children.values()) {
                child.onInitialized(onInitialized);
                child.initialize(isCancelledFn);
            }
        } else {
            this.initialized = true;
        }
    }

    destroyChildren() {
        if (this.children) {
            for (const child of this.children.values()) {
                child.destroy();
            }
            this.children.clear();
        }
    }

    destroy() {
        super.destroy();
        this.tree = undefined;
        this.left = undefined;
        this.right = undefined;
        this.axis = undefined;
        this.destroyChildren();
        this.children = undefined;
        if (this.container) {
            this.container.removeChildren();
        }
        this.container = undefined;
    }

    clearSession() {
        this.session = {};
    }

    getSessionFlags(levelSize) {
        if (this.axis) {
            const mode = getNodeDisplayMode(this.tree, this.axis);
            return {
                mode,
                modeChanged: this.session.mode !== mode,
                positionChanged: this.session.position !== this.axis.center,
                scaleChanged: this.session.scale !== this.axis.scale.tickSize,
                levelSizeChanged: this.session.levelSize !== levelSize
            };
        }
        return {
            mode: SubTreeNodeDisplayModes.outsideViewport,
            modeChanged: false,
            positionChanged: false,
            scaleChanged: false,
            levelSizeChanged: false
        };
    }

    updateSessionFlags(mode, levelSize) {
        if (this.axis) {
            this.session.mode = mode;
            this.session.position = this.axis.center;
            this.session.scale = this.axis.scale.tickSize;
            this.session.levelSize = levelSize;
        }
    }

    hide(clearSession = false) {
        if (!this.visible) {
            return;
        }
        // console.log(this.toString(), 'hiding');
        this.visible = false;
        this.tree.level = 0;
        if (this.graphics) {
            this.graphics.clear();
        }
        if (clearSession) {
            this.clearSession();
        }
        if (this.children) {
            for (const child of this.children.values()) {
                child.hide(true);
            }
        }
    }

    getNodePosition(node = this.tree) {
        if (this.axis && node) {
            const {center} = node.indexRange;
            return this.axis.getCorrectedPositionWithinAxis(center + 0.5);
        }
        return 0;
    }

    getXCoordinate = (axisCoordinate, levelCoordinate) => this.axis && this.axis.scale && this.session
        ? Math.round(
            this.axis.scale.getDeviceDimension(axisCoordinate) * this.direction.x +
            levelCoordinate * this.normal.x * (this.session.levelSize || 0)
        )
        : 0;

    getYCoordinate = (axisCoordinate, levelCoordinate) => this.axis && this.axis.scale && this.session
        ? Math.round(
            this.axis.scale.getDeviceDimension(axisCoordinate) * this.direction.y +
            levelCoordinate * this.normal.y * (this.session.levelSize || 0)
        )
        : 0;

    renderLeafPoint(position, level = 0) {
        if (config.leafPoint) {
            const {
                fill = config.stroke,
                radius = 1
            } = typeof config.leafPoint === 'object' ? config.leafPoint : {};
            this.graphics
                .lineStyle(0, 0, 0)
                .beginFill(fill, 1)
                .drawCircle(
                    this.getXCoordinate(position, level),
                    this.getYCoordinate(position, level),
                    radius
                )
                .endFill();
        }
    }

    renderNodeGraphics(options = {}) {
        const {
            level,
            left,
            right
        } = options;
        const levelSize = this.session.levelSize || 0;
        let radius = 0;
        let levelRadius = 0;
        let axisRadius = 0;
        if (config.edgeRadius > 0) {
            const distance = this.axis.scale.getDeviceDimension(Math.abs(left.position - right.position));
            radius = Math.min(distance / 2.0, levelSize, config.edgeRadius);
            levelRadius = radius / levelSize;
            axisRadius = this.axis.scale.getScaleDimension(radius);
        }
        if (radius < 2) {
            radius = 0;
        }
        const leftAnchor = {
            x: this.getXCoordinate(left.position, level),
            y: this.getYCoordinate(left.position, level)
        };
        const rightAnchor = {
            x: this.getXCoordinate(right.position, level),
            y: this.getYCoordinate(right.position, level)
        };
        this.graphics
            .lineStyle(config.thickness, config.stroke, 1);
        if (left.visible) {
            this.graphics.moveTo(
                this.getXCoordinate(left.position, left.level),
                this.getYCoordinate(left.position, left.level)
            );
            if (radius > 0) {
                this.graphics.lineTo(
                    this.getXCoordinate(left.position, level - levelRadius),
                    this.getYCoordinate(left.position, level - levelRadius),
                );
                this.graphics.quadraticCurveTo(
                    leftAnchor.x,
                    leftAnchor.y,
                    this.getXCoordinate(left.position + axisRadius, level),
                    this.getYCoordinate(left.position + axisRadius, level)
                );
            } else {
                this.graphics.lineTo(
                    leftAnchor.x,
                    leftAnchor.y
                );
            }
        } else {
            this.graphics.moveTo(
                this.getXCoordinate(left.position, level),
                this.getYCoordinate(left.position, level)
            );
        }
        if (radius > 0) {
            this.graphics.lineTo(
                this.getXCoordinate(right.position - axisRadius, level),
                this.getYCoordinate(right.position - axisRadius, level)
            );
        } else {
            this.graphics.lineTo(
                this.getXCoordinate(right.position, level),
                this.getYCoordinate(right.position, level)
            );
        }
        if (right.visible) {
            if (radius > 0) {
                this.graphics.quadraticCurveTo(
                    rightAnchor.x,
                    rightAnchor.y,
                    this.getXCoordinate(right.position, level - levelRadius),
                    this.getYCoordinate(right.position, level - levelRadius)
                );
            }
            this.graphics.lineTo(
                this.getXCoordinate(right.position, right.level),
                this.getYCoordinate(right.position, right.level)
            );
        }
    }

    /**
     *
     * @param {Object} [options]
     * @param {number} [options.levelSize=5]
     * @returns {boolean}
     */
    renderTree(options = {}) {
        const {
            levelSize = 5
        } = options;
        const displayMode = getNodeDisplayMode(this.tree, this.axis);
        if (
            displayMode === SubTreeNodeDisplayModes.outsideViewport ||
            getNodeIsCollapsed(this.tree, this.axis)
        ) {
            this.hide();
            return true;
        }
        this.graphics.clear();
        /**
         *
         * @param {SubTreeNode} node
         * @returns {BinaryTreeGraphics}
         */
        const getChildRenderer = (node) => {
            if (node && this.children.has(node)) {
                return this.children.get(node);
            }
            return undefined;
        };
        /**
         *
         * @param {SubTreeNode} node
         * @returns {{level: number, position: number}}
         */
        const renderNode = (node) => {
            const mode = getNodeDisplayMode(node, this.axis);
            const collapsed = getNodeIsCollapsed(node, this.axis);
            const position = this.getNodePosition(node);
            if (
                mode === SubTreeNodeDisplayModes.outsideViewport ||
                collapsed
            ) {
                const subTrees = getNodeSubTrees(node);
                for (const subTree of subTrees) {
                    const renderer = getChildRenderer(subTree);
                    if (renderer) {
                        renderer.hide(true);
                    }
                }
                if (mode !== SubTreeNodeDisplayModes.outsideViewport) {
                    this.renderLeafPoint(position);
                }
                return {
                    level: 0,
                    position
                };
            }
            if (node.isLeaf && !node.hasSubTree) {
                this.renderLeafPoint(this.getNodePosition(node));
                return {
                    level: 0,
                    position
                };
            }
            const left = {
                level: 0,
                position: 0,
                visible: true,
                isLeaf: false
            };
            const right = {
                level: 0,
                position: 0,
                visible: true,
                isLeaf: false
            };
            if (
                node.isLeaf &&
                node.hasSubTree &&
                node.leftSubTree &&
                node.rightSubTree
            ) {
                const leftRenderer = getChildRenderer(node.leftSubTree);
                const rightRenderer = getChildRenderer(node.rightSubTree);
                if (leftRenderer && rightRenderer) {
                    leftRenderer.render(options);
                    rightRenderer.render(options);
                }
                left.level = node.leftSubTree.level;
                right.level = node.rightSubTree.level;
                left.position = leftRenderer.getNodePosition();
                right.position = rightRenderer.getNodePosition();
                left.visible = getNodeDisplayMode(node.leftSubTree, this.axis) !== SubTreeNodeDisplayModes.outsideViewport;
                right.visible = getNodeDisplayMode(node.rightSubTree, this.axis) !== SubTreeNodeDisplayModes.outsideViewport;
            } else {
                // not a leaf and does not have sub trees
                const {level: lLevel, position: lPosition} = renderNode(node.left);
                const {level: rLevel, position: rPosition} = renderNode(node.right);
                left.level = lLevel;
                left.position = lPosition;
                left.visible = getNodeDisplayMode(node.left, this.axis) !== SubTreeNodeDisplayModes.outsideViewport;
                right.level = rLevel;
                right.position = rPosition;
                right.visible = getNodeDisplayMode(node.right, this.axis) !== SubTreeNodeDisplayModes.outsideViewport;
            }
            const level = Math.max(
                -1,
                left.level,
                right.level
            ) + 1;
            this.renderNodeGraphics({
                level,
                levelSize,
                left,
                right
            });
            return {
                level,
                position: this.axis.getCorrectedPositionWithinAxis(
                    (left.position + right.position) / 2.0
                )
            };
        };
        const {level} = renderNode(this.tree);
        this.tree.level = level;
        this.visible = true;
    }

    /**
     *
     * @param {Object} [options]
     * @param {number} [options.levelSize=5]
     * @returns {boolean}
     */
    render(options = {}) {
        if (!this.initialized) {
            return false;
        }
        const {
            levelSize = 5
        } = options;
        const {
            scaleChanged,
            positionChanged,
            mode,
            modeChanged
        } = this.getSessionFlags(levelSize);
        this.updateSessionFlags(mode, levelSize);
        if (
            modeChanged ||
            scaleChanged ||
            (mode === SubTreeNodeDisplayModes.intersectsViewport && positionChanged)
        ) {
            // console.log(this.toString(), 'render. axis:', this.axis.start.toFixed(2), this.axis.end.toFixed(2), 'mode:', mode);
            return this.renderTree(options);
        } else if (positionChanged) {
            // console.log(this.toString(), 'nothing changed');
        }
        return false;
    }

    toString() {
        return `${this.constructor.name} (${this.tree.indexRange.start} - ${this.tree.indexRange.end})`;
    }
}

export default BinaryTreeGraphics;
