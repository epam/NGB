const bitEnabled = (bit, mask) => {
    return (mask & bit) === bit;
};

const readAllowed = (item, extendedMask = false) => {
    if (!item || item.mask === undefined || item.mask === null) {
        return false;
    }
    if (extendedMask) {
        return bitEnabled(1, collapseMask(item.mask));
    }
    return bitEnabled(1, item.mask);
};

const writeAllowed = (item, extendedMask = false) => {
    if (!item || item.mask === undefined || item.mask === null) {
        return false;
    }
    if (extendedMask) {
        return bitEnabled(2, collapseMask(item.mask));
    }
    return bitEnabled(2, item.mask);
};

const readDenied = (item, extendedMask = false) => {
    if (!item || item.mask === undefined || item.mask === null) {
        return false;
    }
    if (extendedMask) {
        return bitEnabled(1 << 1, item.mask);
    }
    return !bitEnabled(1, item.mask);
};

const writeDenied = (item, extendedMask = false) => {
    if (!item || item.mask === undefined || item.mask === null) {
        return false;
    }
    if (extendedMask) {
        return bitEnabled(1 << 3, item.mask);
    }
    return !bitEnabled(2, item.mask);
};

const isOwner = (item, extendedMask = false) => {
    if (!item || item.mask === undefined || item.mask === null) {
        return false;
    }
    if (extendedMask) {
        return bitEnabled(4, collapseMask(item.mask));
    }
    return bitEnabled(4, item.mask);
};

const extendMask = (mask) => {
    return (
        readAllowed({mask}) |
        !readAllowed({mask}) << 1 |
        writeAllowed({mask}) << 2 |
        !writeAllowed({mask}) << 3
    );
};

const collapseMask = (mask) => {
    const readAllowed = (mask & 1) === 1;
    const writeAllowed = (mask & 4) === 4;
    return readAllowed | writeAllowed << 1;
};

const buildExtendedMask = (readAllowed, readDenied, writeAllowed, writeDenied) => {
    const b = (digit, value) => value ? 1 << digit : 0;
    return b(0, readAllowed) | b(1, readDenied) | b(2, writeAllowed) | b(3, writeDenied);
};

export default {
    readAllowed,
    readDenied,
    writeAllowed,
    writeDenied,
    isOwner,
    buildExtendedMask
};
