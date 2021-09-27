/**
 * Checks if range [a, b] is (-Infinity, Infinity)
 * @param {number} a
 * @param {number} b
 * @returns {boolean}
 */
function isAllValuesRange(a, b) {
    return !Number.isFinite(a) && !Number.isFinite(b) && (a * b < 0);
}

/**
 * Checks if range [a, b] is (-Infinity, x) or (x, Infinity)
 * @param {number} a
 * @param {number} b
 * @returns {boolean}
 */
function isOpenRange(a, b) {
    return !isAllValuesRange(a, b) && (!Number.isFinite(a) || !Number.isFinite(b));
}

/**
 * Checks if range [a, b] is (-Infinity, b]
 * @param {number} a
 * @param {number} b
 * @returns {boolean}
 */
function isLeftOpenRange(a, b) {
    const start = Math.min(a, b);
    const end = Math.max(a, b);
    return start === -Infinity && Number.isFinite(end);
}

/**
 * Checks if range [a, b] is [a, Infinity)
 * @param {number} a
 * @param {number} b
 * @returns {boolean}
 */
function isRightOpenRange(a, b) {
    const start = Math.min(a, b);
    const end = Math.max(a, b);
    return end === Infinity && Number.isFinite(start);
}

/**
 * Returns function that tests if X is inside range [a1, a2]
 * @param {number} a1
 * @param {number} a2
 * @param {number} [margin=0]
 * @returns {function(number): boolean}
 */
function getRangeTestFn(a1, a2, margin = 0) {
    if (isLeftOpenRange(a1, a2)) {
        return x => x + margin < Math.max(a1, a2);
    }
    if (isRightOpenRange(a1, a2)) {
        return x => x - margin > Math.min(a1, a2);
    }
    return x => (x - margin) > Math.min(a1, a2) && (x + margin) < Math.max(a1, a2);
}

/**
 * Checks if infinite/opened ranges [a1, a2] and [b1, b2] overlaps.
 * * "infinite" range is a range (-Infinity, Infinity)
 * * "opened" range is a range (-Infinity, a] or [a, Infinity)
 * @param {number} a1
 * @param {number} a2
 * @param {number} b1
 * @param {number} b2
 * @param {number} [margin=0]
 * @returns {boolean}
 */
function checkOpenedRangesOverlaps(a1, a2, b1, b2, margin = 0) {
    if (isOpenRange(a1, a2) || isOpenRange(b1, b2)) {
        const checkA = getRangeTestFn(a1, a2, margin);
        const checkB = getRangeTestFn(b1, b2, margin);
        return checkA(b1) || checkA(b2) || checkB(a1) || checkB(a2);
    }
    return false;
}

export default function linearDimensionsConflict(a1, a2, b1, b2, margin = 0) {
    const o1p1 = a1;
    const o1p2 = a2;
    const o2p1 = b1;
    const o2p2 = b2;
    if (isAllValuesRange(a1, a2) || isAllValuesRange(b1, b2)) {
        return true;
    }
    if (isOpenRange(a1, a2) || isOpenRange(b1, b2)) {
        return checkOpenedRangesOverlaps(a1, a2, b1, b2);
    }
    const o1size = Math.abs(o1p2 - o1p1);
    const o2size = Math.abs(o2p2 - o2p1);
    const oSize = Math.max(o1p1, o1p2, o2p1, o2p2) - Math.min(o1p1, o1p2, o2p1, o2p2);
    if (!Number.isFinite(o1size + o2size + margin) || !Number.isFinite(oSize)) {
        return true;
    }
    return o1size + o2size + margin > oSize;
}
