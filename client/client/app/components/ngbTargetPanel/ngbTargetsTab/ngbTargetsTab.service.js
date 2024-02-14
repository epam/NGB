const MODE = {
    TABLE: 'table',
    ADD: 'add',
    EDIT: 'edit'
};

export default class ngbTargetsTabService {

    _targetMode;

    _tableLoading = false;
    _tableFailed = false;
    _tableErrorMessageList = null;

    _launchLoading = false;
    _launchFailed = false;
    _launchErrorMessageList = null;

    get mode () {
        return MODE;
    }
    get targetMode() {
        return this._targetMode;
    }
    set targetMode(value) {
        this._targetMode = value;
    }
    get isTableMode() {
        return this.targetMode === this.mode.TABLE;
    }
    get isAddMode() {
        return this.targetMode === this.mode.ADD;
    }
    get isEditMode() {
        return this.targetMode === this.mode.EDIT;
    }

    get tableLoading() {
        return this._tableLoading;
    }
    set tableLoading(value) {
        this._tableLoading = value;
    }
    get tableFailed() {
        return this._tableFailed;
    }
    set tableFailed(value) {
        this._tableFailed = value;
    }
    get tableErrorMessageList() {
        return this._tableErrorMessageList;
    }
    set tableErrorMessageList(value) {
        this._tableErrorMessageList = value;
    }

    get launchLoading() {
        return this._launchLoading;
    }
    get launchFailed() {
        return this._launchFailed;
    }
    set launchFailed(value) {
        this._launchFailed = value;
    }
    get launchErrorMessageList() {
        return this._launchErrorMessageList;
    }
    set launchErrorMessageList(value) {
        this._launchErrorMessageList = value;
    }

    get emptyResults () {
        return !this.loadingData &&
            !this.failedResult &&
            (!this.gridOptions || !this.gridOptions.data || this.gridOptions.data.length === 0);
    }

    static instance (
        $timeout,
        dispatcher,
        ngbTargetPanelService,
        targetDataService,
        projectContext,
        targetContext,
    ) {
        return new ngbTargetsTabService(
            $timeout,
            dispatcher,
            ngbTargetPanelService,
            targetDataService,
            projectContext,
            targetContext,
        );
    }

    constructor(
        $timeout,
        dispatcher,
        ngbTargetPanelService,
        targetDataService,
        projectContext,
        targetContext,
    ) {
        Object.assign(this, {
            $timeout,
            dispatcher,
            ngbTargetPanelService,
            targetDataService,
            projectContext,
            targetContext,
        });
        this.setTableMode();
    }

    setTableMode() {
        this.targetMode = this.mode.TABLE;
        this.targetContext.setTargetsTableActionsVisibility(this.targetMode);
        this.targetContext.setTargetsFormActionsVisibility(this.targetMode);
    }
    setAddMode() {
        this.targetMode = this.mode.ADD;
        this.targetContext.setTargetsTableActionsVisibility(this.targetMode);
        this.targetContext.setTargetsFormActionsVisibility(this.targetMode);
    }
    setEditMode() {
        this.targetMode = this.mode.EDIT;
        this.targetContext.setTargetsTableActionsVisibility(this.targetMode);
        this.targetContext.setTargetsFormActionsVisibility(this.targetMode);
    }

    async getIdentificationData(params, info) {
        this.ngbTargetPanelService.resetIdentificationData();
        this._launchLoading = true;
        const result = await this.launchTargetIdentification(params)
            .then(result => {
                if (result) {
                    this.dispatcher.emit('target:launch:finished', result, info);
                } else {
                    this.setTableMode();
                    this.dispatcher.emit('target:launch:failed');
                    this.$timeout(() => {
                        this._launchFailed = false;
                        this._launchErrorMessageList = null;
                        this.dispatcher.emit('target:launch:failed:refresh');
                    }, 20000);
                }
                return result;
            });
        this.setTableMode();
        return result;
    }

    launchTargetIdentification(request) {
        return new Promise(resolve => {
            this.targetDataService.postTargetIdentification(request)
                .then(result => {
                    if (result && result.desription) {
                        this._launchFailed = false;
                        this._launchErrorMessageList = null;
                    }
                    this._launchLoading = false;
                    resolve(result);
                })
                .catch(err => {
                    this._launchFailed = true;
                    this._launchErrorMessageList = [err.message];
                    this._launchLoading = false;
                    resolve(false);
                });
        });
    }
}
