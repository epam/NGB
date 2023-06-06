const TARGET_STATE = {
    TARGETS: 'TARGETS',
    IDENTIFICATIONS: 'IDENTIFICATIONS'
};

export default class ngbTargetPanelService {

    _isRepeat = false;

    static instance (appLayout, dispatcher) {
        return new ngbTargetPanelService(appLayout, dispatcher);
    }

    get targetState() {
        return TARGET_STATE;
    }

    get isRepeat() {
        return this._isRepeat;
    }
    set isRepeat(value) {
        this._isRepeat = value;
    }

    constructor(appLayout, dispatcher) {
        Object.assign(this, {appLayout, dispatcher});
    }

    panelAddTargetPanel() {
        const layoutChange = this.appLayout.Panels.target;
        layoutChange.displayed = true;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
    }

    panelCloseTargetPanel() {
        this.resetData();
        const layoutChange = this.appLayout.Panels.target;
        layoutChange.displayed = false;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
    }
}
