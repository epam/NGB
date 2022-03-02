import ListElements from './ngbFilterList.elements';

export default class ngbCoverageFilterListController {

    displayText = '';
    listIsDisplayed = false;
    listElements = null;
    selectedItems = [];
    hideListTimeout = null;
    _hideListIsPrevented = false;

    static get UID() {
        return 'ngbCoverageFilterListController';
    }

    get filterInfo() {
        return this.ngbCoveragePanelService.filterInfo;
    }

    constructor($scope, $element, dispatcher, projectContext, ngbCoveragePanelService) {
        Object.assign(this, {$scope, dispatcher, projectContext, ngbCoveragePanelService});
        this.input = $element.find('.ngb-filter-input')[0];
        this.listElements = new ListElements(this.list,  {
            onSearchFinishedCallback: ::this.searchFinished
        });
        this.$scope.$watch('$ctrl.list', () => {
            this.listElements = new ListElements(this.list,  {
                onSearchFinishedCallback: ::this.searchFinished
            });
        });
        if (this.filterInfo) {
            switch (this.field.field) {
                case 'chr': {
                    if (this.filterInfo.chromosomes) {
                        this.selectedItems = this.projectContext.chromosomes
                            .filter(chr => (
                                this.filterInfo.chromosomes.indexOf(chr.name) >= 0
                            ))
                            .map(chr => chr.name);
                        this.displayText = [...this.selectedItems].join(', ');
                    }
                    break;
                }
            }
        } else {
            this.resetFilter();
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

    hideListDelayed() {
        if (this.hideListTimeout) {
            clearTimeout(this.hideListTimeout);
            this.hideListTimeout = null;
        }
        this.hideListTimeout = setTimeout(::this.hideList, 100);
    }

    onChange() {
        const textParts = (this.displayText || '').split(',');
        const lastPart = textParts[textParts.length - 1].trim().toLowerCase();
        this.listElements.refreshList(lastPart);
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
            this.displayText = this.selectedItems.join(', ');
        } else {
            this.displayText = '';
        }
        this.listElements.refreshList(null);
    }

    apply() {
        const parts = this.displayText.split(',')
            .map(part => part.trim().toLowerCase());
        this.selectedItems = parts.filter(part => part !== '');
        this.displayText = this.selectedItems.join(', ');
        this.listIsDisplayed = false;
        switch (this.field.field) {
            case 'chr': {
                const selectedItemsLowerCase = this.selectedItems.map(i => i.toLowerCase());
                const prevValue = (this.filterInfo || {}).chromosomes || [];
                prevValue.sort();
                const prevValueStr = JSON.stringify(prevValue).toUpperCase();
                const currValue = this.projectContext.chromosomes
                    .filter(chr => selectedItemsLowerCase.indexOf(chr.name.toLowerCase()) >= 0)
                    .map(chr => chr.name);
                currValue.sort();
                const currValueStr = JSON.stringify(currValue).toUpperCase();
                if (currValueStr !== prevValueStr) {
                    this.ngbCoveragePanelService.setFilter('chromosomes', currValue);
                    this.dispatcher.emit('coverage:filter:changed');
                }
                break;
            }
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

    resetFilter() {
        this.selectedItems = [];
        this.displayText = '';
    }
}
