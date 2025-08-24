export function deepSearch(obj, term, fieldsToIgnore = []) {
    let result = false;
    for (const key in obj) {
        if (fieldsToIgnore.indexOf(key) > -1) continue;
        if (!obj.hasOwnProperty(key) || !obj[key]) continue;
        if (obj[key] instanceof Object || obj[key] instanceof Array) {
            result = deepSearch(obj[key], term, fieldsToIgnore);
        } else {
            result = obj[key].toString().toLocaleLowerCase()
                .includes(term.toLocaleLowerCase());
        }
        if (result) {
            return true;
        }
    }
    return false;
}
