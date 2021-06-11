export default function () {
    return function (value, decimals) {
        const result = parseFloat(value);
        if (isNaN(result)) {
            return value;
        }
        return `${(result * 100).toFixed(decimals)}%`;
    };
}
