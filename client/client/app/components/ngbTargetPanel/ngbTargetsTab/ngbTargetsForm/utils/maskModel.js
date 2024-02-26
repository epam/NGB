const bitEnabled = (bit, mask) => {
    return (mask & bit) === bit;
};

// 0 - nothing is allowed
// 1 - read is allowed
// 2 - write is allowed
// 3 - read and write are allowed
// 7 - owner

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
    return bitEnabled(2, item.mask);
};

const writeDenied = (item) => {
    if (!item || item.mask === undefined || item.mask === null) {
        return false;
    }
    return !bitEnabled(2, item.mask);
};

const isOwner = (item) => {
    if (!item || item.mask === undefined || item.mask === null) {
        return false;
    }
    return bitEnabled(3, item.mask);
};

export default {
    readAllowed,
    readDenied,
    writeAllowed,
    writeDenied,
    isOwner,
};