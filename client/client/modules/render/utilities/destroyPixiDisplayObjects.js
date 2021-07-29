export default function destroyPixiDisplayObjects(itemsToDestroy = [], destroyChildren) {
    for (let i = itemsToDestroy.length; i > 0; i--) {
        if (itemsToDestroy[i] && !itemsToDestroy[i]._destroyed) {
            itemsToDestroy[i].destroy(destroyChildren);
        }
    }
}
