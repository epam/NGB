const MAX_LENGTH = 40;

export default function () {
    return function (value) {
        if (value && value.length > MAX_LENGTH - 3) {
            return value.slice(0, MAX_LENGTH).concat('...');
        }
        return value || '';
    };
}
