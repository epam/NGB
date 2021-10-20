import config from '../config';

/**
 *
 * @param {SubTreeNode} node
 * @param {HeatmapAxis} axis
 * @param {number} [thresholdPx=2]
 * @returns {boolean}
 */
export default function getNodeIsCollapsed(
    node,
    axis,
    thresholdPx = config.nodeCollapsedThresholdPx
) {
    const {start, end} = node.indexRange;
    const range = end - start;
    const pxRange = range * axis.scale.tickSize;
    return pxRange <= thresholdPx;
}
