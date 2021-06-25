export default function () {
    return function (value) {
        if (!value) {
            return 'N/A';
        }
        return new Date(`${value.replace(' ', 'T')}Z`);
    };
}
