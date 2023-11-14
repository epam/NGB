import ListElements from './ngbFilterList.elements';

export default class ngbTargetsFilterList {

    prevValue;
    displayText = '';
    listIsDisplayed = false;
    listElements = null;
    selectedItems = [];
    hideListTimeout = null;
    _hideListIsPrevented = false;

    get filterInfo() {
        return this.ngbTargetsTableService.filterInfo || {};
    }

    static get UID() {
        return 'ngbTargetsFilterList';
    }

    constructor($scope, $element, dispatcher, ngbTargetsTableService) {
        Object.assign(this, {$scope, dispatcher, ngbTargetsTableService});
        this.input = $element.find('.ngb-filter-input')[0];
        this.selectedItems = (this.filterInfo[this.column.field] || []).map(i => i);
        this.displayText = [...this.selectedItems].join('; ');
        this.dispatcher.on('targets:filters:list', this.setList.bind(this));
        this.dispatcher.on('targets:filters:reset', this.resetFilters.bind(this));
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('targets:filters:list', this.setList.bind(this));
            dispatcher.removeListener('targets:filters:reset', this.resetFilters.bind(this));
        });
        this.setList();
    }

    setList() {
        this.list = this.ngbTargetsTableService.fieldList[this.column.field];
        if (this.list && this.list.length) {
            this.listElements = new ListElements(this.list,  {
                onSearchFinishedCallback: ::this.searchFinished
            });
            this.$scope.$watch('$ctrl.list', () => {
                this.listElements = new ListElements(this.list,  {
                    onSearchFinishedCallback: ::this.searchFinished
                });
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
        this.apply();
        this.$scope.$apply();
    }

    onChange() {
        const textParts = (this.displayText || '').split(';');
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
        const indexes = this.selectedItems
            .map((el, ind) => el.toLowerCase() === item.toLowerCase() ? ind : null)
            .filter(i => i || i === 0);
        if (indexes.length) {
            this.selectedItems = this.selectedItems.filter((el, ind) => (
                !indexes.includes(ind)
            ));
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
        const parts = this.displayText.split(';').map(part => part.trim().toLowerCase());
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
        const parts = this.displayText.split(';')
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
            this.ngbTargetsTableService.setFilter(this.column.field, currValue);
            this.dispatcher.emit('targets:filters:changed');
        }
        if (this.listElements) {
            this.listElements.refreshList(null);
        }
    }

    resetFilters() {
        this.selectedItems = [];
        this.displayText = '';
    }
}
