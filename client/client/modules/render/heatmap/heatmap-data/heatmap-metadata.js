/**
 * @enum {number}
 */
import HeatmapEventDispatcher from '../utilities/heatmap-event-dispatcher';
import events from '../utilities/events';

const HeatmapDataType = {
    /**
     * Number
     */
    number: 0,
    /**
     * String
     */
    string: 1,
    /**
     * Hybrid
     */
    hybrid: 2,
    /**
     *
     * @param {string} type
     * @returns {HeatmapDataType}
     */
    parse(type) {
        switch ((type || '').toLowerCase()) {
            case 'string': return HeatmapDataType.string;
            case 'double':
            case 'integer':
            case 'number':
            default:
                return HeatmapDataType.number;
        }
    }
};

/**
 * @typedef {Object} HeatmapMetadataOptions
 * @property {number} [rowsCount]
 * @property {number} [columnsCount]
 * @property {string[]} [rows]
 * @property {string[]} [columns]
 * @property {HeatmapDataType} [type]
 * @property {number} minimum
 * @property {number} maximum
 * @property {(string|number)[]} [values]
 * @property {{columns: HeatmapBinaryTree, rows: HeatmapBinaryTree}} [tree]
 */

/**
 * @typedef {Object} ApiResponsePayload
 * @property {string[]} [rowLabels]
 * @property {string[]} [columnLabels]
 * @property {string} [cellValueType]
 * @property {number} [minCellValue]
 * @property {number} [maxCellValue]
 * @property {(string|number)[]} [cellValues]
 */

/**
 * @typedef {Object} HeatmapAnnotatedIndex
 * @property {string} name
 * @property {string} [annotation]
 */

/**
 * Parses column/row
 * @param {string|string[]|{[name]: string}|{name: string, annotation: string}} metadataItem
 * @returns {HeatmapAnnotatedIndex}
 */
function parseMetadataItem(metadataItem) {
    if (!metadataItem) {
        return {name: ''};
    }
    if (typeof metadataItem === 'string') {
        return {name: metadataItem};
    }
    if (Array.isArray(metadataItem)) {
        const [name, annotation] = metadataItem;
        return {name, annotation};
    }
    if (
        typeof metadataItem === 'object' &&
        Object.prototype.hasOwnProperty.call(metadataItem, 'name')
    ) {
        return metadataItem;
    }
    if (typeof metadataItem === 'object') {
        const [name] = Object.keys(metadataItem || {});
        return {name, annotation: metadataItem[name]};
    }
    return {name: ''};
}

function createArray(length) {
    return (new Array(length))
        .fill('#')
        .map((element, index) => `${element}${index + 1}`)
        .map(parseMetadataItem);
}

export {HeatmapDataType};

export default class HeatmapMetadata extends HeatmapEventDispatcher {
    /**
     * Creates metadata by response
     * @param {ApiResponsePayload} payload
     * @param {{columns: HeatmapBinaryTree, rows: HeatmapBinaryTree}} [tree]
     */
    static fromResponse(payload = {}, tree = {}) {
        const {
            cellValueType = 'DOUBLE',
            columnLabels: columns = [],
            rowLabels: rows = [],
            minCellValue: min,
            maxCellValue: max,
            cellValues: values = [],
            ...options
        } = payload;
        return new HeatmapMetadata({
            ...options,
            rows,
            columns,
            values,
            tree,
            type: HeatmapDataType.parse(cellValueType),
            minimum: min,
            maximum: max
        });
    }
    /**
     *
     * @param {HeatmapMetadataOptions} options
     */
    constructor(options = {}) {
        super();
        const {
            rows = [],
            columns = [],
            rowsCount = 0,
            columnsCount = 0,
            type = HeatmapDataType.number,
            maximum,
            minimum,
            values = [],
            tree = {}
        } = options;
        /**
         *
         * @type {HeatmapBinaryTree|undefined}
         */
        this.columnsTree = tree && tree.columns && !tree.columns.invalid ? tree.columns : undefined;
        /**
         *
         * @type {HeatmapBinaryTree|undefined}
         */
        this.rowsTree = tree && tree.rows && !tree.rows.invalid ? tree.rows : undefined;
        this.ignoreColumnsTreeOrders = !this.columnsTree;
        this.ignoreRowsTreeOrders = !this.rowsTree;
        this._rows = rows.map(parseMetadataItem);
        this._columns = columns.map(parseMetadataItem);
        if (this._rows.length === 0 && rowsCount > 0) {
            this._rows = createArray(rowsCount);
        }
        if (this._columns.length === 0 && columnsCount > 0) {
            this._columns = createArray(columnsCount);
        }
        /**
         * Data type
         * @type {HeatmapDataType}
         */
        this.type = type;
        /**
         * Maximum value
         * @type {number}
         */
        this.maximum = maximum;
        /**
         * Minimum value
         * @type {number}
         */
        this.minimum = minimum;
        /**
         * Unique values
         * @type {*[]}
         */
        this.values = values.slice().map(o => typeof o === 'string' ? o.trim() : o);
    }

