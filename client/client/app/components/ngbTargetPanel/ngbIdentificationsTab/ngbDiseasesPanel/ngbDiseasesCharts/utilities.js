export function circlePath(radius) {
    if (Number.isNaN(Number(radius))) {
        return '';
    }
    return `M 0,${radius} a ${radius},${radius} 0 1,1 0,-${2 * radius} a ${radius},${radius} 0 1,1 0,${2 * radius}`;
}

export function rectanglePath(width = 5.0, height = width) {
    if (Number.isNaN(Number(width)) || Number.isNaN(Number(height))) {
        return '';
    }
    return `M-${width / 2.0} -${height / 2.0} L${width / 2.0} -${height / 2.0} L${width / 2.0} ${height / 2.0} L-${width / 2.0} ${height / 2.0} L-${width / 2.0} -${height / 2.0}`;
}

export function getCurve(x1, y1, x2, y2, nodeImageSize = 0) {
    if (
        Number.isNaN(Number(x1)) ||
        Number.isNaN(Number(y1)) ||
        Number.isNaN(Number(x2)) ||
        Number.isNaN(Number(y2))
    ) {
        return '';
    }
    const dx = (x2 - x1) / 2.0;
    const s = Math.sign(dx);
    return `M${x2 - s * nodeImageSize} ${y2} C${x2 - dx} ${y2} ${x1 + dx} ${y1} ${x1 + s * nodeImageSize} ${y1}`;
}

export function getTreeDown(node, nodes = []) {
    if (!node) {
        return [];
    }
    const {
        id,
        links = []
    } = node || {};
    const linkedNodes = links
        .map((link) => nodes.find((o) => o.id === link))
        .filter(Boolean);
    return [...new Set([
        id,
        ...links,
        ...linkedNodes
            .map((link) => getTreeDown(link, nodes))
            .reduce((r, c) => ([...r, ...c]), [])
    ])];
}

export function getTreeUp(node, nodes = []) {
    if (!node) {
        return [];
    }
    const {
        id,
    } = node || {};
    const parentNodes = nodes
        .filter((p) => p && (p.links || []).includes(id));
    return [...new Set([
        id,
        ...parentNodes
            .map((link) => getTreeUp(link, nodes))
            .reduce((r, c) => ([...r, ...c]), [])
    ])];
}
