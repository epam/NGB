import angular from 'angular';
import Cytoscape from 'cytoscape';
import dagre from 'cytoscape-dagre';
import dom_node from 'cytoscape-dom-node';

const SELECTED_COLOR = '#4285F4';
const SELECTED_WIDTH = 1;
const SELECTED_OPACITY = 0.2;
const elementOptionsType = {
    NODE: 'nodes',
    EDGE: 'edges'
};

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
        this.actionsManager = {
            ready: false
        };

        Cytoscape.use(dagre);
        Cytoscape.use(dom_node);

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
            this.reloadCytoscape(true);
        }
        if (!!changes.elementsOptions &&
            !!changes.elementsOptions.previousValue &&
            !!changes.elementsOptions.currentValue) {
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
                        nodes: this.wrapNodes(
                            this.applyOptions(this.getPlainNodes(savedLayout.nodes), elementOptionsType.NODE),
                            this.tag,
                            this.settings.style.node
                        ),
                        edges: savedLayout.edges
                    };
                    layoutSettings = this.settings.loadedLayout;
                } else {
                    elements = {
                        nodes: this.wrapNodes(
                            this.applyOptions(this.getPlainNodes(this.elements.nodes), elementOptionsType.NODE),
                            this.tag,
                            this.settings.style.node
                        ),
                        edges: this.elements.edges
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
                const layout = this.viewer.layout(layoutSettings);
                layout.on('layoutready', () => {
                    this.$compile(this.cytoscapeContainer)(this.$scope);
                    this.viewer.on('dragfree', this.saveLayout.bind(this));
                    this.resizeCytoscape();
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
                if (this.zoomLevel) {
                    this.viewer.zoom(this.zoomLevel);
                }
                if (this.panPosition) {
                    this.viewer.pan(this.panPosition);
                }
                const viewerContext = this;
                this.actionsManager = {
                    ZOOM_STEP: 0.25,
                    duration: 250,
                    zoom: () => viewerContext.viewer.zoom(),
                    zoomIn() {
                        const zoom = this.zoom() + this.ZOOM_STEP;
                        viewerContext.viewer.zoom(zoom);
                        this.canZoomIn = zoom < viewerContext.viewer.maxZoom();
                        this.canZoomOut = zoom > viewerContext.viewer.minZoom();
                    },
                    zoomOut() {
                        const zoom = this.zoom() - this.ZOOM_STEP;
                        viewerContext.viewer.zoom(zoom);
                        this.canZoomIn = zoom < viewerContext.viewer.maxZoom();
                        this.canZoomOut = zoom > viewerContext.viewer.minZoom();
                    },
                    restoreDefault: () => {
                        viewerContext.viewer.layout(this.settings.defaultLayout).run();
                        viewerContext.saveLayout();
                    },
                    canZoomIn: true,
                    canZoomOut: true,
                    ready: true
                };
            });
        } else {
            if (this.viewer) {
                this.zoomLevel = this.viewer._private.zoom;
                this.panPosition = this.viewer.pan();
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

    applyOptions(elements, type) {
        if (!this.elementsOptions) {
            return;
        }
        switch (type) {
            case elementOptionsType.NODE: {
                return elements.reduce((r, cv) => {
                    if (this.elementsOptions.nodes[cv.data.id]) {
                        cv.data = {
                            ...cv.data,
                            ...this.elementsOptions.nodes[cv.data.id]
                        };
                    }
                    r.push(cv);
                    return r;
                }, []);
            }
        }
    }
}
