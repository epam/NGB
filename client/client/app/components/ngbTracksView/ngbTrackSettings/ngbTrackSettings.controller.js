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

        const menuOpenedFn = () => {
            if (self.onMenuOpened) {
                self.onMenuOpened(self.trackController);
            }
        };
        const menuClosedFn = () => {
            if (self.onMenuClosed) {
                self.onMenuClosed(self.trackController);
            }
        };

        dispatcher.on('hotkeyPressed', hotkeyPressedFn);
        dispatcher.on('settings:change', globalSettingsChanged);

        $scope.$on('$mdMenuOpen', menuOpenedFn);
        $scope.$on('$mdMenuClose', menuClosedFn);

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
        if (this.onHandle) {
            this.onHandle(this.trackController);
        }
        return false;
    }

    handleButtonField(e, field) {
        e.preventDefault();
        e.stopPropagation();
        field.perform();
        this.lastActionRepeater.rememberAction(field.name);
        if (this.onHandle) {
            this.onHandle(this.trackController);
        }
        return false;
    }
}
