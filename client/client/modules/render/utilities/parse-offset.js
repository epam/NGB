export default function parseOffset(config) {
    if (!Array.isArray(config)) {
        return parseOffset([config]);
    }
    const [top = 0, right = top, bottom = top, left = right] = config || {};
    return {
        top,
        left,
        right,
        bottom
    };
}
