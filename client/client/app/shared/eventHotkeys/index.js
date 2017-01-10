import KeyboardJS from 'keyboardjs';

export default  class eventHotkey {

    static instance(dispatcher, localDataService) {
        return new eventHotkey(dispatcher, localDataService);
    }

    hotkeys = [];
    dispatcher = null;
    localDataService = null;
    settings = null;

    constructor(dispatcher, localDataService) {

        Object.assign(this, {dispatcher, localDataService});

        this.hotkeys = this.localDataService.getSettings().hotkeys;
        const list = this.hotkeys;
        this._bind(list);

    }

    init() {
        this.dispatcher.on('settings:change', this.onUpdate = ::this.update);
        this.dispatcher.on('settings:focusIn', this.onFocusIn = () => {
            KeyboardJS.pause();
        });
        this.dispatcher.on('settings:focusOut', this.onFocusOut = () => {
            KeyboardJS.resume();
        });
    }

    destroy() {
        this.dispatcher.removeListener('settings:change', this.onUpdate);
        this.dispatcher.removeListener('settings:focusIn', this.onFocusIn);
        this.dispatcher.removeListener('settings:focusOut', this.onFocusOut);
    }

    _bind(list) {
        for (const key of Object.keys(list)) {
            if (list[key].hotkey) {
                KeyboardJS.bind(list[key].hotkey.toLowerCase(), () => {
                    this.dispatcher.emitGlobalEvent('hotkeyPressed', key);
                });
            }
        }
    }

    _unbind(list) {
        for (const key of Object.keys(list)) {
            if (list[key].hotkey) {
                KeyboardJS.unbind(list[key].hotkey.toLowerCase());
            }
        }
    }

    update() {
        const list = this.hotkeys;
        this._unbind(list);
        this.hotkeys = this.localDataService.getSettings().hotkeys;
        this._bind(this.hotkeys);
    }


}
