import Trie from './trie';

const cacheSymbol = Symbol('cached request');

const IndexOrder = {
    columnRow: 'column,row',
    rowColumn: 'row,column'
};

const maxItemsPerIteration = 5000;

function* getSectorElements(sector, radius, column, row, columnShift, rowShift) {
    const dC = sector % 2 === 0 ? 1 : 0;
    const dR = sector % 2 === 0 ? 0 : 1;
    const columnCenter = Math.floor(column) + dR * radius * (sector === 1 ? 1 : -1);
    const rowCenter = Math.floor(row) + dC * radius * (sector === 0 ? -1 : 1);
    const sC = columnShift > 0 ? 1 : -1;
    const sR = rowShift > 0 ? 1 : -1;
    yield {column: columnCenter, row: rowCenter};
    for (let r = 1; r <= radius; r += 1) {
        yield {column: columnCenter + sC * dC * r, row: rowCenter + sR * dR * r};
        yield {column: columnCenter - sC * dC * r, row: rowCenter - sR * dR * r};
    }
}

/**
 * @typedef {Object} HeatmapDataItem
 * @property {string} annotation
 * @property {number} column
 * @property {number} row
 * @property {number|string} value
 * @property {boolean} [string=false]
 */

class HeatmapTrie extends Trie {
    static fromData (data = [], options = {}) {
        const {
            column = 'x',
            row = 'y',
            value = 'value',
            columns = 0,
            indexOrder,
            string = false
        } = options;
        const wrapProperty = property => o => Object.prototype.hasOwnProperty.call(o, property)
        && typeof property === 'string'
            ? o[property]
            : undefined;
        const wrapPropertyFn = propertyFn => typeof propertyFn === 'function'
            ? propertyFn
            : wrapProperty(propertyFn);
        const columnFn = wrapPropertyFn(column);
        const rowFn = wrapPropertyFn(row);
        const valueFn = wrapPropertyFn(value);
        const iterationLength = columns > 0
            ? Math.max(1, Math.floor(maxItemsPerIteration / columns)) * columns
            : maxItemsPerIteration;
        const heatmap = new HeatmapTrie({indexOrder});
        const _append = (callback, index = 0) => {
            const nextIterationIndex = index + iterationLength;
            const finalIteration = nextIterationIndex >= data.length;
            if (!finalIteration) {
                setTimeout(() => _append(callback, nextIterationIndex), 0);
            }
            for (let d = index; d < data.length && d < nextIterationIndex; d += 1) {
                const columnIndex = columnFn(data[d]);
                const rowIndex = rowFn(data[d]);
                const value = valueFn(data[d]);
                heatmap.setHeatMapItem(
                    columnIndex,
                    rowIndex,
                    string && value !== undefined ? `${value}`.trim() : value,
                );
            }
            if (finalIteration) {
                heatmap.buildMetadata();
                callback();
            }
        };
        return new Promise((resolve) => {
            _append(() => resolve(heatmap));
        });
    }

    /**
     *
     * @param {*[]} data
     * @param {{indexOrder: string, string: false}} options
     * @returns {Promise<HeatmapTrie>}
     */
    static fromPlainData (data = [], options = {}) {
        const {
            indexOrder,
            string = false
        } = options;
        const heatmap = new HeatmapTrie({indexOrder});
        let rowIndex = 0;
        let columnIndex = 0;
        const _append = (callback) => {
            let interrupted = false;
            let i = 0;
            for (; rowIndex < data.length; rowIndex += 1, columnIndex = 0) {
                const row = data[rowIndex];
                for (; columnIndex < row.length; columnIndex += 1) {
                    i += 1;
                    if (i > maxItemsPerIteration) {
                        interrupted = true;
                        break;
                    }
                    const column = row[columnIndex];
                    let value;
                    let annotation;
                    if (Array.isArray(column)) {
                        [value, annotation] = column;
                    } else if (typeof column === 'object') {
                        const [key] = Object.keys(column);
                        if (key) {
                            value = Number.isNaN(Number(key)) ? key : Number(key);
                            annotation = column[key];
                        }
                    } else {
                        value = column;
                    }
                    if (value !== undefined || annotation !== undefined) {
                        heatmap.setHeatMapItem(
                            columnIndex,
                            rowIndex,
                            string && value !== undefined ? `${value}`.trim() : value,
                            annotation
                        );
                    }
                }
                if (interrupted) {
                    break;
                }
            }
            if (interrupted) {
                setTimeout(() => _append(callback), 0);
            } else {
                heatmap.buildMetadata();
                callback();
            }
        };
        return new Promise((resolve) => {
            _append(() => resolve(heatmap));
        });
    }

    constructor(options = {}) {
        super();
        const {
            indexOrder = IndexOrder.rowColumn
        } = options;
        this.indexOrder = indexOrder;
        this.indexRange = [
            [Infinity, -Infinity],
            [Infinity, -Infinity]
        ];
        /**
         *
         * @type {HeatmapMetadata|undefined}
         * @private
         */
        this._metadata = undefined;
    }

    /**
     *
     * @returns {HeatmapMetadata|undefined}
     */
    get metadata() {
        return this._metadata;
    }

