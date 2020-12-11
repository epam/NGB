const RADIX = 16;

export default class ngbBedColorPreferenceController {
    
    static get UID() {
        return 'ngbBedColorPreferenceController';
    }

    constructor($scope, $mdDialog, dispatcher, color, defaultColor, sources) {
        Object.assign(this, {
            $mdDialog,
            $scope,
            defaultColor,
            dispatcher,
            color,
            sources,
        });
        this.pickerOptions = {
            format: 'hex',
            pos: 'bottom right',
            swatch: true,
            swatchOnly: true,
        };
        this.color = this.preprocessColor(color);
        this.applyToAllTracks = (sources || []).length > 1;
    }

    convertDecimalToHex(decimal) {
        const from = 0;
        const to = 6;
        let hex = Number(decimal).toString(RADIX);
        hex = `#${'000000'.substr(from, to - hex.length)}${hex}`;
        return hex;
    }

    convertHexToDecimal(hex) {
        return parseInt(hex.substring(1), RADIX);
    }

    preprocessColor(color) {
        return typeof color === 'number'
            ? this.convertDecimalToHex(color)
            : color;
    }

    postprocessColors(color = {}) {
        return typeof color === 'string' && color.charAt(0) === '#'
            ? this.convertHexToDecimal(color)
            : color;
    }

    save() {
        this.dispatcher.emitSimpleEvent('track:color:configure:done', {
            cancel: false,
            data: {
                applyToAllTracks: this.applyToAllTracks,
                settings: this.postprocessColors(this.color),
            },
            sources: this.sources,
        });
        this.$mdDialog.hide();
    }

    close() {
        this.$mdDialog.hide();
    }

    resetToDefaults() {
        this.color = this.preprocessColor(this.defaultColor);
    }
}
