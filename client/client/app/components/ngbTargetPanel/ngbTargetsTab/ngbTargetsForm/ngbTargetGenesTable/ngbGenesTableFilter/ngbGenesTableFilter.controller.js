import ListElements from './ngbFilterList.elements';

export default class ngbGenesTableFilterController {

    prevValue;
    displayText = '';
    listIsDisplayed = false;
    listElements = null;
    selectedItems = [];
    hideListTimeout = null;
    _hideListIsPrevented = false;

    static get UID() {
        return 'ngbGenesTableFilterController';
    }

    constructor($scope, $element, dispatcher, ngbTargetGenesTableService, ngbTargetsFormService) {
        Object.assign(this, {$scope, dispatcher, ngbTargetGenesTableService, ngbTargetsFormService});
        this.input = $element.find('.ngb-filter-input')[0];

        this.dispatcher.on('target:form:filters:list', this.setList.bind(this));
        this.dispatcher.on('target:form:filter:confirmed', this.applyConfirmed.bind(this));
        this.dispatcher.on('target:form:changes:save', this.applyCanceled.bind(this));
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:form:filters:list', this.setList.bind(this));
            dispatcher.removeListener('target:form:filter:confirmed', this.applyConfirmed.bind(this));
            dispatcher.removeListener('target:form:changes:save', this.applyCanceled.bind(this));
        });
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        this.selectedItems = ((this.filterInfo || {})[this.column.field] || []).map(i => i);
        this.displayText = [...this.selectedItems].join('; ');
        this.setList();
    }

    get filterInfo() {
        return this.ngbTargetGenesTableService.filterInfo || {};
    }

    setList() {
        this.list = this.ngbTargetGenesTableService.fieldList[this.column.field];
        if (this.list && this.list.length) {
            this.listElements = new ListElements(this.list,  {
                onSearchFinishedCallback: this.searchFinished.bind(this)
            });
        }
    }

    preventListFromClosing() {
        this._hideListIsPrevented = true;
        this.input.focus();
    }

    stopPreventListFromClosing() {
        this._hideListIsPrevented = false;
        this.input.focus();
    }

    displayList() {
        if (this.hideListTimeout) {
            clearTimeout(this.hideListTimeout);
            this.hideListTimeout = null;
        }
        this.listIsDisplayed = true;
    }

    hideList() {
        if (this.hideListTimeout) {
            clearTimeout(this.hideListTimeout);
            this.hideListTimeout = null;
        }
        if (this._hideListIsPrevented) {
            return;
        }
        this.listIsDisplayed = false;
        if (this.applying) {
            this.$scope.$apply();
            return;
        }
        this.apply();
        this.$scope.$apply();
    }

    onKeyPress (event) {
        switch ((event.code || '').toLowerCase()) {
            case 'enter':
                this.applying = true;
                this.apply();
                break;
            default:
                break;
        }
    }

    onChange() {
        const textParts = (this.displayText || '').split('; ');
        const lastPart = textParts[textParts.length - 1].trim().toLowerCase();
        if (this.listElements) {
            this.listElements.refreshList(lastPart);
        }
    }

    hideListDelayed() {
        if (this.hideListTimeout) {
            clearTimeout(this.hideListTimeout);
            this.hideListTimeout = null;
        }
        this.hideListTimeout = setTimeout(::this.hideList, 100);
    }

    itemIsSelected(item) {
        if (this.selectedItems) {
            const selectedItems = [...this.selectedItems].filter(listItem => (
                listItem.toLowerCase() === item.toLowerCase()
            ));
            return selectedItems.length > 0;
        }
        return false;
    }

    didClickOnItem(item) {
        if (this.hideListTimeout) {
            clearTimeout(this.hideListTimeout);
            this.hideListTimeout = null;
        }
        this.input.focus();
        const index = this.selectedItems.indexOf(item);
        if (index >= 0) {
            this.selectedItems.splice(index, 1);
        } else {
            this.selectedItems.push(item);
        }
        if (this.selectedItems.length) {
            this.displayText = this.selectedItems.join('; ');
        } else {
            this.displayText = '';
        }
        this.listElements.refreshList(null);
    }

    searchFinished(searchString, shouldUpdateScope) {
        const parts = this.displayText.split('; ').map(part => part.trim().toLowerCase());
        let last = '';
        if (parts.length) {
            last = parts[parts.length - 1];
            parts.splice(parts.length - 1, 1);
        }
        const fullList = this.listElements.fullList;
        if (fullList && fullList.length > 0) {
            this.selectedItems = fullList.filter(item => (
                parts.indexOf(item.toLowerCase()) >= 0
            ));
            const [fullMatch] = fullList.filter(item => (
                item.toLowerCase() === last.toLowerCase()
            ));
            if (fullMatch) {
                this.selectedItems.push(fullMatch);
            }
        }
        if (shouldUpdateScope) {
            this.$scope.$apply();
        }
    }

    apply() {
        const parts = this.displayText.split('; ')
            .map(part => part.trim());
        const selectedItems = parts.filter(part => part !== '');
        const prevValue = (this.filterInfo || {})[this.column.field] || [];
        prevValue.sort();
        const prevValueStr = JSON.stringify(prevValue).toUpperCase();
        const currValue = (this.selectedItems || []);
        currValue.sort();
        const currValueStr = JSON.stringify(currValue).toUpperCase();
        if (currValueStr !== prevValueStr) {
            if (this.ngbTargetsFormService.needSaveGeneChanges()) {
                this.dispatcher.emit('target:form:confirm:filter');
            } else {
                this.applying = false;
                this.selectedItems = selectedItems;
                this.displayText = this.selectedItems.join('; ');
                this.listIsDisplayed = false;
                this.ngbTargetGenesTableService.setFilter(this.column.field, currValue);
                this.dispatcher.emit('target:form:filters:changed');
            }
        } else {
            this.selectedItems = selectedItems;
            this.displayText = this.selectedItems.join('; ');
            this.listIsDisplayed = false;
        }
        if (this.listElements) {
            this.listElements.refreshList(null);
        }
    }

    applyCanceled() {
        this.applying = false;
        this.selectedItems = ((this.filterInfo || {})[this.column.field] || []).map(i => i);
        this.displayText = [...this.selectedItems].join('; ');
        this.setList();
    }

    applyConfirmed() {
        this.applying = false;
        const parts = this.displayText.split('; ')
            .map(part => part.trim());
        this.selectedItems = parts.filter(part => part !== '');
        this.displayText = this.selectedItems.join('; ');
        this.listIsDisplayed = false;
        const prevValue = (this.filterInfo || {})[this.column.field] || [];
        prevValue.sort();
        const prevValueStr = JSON.stringify(prevValue).toUpperCase();
        const currValue = (this.selectedItems || []);
        currValue.sort();
        const currValueStr = JSON.stringify(currValue).toUpperCase();
        if (currValueStr !== prevValueStr) {
            this.ngbTargetGenesTableService.setFilter(this.column.field, currValue);
            this.dispatcher.emit('target:form:filters:changed');
        }
        if (this.listElements) {
            this.listElements.refreshList(null);
        }
    }
}
