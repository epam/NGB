import angular from 'angular';
import dom_node from 'cytoscape-dom-node';
import {formatColor} from '../../../../modules/render/heatmap/color-scheme/helpers';

const Cytoscape = require('cytoscape');
const graphml = require('cytoscape-graphml');
const sbgnStylesheet = require('cytoscape-sbgn-stylesheet');
const $ = require('jquery');

const SCALE = 0.3;
const searchedColor = '#00cc00';
let defaultNodeStyle = {};
const annotationTypeList = {
    HEATMAP: 0,
    CSV: 1,
    MANUAL: 2
};
const configurationType = {
    STRING: 0,
    NUMBER: 1,
    RANGE: 2
};

const DATABASE_SOURCES = {
    CUSTOM: 'CUSTOM',
    BIOCYC: 'BIOCYC',
    COLLAGE: 'COLLAGE'
};

function deepSearch(obj, term, type = configurationType.STRING, fieldsToIgnore = []) {
    let result = false;
    for (const key in obj) {
        if (fieldsToIgnore.indexOf(key) > -1) continue;
        if (!obj.hasOwnProperty(key) || !obj[key]) continue;
        if (obj[key] instanceof Object || obj[key] instanceof Array) {
            result = deepSearch(obj[key], term, type, fieldsToIgnore);
        } else {
            switch (type) {
                case configurationType.STRING: {
                    result = obj[key].toString().toLocaleLowerCase()
                        .includes(term.toLocaleLowerCase());
                    break;
                }
                case configurationType.NUMBER: {
                    result = Number(obj[key]) === Number(term);
                    break;
                }
                case configurationType.RANGE: {
                    const numericValue = Number(obj[key]);
                    if (isNaN(numericValue)) {
                        result = false;
                    } else {
                        result = numericValue >= term[0] && numericValue <= term[1];
                    }
                    break;
                }
            }
        }
        if (result) {
            return true;
        }
    }
    return false;
}

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
        Cytoscape.use(dom_node);
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
            if (changes.searchParams.currentValue.annotations) {
                this.annotateTree(changes.searchParams.currentValue.annotations);
            }
        }
    }

    wrapNodes(nodes, nodeTag, nodeStyle, isDom) {
        const wrappedNodes = [];

        function decorateData(data) {
            if (data.label) {
                data.label = data.label.replace('-', '\u2011');
            }
            return data;
        }

        function wrapNode(node) {
            node.data = decorateData(node.data);
            if (isDom) {
                const div = document.createElement(nodeTag);
                div.setAttribute('data-node-data-json', JSON.stringify(node.data));
                div.setAttribute('data-on-element-click', '$ctrl.onElementClick({data: data})');
                div.setAttribute('id', node.data.id);
                div.classList = `${node.classes || node.class || ''} internal-pathway-cytoscape-node`;
                div.style.width = `${nodeStyle.width}px`;
                div.style.height = `${nodeStyle.height}px`;
                node.data.dom = div;
            }

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
                let cytoscapeStyle;
                if (this.isDom()) {
                    cytoscapeStyle = [
                        {
                            selector: 'node',
                            style: this.settings.style.node
                        }
                    ];
                    defaultNodeStyle = {
                        ...this.settings.style.node
                    };
                } else {
                    cytoscapeStyle = sbgnStylesheet(Cytoscape);
                    defaultNodeStyle = {
                        ...this.getSbgnNodeStyle(cytoscapeStyle)
                    };
                }
                const savedState = JSON.parse(localStorage.getItem(this.storageName) || '{}');
                const savedLayout = savedState.layout ? savedState.layout[this.elements.id] : undefined;
                let elements;
                if (savedLayout) {
                    elements = {
                        nodes: this.wrapNodes(
                            this.getPlainNodes(savedLayout.nodes),
                            this.tag,
                            defaultNodeStyle,
                            this.isDom()
                        ),
                        edges: savedLayout.edges
                    };
                } else {
                    elements = {
                        nodes: this.wrapNodes(
                            this.getPlainNodes(this.positionedNodes(this.elements.nodes)),
                            this.tag,
                            defaultNodeStyle,
                            this.isDom()
                        ),
                        edges: this.elements.edges
                    };
                }
                this.viewer = Cytoscape({
                    container: this.cytoscapeContainer,
                    style: cytoscapeStyle,
                    layout: {name: 'preset'},
                    elements: elements,
                    ...this.settings.options
                });
                this.viewer.domNode();
                const layout = this.viewer.layout(this.settings.loadedLayout);
                layout.on('layoutready', () => {
                    this.$compile(this.cytoscapeContainer)(this.$scope);
                    this.viewer.on('dragfree', this.saveLayout.bind(this));
                    this.resizeCytoscape();
                    if (this.searchParams.annotations && this.searchParams.annotations.length) {
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
                    ZOOM_STEP: viewerContext.settings.externalOptions.zoomStep,
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
                classes: cv.classes,
                class: cv.data.class,
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

    // TODO: apply dome_node to sbgn-styled pathways
    isDom() {
        return this.elements.source === DATABASE_SOURCES.COLLAGE;
    }

    searchNode(node, term) {
        const defaultStyle = node.data('default-style') || defaultNodeStyle;
        let style;
        if (term === '' || !deepSearch(node.data(), term, undefined, ['dom'])) {
            style = {
                'color': defaultStyle.color,
                'border-color': defaultStyle['border-color'],
                'font-weight': defaultStyle['font-weight']
            };
            this.setStyleToNode(node, style);
            node.data('isFound', false);
            if (node.data('isAnnotated') && this.searchParams.annotations) {
                this.annotateNode(node, this.searchParams.annotations);
            }
        } else {
            style = {
                color: searchedColor,
                'border-color': searchedColor,
                'font-weight': 'bold'
            };
            if (this.isDom()) {
                style.background = defaultStyle.background;
            } else {
                style['background-color'] = defaultStyle['background-color'];
            }
            if (!node.data('isAnnotated')) {
                this.backupNodeStyle(node, style);
            }
            this.setStyleToNode(node, style);
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
        if (node.data('isAnnotated')) {
            this.clearAnnotation(node);
        }
        for (const annotation of annotationList) {
            if (!node.data('isAnnotated')) {
                if (annotation.type === annotationTypeList.MANUAL) {
                    this.manualAnnotation(node, annotation);
                } else {
                    this.fileAnnotation(node, annotation);
                }
            }
        }
    }

    clearAnnotation(node) {
        node.data('isAnnotated', false);
        if (!node.data('isFound')) {
            const defaultStyle = node.data('default-style') || defaultNodeStyle;
            const style = {
                color: defaultStyle.color,
                'border-color': defaultStyle['border-color']
            };
            if (this.isDom()) {
                style.background = defaultStyle.background;
            } else {
                style['background-color'] = defaultStyle['background-color'];
            }
            this.setStyleToNode(node, style);
        }
    }

    manualAnnotation(node, annotation) {
        let style;
        for (const terms of annotation.value) {
            if (deepSearch(node.data(), terms.term, undefined, ['dom'])) {
                if (!node.data('isFound')) {
                    if (this.isDom()) {
                        style = {
                            background: terms.backgroundColor
                        };
                    } else {
                        style = {
                            'background-color': terms.backgroundColor
                        };
                    }
                    if (terms.foregroundColor) {
                        style.color = terms.foregroundColor;
                        style['border-color'] = terms.foregroundColor;
                    }
                    if (!node.data('isAnnotated')) {
                        this.backupNodeStyle(node, style);
                    }
                    this.setStyleToNode(node, style);
                }
                node.data('isAnnotated', true);
            }
        }
    }

    fileAnnotation(node, annotation) {
        let termsList = [];
        const style = {};
        const terms = annotation.value;

        for (let labelIndex = 0; labelIndex < terms.labels.length; labelIndex++) {
            if (deepSearch(node.data(), terms.labels[labelIndex], annotation.colorScheme.dataType, ['dom'])) {
                termsList = termsList.concat(terms.values[labelIndex]);
            }
            const colorList = [];
            termsList.forEach(term => {
                colorList.push(formatColor(annotation.colorScheme.getColorForValue(term), annotation.colorScheme.colorFormat));
            });
            if (colorList.length) {
                if (!node.data('isFound')) {
                    if (this.isDom()) {
                        style.background = this.buildGradientBackground(colorList);
                    } else {
                        style['background-color'] = colorList[0];
                    }
                    if (!node.data('isAnnotated')) {
                        this.backupNodeStyle(node, style);
                    }
                    this.setStyleToNode(node, style);
                }
                node.data('isAnnotated', true);
            }
        }
    }

    buildGradientBackground(colorList) {
        const percentage = Math.ceil(100 / colorList.length);
        let background = 'linear-gradient(to right';
        for (let i = 0; i < colorList.length; i++) {
            background += `, ${colorList[i]} ${percentage * i}%, ${colorList[i]} ${percentage * (i + 1)}%`;
        }
        background += ')';
        return background;
    }

    backupNodeStyle(node, style) {
        let backedStyle = {};
        if (this.isDom()) {
            const domElem = $(document).find(`#${node.data('id')} .internal-pathway-node-container`);
            if (domElem) {
                Object.keys(style).forEach(key => {
                    backedStyle[key] = domElem.css(key);
                });
            }
        } else {
            backedStyle = node.style();
        }
        const oldBackedStyle = node.data('default-style');
        node.data('default-style', {
            ...oldBackedStyle,
            ...backedStyle
        });
    }

    setStyleToNode(node, style) {
        if (this.isDom()) {
            this.setStyleToDomNode(node.data('id'), style);
        } else {
            node.style(style);
        }
    }

    setStyleToDomNode(domId, style) {
        const domElem = $(document).find(`#${domId} .internal-pathway-node-container`);
        if (domElem) {
            Object.keys(style).forEach(key => {
                domElem.css(key, style[key]);
            });
        }
    }

    getSbgnNodeStyle(style) {
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
