export default async function destroyPixiDisplayObjects(itemsToDestroy = [], destroyChildren) {
    const remove = (itemIndex) => new Promise((resolve) => {
        if (itemIndex < 0) {
            return Promise.resolve();
        }
        if (itemsToDestroy[itemIndex] && !itemsToDestroy[itemIndex]._destroyed) {
            itemsToDestroy[itemIndex].destroy(destroyChildren);
            itemsToDestroy[itemIndex] = null;
        }
        setTimeout(async () => {
            await remove(itemIndex - 1);
            resolve();
        }, Math.random() * 1000);
    });
    await remove(itemsToDestroy.length - 1);
}
