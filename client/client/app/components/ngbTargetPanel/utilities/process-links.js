export default function processLinks (text) {
    let result = '';
    const regExp = /(<a\s+)([^>]*href=[^>]*\/?>)/ig;
    let e = regExp.exec(text);
    let previous = 0;
    while (e) {
        result = result
            .concat(text.slice(previous, e.index))
            .concat(e[1])
            .concat(' target="_blank" ')
            .concat(e[2]);
        previous = e.index + e[0].length;
        e = regExp.exec(text);
    }
    result = result.concat(text.slice(previous));
    return result;
}
