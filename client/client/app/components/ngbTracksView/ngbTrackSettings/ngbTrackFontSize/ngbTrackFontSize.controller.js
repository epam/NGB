const MAX_SIZE = 20;

export default class ngbTrackFontSizeController {

    static get UID() {
        return 'ngbTrackFontSizeController';
    }

    constructor($scope, $mdDialog, projectContext, dispatcher, settings, defaults, source) {
        Object.assign(this, {
            $mdDialog,
            $scope,
            defaults,
            dispatcher,
            projectContext,
            settings,
            source,
        });
        this.prevSettings = Object.assign({}, settings);
        this.applyToAllTracks = false;
    }

    get settingsChanged() {
        return (this.settings.fontSize !== this.prevSettings.fontSize) || this.applyToAllTracks;
    }

    get canApplyDefaults() {
        return (this.settings.fontSize !== this.defaults.fontSize) || this.applyToAllTracks;
    }

    getSetFontSize(size) {
        return arguments.length
          ? this.settings.fontSize = `${size > MAX_SIZE ? MAX_SIZE : size}px`
          : parseInt((this.settings.fontSize || this.defaults.fontSize), 10);
    }

    save() {
        this.dispatcher.emitSimpleEvent('tracks:header:style:configure:done', {
            cancel: false,
            data: {
                applyToAllTracks: this.applyToAllTracks,
                settings: this.settings,
            },
            source: this.source,
        });
        this.$mdDialog.hide();
    }

    close() {
        this.$mdDialog.hide();
    }

    resetToDefaults() {
        this.settings.fontSize = this.defaults.fontSize;
    }
}