    /**
     *
     * @param {HeatmapMetadata|undefined} metadata
     */
    set metadata(metadata) {
        this._metadata = metadata;
    }

    getColumnOrder(column) {
        if (this.metadata) {
            return this.metadata.getColumnOrder(column);
        }
        return column;
    }

    getColumnOriginalOrder(column) {
        if (this.metadata) {
            return this.metadata.getColumnOriginalOrder(column);
        }
        return column;
    }

    getRowOrder(row) {
        if (this.metadata) {
            return this.metadata.getRowOrder(row);
        }
        return row;
    }

    getRowOriginalOrder(row) {
        if (this.metadata) {
            return this.metadata.getRowOriginalOrder(row);
        }
        return row;
    }

    cacheRequest (index, create = false) {
        if (this[cacheSymbol] && this[cacheSymbol].index === index) {
            return this[cacheSymbol].cache;
        }
        const node = create ? this.ensure(index, new Trie()) : this.get(index);
        this[cacheSymbol] = {
            index,
            cache: node
        };
        return node;
    }

    getOrderedIndex (column, row) {
        return this.indexOrder === IndexOrder.rowColumn
            ? [row, column]
            : [column, row];
    }

    getRowColumn (index1, index2) {
        return this.indexOrder === IndexOrder.rowColumn
            ? {row: index1, column: index2}
            : {column: index1, row: index2};
    }

    setHeatMapItem(column, row, value, annotation) {
        const [index1, index2] = this.getOrderedIndex(column, row);
        const node = this.cacheRequest(index1, true);
        this.indexRange[0] = [
            Math.min(index1, this.indexRange[0][0]),
            Math.max(index1, this.indexRange[0][1]),
        ];
        this.indexRange[1] = [
            Math.min(index2, this.indexRange[1][0]),
            Math.max(index2, this.indexRange[1][1]),
        ];
        return node.data.set(index2, {value, annotation});
    }

    getHeatMapItem(column, row) {
        const [index1, index2] = this.getOrderedIndex(
            this.getColumnOriginalOrder(column),
            this.getRowOriginalOrder(row)
        );
        return this.getHeatMapItemByOrderedIndex(index1, index2);
    }

    /**
     *
     * @param {*} data
     * @param {number} index1
     * @param {number} index2
     * @returns {undefined|HeatmapDataItem}
     */
    formatData(data, index1, index2) {
        if (data) {
            const {
                column,
                row
            } = this.getRowColumn(index1, index2);
            return {
                ...data,
                column: this.getColumnOrder(column),
                row: this.getRowOrder(row)
            };
        }
        return undefined;
    }

    getHeatMapItemByOrderedIndex(index1, index2) {
        const node = this.cacheRequest(index1);
        if (node && node.data) {
            const data = node.data.getData(index2);
            return this.formatData(data, index1, index2);
        }
        return undefined;
    }

    buildMetadata(node = this) {
        if (node && node.data) {
            node.data.calculateSize();
        }
        let count = node && node.data ? node.data.count : 0;
        for (const child of node.children.values()) {
            this.buildMetadata(child);
            count += child.count;
        }
        node.count = count;
    }

    /**
     * Returns all items with column/row index
     * @returns {Generator<HeatmapDataItem, void, *>}
     */
    *entries() {
        for (const child of this.leavesWithIndex()) {
            const {index: index1, data: subTree} = child;
            for (const subTreeChild of subTree.leavesWithIndex()) {
                const {index: index2, data} = subTreeChild;
                if (data) {
                    yield this.formatData(data, index1, index2);
                }
            }
        }
    }

    /**
     *
     * @param {Object} center
     * @param {number} center.column
     * @param {number} center.row
     * @param {number} radius
     * @returns {Generator<HeatmapDataItem, void, *>}
     */
    *entriesWithinRadius(center, radius) {
        const {
            column,
            row
        } = center || {};
        if (column === undefined || row === undefined || radius === undefined || radius <= 0) {
            return this.entries();
        } else {
            const columnShift = (2.0 * (column - Math.floor(column)) - 1) || 1;
            const rowShift = (2.0 * (row - Math.floor(row)) - 1) || 1;
            const columnShiftM = Math.abs(columnShift);
            const rowShiftM = Math.abs(rowShift);
            let bestSector = 0;
            if (columnShiftM < rowShiftM) {
                bestSector = Math.sign(rowShift) < 0 ? 0 : 2;
            } else {
                bestSector = Math.sign(columnShift) < 0 ? 3 : 1;
            }
            const correctSector = s => s % 4;
            const sectorsOrder = [
                correctSector(bestSector),
                correctSector(bestSector + 1),
                correctSector(bestSector + 2),
                correctSector(bestSector + 3)
            ];
            for (let r = 0; r <= Math.ceil(radius); r += 1) {
                for (const sector of sectorsOrder) {
                    for (const coordinates of getSectorElements(sector, r, column, row, columnShift, rowShift)) {
                        const data = this.getHeatMapItem(coordinates.column, coordinates.row);
                        if (data) {
                            yield data;
                        }
                    }
                }
            }
        }
    }
}

HeatmapTrie.IndexOrder = IndexOrder;

export default HeatmapTrie;
