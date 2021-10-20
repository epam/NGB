const DEFAULT_DEPTH = 8;

/**
 * @typedef {Object} SubTreeNode
 * @property {HeatmapBinaryTree} original
 * @property {{start: number, end: number}} indexRange
 * @property {boolean} isLeaf
 * @property {boolean} hasSubTree
 * @property {number} level
 * @property {SubTreeNode} [left] - left child (within current sub-tree)
 * @property {SubTreeNode} [right] - right child (within current sub-tree)
 * @property {SubTreeNode} [leftSubTree] - left sub-tree child ("next" left sub-tree)
 * @property {SubTreeNode} [rightSubTree] - right sub-tree child ("next" right sub-tree)
 */

/**
 * Splits binary tree by depth - makes "tree of trees".
 * That is a helper data structure to reduce total graphics objects -
 * we'll create a single PIXI.Graphics for a sub-tree of depth instead of
 * creating a PIXI.Graphics for each node.
 * @param {HeatmapBinaryTree} tree
 * @param {number} [depth=2]
 * @returns {SubTreeNode}
 */
export default function splitTree(tree, depth = DEFAULT_DEPTH) {
    /**
     *
     * @param {HeatmapBinaryTree} binaryTree
     * @param remainingDepth
     * @returns {{indexRange: *, isLeaf: boolean}}
     */
    const split = (binaryTree, remainingDepth = depth) => {
        if (binaryTree.isLeaf || !binaryTree.left || !binaryTree.right) {
            return {
                indexRange: binaryTree.indexRange,
                isLeaf: true,
                hasSubTree: false,
                original: binaryTree,
                level: 0
            };
        }
        if (remainingDepth === 0) {
            return {
                indexRange: binaryTree.indexRange,
                isLeaf: true,
                hasSubTree: true,
                leftSubTree: split(binaryTree.left, depth),
                rightSubTree: split(binaryTree.right, depth),
                original: binaryTree,
                level: 0
            };
        }
        return {
            indexRange: binaryTree.indexRange,
            isLeaf: false,
            hasSubTree: false,
            left: split(binaryTree.left, remainingDepth - 1),
            right: split(binaryTree.right, remainingDepth - 1),
            original: binaryTree,
            level: 0
        };
    };
    return split(tree);
}