    onColumnsRowsReordered(callback) {
        this.addEventListener(events.data.sorting, callback);
    }

    get dendrogramAvailable() {
        return !!this.columnsTree && !!this.rowsTree;
    }

    get columnsRowsReordered() {
        return !this.ignoreRowsTreeOrders || !this.ignoreColumnsTreeOrders;
    }

    set columnsRowsReordered(columnsRowsReordered) {
        const changed = this.ignoreColumnsTreeOrders !== this.ignoreRowsTreeOrders ||
            this.columnsRowsReordered !== columnsRowsReordered;
        if (changed) {
            this.ignoreColumnsTreeOrders = !columnsRowsReordered;
            this.ignoreRowsTreeOrders = !columnsRowsReordered;
            this.emit(events.data.sorting);
        }
    }

    buildOrderedColumns() {
        if (
            (!this._sortedColumns || !this._columnsOrder || !this._columnsOriginalOrder) &&
            this.columnsTree
        ) {
            this._columnsOrder = new Map();
            this._columnsOriginalOrder = new Map();
            this._sortedColumns = [];
            this.columnsTree.getItemsOrderInfo(this._columns, o => o.name)
                .forEach(({item, originalOrder, order}) => {
                    this._columnsOrder.set(originalOrder, order);
                    this._columnsOriginalOrder.set(order, originalOrder);
                    this._sortedColumns.push(item);
                });
        }
    }

    buildOrderedRows() {
        if (
            (!this._sortedRows || !this._rowsOrder || !this._rowsOriginalOrder) &&
            this.rowsTree
        ) {
            this._rowsOrder = new Map();
            this._rowsOriginalOrder = new Map();
            this._sortedRows = [];
            this.rowsTree.getItemsOrderInfo(this._rows, o => o.name)
                .forEach(({item, originalOrder, order}) => {
                    this._rowsOrder.set(originalOrder, order);
                    this._rowsOriginalOrder.set(order, originalOrder);
                    this._sortedRows.push(item);
                });
        }
    }

    /**
     * Column names
     * @type {string[]}
     */
    get columns() {
        if (this.ignoreColumnsTreeOrders || !this.columnsTree) {
            return this._columns;
        }
        this.buildOrderedColumns();
        return this._sortedColumns;
    }

    /**
     * Row names
     * @type {string[]}
     */
    get rows() {
        if (this.ignoreRowsTreeOrders || !this.rowsTree) {
            return this._rows;
        }
        this.buildOrderedRows();
        return this._sortedRows;
    }

    getColumnOrder(originalOrder) {
        if (this.ignoreColumnsTreeOrders || !this.columnsTree) {
            return originalOrder;
        }
        this.buildOrderedColumns();
        if (!this._columnsOrder || !this._columnsOrder.has(originalOrder)) {
            return originalOrder;
        }
        return this._columnsOrder.get(originalOrder);
    }

    getColumnOriginalOrder(order) {
        if (this.ignoreColumnsTreeOrders || !this.columnsTree) {
            return order;
        }
        this.buildOrderedColumns();
        if (!this._columnsOriginalOrder || !this._columnsOriginalOrder.has(order)) {
            return order;
        }
        return this._columnsOriginalOrder.get(order);
    }

    getRowOrder(originalOrder) {
        if (this.ignoreRowsTreeOrders || !this.rowsTree) {
            return originalOrder;
        }
        this.buildOrderedRows();
        if (!this._rowsOrder || !this._rowsOrder.has(originalOrder)) {
            return originalOrder;
        }
        return this._rowsOrder.get(originalOrder);
    }

    getRowOriginalOrder(order) {
        if (this.ignoreRowsTreeOrders || !this.rowsTree) {
            return order;
        }
        this.buildOrderedRows();
        if (!this._rowsOriginalOrder || !this._rowsOriginalOrder.has(order)) {
            return order;
        }
        return this._rowsOriginalOrder.get(order);
    }
}
