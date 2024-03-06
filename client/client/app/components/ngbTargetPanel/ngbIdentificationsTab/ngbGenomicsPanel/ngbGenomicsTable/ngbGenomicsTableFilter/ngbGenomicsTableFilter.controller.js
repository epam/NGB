import ListElements from './ngbFilterList.elements';

export default class ngbGenomicsTableFilterController {

    prevValue;
    displayText = '';
    listIsDisplayed = false;
    listElements = null;
    selectedItems = [];
    hideListTimeout = null;
    _hideListIsPrevented = false;

    static get UID() {
        return 'ngbGenomicsTableFilterController';
    }

    constructor($scope, $element, dispatcher, ngbGenomicsPanelService) {
        Object.assign(this, {$scope, dispatcher, ngbGenomicsPanelService});
        this.input = $element.find('.ngb-filter-input')[0];
        this.selectedItems = ((this.filterInfo || {})[this.column.field] || []).map(i => i);
        this.setDisplayText();

        const setList = this.setList.bind(this);
        const resetFilters = this.resetFilters.bind(this);
        this.dispatcher.on('genomics:filters:list', setList);
        this.dispatcher.on('genomics:filters:reset', resetFilters);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('genomics:filters:list', setList);
            dispatcher.removeListener('genomics:filters:reset', resetFilters);
        });
        this.setList();
    }

    setDisplayText() {
        if (this.column.field === 'species') {
            this.displayText = this.selectedItems.map(i => i.name).join(', ');
        } else {
            this.displayText = [...this.selectedItems].join(', ');
        }
    }

    get filterInfo() {
        return this.ngbGenomicsPanelService.filterInfo || {};
    }

    setList() {
        this.list = this.ngbGenomicsPanelService.fieldList[this.column.field];
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
        const textParts = (this.displayText || '').split(',');
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
            const selectedItems = this.selectedItems.filter(listItem => {
                if (this.column.field === 'species') {
                    return listItem.taxId === item.taxId;
                }
                return listItem.toLowerCase() === item.toLowerCase();
            });
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
            this.setDisplayText();
        } else {
            this.displayText = '';
        }
        this.listElements.refreshList(null);
    }

    searchFinished(searchString, shouldUpdateScope) {
        const parts = this.displayText.split(',').map(part => part.trim().toLowerCase());
        let last = '';
        if (parts.length) {
            last = parts[parts.length - 1];
            parts.splice(parts.length - 1, 1);
        }
        const fullList = this.listElements.fullList;
        if (fullList && fullList.length > 0) {
            this.selectedItems = fullList.filter(item => {
                if (this.column.field === 'species') {
                    return parts.indexOf(item.name.toLowerCase()) >= 0;
                }
                return parts.indexOf(item.toLowerCase()) >= 0;
            });
            const [fullMatch] = fullList.filter(item => {
                if (this.column.field === 'species') {
                    return item.name.toLowerCase() === last.toLowerCase();
                }
                return item.toLowerCase() === last.toLowerCase();
            });
            if (fullMatch) {
                this.selectedItems.push(fullMatch);
            }
        }
        if (shouldUpdateScope) {
            this.$scope.$apply();
        }
    }

    apply() {
        const parts = this.displayText.toLowerCase()
            .split(',')
            .map(part => part.trim());
        switch (this.column.field) {
            case 'species': {
                if (this.listElements.fullList && this.listElements.fullList.length > 0) {
                    this.listElements.fullList.forEach((item, index) => {
                        if (parts.indexOf(item.name.toLowerCase()) >= 0) {
                            if (!this.selectedItems.includes(this.listElements.fullList[index])) {
                                this.selectedItems.push(this.listElements.fullList[index]);
                            }
                        }
                    });
                }
                break;
            }
            default: {
                this.selectedItems = parts.filter(part => part !== '');
            }
        }
        this.setDisplayText();
        this.listIsDisplayed = false;
        const prevValue = (this.filterInfo || {})[this.column.field] || [];
        prevValue.sort();
        const prevValueStr = JSON.stringify(prevValue).toUpperCase();
        const currValue = (this.selectedItems || []);
        currValue.sort();
        const currValueStr = JSON.stringify(currValue).toUpperCase();
        if (currValueStr !== prevValueStr) {
            this.ngbGenomicsPanelService.setFilter(this.column.field, currValue);
            this.dispatcher.emit('target:identification:genomics:filters:changed');
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
