/* U. Dogrusoz , A. Karacelik, I. Safarli, H. Balci, L. Dervishi, and M.C. Siper,
"Efficient methods and readily customizable libraries for managing complexity of large networks", PLoS ONE, 13(5): e0197238, 2018.
 */

import angular from 'angular';
import Cytoscape from 'cytoscape';
import contextMenus from 'cytoscape-context-menus';
import dagre from 'cytoscape-dagre';
import dom_node from 'cytoscape-dom-node';
import edgeEditing from 'cytoscape-edge-editing';

import $ from 'jquery';
import konva from 'konva';

const SELECTED_COLOR = '#4285F4';
const SELECTED_WIDTH = 1;
const SELECTED_OPACITY = 0.2;

const clearEdgesSelectionStyle = (cy, settings) => {
    if (cy && settings) {
        cy.edges().css('width', settings.style.edge.width);
        cy.edges().css('line-color', settings.style.edge['line-color']);
        cy.edges().css('target-arrow-color', settings.style.edge['target-arrow-color']);
        cy.edges().css('underlay-opacity', settings.style.edge['underlay-opacity']);
    }
};

const setEdgeSelectionStyle = edge => {
    edge.css('width', SELECTED_WIDTH);
    edge.css('line-color', SELECTED_COLOR);
    edge.css('target-arrow-color', SELECTED_COLOR);
    edge.css('underlay-opacity', SELECTED_OPACITY);
};

export default class ngbCytoscapeController {
    constructor($element, $scope, $compile, $window, $timeout, dispatcher, cytoscapeSettings) {
        this.$scope = $scope;
        this.$compile = $compile;
        this.cytoscapeContainer = $element.find('.cytoscape-container')[0];
        this.settings = cytoscapeSettings;
        this.dispatcher = dispatcher;
        this.$timeout = $timeout;
        this.savedViewerState = {};
        this.actionsManager = {
            ready: false
        };

        Cytoscape.use(dagre);
        Cytoscape.use(dom_node);
        Cytoscape.use(contextMenus);
        edgeEditing( Cytoscape, $, konva );

        const resizeHandler = () => {
            if (this.resizeCytoscape()) {
                this.centerCytoscape();
            }
        };
        angular.element($window).on('resize', resizeHandler);
        const cytoscapeActiveEventHandler = this.reloadCytoscape.bind(this);
        this.dispatcher.on('cytoscape:panel:active', cytoscapeActiveEventHandler);
        const handleSelectionChange = (e) => {
            clearEdgesSelectionStyle(this.viewer, this.settings);
            if (this.viewer && e && e.id) {
                this.viewer.edges().forEach((edge) => {
                    const data = edge.data();
                    if (data.id === e.id) {
                        setEdgeSelectionStyle(edge);
                    }
                });
            }
        };
        this.dispatcher.on('cytoscape:selection:change', handleSelectionChange);
        $scope.$on('$destroy', () => {
            this.dispatcher.removeListener('cytoscape:panel:active', cytoscapeActiveEventHandler);
            this.dispatcher.removeListener('cytoscape:selection:change', handleSelectionChange);
            angular.element($window).off('resize', resizeHandler);
        });
    }

    static get UID() {
        return 'ngbCytoscapeController';
    }

    $onChanges(changes) {
        if (!!changes.elements &&
            !!changes.elements.previousValue &&
            !!changes.elements.currentValue &&
            (changes.elements.previousValue.id !== changes.elements.currentValue.id)) {
            this.savedViewerState = {};
            this.reloadCytoscape(true);
        }
    }

    wrapNodes(nodes, nodeTag, nodeStyle) {
        const wrappedNodes = [];

        function wrapNode(node) {
            const div = document.createElement(nodeTag);
            div.setAttribute('data-node-data-json', JSON.stringify(node.data));
            div.setAttribute('data-on-element-click', '$ctrl.onElementClick({data: data})');
            div.classList = ['strain-lineage-cytoscape-node'];
            div.style.width = `${nodeStyle.width}px`;
            div.style.height = `${nodeStyle.height}px`;
            node.data.dom = div;

            return node;
        }

        for (const node of nodes) {
            wrappedNodes.push(wrapNode(node));
        }
        return wrappedNodes;
    }

    configureEdges(edges) {
        return edges.map(edge => ({
            ...edge,
        }));
    }

