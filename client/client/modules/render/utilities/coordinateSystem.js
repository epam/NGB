const postfixBase = 10 ** 3;

const postfixes = [
    '', // 10^0
    'K', // 1000^1
    'M', // 1000^2
    'G', // 1000^3
    'T', // 1000^4
    'P' // 1000^5
];

function getDisplayValue (value, base) {
    if (value === 0) {
        return '0';
    }
    const unsignedValue = Math.abs(value);
    const sign = Math.sign(value) < 0 ? '-' : '';
    const maximumBase = Math.min(base ? Math.floor(Math.log10(base) / 3) : Infinity, postfixes.length - 1);
    for (let k = maximumBase; k >= 0; k--) {
        const baseValue = postfixBase ** k;
        if (unsignedValue >= baseValue) {
            return `${sign}${Math.floor(unsignedValue / baseValue * postfixBase) / postfixBase}${postfixes[k]}`;
        }
    }
    const digitsPower = Math.max(1, -Math.floor(base), -Math.floor(Math.log10(unsignedValue)));
    const digits = 10 ** digitsPower;
    if (digitsPower > 3) {
        return `${sign}${Math.floor(unsignedValue * digits)}e-${digitsPower}`;
    }
    return `${sign}${Math.floor(unsignedValue * digits) / digits}`;
}

function getClosesBase (value, log = false) {
    if (value <= 0) {
        return 0;
    }
    const digits = Math.floor(Math.log10(value));
    const base = 10 ** digits;
    const test = [
        10 * base,
        !log && 5 * base,
        !log && 2.5 * base,
        base
    ].filter(Boolean);
    const roundFactor = digits < 0 ? (10 ** (-digits)) : 1;
    const round = o => Math.floor(o * roundFactor) / roundFactor;
    const [best] = test
        .sort((a, b) => Math.abs(value - a) - Math.abs(value - b));
    return {
        base,
        closestBase: round(best)
    };
}

function fillRange(from, to, tickRange) {
    if (tickRange <= 0) {
        return [];
    }
    let tick = Math.floor(from / tickRange) * tickRange;
    while (tick < from) {
        tick += tickRange;
    }
    const ticks = [];
    while (tick <= to) {
        ticks.push(tick);
        tick += tickRange;
    }
    return ticks;
}

export default function generateCoordinateSystemTicks (from, to, log = false, count = 3) {
    count = (!count || !Number.isFinite(Number(count)) || count <= 0)
        ? 3
        : count;
    if (to - from === 0 || !Number.isFinite(Number(from)) || !Number.isFinite(Number(to))) {
        return [];
    }
    if (log) {
        const getPower = o => o <= 0 ? 0 : Math.floor(Math.log10(o));
        const maxPower = Math.max(getPower(to), getPower(from));
        const minPower = Math.min(getPower(to), getPower(from));
        const range = Math.max(1, maxPower - minPower);
        const step = Math.max(1, Math.floor(range / count));
        const powers = fillRange(minPower, maxPower, step);
        return powers.map(o => ({
            value: 10 ** o,
            display: getDisplayValue(10 ** o)
        }));
    }
    const range = Math.abs(from - to);
    const rawTickRange = range / (count + 1);
    const {
        base,
        closestBase: tickRange
    } = getClosesBase(rawTickRange, log);
    return fillRange(from, to, tickRange)
        .map(o => ({
            value: o,
            display: getDisplayValue(o, base)
        }));
}
