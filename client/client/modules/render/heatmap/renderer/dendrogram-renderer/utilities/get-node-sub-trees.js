/**
 *
 * @param {SubTreeNode} node
 * @return {SubTreeNode[]}
 */
export default function getNodeSubTrees(node) {
    if (node.hasSubTree) {
        return [node.leftSubTree, node.rightSubTree].filter(Boolean);
    }
    if (node.left && node.right) {
        return [
            ...getNodeSubTrees(node.left),
            ...getNodeSubTrees(node.right)
        ];
    }
    return [];
}
