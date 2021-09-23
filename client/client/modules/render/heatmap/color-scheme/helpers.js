/**
 * @enum {number}
 */
const ColorFormats = {
    /**
     * Number format
     */
    number: 0,
    /**
     * String format, i.e. "#rrggbb"
     */
    hex: 1
};

export {ColorFormats};

/**
 * Parses color channels (red, green, blue)
 * @param {number} color
 * @param {boolean} [integer=true] - channel value format (integer - 0...255, float - 0...1)
 * @returns {{r: number, b: number, g: number}}
 */
export function getChannels(color, integer = true) {
    const r = color >> 0x10;
    const g = (color >> 0x8) & 0xff;
    const b = color & 0xff;
    if (integer) {
        return {r, g, b};
    }
    const byte = 255;
    return {
        r: r / byte,
        g: g / byte,
        b: b / byte
    };
}

/**
 * Returns color value for channels
 * @param {Object} channels
 * @param {number} channels.r - red channel integer value (0...255)
 * @param {number} channels.g - green channel integer value (0...255)
 * @param {number} channels.b - blue channel integer value (0...255)
 * @returns {number}
 */
function getColor({r, g, b}) {
    const ensure = o => o & 0xff;
    return (ensure(r) << 0x10) | (ensure(g) << 0x8) | ensure(b);
}

/**
 * Interpolates value for ratio
 * @param {number} start
 * @param {number} stop
 * @param {number} ratio
 * @returns {number}
 */
function interpolate(start, stop, ratio) {
    return start + (stop - start) * ratio;
}

/**
 * Interpolates color for ratio
 * @param {number} start
 * @param {number} stop
 * @param {number} ratio
 * @returns {number}
 */
export function interpolateColors(start, stop, ratio) {
    const channelsStart = getChannels(start);
    const channelsStop = getChannels(stop);
    return getColor({
        r: interpolate(channelsStart.r, channelsStop.r, ratio),
        g: interpolate(channelsStart.g, channelsStop.g, ratio),
        b: interpolate(channelsStart.b, channelsStop.b, ratio)
    });
}

/**
 * Returns passed color as value of requested format
 * @param {number|string} color - either hex value (0xrrggbb) or string value ('#rrggbb')
 * @param {ColorFormats} format - either 'hex' or 'string'
 * @returns {string|number}
 */
export function formatColor(color, format) {
    if (format === ColorFormats.hex && typeof color === 'number') {
        const hex = `000000${color.toString(16)}`.slice(-6);
        return `#${hex}`;
    } else if ((!format || format === ColorFormats.number) && typeof color === 'string') {
        return parseInt(color.replace('#', ''), 16);
    }
    return color;
}

/**
 * Returns hex color value for passed color
 * @param {number|string} color
 * @returns {number}
 */
export function systemColorValue(color) {
    if (typeof color === 'string') {
        return parseInt(color.replace('#', ''), 16);
    }
    return color;
}
