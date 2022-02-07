import angular from 'angular';

const Cytoscape = require('cytoscape');
const graphml = require('cytoscape-graphml');
const sbgnStylesheet = require('cytoscape-sbgn-stylesheet');
const $ = require('jquery');

const SCALE = 0.3;
const searchedColor = '#00cc00';
let defaultNodeStyle = {};

function deepSearch(obj, term) {
    let result = false;
    for (const key in obj) {
        if (!obj.hasOwnProperty(key) || !obj[key]) continue;
        if (obj[key] instanceof Object || obj[key] instanceof Array) {
            result = deepSearch(obj[key], term);
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


export default class ngbCytoscapePathwayController {
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
            if (changes.searchParams.currentValue.search !== null
                && changes.searchParams.previousValue.search !== changes.searchParams.currentValue.search) {
                this.searchTree(changes.searchParams.currentValue.search);
            }
            if (changes.searchParams.currentValue.annotations && changes.searchParams.currentValue.annotations.length) {
                this.annotateTree(changes.searchParams.currentValue.annotations);
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
                    if(this.searchParams.annotations && this.searchParams.annotations.length) {
                        this.annotateTree(this.searchParams.annotations);
                    }
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
            cv.data.isFound = false;
            cv.data.isAnnotated = false;
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


    searchTree(term) {
        if (!this.viewer) {
            return;
        }
        this.viewer.nodes().forEach(node => {
            this.searchNode(node, term);
        });
    }

    searchNode(node, term) {
        if (term === '' || !deepSearch(node.data(), term)) {
            node.style({
                'color': defaultNodeStyle.color,
                'border-color': defaultNodeStyle['border-color'],
                'font-weight': defaultNodeStyle['font-weight']
            });
            node.data('isFound', false);
            if (node.data('isAnnotated') && this.searchParams.annotations) {
                this.annotateNode(node, this.searchParams.annotations);
            }
        } else {
            node.style({
                'color': searchedColor,
                'border-color': searchedColor,
                'background-color': defaultNodeStyle['background-color'],
                'font-weight': 'bold'
            });
            node.data('isFound', true);
        }
    }

    annotateTree(annotationList) {
        if (!this.viewer) {
            return;
        }
        // TODO: figure out why dfs() ignores half of a tree (roots?)
        // const roots = this.viewer.nodes().roots();
        // this.viewer.nodes().dfs({
        //     root: roots,
        //     visit: node => {
        this.viewer.nodes().forEach(node => {
            this.annotateNode(node, annotationList);
        });
    }

    annotateNode(node, annotationList) {
        let style = {};
        if (node.data('isAnnotated')) {
            node.data('isAnnotated', false);
            if (!node.data('isFound')) {
                style = {
                    'background-color': defaultNodeStyle['background-color'],
                    color: defaultNodeStyle.color,
                    'border-color': defaultNodeStyle['border-color']
                };
                node.style(style);
            }
        }
        for (const annotation of annotationList) {
            for (const termsList of annotation.value) {
                if (deepSearch(node.data(), termsList.term) && !node.data('isAnnotated')) {
                    node.data('isAnnotated', true);
                    if (!node.data('isFound')) {
                        style = {
                            'background-color': termsList.backgroundColor
                        };
                        if (termsList.foregroundColor) {
                            style.color = termsList.foregroundColor;
                            style['border-color'] = termsList.foregroundColor;
                        }
                        node.style(style);
                    }
                }
            }
        }
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
