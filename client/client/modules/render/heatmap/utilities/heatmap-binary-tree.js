const OBJECT_DATA_ITEM_NAME_FIELD = 'name';

function dataItemIsArray(data) {
    return data && Array.isArray(data) && data.length > 0 && typeof data[0] === 'string';
}

function parseDataItemAsArray(data) {
    if (dataItemIsArray(data)) {
        const [name, value, ...info] = data;
        return {
            name,
            value,
            info
        };
    }
    return undefined;
}

function dataItemIsString(data) {
    return data && typeof data === 'string';
}

function parseDataItemAsString(data) {
    if (dataItemIsString(data)) {
        return {
            name: data
        };
    }
    return undefined;
}

function dataItemIsObject(data) {
    return data &&
        typeof data === 'object' &&
        Object.prototype.hasOwnProperty.call(data, OBJECT_DATA_ITEM_NAME_FIELD) &&
        data[OBJECT_DATA_ITEM_NAME_FIELD];
}

function parseDataItemAsObject(data) {
    if (dataItemIsObject(data)) {
        return {
            ...data,
            name: data[OBJECT_DATA_ITEM_NAME_FIELD],
            value: data.weight || 0
        };
    }
    return undefined;
}

function dataIsDataItem(data) {
    return dataItemIsArray(data) || dataItemIsString(data) || dataItemIsObject(data);
}

function dataIsTreeArray(data) {
    return data && Array.isArray(data) && data.length === 2 && !dataItemIsArray(data);
}

function dataIsTreeObject(data) {
    return data && data.children && Array.isArray(data.children) && data.children.length > 0;
}

function dataIsTree(data) {
    return dataIsTreeObject(data) || dataIsTreeArray(data);
}

function parseDataAsTree(data) {
    if (dataIsTreeObject(data)) {
        const [left, right] = data.children || [];
        return {
            left,
            right
        };
    }
    if (dataIsTreeArray(data)) {
        const [left, right] = data;
        return {
            left,
            right
        };
    }
    return undefined;
}

function parseDataItem(data) {
    return parseDataItemAsArray(data) ||
        parseDataItemAsObject(data) ||
        parseDataItemAsString(data);
}

/**
 * @typedef {Object} HeatmapTreeItem
 * @property {string} name
 * @property {string} [annotation]
 * @property {number} [weight]
 * @property {*[]} [info]
 */

export default class HeatmapBinaryTree {
    /**
     *
     * @param {*[]|Object|string} data
     */
    constructor(data) {
        if (data && Array.isArray(data) && data.length === 1) {
            return new HeatmapBinaryTree(data[0]);
        }
        if (
            data &&
            data.children &&
            Array.isArray(data.children) &&
            data.children.length === 1
        ) {
            return new HeatmapBinaryTree(data.children[0]);
        }
        /**
         *
         * @type {HeatmapBinaryTree|undefined}
         */
        this.left = undefined;
        /**
         *
         * @type {HeatmapBinaryTree|undefined}
         */
        this.right = undefined;
        /**
         *
         * @type {HeatmapTreeItem}
         */
        this.data = undefined;
        this.orders = new Map();
        this.reverseOrders = new Map();
        this.plain = [];
        if (dataIsDataItem(data)) {
            this.data = parseDataItem(data);
        } else if (dataIsTree(data)) {
            const {
                left: leftSubTree,
                right: rightSubTree
            } = parseDataAsTree(data) || {};
            this.left = new HeatmapBinaryTree(leftSubTree);
            this.right = new HeatmapBinaryTree(rightSubTree);
        }
    }

    get isLeaf() {
        return !!this.data && !this.left && !this.right;
    }

    get invalid() {
        return !this.left && !this.right && !this.data;
    }

    getPlainData() {
        if (this.isLeaf) {
            return [this];
        }
        if (this.right && this.left) {
            return [...this.left.getPlainData(), ...this.right.getPlainData()];
        }
        return [];
    }

    prepare() {
        this.buildOrders();
        this.setDepth();
    }

    setDepth() {
        const leftDepth = this.left ? (this.left.setDepth() + 1) : 0;
        const rightDepth = this.right ? (this.right.setDepth() + 1) : 0;
        this.depth = Math.max(leftDepth, rightDepth);
        return this.depth;
    }

    buildIndexRanges() {
        if (!this.isLeaf && this.left && this.right) {
            this.left.buildIndexRanges();
            this.right.buildIndexRanges();
            const {
                indexRange: lIndexRange = {}
            } = this.left;
            const {
                indexRange: rIndexRange = {}
            } = this.right;
            const indexArray = [
                lIndexRange.start || 0,
                lIndexRange.end || 0,
                rIndexRange.start || 0,
                rIndexRange.end || 0
            ];
            this.indexRange = {
                start: Math.min(...indexArray),
                end: Math.max(...indexArray),
                center: ((lIndexRange.center || 0) + (rIndexRange.center || 0)) / 2.0
            };
        }
    }

    buildOrders() {
        this.plain = this.getPlainData();
        this.orders.clear();
        this.reverseOrders.clear();
        this.plain.forEach((item, index) => {
            item.indexRange = {
                start: index,
                end: index,
                center: index
            };
            this.orders.set(item.data.name, index);
            this.reverseOrders.set(index, item.data.name);
        });
        this.buildIndexRanges();
    }

    getOrder(item, valueFn = (o => o)) {
        if (!this.orders.has(valueFn(item))) {
            return undefined;
        }
        return this.orders.get(valueFn(item));
    }

    getItemsOrderInfo(items = [], valueFn = (o => o)) {
        return items.map((item, index) => ({
            item,
            originalOrder: index,
            order: this.getOrder(item, valueFn) || 0
        }))
            .sort((a, b) => a.order - b.order);
    }

    orderItems(items = [], valueFn = (o => o)) {
        const info = this.getItemsOrderInfo(items, valueFn);
        return info.map(orderedItem => orderedItem.item);
    }
}
