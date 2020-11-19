const RADIX = 16;

export default class ngbWigColorPreferenceController {

    static get UID() {
        return 'ngbWigColorPreferenceController';
    }

    constructor($scope, $mdDialog, dispatcher, settings = {}, defaults = {}, source) {
        Object.assign(this, {
            $mdDialog,
            $scope,
            defaults,
            dispatcher,
            settings,
            source,
        });
        this.pickerOptions = {
            format: 'hex',
            pos: 'bottom right',
            swatch: true,
            swatchOnly: true,
        };
        this.settings = this.preprocessColors(settings);
        this.prevSettings = Object.assign({}, this.settings);
        this.applyToWIGTracks = false;
    }

    get settingsChanged () {
        return (
          JSON.stringify(this.preprocessColors(this.prevSettings)) !==
            JSON.stringify(this.preprocessColors(this.settings)) ||
          this.applyToWIGTracks
        ); 
    }

    get isDefaultSettings() {
        return JSON.stringify(this.preprocessColors(this.defaults)) === JSON.stringify(this.preprocessColors(this.settings)); 
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

    preprocessColors(settings) {
        return Object.fromEntries(Object.entries(settings)
          .map(([key, color]) => (
            typeof color === 'number'
              ? [key, this.convertDecimalToHex(color)]
              : [key, color]
          ))
        );
    }

    postprocessColors(settings) {
        return Object.fromEntries(Object.entries(settings)
          .map(([key, color]) => (
            typeof color === 'string' && color.charAt(0) === '#'
              ? [key, this.convertHexToDecimal(color)]
              : [key, color]
          ))
        );
    }

    save() {
        this.dispatcher.emitSimpleEvent('wig:color:configure:done', {
            cancel: false,
            data: {
                applyToWIGTracks: this.applyToWIGTracks,
                settings: this.postprocessColors(this.settings),
            },
            source: this.source,
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
