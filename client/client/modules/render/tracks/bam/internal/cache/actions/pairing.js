export function pair(read, pairedRead) {
    if (read && pairedRead) {
        if (read.rnext === pairedRead.rnext) {
            const startIndex = Math.min(read.startIndex, pairedRead.startIndex);
            const endIndex = Math.max(read.endIndex, pairedRead.endIndex);
            if (read.startIndex < pairedRead.startIndex) {
                read.leftPair = Object.assign({}, read);
                read.rightPair = pairedRead;
            } else {
                read.rightPair = Object.assign({}, read);
                read.leftPair = pairedRead;
            }
            read.rightPair.isRightInPair = true;
            read.isPairedReads = true;
            read.startIndex = startIndex;
            read.endIndex = endIndex;
        }
    }
}

export function fakePair(read, coverageRange) {
    if (read) {
        if (read.pnext >= coverageRange.startIndex && read.pnext <= coverageRange.endIndex) {
            // Pair read should be already mapped, because it's position fits boundaries, but possibly was downsampled.
            // We shouldn't make fake pair read
            return;
        }
        const startIndex = Math.min(read.startIndex, read.pnext);
        const endIndex = Math.max(read.endIndex, read.pnext);
        if (endIndex - startIndex > 7000)
            return;
        if (read.startIndex < read.pnext) {
            read.leftPair = Object.assign({}, read);
            read.rightPair = {isUnknown: true, startIndex: read.pnext, endIndex: read.pnext + 1};
        } else {
            read.rightPair = Object.assign({}, read);
            read.leftPair = {isUnknown: true, startIndex: read.pnext - 1, endIndex: read.pnext};
        }
        read.rightPair.isRightInPair = true;
        read.isPairedReads = true;
        read.startIndex = startIndex;
        read.endIndex = endIndex;
    }
}