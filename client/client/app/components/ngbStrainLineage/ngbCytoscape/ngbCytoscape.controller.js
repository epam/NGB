import angular from 'angular';
import Cytoscape from 'cytoscape';
import dagre from 'cytoscape-dagre';
import dom_node from 'cytoscape-dom-node';

export default class ngbCytoscapeController {
    constructor($element, $scope, $compile, $window, $timeout, dispatcher, cytoscapeSettings) {
        this.$scope = $scope;
        this.$compile = $compile;
        this.cytoscapeContainer = $element.find('.cytoscape-container')[0];
        this.settings = cytoscapeSettings;
        this.dispatcher = dispatcher;
        this.$timeout = $timeout;

        Cytoscape.use(dagre);
        Cytoscape.use(dom_node);

        const resizeHandler = () => {
            this.centerCytoscape();
        };
        angular.element($window).on('resize', resizeHandler);
        const cytoscapeActiveEventHandler = this.reloadCytoscape.bind(this);
        this.dispatcher.on('cytoscape:panel:active', cytoscapeActiveEventHandler);
        $scope.$on('$destroy', () => {
            this.dispatcher.removeListener('cytoscape:panel:active', cytoscapeActiveEventHandler);
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
                        nodes: this.wrapNodes(this.getPlainNodes(savedLayout.nodes), this.tag, this.settings.style.node),
                        edges: savedLayout.edges
                    };
                    layoutSettings = this.settings.loadedLayout;
                } else {
                    elements = {
                        nodes: this.wrapNodes(this.getPlainNodes(this.elements.nodes), this.tag, this.settings.style.node),
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
                });
                this.viewer.edges().on('click', e => {
                    this.onElementClick({data: e.target.data()});
                });
                layout.run();

            });
        }
    }

    centerCytoscape() {
        if (this.viewer) {
            this.viewer.resize();
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
}
