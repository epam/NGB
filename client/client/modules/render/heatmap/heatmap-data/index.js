import HeatmapBinaryTree from '../utilities/heatmap-binary-tree';
import {HeatmapDataService} from '../../../../dataServices';
import HeatmapDataType from './heatmap-data-type';
import HeatmapEventDispatcher from '../utilities/heatmap-event-dispatcher';
import HeatmapMetadata from './heatmap-metadata';
import HeatmapTrie from '../utilities/heatmap-trie';
import events from '../utilities/events';

const heatMapDataService = new HeatmapDataService();

const IGNORE_TREE = true;

export {HeatmapDataType};
export default class HeatmapData extends HeatmapEventDispatcher {
    constructor(options = {}) {
        super();
        this.dendrogramModeChanged = this.emit.bind(this, events.data.dendrogram);
        this.clear();
        this.options = options;
    }

    get options() {
        return this._options || {};
    }

    set options(options) {
        if (this.anotherOptions(options)) {
            this._options = {...options};
            setTimeout(this.reloadAll.bind(this), 0);
        }
    }

    anotherOptions(options) {
        const {id: currentId} = this._options || {};
        const {id} = options || {};
        return id !== currentId;
    }

    destroy() {
        super.destroy();
        this.clear();
    }

    get error() {
        return this.metadataError || this.dataError;
    }

    /**
     * Metadata
     * @returns {HeatmapMetadata}
     */
    get metadata() {
        if (!this._metadata) {
            this.assignMetadata(new HeatmapMetadata());
        }
        return this._metadata;
    }

    get referenceId() {
        return this.metadata.referenceId;
    }

    set referenceId(referenceId) {
        this.metadata.referenceId = referenceId;
    }

    /**
     *
     * @param {HeatmapMetadata} metadata
     */
    assignMetadata(metadata) {
        let referenceId;
        if (this._metadata) {
            referenceId = this._metadata.referenceId;
            this._metadata.destroy();
            this._metadata = undefined;
        }
        this._metadata = metadata;
        this._metadata.referenceId = referenceId;
        this._metadata.onColumnsRowsReordered(this.dendrogramModeChanged);
    }

    clear() {
        if (this._metadata) {
            this._metadata.destroy();
        }
        /**
         * Metadata (columns, rows, min, max, etc.)
         * @type {HeatmapMetadata|undefined}
         */
        this.assignMetadata(new HeatmapMetadata());
        this._metadata.onColumnsRowsReordered(this.dendrogramModeChanged);
        this.data = [];
        this.columnsTree = new HeatmapBinaryTree([]);
        this.rowsTree = new HeatmapBinaryTree([]);
        this.dataReady = false;
        this.treeReady = false;
        this.metadataReady = false;
        this.dataError = undefined;
        this.treeError = undefined;
        this.metadataError = undefined;
        this.emit(events.clear);
    }

    reloadAll() {
        this.clear();
        this.fetchMetadataPromise = undefined;
        this.fetchTreePromise = undefined;
        this.fetchPromise = undefined;
        return new Promise((resolve) => {
            Promise.all([
                this.fetchTree(),
                this.fetchMetadata(),
                this.fetch()
            ])
                .then(() => resolve(!this.error));
        });
    }

    fetchMetadata() {
        const {id} = this.options;
        if (!id) {
            return Promise.resolve();
        }
        if (!this.fetchMetadataPromise) {
            this.fetchMetadataPromise = new Promise((resolve) => {
                heatMapDataService
                    .loadHeatmapMetadata(id)
                    .then(this.waitForTreeData.bind(this))
                    .then((metadata = {}) => {
                        this.assignMetadata(
                            HeatmapMetadata.fromResponse(
                                metadata,
                                {
                                    columns: this.columnsTree,
                                    rows: this.rowsTree
                                }
                            )
                        );
                        this.metadataReady = true;
                        this.metadataError = undefined;
                        this.emit(events.data.metadata);
                    })
                    .catch(e => {
                        this.metadataReady = false;
                        this.metadataError = e.message;
                        // eslint-disable-next-line no-console
                        console.warn(`Error fetching heatmap metadata: ${this.metadataError}`);
                    })
                    .then(() => resolve(!this.metadataError));
            });
        }
        return this.fetchMetadataPromise;
    }

    fetchTree() {
        const {id} = this.options;
        if (!id) {
            return Promise.resolve();
        }
        if (IGNORE_TREE) {
            this.rowsTree = new HeatmapBinaryTree([]);
            this.columnsTree = new HeatmapBinaryTree([]);
            this.treeReady = true;
            this.treeError = undefined;
            return Promise.resolve();
        }
        if (!this.fetchTreePromise) {
            this.fetchTreePromise = new Promise((resolve) => {
                heatMapDataService
                    .loadHeatmapTree(id)
                    .then((tree) => {
                        const {
                            columns = [],
                            rows = []
                        } = tree || {};
                        this.columnsTree = new HeatmapBinaryTree(columns);
                        this.rowsTree = new HeatmapBinaryTree(rows);
                        this.columnsTree.buildOrders();
                        this.rowsTree.buildOrders();
                        this.treeReady = true;
                        this.treeError = undefined;
                    })
                    .catch((e) => {
                        this.rowsTree = new HeatmapBinaryTree([]);
                        this.columnsTree = new HeatmapBinaryTree([]);
                        this.treeReady = false;
                        this.treeError = e.message;
                        // eslint-disable-next-line no-console
                        console.warn(`Error fetching heatmap metadata: ${this.treeError}`);
                    })
                    .then(() => resolve(!this.treeError));
            });
        }
        return this.fetchTreePromise;
    }

    waitForTreeData(proxyResults) {
        return new Promise((resolve) => {
            this.fetchTree()
                .then(() => resolve(proxyResults));
        });
    }

    waitForMetadata(proxyResults) {
        return new Promise((resolve) => {
            this.fetchMetadata()
                .then(() => resolve(proxyResults));
        });
    }

    fetch() {
        const {id} = this.options;
        if (!id) {
            return Promise.resolve();
        }
        if (!this.fetchPromise) {
            this.fetchPromise = new Promise((resolve) => {
                heatMapDataService
                    .loadHeatmap(id)
                    .then(this.waitForMetadata.bind(this))
                    .then(data => HeatmapTrie.fromPlainData(
                        data,
                        {
                            string: this.metadata && this.metadata.type === HeatmapDataType.string
                        }
                    ))
                    .then((heatmap) => {
                        this.data = heatmap;
                        if (this.data) {
                            this.data.metadata = this.metadata;
                        }
                        this.dataReady = true;
                        this.dataError = undefined;
                        this.emit(events.data.loaded);
                    })
                    .catch(e => {
                        this.dataError = e.message;
                        // eslint-disable-next-line no-console
                        console.warn(`Error fetching data: ${e.message}`);
                        this.dataReady = false;
                    })
                    .then(() => resolve(!this.dataError));
            });
        }
        return this.fetchPromise;
    }

    onClear(callback) {
        this.addEventListener(events.clear, callback);
    }

    onMetadataLoaded(callback) {
        this.addEventListener(events.data.metadata, callback);
    }

    onTreeLoaded(callback) {
        this.addEventListener(events.data.tree, callback);
    }

    onDataLoaded(callback) {
        this.addEventListener(events.data.loaded, callback);
    }

    onColumnsRowsReordered(callback) {
        this.addEventListener(events.data.sorting, callback);
    }
}
