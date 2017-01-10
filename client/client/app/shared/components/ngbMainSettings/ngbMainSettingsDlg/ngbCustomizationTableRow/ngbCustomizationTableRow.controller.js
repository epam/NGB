export default class customizationTableRowController {
    static get UID() {
        return 'ngbCustomizationTableRowController';
    }

    constructor() {
        this.pickerOptions = {
            swatch: true,
            swatchOnly: true,
            pos: 'bottom right',
            format: 'hex'
        };
    }
}
