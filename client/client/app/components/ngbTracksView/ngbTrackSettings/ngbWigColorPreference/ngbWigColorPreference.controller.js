export default class ngbWigColorPreferenceController {

    static get UID() {
        return 'ngbWigColorPreferenceController';
    }

    constructor($scope, $mdDialog, dispatcher, settings, defaults, source) {
        Object.assign(this, {
            $mdDialog,
            $scope,
            defaults,
            dispatcher,
            source,
        });
        this.pickerOptions = {
            format: 'hex',
            pos: 'bottom right',
            swatch: true,
            swatchOnly: true,
        };
        this.settings = this.preprocessColors(settings);
        this.applyToWIGTracks = false;
        this.applyToCurrentTrack =  true;
    }

    get settingsChanged () {
        return JSON.stringify(this.preprocessColors(this.defaults)) !== JSON.stringify(this.preprocessColors(this.settings)); 
    }

    convertDecimalToHex(decimal) {
        let hex = Number(decimal).toString(16);
        hex = `#${  '000000'.substr(0, 6 - hex.length)  }${hex}`;
        return hex;
    }

    convertHexToDecimal(hex) {
        return parseInt(hex.substring(1), 16);
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
        this.dispatcher.emitSimpleEvent('wig:color:configure:done', {
            cancel: false,
            data: {
                applyToCurrentTrack: this.applyToCurrentTrack,
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
        this.dispatcher.emitSimpleEvent('wig:color:configure:done', {
            cancel: true,
            source: this.source,
        });
        this.settings = this.preprocessColors(this.defaults);
    }
}
