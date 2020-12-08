const HIDDEN_MENU_ITEMS_BUTTON_WIDTH = 24;

export default class ngbTrackMenuController {

    static get UID() {
        return 'ngbTrackSettingsController';
    }

    settings;
    projectContext;

    constructor($scope, $element, lastActionRepeater, localDataService, projectContext) {
        this.lastActionRepeater = lastActionRepeater;
        this.projectContext = projectContext;
        this.element = $element;
        const dispatcher = this.lastActionRepeater.dispatcher;
        this.elements = {};
        const self = this;
        $scope.collapsibleTrackMenu = true;
        $scope.registerMenuItem = (element, field) => {
            if (field && field.name) {
                self.elements[field.name] = element;
                self.correctHiddenItems();
            }
        };
        $scope.unregisterMenuItem = (element, field) => {
            if (field && field.name) {
                delete this.elements[field.name];
                self.correctHiddenItems();
            }
        };
        $scope.correctHiddenItems = () => self.correctHiddenItems();

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

        $scope.$watch(function () { return $element.outerWidth(); }, () => {
            self.correctHiddenItems();
        });

        $scope.$on('$destroy', () => {
            dispatcher.removeListener('hotkeyPressed', hotkeyPressedFn);
            dispatcher.removeListener('settings:change', globalSettingsChanged);
        });
    }

    correctHiddenItems() {
        if (!this.element || !this.settings) {
            return;
        }
        let current = 0;
        const getSettingWidth = (setting) => this.elements[setting.name]
            ? this.elements[setting.name].outerWidth()
            : 0;
        const totalWidth = this.settings.reduce((r, c) => r + getSettingWidth(c), 0);
        const width = this.element.outerWidth();
        if (totalWidth > width) {
            current += HIDDEN_MENU_ITEMS_BUTTON_WIDTH;
        }
        this.settings.forEach(setting => {
            const settingWidth = getSettingWidth(setting);
            setting.__menuHidden = current + settingWidth > width;
            current += settingWidth;
        });
    }

    get hiddenSettings () {
        return (this.settings || []).filter(o => o.__menuHidden);
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
