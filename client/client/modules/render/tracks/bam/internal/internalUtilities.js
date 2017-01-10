export function dumpPart({type, startIndex, length, ...rest}) {
    return [type, startIndex, length, rest];
}

export function undumpPart([type, startIndex, length, rest]) {
    return {type, startIndex, length, endIndex: startIndex + length, ...rest};
}

export function dumpInfo(block, spec) {
    const isPaired = (block.flagMask & 0x1) !== 0;
    return [
        block.name,
        `${block.startIndex}-${block.endIndex}`,
        block.cigarString,
        (block.flagMask & 0x4) === 0,
        block.mappingQuality,
        !isPaired ? NaN : (block.flagMask & 0x8) === 0,
        !isPaired ? NaN : `${block.rnext}:${block.pnext}`,
        !isPaired ? NaN : block.tlen,
        !isPaired ? NaN :
            ((block.flagMask & 0x40) !== 0
                ? true
                : ((block.flagMask & 0x80) !== 0 ? false : undefined)),
        !isPaired ? NaN : spec.pair
    ];
}

export function undumpInfo(cache) {
    return [
        ['Read name', cache[0]],
        // ['Sample', cache.shift()],
        ['Location', cache[1]],
        ['Cigar', cache[2]],
        ['Mapped', cache[3]],
        ['Mapping quality', cache[4]],
        ['Mate is mapped', cache[5]],
        ['Mate start', cache[6]],
        ['Insert size', cache[7]],
        ['First in pair', cache[8]],
        ['Pair orientation', cache[9]],
    ]
        .filter(r => !Number.isNaN(r[1]));
}