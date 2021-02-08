const MIN_ALIGNMENT_SPACING = 3;

interface Range {
    start: number;
    end: number;
}

class UnsortedReadsColumnsCollection {

    columns: Array;
    range: Range;
    _shouldRebuildPointers;

    constructor(reads) {
        let min = Infinity;
        let max = -Infinity;
        this._shouldRebuildPointers = true;
        for (let i = 0; i < reads.length; i++) {
            if (min > reads[i].startIndex) {
                min = reads[i].startIndex;
            }
            if (max < reads[i].startIndex) {
                max = reads[i].startIndex;
            }
        }
        if (reads && reads.length > 0) {
            this.range = {
                end: max,
                start: min
            };
            this.columns = new Array(this.range.end - this.range.start);
            for (let i = 0; i < reads.length; i++) {
                const read = reads[i];
                const index = read.startIndex - this.range.start;
                let column: UnsortedReadsColumn = this.columns[index];
                if (!column) {
                    column = new UnsortedReadsColumn();
                    this.columns[index] = column;
                }
                column.pushRead(read);
            }
        }
        else {
            this.range = {end: 0, start: 0};
            this.columns = new Array(0);
        }
        this.createPointers();
    }

    createPointers() {
        let lastNotEmptyColumn : UnsortedReadsColumn = null;
        for (let i = this.columns.length - 1; i >= 0; i--) {
            let column = this.columns[i];
            if (!column) {
                column = new UnsortedReadsColumn();
                this.columns[i] = column;
            }
            if (lastNotEmptyColumn) {
                lastNotEmptyColumn._prevs.push(this.columns[i]);
            }
            this.columns[i]._next = lastNotEmptyColumn;
            if (!this.columns[i].isEmpty) {
                lastNotEmptyColumn = this.columns[i];
            }
        }
    }

    getNextRead(position) {
        const index = position - this.range.start;
        if (index >= 0 && index < this.columns.length) {
            let column:UnsortedReadsColumn = this.columns[index];
            while (column && column.isEmpty) {
                column = column._next;
            }
            if (column) {
                return column.popRead();
            }
        }
        return null;
    }

    clear() {
        for (let i = 0; i < this.columns.length; i++) {
            this.columns[i].clear();
        }
        this.columns = null;
    }

}

class UnsortedReadsColumn {

    reads: Array;
    index: number;
    _next: UnsortedReadsColumn = null;
    _prevs: UnsortedReadsColumn[] = [];

    constructor() {
        this.index = 0;
        this.reads = [];
    }

    get isEmpty() {
        return this.index >= this.reads.length;
    }

    popRead() {
        if (this.isEmpty)
            return null;
        const read = this.reads[this.index];
        this.index++;
        if (this.isEmpty) {
            for (let i = 0; i < this._prevs.length; i++) {
                this._prevs[i]._next = this._next;
            }
        }
        return read;
    }

    pushRead(read) {
        this.reads.push(read);
    }

    clear() {
        this.reads = null;
        this._prevs = null;
        this._next = null;
    }
}

export function layoutReads(reads) {
    let collection = new UnsortedReadsColumnsCollection(reads);
    const totalCount = reads.length;
    let allocatedCount = 0;
    const range = collection.range;
    let currentRowNum = 0;

    const result = [];

    while (allocatedCount < totalCount) {
        let position = range.start;
        let endOfLineOrNoReadFound = false;
        while (!endOfLineOrNoReadFound) {
            const read = collection.getNextRead(position);
            if (read) {
                read.lineIndex = currentRowNum;
                read.initialLineIndex = currentRowNum;
                allocatedCount++;
                result.push(read);
                position = read.endIndex + MIN_ALIGNMENT_SPACING;
            }
            endOfLineOrNoReadFound = !read || position > range.end;
        }
        currentRowNum++;
    }
    collection.clear();
    collection = null;
    reads = null;
    return result;
}