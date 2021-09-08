const postfixBase = 10 ** 3;

const postfixes = [
    '', // 10^0
    'K', // 1000^1
    'M', // 1000^2
    'G', // 1000^3
    'T', // 1000^4
    'P' // 1000^5
];

export default function getDisplayValue (value, base) {
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
