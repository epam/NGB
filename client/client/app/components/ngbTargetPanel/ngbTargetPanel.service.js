export default class ngbTargetPanelService {

    _isRepeat = false;
    _identificationData = null;
    _identificationParams = null;

    static instance (appLayout, dispatcher) {
        return new ngbTargetPanelService(appLayout, dispatcher);
    }

    get isRepeat() {
        return this._isRepeat;
    }
    set isRepeat(value) {
        this._isRepeat = value;
    }

    get identificationData() {
        return this._identificationData;
    }
    get identificationParams() {
        return this._identificationParams;
    }

    constructor(appLayout, dispatcher) {
        Object.assign(this, {appLayout, dispatcher});
        dispatcher.on('target:launch:finished', this.showIdentificationTab.bind(this));
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

    showIdentificationTab(data, params) {
        this._identificationData = data;
        this._identificationParams = params;
    }
}
