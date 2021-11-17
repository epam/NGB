export default class ngbStrainLineageService {

    cutLineageTrees = [];
    currentReferenceId = null;
    _selectedTreeId = null;

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

    get selectedTreeId() {
        return this._selectedTreeId;
    }

    set selectedTreeId(value) {
        this._selectedTreeId = value;
    }

    static instance(genomeDataService, dispatcher) {
        return new ngbStrainLineageService(genomeDataService, dispatcher);
    }

    getUniqueTrees(trees) {
        return trees.filter((tree, index, self) =>
            index === self.findIndex((t) => t.id === tree.id)
        );
    }

    async loadStrainLineages(referenceId) {
        if (!referenceId) {
            return [];
        }
        try {
            const data = await this.genomeDataService.getLineageTreesByReference(referenceId);
            if (data) {
                if (this.selectedTreeId) {
                    this.cutLineageTrees = this.getUniqueTrees(
                        this.cutLineageTrees.filter(tree => tree.id === this.selectedTreeId)
                            .concat(data.map(this._formatCutListToClient))
                    );
                } else {
                    this.cutLineageTrees = data.map(this._formatCutListToClient);
                    this.selectedTreeId = this.cutLineageTrees.length ? this.cutLineageTrees[0].id : null;
                }
                return {
                    data: this.cutLineageTrees,
                    error: false
                };
            } else {
                return {
                    data: [],
                    error: false
                };
            }
        } catch (e) {
            return {
                data: [],
                error: e.message
            };
        }
    }

    _formatCutListToClient(tree) {
        return {
            id: tree.lineageTreeId,
            name: tree.name,
            prettyName: tree.prettyName,
        };
    }

    _formatTreeToClient(data) {
        const formatNodes = (node = []) => ({
            data: {
                id: `n_${node.lineageTreeNodeId}`,
                title: node.name,
                description: node.description,
                creationDate: node.creationDate,
                referenceId: node.referenceId,
                tooltip: node.attributes
            }
        });
        const formatEdges = (edge = []) => ({
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

    async getLineageTreeById(id) {
        try {
            const data = await this.genomeDataService.getLineageTreeById(id);
            if (data) {
                return {
                    tree: this._formatTreeToClient(data),
                    error: false
                };
            } else {
                return {
                    tree: null,
                    error: false
                };
            }
        } catch (e) {
            return {
                tree: null,
                error: e.message
            };
        }
    }

}
