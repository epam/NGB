function getKey (index) {
    return index & 0xf;
}

function shiftIndex (index) {
    return (index >> 4);
}

function isLeaf (index) {
    return index <= 0xf;
}

function recoverIndex(parent, child) {
    return (child << 4) | (parent & 0xf);
}

class Trie {
    /**
     * @param {any} data
     */
    constructor(data) {
        /**
         * Node data
         * @type {any}
         */
        this.data = data;
        /**
         * Child nodes
         * @type {Map<number, Trie>}
         */
        this.children = new Map();
        this.count = 0;
    }

    get(index) {
        const leaf = isLeaf(index);
        const childIndex = getKey(index);
        const child = this.children.get(childIndex);
        if (child) {
            return leaf ? child : child.get(shiftIndex(index));
        }
        return undefined;
    }
    set(index, data) {
        const leaf = isLeaf(index);
        const childIndex = getKey(index);
        if (!this.children.has(childIndex)) {
            this.children.set(childIndex, new Trie());
        }
        const child = this.children.get(childIndex);
        if (leaf) {
            child.setData(data);
            return child;
        }
        const shift = shiftIndex(index);
        return child.set(shift, data);
    }

    assign(index, data) {
        if (typeof data !== 'object' && typeof data !== 'function') {
            return this.set(index, data);
        }
        const leaf = isLeaf(index);
        const childIndex = getKey(index);
        if (!this.children.has(childIndex)) {
            this.children.set(childIndex, new Trie());
        }
        const child = this.children.get(childIndex);
        if (leaf) {
            if (typeof data === 'function') {
                child.setData(data(child.data));
            } else {
                child.setData(Object.assign({}, child.data || {}, data || {}));
            }
            return child;
        }
        const shift = shiftIndex(index);
        return child.assign(shift, data);
    }

    setData(data) {
        this.data = data;
    }

    getData(index) {
        const node = this.get(index);
        if (node) {
            return node.data;
        }
        return undefined;
    }

    /**
     *
     * @param index {number}
     * @param defaultValue {any}
     * @return {Trie}
     */
    ensure(index, defaultValue = undefined) {
        const leaf = isLeaf(index);
        const childIndex = getKey(index);
        const nodeExists = this.children.has(childIndex);
        if (!nodeExists) {
            this.children.set(childIndex, new Trie());
        }
        const child = this.children.get(childIndex);
        if (leaf) {
            if (!child.data) {
                child.setData(defaultValue);
            }
            return child;
        }
        const shift = shiftIndex(index);
        return child.ensure(shift, defaultValue);
    }

    has(index) {
        return !!this.getData(index);
    }

    /**
     * Returns all items with index
     * @returns {Generator<{data: *, index: number}, void, *>}
     */
    *leavesWithIndex() {
        for (const [parentIndex, child] of this.children.entries()) {
            if (child.data !== undefined) {
                yield {index: parentIndex, data: child.data};
            }
            for (const leaf of child.leavesWithIndex()) {
                const {
                    data,
                    index
                } = leaf;
                yield {index: recoverIndex(parentIndex, index), data};
            }
        }
    }

    calculateSize() {
        let count = this.data ? 1 : 0;
        for (const child of this.children.values()) {
            child.calculateSize();
            count += child.count;
        }
        this.count = count;
    }
}

export default Trie;
