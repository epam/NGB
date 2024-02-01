export default class ngbTargetGenesFilterInputController {

    prevValue;
    displayText = '';

    static get UID() {
        return 'ngbTargetGenesFilterInputController';
    }

    constructor($scope, $element, dispatcher, ngbTargetGenesTableService, ngbTargetsFormService) {
        Object.assign(this, {$scope, dispatcher, ngbTargetGenesTableService, ngbTargetsFormService});
    }

    get filterInfo() {
        return this.ngbTargetGenesTableService.filterInfo || {};
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        this.displayText = (this.filterInfo || {})[this.column.field] || '';
    }

    onBlur() {
        this.apply();
    }

    onKeyPress (event) {
        switch ((event.code || '').toLowerCase()) {
            case 'enter':
                this.apply();
                break;
            default:
                break;
        }
    }

    apply() {
        if (this.prevValue !== this.displayText) {
            if (this.ngbTargetsFormService.needSaveGeneChanges()) {
                this.dispatcher.emit('target:form:confirm:filter', {
                    save: this.applyCanceled.bind(this),
                    cancel: this.applyConfirmed.bind(this)
                });
            } else {
                this.ngbTargetGenesTableService.setFilter(this.column.field, this.displayText);
                this.prevValue = this.displayText;
                this.dispatcher.emit('target:form:filters:changed');
            }
        }
    }

    applyCanceled() {
        this.displayText = this.prevValue;
    }

    applyConfirmed() {
        this.ngbTargetGenesTableService.setFilter(this.column.field, this.displayText);
        this.prevValue = this.displayText;
        this.dispatcher.emit('target:form:filters:changed');
    }
}
