/**
 * @typedef {Object} Point2D
 * @property {number} x
 * @property {number} y
 */
/**
 * @typedef {{start: Point2D, end: Point2D}|Point2D} Vector
 */

/**
 * Creates vector from two points
 * @param {Point2D} from
 * @param {Point2D} to
 * @returns {Vector}
 */
function makeVector(from, to) {
    const {
        x: xa = 0,
        y: ya = 0
    } = from || {};
    const {
        x: xb = 0,
        y: yb = 0
    } = to || {};
    return {
        x: xb - xa,
        y: yb - ya
    };
}

/**
 * Gets vector length
 * @param {Vector} vector
 * @returns {number}
 */
function getVectorLength(vector) {
    const {
        start = {},
        end = {},
        x,
        y
    } = vector || {};
    const {
        x: xs = 0,
        y: ys = 0
    } = start;
    const {
        x: xe = x || 0,
        y: ye = y || 0
    } = end;
    return Math.sqrt((xe - xs) ** 2 + (ye - ys) ** 2);
}

/**
 * Normalizes vector
 * @param {Vector} vector
 */
function normalizeVector(vector) {
    const {
        start = {},
        end = {},
        x,
        y
    } = vector || {};
    const {
        x: xs = 0,
        y: ys = 0
    } = start;
    const {
        x: xe = x || 0,
        y: ye = y || 0
    } = end;
    const length = getVectorLength(vector);
    const result = {
        x: length > 0 ? (xe - xs) / length : 0,
        y: length > 0 ? (ye - ys) / length : 0
    };
    return {
        start: {x: 0, y: 0},
        end: result,
        ...result
    };
}

/**
 * Gets the angle (in radians) for vector
 * @param {Vector} vector
 */
function getVectorAngle(vector) {
    const {
        start = {},
        end = {},
        x,
        y
    } = vector || {};
    const {
        x: xs = 0,
        y: ys = 0
    } = start;
    const {
        x: xe = x || 0,
        y: ye = y || 0
    } = end;
    return Math.atan2(ye - ys, xe - xs);
}

/**
 * Makes radians value to fit range [-Math.PI, Math.PI]
 * @param {number} [diff = 0]
 * @returns {number}
 */
function fitRadians (diff = 0) {
    let corrected = diff;
    while (corrected > Math.PI) {
        corrected -= 2.0 * Math.PI;
    }
    while (corrected < -Math.PI) {
        corrected += 2.0 * Math.PI;
    }
    return corrected;
}

/**
 * Gets angle in radians between two vectors
 * @param {Vector} a
 * @param {Vector} b
 * @returns {number}
 */
function getAngleBetweenVectors(a, b) {
    const correctDiff = (diff) => Math.abs(fitRadians(diff));
    const aAngle = getVectorAngle(a);
    const bAngle = getVectorAngle(b);
    return correctDiff(Math.abs(aAngle - bAngle));
}

/**
 * @typedef {Object} Section
 * @property {Point2D} a
 * @property {Point2D} b
 */

/**
 * @typedef {Object} PointProjectionInfo
 * @property {boolean} fitsSection
 * @property {number} distance
 * @property {Point2D} projection
 * @property {number} projectionDistanceFromStart
 */

/**
 * Gets the point projection on the section
 * @param {{x: number, y: number}} point
 * @param {Section} section
 * @returns {PointProjectionInfo}
 */
export function getPointProjectionInfo(point, section) {
    const {
        a = {},
        b = {}
    } = section;
    const {
        x: xa = 0,
        y: ya = 0
    } = a;
    const sectionStartVector = normalizeVector(makeVector(a, b));
    const sectionEndVector = normalizeVector(makeVector(b, a));
    const startToPointVector = makeVector(a, point);
    const endToPointVector = makeVector(b, point);
    const startToPointAngle = getAngleBetweenVectors(startToPointVector, sectionStartVector);
    const endToPointAngle = getAngleBetweenVectors(endToPointVector, sectionEndVector);
    const valueOnScaleFromStart = Math.cos(startToPointAngle) * getVectorLength(startToPointVector);
    const valueOnScaleFromEnd = Math.cos(endToPointAngle) * getVectorLength(endToPointVector);
    const projection = Math.sin(startToPointAngle) * getVectorLength(startToPointVector);
    const pointOnSection = {
        x: xa + valueOnScaleFromStart * sectionStartVector.x,
        y: ya + valueOnScaleFromStart * sectionEndVector.y
    };
    return {
        fitsSection: valueOnScaleFromStart >= 0 && valueOnScaleFromEnd >= 0,
        distance: projection,
        projection: pointOnSection,
        projectionDistanceFromStart: valueOnScaleFromStart
    };
}

