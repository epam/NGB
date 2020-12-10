const MAX_SIZE = 18;

export default class ngbTrackFontSizeController {

    static get UID() {
        return 'ngbTrackFontSizeController';
    }

    constructor($scope, $mdDialog, projectContext, dispatcher, settings, defaults, sources, types, options) {
        Object.assign(this, {
            $mdDialog,
            $scope,
            defaults,
            dispatcher,
            options,
            projectContext,
            settings,
            sources,
            types
        });
        this.prevSettings = Object.assign({}, settings);
        this.applyToAllTracks = false;
        this.applyToAllTracksOfType = false;
    }

    get settingsChanged() {
        return (this.settings.fontSize !== this.prevSettings.fontSize) || this.applyToAllTracks || this.applyToAllTracksOfType;
    }

    get canApplyDefaults() {
        return (this.settings.fontSize !== this.defaults.fontSize) || this.applyToAllTracks || this.applyToAllTracksOfType;
    }

    get fontSize() {
        if (this.settings.fontSize && /^[\d]+px$/.test(this.settings.fontSize)) {
            return Number(/^([\d]+)px$/i.exec(this.settings.fontSize)[1]);
        }
        return undefined;
    }

    set fontSize(size) {
        this.settings.fontSize = `${size > MAX_SIZE ? MAX_SIZE : size}px`;
    }

    save() {
        this.dispatcher.emitSimpleEvent('tracks:header:style:configure:done', {
            cancel: false,
            data: {
                applyToAllTracks: this.applyToAllTracks,
                applyToAllTracksOfType: this.applyToAllTracksOfType,
                settings: this.settings,
                type: (this.types || [])[0]
            },
            sources: this.sources,
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
