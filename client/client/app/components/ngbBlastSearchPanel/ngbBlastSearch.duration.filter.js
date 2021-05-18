export default function() {
    return function (value) {
        if (value < 0) return value;
        const mins = Math.floor(value / 60);
        const secs = value % 60;
        let result = mins ? `${mins} min ` : '';
        if (secs) {
            result += `${secs} sec`;
        }
        return result.trim() || '0 sec';
    };
}
