const sizePostfix = ['bytes', 'Kb', 'Mb', 'Gb', 'Tb', 'Pb', 'Eb', 'Zb', 'Yb'];
const kb = 1024;

export default function () {
    return function (size) {
        if (size && size > 0 && !Number.isNaN(Number(size))) {
            let sizeValue = +size;
            let index = 0;
            while (sizeValue > kb && index < sizePostfix.length - 1) {
                index += 1;
                sizeValue /= kb;
            }
            if (index === 0) {
                return `${sizeValue} ${sizePostfix[index]}`;
            }
            return `${sizeValue.toFixed(2)} ${sizePostfix[index]}`;
        }
        return '';
    };
}
