export function stringToDash(item) {
    return item.replace(/([A-Z])/g, function ($1) {
        return `-${  $1.toLowerCase()}`;
    });
}
export function isString(val) {
    return typeof val === 'string' || ((!!val && typeof val === 'object') && Object.prototype.toString.call(val) === '[object String]');
}

/** Splits camelCase & PascalCase to capitalized words  */
export function camelPad(str) {
    return str
        // Look for long acronyms and filter out the last letter
        .replace(/([A-Z]+)([A-Z][a-z])/g, ' $1 $2')
        // Look for lower-case letters followed by upper-case letters
        .replace(/([a-z\d])([A-Z])/g, '$1 $2')
        // Look for lower-case letters followed by numbers
        .replace(/([a-zA-Z])(\d)/g, '$1 $2')
        .replace(/^./, (str) => { return str.toUpperCase(); })
        // Remove any white space left around the word
        .trim();
}