/**
 * Checks if point fits section (i.e. inside section with appropriate distance)
 * @param {Point2D} point
 * @param {Section} section
 * @param {number} [margin = 0] - allowable distance
 * @returns {boolean}
 */
export function pointFitsSection(point, section, margin = 0) {
    const {
        fitsSection,
        distance
    } = getPointProjectionInfo(point, section);
    return fitsSection && Math.abs(distance) <= margin;
}

/**
 * Creates section by direction vector and start & end values on it
 * @param {Vector} direction
 * @param {number} [start = 0]
 * @param {number} [end = 0]
 * @returns {Section}
 */
export function makeSection(direction, start = 0, end= 0) {
    const {
        x: dx = 0,
        y: dy = 0
    } = normalizeVector(direction);
    return {
        a: {
            x: start * dx,
            y: start * dy
        },
        b: {
            x: end * dx,
            y: end * dy
        }
    };
}

/**
 * Makes orthogonal vector
 * @param {Vector} vector
 * @returns {Vector}
 */
export function orthogonalVector(vector) {
    const normalized = normalizeVector(vector);
    return normalizeVector({
        x: normalized.y,
        y: -normalized.x
    });
}

/**
 * Moves point by vector & distance
 * @param {Point2D} point
 * @param {Vector} vector
 * @param {number} distance
 * @returns {Point2D}
 */
export function movePoint(point, vector, distance = 0) {
    const {
        x = 0,
        y = 0
    } = point || {};
    const normalized = normalizeVector(vector);
    const length = getVectorLength(vector);
    return {
        x: x + (distance || length) * normalized.x,
        y: y + (distance || length) * normalized.y
    };
}

/**
 * Moves section by vector & distance
 * @param {Section} section
 * @param {Vector} vector
 * @param {number} distance
 * @returns {Section}
 */
export function moveSection(section, vector, distance) {
    const {
        a,
        b
    } = section;
    return {
        a: movePoint(a, vector, distance),
        b: movePoint(b, vector, distance)
    };
}

/**
 * Rotates vector anticlockwise by angle
 * @param {Vector} vector
 * @param {number} radians
 * @returns {Vector}
 */
export function rotate(vector, radians) {
    const {
        start = {},
        end = {},
        x,
        y
    } = vector || {};
    const {
        x: xs = 0,
        y: ys = 0
    } = start;
    const {
        x: xe = x,
        y: ye = y
    } = end;
    const vx = xe - xs;
    const vy = ye - ys;
    const rx = Math.cos(radians) * vx - Math.sin(radians) * vy;
    const ry = Math.sin(radians) * vx + Math.cos(radians) * vy;
    return {
        start: {x: xs, y: ys},
        end: {x: rx, y: ry},
        x: rx - xs,
        y: ry - ys
    };
}

/**
 * @typedef {Object} Rectangle
 * @property {number} x
 * @property {number} y
 * @property {number} width
 * @property {number} height
 */

/**
 * Rotates rectangle anticlockwise by angle
 * @param {Rectangle} rect
 * @param {number} radians
 * @returns {{a: Point2D, b: Point2D, c: Point2D, d: Point2D}}
 */
export function rotateRectangle(rect, radians) {
    if (!rect) {
        return rect;
    }
    const {
        x,
        y,
        width,
        height
    } = rect;
    const a = {x, y};
    const b = {x: x + width, y};
    const c = {x: x + width, y: y + height};
    const d = {x, y: y + height};
    return {
        a: rotate(a, radians),
        b: rotate(b, radians),
        c: rotate(c, radians),
        d: rotate(d, radians)
    };
}

/**
 * Rotates rectangle anticlockwise by angle and returns it's bounds (rectangle)
 * @param {Rectangle} rect
 * @param {number} radians
 * @returns {Rectangle}
 */
export function getRotatedRectangleBounds(rect, radians) {
    if (!rect) {
        return rect;
    }
    const {a, b, c, d} = rotateRectangle(rect, radians);
    const x1 = Math.min(a.x, b.x, c.x, d.x);
    const y1 = Math.min(a.y, b.y, c.y, d.y);
    const x2 = Math.max(a.x, b.x, c.x, d.x);
    const y2 = Math.max(a.y, b.y, c.y, d.y);
    return {
        x: x1,
        y: y1,
        width: x2 - x1,
        height: y2 - y1
    };
}

/**
 *
 * @param {Rectangle} rect
 * @param {number} [margin=0]
 */
export function extendRectangleByMargin(rect, margin = 0) {
    if (!rect || !margin || margin <= 0) {
        return rect;
    }
    return {
        x: rect.x - margin,
        y: rect.y - margin,
        width: rect.width + 2.0 * margin,
        height: rect.height + 2.0 * margin
    };
}
