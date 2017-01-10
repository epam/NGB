export default class ngbHotkeyInputController {
    static get UID() {
        return 'ngbHotkeyInputController';
    }

    constructor($element, $scope, ngbHotkeyInputMessagesConstant, dispatcher) {

        Object.assign(this, {$element, $scope,  dispatcher});

        this.noneHotkey = 'None';
        this.messages = ngbHotkeyInputMessagesConstant;

        this._setHotkeyToDisplay(this.item.hotkey);

        this.$element.focusin(() => this._focusInCallback());
        this.$element.focusout(() => this._focusOutCallback());

        this.$scope.$on('hotkeyInputFocused', () => {
            this._clearMessages();
            this._applyUIChanges();
        });

        this.$scope.$on('hotkeyInputFocusedOut', () => {
            this._setHotkeyToDisplay(this.item.hotkey);
            this._applyUIChanges();
        });

        this.$scope.$on('setToDefault', () => {
            this._setHotkeyToDisplay(this.item.hotkey);
            this._clearMessages();
        });
    }

    _setHotkeyToDisplay(hotkey) {
        this.hotkeyToDisplay = hotkey.toUpperCase() || this.noneHotkey;
    }

    _clearMessages() {
        this.errorMessage = '';
        this.warningMessage = '';
    }

    _applyUIChanges() {
        this.$scope.$apply();
    }

    _emitFocusInLocalEvent() {
        this.$scope.$root.$broadcast('hotkeyInputFocused');
    }

    _emitFocusInGlobalEvent() {
        this.dispatcher.emitGlobalEvent('settings:focusIn');
    }

    _emitFocusOutLocalEvent() {
        this.$scope.$root.$broadcast('hotkeyInputFocusedOut');
    }

    _emitFocusOutGlobalEvent() {
        this.dispatcher.emitGlobalEvent('settings:focusOut');
    }

    _resetCapturedHotkey() {
        this.capturedHotkey = '';
    }

    _saveNewHotkeyToItem(item, hotkey) {
        item.hotkey = hotkey === this.noneHotkey ? '' : hotkey;
    }

    _focusInCallback() {
        this._emitFocusInLocalEvent();
        this._emitFocusInGlobalEvent();
        this._setHotkeyToDisplay(this.messages.enterNewShortcut);

        this._resetCapturedHotkey();

        let memoryLetter = '';

        this.$element.on('keydown', (e) => {
            this._resetCapturedHotkey();

            if (e.ctrlKey) this.capturedHotkey += 'Ctrl + ';
            if (e.shiftKey) this.capturedHotkey += 'Shift + ';
            if (e.altKey) this.capturedHotkey += 'Alt + ';
            if (e.key !== 'Control' && e.key !== 'Shift' && e.key !== 'Alt') {

                this.capturedHotkey += memoryLetter + e.key;
                memoryLetter = `${e.key} + `;
            }
            this._setHotkeyToDisplay(this.capturedHotkey);
            this._applyUIChanges();
        });
    }

    _focusOutCallback() {
        this._emitFocusOutGlobalEvent();
        this.$element.off('keydown');

        this.errorMessage = this._validate(this.hotkeyToDisplay, this.hotkeys);
        if (this.capturedHotkey === '' || this.errorMessage) {
            //set hotkeyToDisplay to old value
            this._setHotkeyToDisplay(this.item.hotkey);
            this._applyUIChanges();
            this._emitFocusOutLocalEvent();
            return;
        }

        const pathToSameHotkey = this._findItemWithSameHotkey(this.hotkeyToDisplay, this.item, this.hotkeys, []);
        if (pathToSameHotkey) {
            this.warningMessage = this.messages.alreadyUsed + pathToSameHotkey;
        }
        this._saveNewHotkeyToItem(this.item, this.hotkeyToDisplay);

        this._applyUIChanges();
        this._emitFocusOutLocalEvent();
    }

    _findItemWithSameHotkey(capturedHotkey, currentItem, hotkeys, path) {
        for (const item of hotkeys) {
            path.push(item.label);
            if (item.hotkey && item !== currentItem && item.hotkey.toUpperCase() === capturedHotkey.toUpperCase()) {
                this._saveNewHotkeyToItem(item, this.noneHotkey);
                return path.join(' : ');
            }
            if (item.subItems) {
                const found = this._findItemWithSameHotkey(capturedHotkey, currentItem, item.subItems, path);
                if (found) {
                    return found;
                }
            }
            path.pop();
        }
        return '';
    }

    _validate(result) {
        if (result.length === 1) {
            return result + this.messages.containedOnlyOneCharacter;
        }
        if (result.endsWith('+ ')) {
            return result + this.messages.onlyControls;
        }
        return '';

    }
}