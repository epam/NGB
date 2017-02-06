export default class ngbTrackMenuController {

    static get UID() {
        return 'ngbTrackSettingsController';
    }

    settings;
    projectContext;

    constructor($scope, lastActionRepeater, localDataService, projectContext) {
        this.lastActionRepeater = lastActionRepeater;
        this.projectContext = projectContext;
        const dispatcher = this.lastActionRepeater.dispatcher;
        const self = this;

        const hotkeyPressedFn = () => $scope.$apply();
        const globalSettingsChanged = () => {
            let hotkeys = this.projectContext.hotkeys || localDataService.getSettings().hotkeys;
            if(self.settings) {
                self.settings.forEach(menuEntry => {
                    menuEntry.fields.forEach(field => {
                        let objHotkey = hotkeys[field.name];
                        if (objHotkey && objHotkey.hotkey) {
                            field.hotkey = objHotkey.hotkey;
                        }
                    });
                });
            }
        };

        globalSettingsChanged();
        dispatcher.on('hotkeyPressed', hotkeyPressedFn);
        dispatcher.on('settings:change', globalSettingsChanged);

        $scope.$on('$destroy', () => {
            dispatcher.removeListener('hotkeyPressed', hotkeyPressedFn);
            dispatcher.removeListener('settings:change', globalSettingsChanged);
        });
    }

    handleField(e, field) {
        e.preventDefault();
        e.stopPropagation();
        field.isEnabled() ? field.disable() : field.enable();
        this.lastActionRepeater.rememberAction(field.name);
        return false;
    }

    handleButtonField(e, field) {
        e.preventDefault();
        e.stopPropagation();
        field.perform();
        this.lastActionRepeater.rememberAction(field.name);
        return false;
    }
}
