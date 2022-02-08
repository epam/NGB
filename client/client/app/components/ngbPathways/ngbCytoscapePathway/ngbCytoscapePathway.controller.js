import angular from 'angular';

const Cytoscape = require('cytoscape');
const graphml = require('cytoscape-graphml');
const sbgnStylesheet = require('cytoscape-sbgn-stylesheet');
const $ = require('jquery');

const SCALE = 0.3;
const searchedColor = '#00cc00';
let defaultNodeStyle = {};

export default class ngbCytoscapePathwayController {
    constructor($element, $scope, $compile, $window, $timeout, dispatcher, cytoscapePathwaySettings) {
        this.$scope = $scope;
        this.$compile = $compile;
        this.cytoscapeContainer = $element.find('.cytoscape-container')[0];
        this.settings = cytoscapePathwaySettings;
        this.dispatcher = dispatcher;
        this.$timeout = $timeout;
        this.actionsManager = {
            ready: false
        };

        const resizeHandler = () => {
            if (this.resizeCytoscape()) {
                this.centerCytoscape();
            }
        };
        graphml(Cytoscape, $);
        angular.element($window).on('resize', resizeHandler);
        const cytoscapeActiveEventHandler = this.reloadCytoscape.bind(this);
        this.dispatcher.on('cytoscape:panel:active', cytoscapeActiveEventHandler);
        const handleSelectionChange = (e) => {
            if (this.viewer && e && e.id) {
                this.viewer.edges().forEach((edge) => {
                    const data = edge.data();
                    if (data.id === e.id) {
                        // deselect logic
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
        return 'ngbCytoscapePathwayController';
    }

    $onChanges(changes) {
        if (!!changes.elements &&
            !!changes.elements.previousValue &&
            !!changes.elements.currentValue &&
            (changes.elements.previousValue.id !== changes.elements.currentValue.id)) {
            this.reloadCytoscape(true);
        }
        if (!!changes.searchParams &&
            !!changes.searchParams.previousValue &&
            !!changes.searchParams.currentValue) {
            if (changes.searchParams.currentValue.search
                && changes.searchParams.previousValue.search !== changes.searchParams.currentValue.search) {
                this.searchNode(
                    changes.searchParams.currentValue.search,
                    node => {
                        node.style({
                            'color': searchedColor,
                            'border-color': searchedColor
                        });
                    },
                    node => {
                        node.style({
                            'color': defaultNodeStyle.color,
                            'border-color': defaultNodeStyle['border-color']
                        });
                    }
                );
            }
        }
    }

    reloadCytoscape(active) {
        if (active) {
            if (this.viewer) {
                this.viewer.destroy();
                this.viewer = null;
            }
            this.$timeout(() => {
                const sbgnStyle = sbgnStylesheet(Cytoscape);
                defaultNodeStyle = {
                    ...this.settings.style.node,
                    ...this.getNodeStyle(sbgnStyle)
                };
                const savedState = JSON.parse(localStorage.getItem(this.storageName) || '{}');
                const savedLayout = savedState.layout ? savedState.layout[this.elements.id] : undefined;
                let elements;
                if (savedLayout) {
                    elements = {
                        nodes: this.getPlainNodes(savedLayout.nodes),
                        edges: savedLayout.edges
                    };
                } else {
                    elements = {
                        nodes: this.positionedNodes(this.elements.nodes),
                        edges: this.elements.edges
                    };
                }
                this.viewer = Cytoscape({
                    container: this.cytoscapeContainer,
                    style: sbgnStyle,
                    layout: {name: 'preset'},
                    elements: elements,
                });
                const layout = this.viewer.layout(this.settings.loadedLayout);
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
                const viewerContext = this;
                this.actionsManager = {
                    ZOOM_STEP: 0.125,
                    duration: 250,
                    zoom: () => viewerContext.viewer.zoom(),
                    zoomIn() {
                        const zoom = this.zoom() + this.ZOOM_STEP;
                        viewerContext.viewer.zoom(zoom);
                        viewerContext.centerCytoscape();
                        this.canZoomIn = zoom < viewerContext.viewer.maxZoom();
                        this.canZoomOut = zoom > viewerContext.viewer.minZoom();
                    },
                    zoomOut() {
                        const zoom = this.zoom() - this.ZOOM_STEP;
                        viewerContext.viewer.zoom(zoom);
                        viewerContext.centerCytoscape();
                        this.canZoomIn = zoom < viewerContext.viewer.maxZoom();
                        this.canZoomOut = zoom > viewerContext.viewer.minZoom();
                    },
                    restoreDefault: () => {
                        this.viewer.batch(() => {
                            this.viewer.remove(this.viewer.nodes());
                            this.viewer.remove(this.viewer.edges());
                            this.viewer.add(this.positionedNodes(this.elements.nodes));
                            this.viewer.add(this.elements.edges);
                        });
                        // viewerContext.viewer.layout(this.settings.defaultLayout).run();
                        viewerContext.saveLayout();
                    },
                    canZoomIn: true,
                    canZoomOut: true,
                    ready: true
                };
            });
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

    positionedNodes(nodes) {
        nodes.forEach(node => {
            if (node.data.bbox) {
                node.position = {
                    x: node.data.bbox.x / SCALE,
                    y: node.data.bbox.y / SCALE
                };
            }
        });
        return nodes;
    }

    searchNode(term, onSatisfy, onDeny) {
        if (!this.viewer) {
            return;
        }
        const roots = this.viewer.nodes().roots();
        this.viewer.nodes().dfs({
            root: roots,
            visit: node => {
                if (this.deepSearch(node.data(), term)) {
                    onSatisfy(node);
                } else {
                    onDeny(node);
                }
            }
        });
    }

    deepSearch(obj, term) {
        let result = false;
        for (const key in obj) {
            if (!obj.hasOwnProperty(key) || !obj[key]) continue;
            if (obj[key] instanceof Object || obj[key] instanceof Array) {
                result = this.deepSearch(obj[key], term);
            } else {
                result = obj[key].toString().toLocaleLowerCase()
                    .includes(term.toLocaleLowerCase());
            }
            if (result) {
                return true;
            }
        }
        return false;
    }

    getNodeStyle(style) {
        const result = {};
        Object.keys(style).forEach(key => {
            if (style[key].selector === 'node') {
                Object.keys(style[key].properties).forEach(propKey => {
                    result[style[key].properties[propKey].name] = style[key].properties[propKey].value;
                });
            }
        });
        return result;
    }
}