    reloadCytoscape(active) {
        if (active) {
            if (this.viewer) {
                this.viewer.destroy();
                this.viewer = null;
            }
            this.$timeout(() => {
                const savedState = JSON.parse(localStorage.getItem(this.storageName) || '{}');
                const savedLayout = savedState.layout ? savedState.layout[this.elements.id] : undefined;
                let elements, layoutSettings;
                if (savedLayout) {
                    elements = {
                        nodes: this.wrapNodes(this.getPlainNodes(savedLayout.nodes), this.tag, this.settings.style.node),
                        edges: this.configureEdges(savedLayout.edges)
                    };
                    layoutSettings = this.settings.loadedLayout;
                } else {
                    elements = {
                        nodes: this.wrapNodes(this.getPlainNodes(this.elements.nodes), this.tag, this.settings.style.node),
                        edges: this.configureEdges(this.elements.edges)
                    };
                    layoutSettings = this.settings.defaultLayout;
                }
                this.viewer = Cytoscape({
                    container: this.cytoscapeContainer,
                    layout: {name: 'preset'},
                    elements: elements,
                    style: [
                        {
                            selector: 'node',
                            style: this.settings.style.node
                        },
                        {
                            selector: 'edge',
                            style: this.settings.style.edge
                        },
                        {
                            selector: 'edge[label]',
                            style: this.settings.style.edgeLabel
                        }
                    ],
                    ...this.settings.options
                });
                this.viewer.domNode();
                this.viewer.contextMenus({
                    menuItemClasses: 'cytoscape-context-item-menu'
                });
                this.viewer.edgeEditing({
                    zIndex: 998,
                    undoable: false,
                    bendRemovalSensitivity: 16,
                    enableMultipleAnchorRemovalOption: true,
                    initAnchorsAutomatically: false,
                    useTrailingDividersAfterContextMenuOptions: false,
                    enableCreateAnchorOnDrag: true
                });
                const layout = this.viewer.layout(layoutSettings);
                layout.on('layoutready', () => {
                    this.$compile(this.cytoscapeContainer)(this.$scope);
                    this.viewer.on('dragfree', this.saveLayout.bind(this));
                    this.viewer.on('tapend', 'edge', this.saveLayout.bind(this));
                    this.viewer.on('pan', () => {
                        this.savedViewerState.pan = this.viewer.pan();
                    });
                    this.resizeCytoscape();
                    this.loadSavedState(this.savedViewerState);
                    this.viewer.style().update();
                });
                this.viewer.edges().on('click', e => {
                    const edgeData = e.target.data();
                    const {
                        label,
                        tooltip
                    } = edgeData || {};
                    if (label || tooltip) {
                        this.onElementClick({
                            data: {
                                id: edgeData.id,
                                tooltip: edgeData.tooltip,
                                title: edgeData.fullLabel
                            }
                        });
                    } else {
                        this.onElementClick(null);
                    }
                });
                layout.run();
                const viewerContext = this;
                this.actionsManager = {
                    NODE_ZOOM: 2,
                    ZOOM_STEP: 0.25,
                    duration: 250,
                    zoom: () => viewerContext.viewer.zoom(),
                    zoomIn() {
                        const zoom = this.zoom() + this.ZOOM_STEP;
                        viewerContext.viewer.zoom(zoom);
                        viewerContext.centerCytoscape();
                        viewerContext.savedViewerState.zoom = viewerContext.viewer.zoom();
                        this.canZoomIn = zoom < viewerContext.viewer.maxZoom();
                        this.canZoomOut = zoom > viewerContext.viewer.minZoom();
                    },
                    zoomOut() {
                        const zoom = this.zoom() - this.ZOOM_STEP;
                        viewerContext.viewer.zoom(zoom);
                        viewerContext.centerCytoscape();
                        viewerContext.savedViewerState.zoom = viewerContext.viewer.zoom();
                        this.canZoomIn = zoom < viewerContext.viewer.maxZoom();
                        this.canZoomOut = zoom > viewerContext.viewer.minZoom();
                    },
                    restoreDefault: () => {
                        viewerContext.viewer.layout(this.settings.defaultLayout).run();
                        viewerContext.saveLayout();
                    },
                    searchNode(filter) {
                        return viewerContext.viewer.nodes().filter(node =>
                            node.data().title.toLocaleLowerCase().includes(filter.toLocaleLowerCase()))
                            .map(node => ({
                                id: node.data().id,
                                title: node.data().title
                            }));
                    },
                    selectNode(nodeId) {
                        const [node] = viewerContext.viewer.nodes().filter(node => node.data().id === nodeId);
                        if (!node) {
                            return;
                        }
                        if (this.zoom() === this.NODE_ZOOM) {
                            viewerContext.viewer.center(node);
                            viewerContext.savedViewerState.pan = viewerContext.viewer.pan();
                        } else {
                            viewerContext.viewer.zoom({
                                level: this.NODE_ZOOM,
                                position: node.position()
                            });
                            viewerContext.savedViewerState.zoom = viewerContext.viewer.zoom();
                            this.canZoomIn = viewerContext.viewer.zoom() < viewerContext.viewer.maxZoom();
                            this.canZoomOut = viewerContext.viewer.zoom() > viewerContext.viewer.minZoom();
                        }
                    },
                    canZoomIn: viewerContext.viewer.zoom() < viewerContext.viewer.maxZoom(),
                    canZoomOut: viewerContext.viewer.zoom() > viewerContext.viewer.minZoom(),
                    ready: true
                };
            });
        } else {
            if (this.viewer) {
                this.viewer.off('pan');
                this.viewer.off('dragfree');
                this.viewer.off('tapend', 'edge');
            }
        }
    }

    resizeCytoscape() {
        if (this.viewer) {
            this.viewer.resize();
            const newSize = {
                width: this.viewer.width(),
                height: this.viewer.height()
            };
            const changed = !this.viewerSize ||
                this.viewerSize.width !== newSize.width ||
                this.viewerSize.height !== newSize.height;
            this.viewerSize = newSize;
            return changed;
        }
        return false;
    }

    centerCytoscape() {
        if (this.viewer) {
            this.viewer.center();
        }
    }

    loadSavedState(savedViewerState) {
        if (savedViewerState.zoom) {
            this.viewer.zoom(savedViewerState.zoom);
        }
        if (savedViewerState.pan) {
            this.viewer.pan(savedViewerState.pan);
        }
    }

    saveLayout() {
        const savedState = JSON.parse(localStorage.getItem(this.storageName) || '{}');
        if (!Object.prototype.hasOwnProperty.call(savedState, 'layout')) {
            savedState.layout = {};
        }
        savedState.layout = {
            ...savedState.layout,
            [this.elements.id]: {
                nodes: this.getPlainNodes(this.viewer.nodes().jsons()),
                edges: this.viewer.edges().jsons()
            }
        };

        localStorage.setItem(this.storageName, JSON.stringify(savedState));
    }

    getPlainNodes(nodes) {
        return nodes.reduce((r, cv) => {
            delete cv.data.dom;
            r.push({
                data: cv.data,
                position: cv.position,
                selected: cv.selected
            });
            return r;
        }, []);
    }
}
