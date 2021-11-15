export default class ngbStrainLineageService {

    lineageTrees = [];
    currentReferenceId = null;

    constructor(genomeDataService, dispatcher) {
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
        this.initEvents();
    }

    initEvents() {
        // this.dispatcher.on('read:show:lineage', data => {
        //     this.currentReferenceId = data.referenceId;
        // });
    }

    _currentTreeId = null;

    get currentTreeId() {
        return this._currentTreeId;
    }

    set currentTreeId(value) {
        this._currentTreeId = value;
    }

    static instance(genomeDataService, dispatcher) {
        return new ngbStrainLineageService(genomeDataService, dispatcher);
    }

    async loadStrainLineages(referenceId) {
        if (!referenceId) {
            return [];
        }
        const data = await this.genomeDataService.getLineageTreesByReference(referenceId);
        if (data) {
            this.lineageTrees = data.map(this._formatServerToClient);
            this.currentTreeId = this.lineageTrees.length ? this.lineageTrees[0].id : null;
            return this.lineageTrees.map(tree => ({
                id: tree.id,
                name: tree.metadata.name,
                prettyName: tree.metadata.prettyName,
            }));
        } else {
            return [];
        }
    }

    _formatServerToClient(data) {
        const formatNodes = node => ({
            data: {
                id: `n_${node.lineageTreeNodeId}`,
                title: node.name,
                description: node.description,
                creationDate: node.creationDate,
                referenceId: node.referenceId,
                tooltip: node.attributes
            }
        });
        const formatEdges = edge => ({
            data: {
                id: `e_${edge.lineageTreeEdgeId.toString()}`,
                source: `n_${edge.nodeFromId.toString()}`,
                target: `n_${edge.nodeToId.toString()}`,
                label: edge.typeOfInteraction,
                tooltip: edge.attributes
            }
        });
        return {
            id: data.lineageTreeId,
            metadata: {
                description: data.description,
                name: data.name,
                prettyName: data.prettyName,
            },
            nodes: data.nodes.map(formatNodes),
            edges: data.edges.map(formatEdges)
        };
    }

    getLineageTreeById(id) {
        return this.lineageTrees.filter(tree => tree.id === id)[0];
    }

}
