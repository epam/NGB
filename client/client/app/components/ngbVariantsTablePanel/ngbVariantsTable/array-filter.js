export default function () {
    return function (value) {
        if (!value || !Array.isArray(value)) {
            return value;
        }
        return value.join(', ');
    };
}
