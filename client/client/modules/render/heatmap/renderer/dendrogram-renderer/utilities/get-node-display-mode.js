/**
 * @enum {string}
 */
const SubTreeNodeDisplayModes = {
    /**
     * Node range is out of the viewport
     */
    outsideViewport: 'outside',
    /**
     * Node range is withing the viewport
     */
    withinViewport: 'within',
    /**
     * Node range intersects viewport border
     */
    intersectsViewport: 'intersects'
};

export {SubTreeNodeDisplayModes};

/**
 *
 * @param {SubTreeNode} node
 * @param {HeatmapAxis} axis
 * @returns {SubTreeNodeDisplayModes}
 */
export function getNodeDisplayMode(node, axis) {
    let {start, end} = node.indexRange;
    start += 0.5;
    end += 0.5;
    if (end <= axis.start || start >= axis.end) {
        return SubTreeNodeDisplayModes.outsideViewport;
    }
    if (start >= axis.start && end <= axis.end) {
        return SubTreeNodeDisplayModes.withinViewport;
    }
    return SubTreeNodeDisplayModes.intersectsViewport;
}
