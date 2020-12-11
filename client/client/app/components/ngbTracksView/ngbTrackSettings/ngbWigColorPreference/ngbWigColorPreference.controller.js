const RADIX = 16;

export default class ngbWigColorPreferenceController {

    static get UID() {
        return 'ngbWigColorPreferenceController';
    }

    constructor($scope, $mdDialog, dispatcher, settings, defaults, sources) {
        Object.assign(this, {
            $mdDialog,
            $scope,
            defaults,
            dispatcher,
            settings,
            sources,
        });
        this.pickerOptions = {
            format: 'hex',
            pos: 'bottom right',
            swatch: true,
            swatchOnly: true,
        };
        this.settings = this.preprocessColors(settings);
        this.prevSettings = Object.assign({}, this.settings);
        this.applyToWIGTracks = (sources || []).length > 1;
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

    preprocessColors(settings = {}) {
        return Object.fromEntries(Object.entries(settings)
          .map(([key, color]) => (
            typeof color === 'number'
              ? [key, this.convertDecimalToHex(color)]
              : [key, color]
          ))
        );
    }

    postprocessColors(settings = {}) {
        return Object.fromEntries(Object.entries(settings)
          .map(([key, color]) => (
            typeof color === 'string' && color.charAt(0) === '#'
              ? [key, this.convertHexToDecimal(color)]
              : [key, color]
          ))
        );
    }

    save() {
        this.dispatcher.emitSimpleEvent('track:color:configure:done', {
            cancel: false,
            data: {
                applyToWIGTracks: this.applyToWIGTracks,
                settings: this.postprocessColors(this.settings),
            },
            sources: this.sources,
        });
        this.$mdDialog.hide();
    }

    close() {
        this.$mdDialog.hide();
    }

    resetToDefaults() {
        this.settings = this.preprocessColors(this.defaults);
    }
}
