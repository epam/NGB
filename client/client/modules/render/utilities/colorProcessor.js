const Math = window.Math;

export default class ColorProcessor {

    static darkenColor(hex, amount = 0.4) {
        let value = 0x000000;
        for (let i = 0; i < 3; i++) {
            let part = (hex >> (i * 8) & 0x0000FF);
            part = Math.round(Math.min(Math.max(0, part - part * amount), 255)) << (i * 8);
            value = value | part;
        }
        return value;
    }

    static lightenColor(hex, amount = 0.4) {
        let value = 0x000000;
        for (let i = 0; i < 3; i++) {
            let part = (hex >> (i * 8) & 0x0000FF);
            part = Math.round(Math.min(Math.max(0, part + part * amount), 255)) << (i * 8);
            value = value | part;
        }
        return value;
    }
}