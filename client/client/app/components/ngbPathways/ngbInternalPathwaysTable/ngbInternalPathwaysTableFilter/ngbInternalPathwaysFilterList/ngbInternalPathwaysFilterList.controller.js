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
                        .filter(organism => this.ngbInternalPathwaysTableService.internalPathwaysFilter.organisms.indexOf(organism.taxId) >= 0)
                        .map(organism => organism.speciesName);
                    this.displayText = [...this.selectedItems].join(', ');
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
        if (this.listElements.fullList && this.listElements.fullList.model.length > 0) {
            this.selectedItems = this.listElements.fullList.model.filter(item => parts.indexOf(item.toLowerCase()) >= 0);
            const [fullMatch] = this.listElements.fullList.model.filter(item => item.toLowerCase() === last.toLowerCase());
            if (fullMatch) {
                this.selectedItems.push(fullMatch);
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
        return this.selectedItems.filter(listItem => listItem.toLowerCase() === item.toLowerCase()).length > 0;
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
        const index = this.selectedItems.indexOf(item);
        if (index >= 0) {
            this.selectedItems.splice(index, 1);
        } else {
            this.selectedItems.push(item);
        }
        if (this.selectedItems.length) {
            this.displayText = [...this.selectedItems, ''].join(', ');
        } else {
            this.displayText = '';
        }
        this.listElements.refreshList(null);
    }

    apply() {
        const parts = this.displayText.split(',').map(part => part.trim().toLowerCase());
        if (this.listElements.fullList && this.listElements.length > 0) {
            this.selectedItems = this.listElements.fullList.model.filter(item => parts.indexOf(item.toLowerCase()) >= 0);
        } else {
            this.selectedItems = parts.filter(part => part !== '');
        }
        this.displayText = this.selectedItems.join(', ');
        this.listIsDisplayed = false;
        if (!this.ngbInternalPathwaysTableService.canScheduleFilterInternalPathways()) {
            return;
        }
        switch (this.field.field) {
            case 'organisms': {
                const selectedItemsLowerCase = this.selectedItems.map(i => i.toLowerCase());
                const prevValue = (this.ngbInternalPathwaysTableService.internalPathwaysFilter.organisms || []);
                prevValue.sort();
                const prevValueStr = JSON.stringify(prevValue).toUpperCase();
                const currValue = this.ngbInternalPathwaysTableService.speciesList
                    .filter(organism => organism.speciesName
                        ? selectedItemsLowerCase.indexOf(organism.speciesName.toLowerCase()) >= 0
                        : selectedItemsLowerCase.indexOf(organism.taxId.toString()) >= 0
                    )
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
