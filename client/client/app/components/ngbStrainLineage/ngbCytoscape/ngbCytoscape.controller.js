import angular from 'angular';
import Cytoscape from 'cytoscape';
import dagre from 'cytoscape-dagre';
import dom_node from 'cytoscape-dom-node';

export default class ngbCytoscapeController {
    constructor($element, $scope, $window, $timeout, dispatcher, cytoscapeSettings, cytoscapeContext) {
        this.$scope = $scope;
        this.cytoscapeContainer = $element.find('.cytoscape-container')[0];
        this.settings = cytoscapeSettings;
        this.cytoscapeContext = cytoscapeContext;
        this.dispatcher = dispatcher;
        this.$timeout = $timeout;

        Cytoscape.use(dagre);
        Cytoscape.use(dom_node);

        const resizeHandler = () => {};
        angular.element($window).on('resize', resizeHandler);
        const cytoscapeActiveEventHandler = this.cytoscapePanelActiveChanged.bind(this);
        this.dispatcher.on('cytoscape:panel:active', cytoscapeActiveEventHandler);
        $scope.$on('$destroy', () => {
            this.dispatcher.removeListener('cytoscape:panel:active', cytoscapeActiveEventHandler);
            angular.element($window).off('resize', resizeHandler);
        });
        this.cytoscapePanelActiveChanged(true);

    }

    static get UID() {
        return 'ngbCytoscapeController';
    }

    wrapNodes(nodes, nodeStyle) {
        const wrappedNodes = [];
        function wrapNode(nodeData, rp) {
            const id = nodeData.id;
            const div = document.createElement('div');
            div.innerHTML = `node ${nodeData.label}`;
            div.classList = ['strain-lineage-cytoscape-node'];
            div.style.width = `${nodeStyle.width}px`;
            div.style.height = `${nodeStyle.height}px`;

            return {
                data: {
                    id: id,
                    label: nodeData.label,
                    dom: div,
                },
                // renderedPosition: rp,
            };
        }

        for (const node of nodes) {
            wrappedNodes.push(wrapNode(node.data));
        }
        return wrappedNodes;
    }


    cytoscapePanelActiveChanged(active) {
        if (active) {
            if (this.viewer) {
                this.viewer = null;
            }
            this.$timeout(() => {
                this.viewer = Cytoscape({
                    container: this.cytoscapeContainer,
                    elements: {
                        nodes: this.wrapNodes(this.elements.nodes, this.settings.style.node),
                        edges: this.elements.edges
                    },
                    style: [
                        {
                            selector: 'node',
                            style: this.settings.style.node
                        },
                        {
                            selector: 'edge',
                            style: this.settings.style.edge
                        }
                    ],
                    ...this.settings.options
                });
                this.viewer.domNode();
                this.viewer.layout(this.settings.defaultLayout).run();
            });
        }
    }
}
