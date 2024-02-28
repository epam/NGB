const bitEnabled = (bit, mask) => {
    return (mask & bit) === bit;
};

// 0 - nothing is allowed
// 1 - read is allowed
// 2 - read is denied
// 4 - write is allowed
// 5 - read and write are allowed
// 6 - write is allowed, read is denied
// 7 - owner
// 8 - write is denied
// 9 - read is allowed, write is denied
// 10 - read and write are denied

const readAllowed = (item) => {
    if (!item || item.mask === undefined || item.mask === null) {
        return false;
    }
    return bitEnabled(1, item.mask);
};

const readDenied = (item) => {
    if (!item || item.mask === undefined || item.mask === null) {
        return false;
    }
    return !bitEnabled(1, item.mask);
};

const writeAllowed = (item) => {
    if (!item || item.mask === undefined || item.mask === null) {
        return false;
    }
    return bitEnabled(4, item.mask);
};

const writeDenied = (item) => {
    if (!item || item.mask === undefined || item.mask === null) {
        return false;
    }
    return !bitEnabled(4, item.mask);
};

const isOwner = (item) => {
    if (!item || item.mask === undefined || item.mask === null) {
        return false;
    }
    return bitEnabled(7, item.mask);
};

export default {
    readAllowed,
    readDenied,
    writeAllowed,
    writeDenied,
    isOwner,
};