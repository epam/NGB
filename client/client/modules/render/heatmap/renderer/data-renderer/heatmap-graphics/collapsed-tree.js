const TOP = 0;
const BOTTOM = 1;
const LEFT = 0;
const RIGHT = 1;

const names = ['topLeftChild', 'topRightChild', 'bottomLeftChild', 'bottomRightChild'];
const indexOf = (horizontal, vertical) => vertical * 2 + horizontal;
const getIndex = (horizontal, vertical) => names[indexOf(horizontal, vertical)];

/**
 * @typedef {Object} HeatmapDataItemWithSize
 * @property {number} column
 * @property {number} row
 * @property {number} [width=1]
 * @property {number} [height=1]
 * @property {number|string} value
 */

export default class CollapsedTree {
    constructor(column, row, width, height) {
        this.column = column;
        this.row = row;
        this.width = width;
        this.height = height;
        this.columnCenter = this.column + Math.ceil(width / 2.0);
        this.rowCenter = this.row + Math.ceil(height / 2.0);
        this.isLeaf = this.width === 1 && this.height === 1;
        this.data = undefined;
        this.count = 0;
        this[getIndex(LEFT, TOP)] = undefined;
        this[getIndex(RIGHT, TOP)] = undefined;
        this[getIndex(LEFT, BOTTOM)] = undefined;
        this[getIndex(RIGHT, BOTTOM)] = undefined;
    }

    getValue() {
        if (this.isLeaf && this.data !== undefined) {
            return {
                column: this.column,
                row: this.row,
                width: this.width,
                height: this.height,
                value: this.data
            };
        }
        return undefined;
    }

    /**
     *
     * @param {HeatmapDataItem} item
     * @returns {HeatmapDataItemWithSize|undefined}
     */
    append(item) {
        if (!item) {
            return undefined;
        }
        const {
            column,
            row
        } = item;
        if (this.width === 1 && this.height === 1) {
            this.data = item.value;
            this.count = 1;
            return this.getValue();
        } else {
            const horizontal = column < this.columnCenter ? LEFT : RIGHT;
            const vertical = row < this.rowCenter ? TOP : BOTTOM;
            const childIndex = getIndex(horizontal, vertical);
            if (!this[childIndex]) {
                const w = Math.abs(this.column + (horizontal === LEFT ? 0 : this.width) - this.columnCenter);
                const h = Math.abs(this.row + (vertical === TOP ? 0 : this.height) - this.rowCenter);
                this[childIndex] = new CollapsedTree(
                    horizontal === LEFT ? this.column : this.columnCenter,
                    vertical === TOP ? this.row : this.rowCenter,
                    w,
                    h
                );
            }
            const child = this[childIndex];
            const result = child.append(item);
            if (child.isLeaf) {
                this.join();
            }
            this.calculateSize();
            return result;
        }
    }

    hasValues() {
        return this.count > 0;
    }

    /**
     *
     * @returns {Generator<HeatmapDataItemWithSize, void, *>}
     */
    *values() {
        if (this.isLeaf && this.data !== undefined) {
            yield this.getValue();
        } else {
            const topLeft = this.topLeft;
            const bottomLeft = this.bottomLeft;
            const topRight = this.topRight;
            const bottomRight = this.bottomRight;
            const joinedAtTop = topLeft && this.horizontalValuesAreTheSame(TOP);
            const joinedAtBottom = bottomLeft && this.horizontalValuesAreTheSame(BOTTOM);
            const joinedAtLeft = topLeft && this.verticalValuesAreTheSame(LEFT);
            const joinedAtRight = topRight && this.verticalValuesAreTheSame(RIGHT);
            if (joinedAtTop) {
                yield {
                    column: this.column,
                    row: topLeft.row,
                    width: this.width,
                    height: topLeft.height,
                    value: topLeft.data
                };
            }
            if (joinedAtBottom) {
                yield {
                    column: this.column,
                    row: bottomLeft.row,
                    width: this.width,
                    height: bottomLeft.height,
                    value: bottomLeft.data
                };
            }
            if (joinedAtLeft) {
                yield {
                    column: topLeft.column,
                    row: this.row,
                    width: topLeft.width,
                    height: this.height,
                    value: topLeft.data
                };
            }
            if (joinedAtRight) {
                yield {
                    column: topRight.column,
                    row: this.row,
                    width: topRight.width,
                    height: this.height,
                    value: topRight.data
                };
            }
            if (topLeft && !joinedAtTop && !joinedAtLeft) {
                yield* topLeft.values();
            }
            if (topRight && !joinedAtTop && !joinedAtRight) {
                yield* topRight.values();
            }
            if (bottomLeft && !joinedAtBottom && !joinedAtLeft) {
                yield* bottomLeft.values();
            }
            if (bottomRight && !joinedAtBottom && !joinedAtRight) {
                yield* bottomRight.values();
            }
        }
    }

    getChild(horizontal, vertical) {
        const childIndex = getIndex(horizontal, vertical);
        return this[childIndex];
    }

    get topLeft() {
        return this.getChild(LEFT, TOP);
    }

    get topRight() {
        return this.getChild(RIGHT, TOP);
    }

    get bottomLeft() {
        return this.getChild(LEFT, BOTTOM);
    }

    get bottomRight() {
        return this.getChild(RIGHT, BOTTOM);
    }

    horizontalValuesAreTheSame(vertical) {
        if (this.width === 1) {
            return !!this.getChild(LEFT, vertical);
        }
        const left = this.getChild(LEFT, vertical);
        const right = this.getChild(RIGHT, vertical);
        return left && right &&
            left.isLeaf && right.isLeaf &&
            left.data !== undefined && right.data !== undefined &&
            left.data === right.data;
    }

    verticalValuesAreTheSame(horizontal) {
        if (this.height === 1) {
            return !!this.getChild(horizontal, TOP);
        }
        const top = this.getChild(horizontal, TOP);
        const bottom = this.getChild(horizontal, BOTTOM);
        return top && bottom &&
            top.isLeaf && bottom.isLeaf &&
            top.data !== undefined && bottom.data !== undefined &&
            top.data === bottom.data;
    }

    holdsSingleValue() {
        if (this.width === 1 && this.height === 1) {
            return true;
        }
        const topLeft = this.topLeft;
        const bottomLeft = this.bottomLeft;
        const topRight = this.topRight;
        const joinedAtTop = topLeft && this.horizontalValuesAreTheSame(TOP);
        const joinedAtBottom = bottomLeft && this.horizontalValuesAreTheSame(BOTTOM);
        const joinedAtLeft = topLeft && this.verticalValuesAreTheSame(LEFT);
        const joinedAtRight = topRight && this.verticalValuesAreTheSame(RIGHT);
        return (joinedAtTop && joinedAtBottom && topLeft.data === bottomLeft.data) ||
            (joinedAtLeft && joinedAtRight && topLeft.data === topRight.data);
    }

    join() {
        if (this.width === 1 && this.height === 1) {
            this.isLeaf = true;
            return;
        }
        this.isLeaf = this.holdsSingleValue();
        if (this.isLeaf) {
            this.data = this.topLeft.data;
        } else {
            this.data = undefined;
        }
        this.calculateSize();
    }

    calculateSize() {
        if (this.isLeaf) {
            this.count = 1;
        } else {
            const empty = {count: 0};
            const topLeft = this.topLeft || empty;
            const topRight = this.topRight || empty;
            const bottomLeft = this.bottomLeft || empty;
            const bottomRight = this.bottomRight || empty;
            this.count = topLeft.count +
                topRight.count +
                bottomLeft.count +
                bottomRight.count;
        }
    }
}
