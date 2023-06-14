export default class ngbTargetPanelService {

    _identificationData = null;
    _identificationTarget = null;

    static instance (appLayout, dispatcher) {
        return new ngbTargetPanelService(appLayout, dispatcher);
    }

    get identificationData() {
        return this._identificationData;
    }
    get identificationTarget() {
        return this._identificationTarget;
    }

    constructor(appLayout, dispatcher) {
        Object.assign(this, {appLayout, dispatcher});
        dispatcher.on('target:launch:finished', this.setIdentificationData.bind(this));
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

    resetIdentificationData() {
        this.dispatcher.emit('reset:identification:data');
        this._identificationData = null;
        this._identificationTarget = null;
    }

    setIdentificationData(data, info) {
        this._identificationData = data;
        this._identificationTarget = info;
    }
}
