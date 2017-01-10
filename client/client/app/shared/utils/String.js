export function stringToDash(item) {
    return item.replace(/([A-Z])/g, function ($1) {
        return `-${  $1.toLowerCase()}`;
    });
}
export function isString(val) {
    return typeof val === 'string' || ((!!val && typeof val === 'object') && Object.prototype.toString.call(val) === '[object String]');
}

