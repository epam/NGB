export function findMenuItem(menu, name) {
    for (const item of menu) {
        if (item.name === name) {
            return item;
        }
        if (item.fields) {
            const found = findMenuItem(item.fields, name);
            if (found) {
                return found;
            }
        }
    }
}