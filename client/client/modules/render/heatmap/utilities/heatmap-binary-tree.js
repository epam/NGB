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
            value: data.value
        };
    }
    return undefined;
}

function dataIsDataItem(data) {
    return dataItemIsArray(data) || dataItemIsString(data) || dataItemIsObject(data);
}

function dataIsTree(data) {
    return data && Array.isArray(data) && data.length === 2 && !dataItemIsArray(data);
}

function parseDataAsTree(data) {
    if (dataIsTree(data)) {
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
            return [this.data];
        }
        if (this.right && this.left) {
            return [...this.left.getPlainData(), ...this.right.getPlainData()];
        }
        return [];
    }

    buildOrders() {
        this.plain = this.getPlainData();
        this.orders.clear();
        this.reverseOrders.clear();
        this.plain.forEach((item, index) => {
            this.orders.set(item.name, index);
            this.reverseOrders.set(index, item.name);
        });
    }

    getOrder(item) {
        if (!this.orders.has(item)) {
            return undefined;
        }
        return this.orders.get(item);
    }

    getItemsOrderInfo(items = []) {
        return items.map((item, index) => ({
            item,
            originalOrder: index,
            order: this.getOrder(item) || 0
        }))
            .sort((a, b) => a.order - b.order);
    }

    orderItems(items = []) {
        const info = this.getItemsOrderInfo(items);
        return info.map(orderedItem => orderedItem.item);
    }
}
