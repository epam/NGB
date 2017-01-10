export function stringParseInt(str) {
    if (!str) return 0;
    str += '';
    const testStr = str.replace(/[^0-9]/g, '');
    if (!testStr) return 0;
    return parseInt(testStr);
}