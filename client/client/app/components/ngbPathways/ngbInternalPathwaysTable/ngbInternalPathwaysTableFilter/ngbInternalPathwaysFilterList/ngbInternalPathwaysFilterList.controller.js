import ListElements from './ngbFilterList.elements';

export default class ngbInternalPathwaysFilterListController {
    listIsDisplayed = false;
    hideListTimeout = null;
    scope;
    input;
    selectedItems = [];
    displayText = '';
    listElements = null;
    _hideListIsPrevented = false;

    constructor($scope, $element, ngbInternalPathwaysTableService) {
        this.scope = $scope;
        this.ngbInternalPathwaysTableService = ngbInternalPathwaysTableService;
        this.input = $element.find('.ngb-filter-input')[0];
        this.listElements = new ListElements(this.list, {
            onSearchFinishedCallback: this.searchFinished.bind(this)
        });
        this.scope.$watch('$ctrl.list', () => {
            this.listElements = new ListElements(this.list, {
                onSearchFinishedCallback: this.searchFinished.bind(this)
            });
        });
        switch (this.field.field) {
            case 'organisms': {
                if (this.ngbInternalPathwaysTableService.internalPathwaysFilter.organisms) {
                    this.selectedItems = this.ngbInternalPathwaysTableService.speciesList
                        .filter(organism => this.ngbInternalPathwaysTableService.internalPathwaysFilter.organisms.indexOf(organism.taxId) >= 0);
                    this.displayText = [...this.selectedItems].map(organism => organism.speciesName || organism.taxId).join(', ');
                }
                break;
            }
        }
    }

    static get UID() {
        return 'ngbInternalPathwaysFilterListController';
    }

    searchFinished(searchString, shouldUpdateScope) {
        const parts = this.displayText.split(',').map(part => part.trim().toLowerCase());
        let last = '';
        if (parts.length) {
            last = parts[parts.length - 1];
            parts.splice(parts.length - 1, 1);
        }
        if (this.listElements.fullList && this.listElements.fullList.view.length > 0) {
            switch (this.field.field) {
                case 'organisms': {
                    this.selectedItems = this.listElements.fullList.view
                        .filter(item => {
                            if (item.speciesName) {
                                return parts.indexOf(item.speciesName.toLowerCase()) >= 0;
                            } else {
                                return parts.indexOf(item.taxId) >= 0;
                            }
                        });
                    const [fullMatch] = this.listElements.fullList.view
                        .filter(item => {
                            if (item.speciesName) {
                                return item.speciesName.toLowerCase() === last.toLowerCase() || item.taxId === last.toLowerCase();
                            } else {
                                return item.taxId === last.toLowerCase();
                            }
                        });
                    if (fullMatch) {
                        this.selectedItems.push(fullMatch);
                    }
                    break;
                }
            }
        }
        if (shouldUpdateScope) {
            this.scope.$apply();
        }
    }

    displayList() {
        if (this.hideListTimeout) {
            clearTimeout(this.hideListTimeout);
            this.hideListTimeout = null;
        }
        this.listIsDisplayed = true;
    }

    preventListFromClosing() {
        this._hideListIsPrevented = true;
        this.input.focus();
    }

    stopPreventListFromClosing() {
        this._hideListIsPrevented = false;
        this.input.focus();
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
        this.scope.$apply();
    }

    hideListDelayed() {
        if (this.hideListTimeout) {
            clearTimeout(this.hideListTimeout);
            this.hideListTimeout = null;
        }
        this.hideListTimeout = setTimeout(::this.hideList, 100);
    }

    itemIsSelected(item) {
        // TODO: make optional fn to determine display value text and refactor mappedSelectedItems and other dependant entries
        let mappedSelectedItems, selectedItem;
        switch (this.field.field) {
            case 'organisms': {
                selectedItem = item.speciesName || item.taxId.toString();
                mappedSelectedItems = this.selectedItems.map(item => item.speciesName || item.taxId);
                break;
            }
            default: {
                selectedItem = item;
                mappedSelectedItems = this.selectedItems;
            }
        }
        return mappedSelectedItems.filter(listItem => listItem.toLowerCase() === selectedItem.toLowerCase()).length > 0;
    }

    inputChanged() {
        const textParts = (this.displayText || '').split(',');
        const lastPart = textParts[textParts.length - 1].trim().toLowerCase();
        this.listElements.refreshList(lastPart);
    }

    didClickOnItem(item) {
        if (this.hideListTimeout) {
            clearTimeout(this.hideListTimeout);
            this.hideListTimeout = null;
        }
        this.input.focus();
        let mappedSelectedItems, selectedItem;
        switch (this.field.field) {
            case 'organisms': {
                selectedItem = item.speciesName || item.taxId.toString();
                mappedSelectedItems = this.selectedItems.map(item => item.speciesName || item.taxId);
                break;
            }
            default: {
                selectedItem = item;
                mappedSelectedItems = this.selectedItems;
            }
        }
        const index = mappedSelectedItems.indexOf(selectedItem);
        if (index >= 0) {
            mappedSelectedItems.splice(index, 1);
        } else {
            mappedSelectedItems.push(selectedItem);
        }
        this.selectedItems = mappedSelectedItems;
        if (this.selectedItems.length) {
            this.displayText = [...this.selectedItems, ''].join(', ');
        } else {
            this.displayText = '';
        }
        this.listElements.refreshList(null);
    }

    apply() {
        const parts = this.displayText.split(',').map(part => part.trim().toLowerCase());
        if (this.listElements.fullList && this.listElements.fullList.view.length > 0) {
            this.selectedItems = this.listElements.fullList.view
                .filter(item => {
                    if (item.speciesName) {
                        return parts.indexOf(item.speciesName.toLowerCase()) >= 0;
                    } else {
                        return parts.indexOf(item.taxId.toLowerCase()) >= 0;
                    }
                });
        } else {
            this.selectedItems = parts.filter(part => part !== '');
        }
        this.displayText = this.selectedItems.map(item => item.speciesName || item.taxId).join(', ');
        this.listIsDisplayed = false;
        if (!this.ngbInternalPathwaysTableService.canScheduleFilterInternalPathways()) {
            return;
        }
        switch (this.field.field) {
            case 'organisms': {
                const selectedItemsLowerCase = this.selectedItems.map(i => i.taxId.toString());
                const prevValue = (this.ngbInternalPathwaysTableService.internalPathwaysFilter.organisms || []);
                prevValue.sort();
                const prevValueStr = JSON.stringify(prevValue).toUpperCase();
                const currValue = this.ngbInternalPathwaysTableService.speciesList
                    .filter(organism => selectedItemsLowerCase.indexOf(organism.taxId.toString()) >= 0)
                    .map(organism => organism.taxId);
                currValue.sort();
                const currValueStr = JSON.stringify(currValue).toUpperCase();
                if (currValueStr !== prevValueStr) {
                    this.ngbInternalPathwaysTableService.internalPathwaysFilter.organisms = currValue;
                    this.ngbInternalPathwaysTableService.scheduleFilterInternalPathways();
                }
                break;
            }
        }
    }
}
