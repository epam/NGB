import ListElements from './ngbFilterList.elements';

export default class ngbGenesFilterListController {
    projectContext;
    listIsDisplayed = false;
    hideListTimeout = null;
    scope;
    input;
    selectedItems = [];
    displayText = '';
    listElements = null;
    _hideListIsPrevented = false;
    defaultPrefix = '';

    constructor($scope, $element, ngbGenesTableService, projectContext) {
        this.scope = $scope;
        this.ngbGenesTableService = ngbGenesTableService;
        this.projectContext = projectContext;
        this.defaultPrefix = this.ngbGenesTableService.defaultPrefix;
        this.input = $element.find('.ngb-filter-input')[0];
        this.listElements = new ListElements(this.list, {
            onSearchFinishedCallback: ::this.searchFinished
        });
        this.scope.$watch('$ctrl.list', () => {
            this.listElements = new ListElements(this.list, {
                onSearchFinishedCallback: ::this.searchFinished
            });
        });
        switch (this.field.field) {
            case `${this.defaultPrefix}chromosome`: {
                if (this.ngbGenesTableService.genesFilter[`${this.defaultPrefix}chromosome`]) {
                    this.selectedItems = this.projectContext.chromosomes
                        .filter(chr => this.ngbGenesTableService.genesFilter[`${this.defaultPrefix}chromosome`].indexOf(chr.id) >= 0)
                        .map(chr => chr.name);
                    this.displayText = [...this.selectedItems].join(', ');
                }
                break;
            }
            case `${this.defaultPrefix}featureName`: {
                if (this.ngbGenesTableService.genesFilter[`${this.defaultPrefix}featureName`]) {
                    this.selectedItems = (this.ngbGenesTableService.genesFilter[`${this.defaultPrefix}featureName`] || []).map(t => t);
                    this.displayText = [...this.selectedItems].join(', ');
                }
                break;
            }
            case `${this.defaultPrefix}featureType`: {
                if (this.ngbGenesTableService.genesFilter[`${this.defaultPrefix}featureType`]) {
                    this.selectedItems = (this.ngbGenesTableService.genesFilter[`${this.defaultPrefix}featureType`] || []).map(t => t);
                    this.displayText = [...this.selectedItems].join(', ');
                }
                break;
            }
        }
    }

    static get UID() {
        return 'ngbGenesFilterListController';
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
        if (!this.ngbGenesTableService.canScheduleFilterGenes()) {
            return;
        }
        switch (this.field.field) {
            case `${this.defaultPrefix}chromosome`: {
                const selectedItemsLowerCase = this.selectedItems.map(i => i.toLowerCase());
                const prevValue = (this.ngbGenesTableService.genesFilter[`${this.defaultPrefix}chromosome`] || []);
                prevValue.sort();
                const prevValueStr = JSON.stringify(prevValue).toUpperCase();
                const currValue = this.projectContext.chromosomes
                    .filter(chr => selectedItemsLowerCase.indexOf(chr.name.toLowerCase()) >= 0).map(chr => chr.id);
                currValue.sort();
                const currValueStr = JSON.stringify(currValue).toUpperCase();
                if (currValueStr !== prevValueStr) {
                    this.ngbGenesTableService.genesFilter[`${this.defaultPrefix}chromosome`] = currValue;
                    this.ngbGenesTableService.scheduleFilterGenes();
                }
                break;
            }
            case `${this.defaultPrefix}featureName`: {
                const prevValue = (this.ngbGenesTableService.genesFilter[`${this.defaultPrefix}featureName`] || []);
                prevValue.sort();
                const prevValueStr = JSON.stringify(prevValue).toUpperCase();
                const currValue = (this.selectedItems || []);
                currValue.sort();
                const currValueStr = JSON.stringify(currValue).toUpperCase();
                if (currValueStr !== prevValueStr) {
                    this.ngbGenesTableService.genesFilter[`${this.defaultPrefix}featureName`] = currValue;
                    this.ngbGenesTableService.scheduleFilterGenes();
                }
                break;
            }
            case `${this.defaultPrefix}featureType`: {
                const prevValue = (this.ngbGenesTableService.genesFilter[`${this.defaultPrefix}featureType`] || []);
                prevValue.sort();
                const prevValueStr = JSON.stringify(prevValue).toUpperCase();
                const currValue = (this.selectedItems || []);
                currValue.sort();
                const currValueStr = JSON.stringify(currValue).toUpperCase();
                if (currValueStr !== prevValueStr) {
                    this.ngbGenesTableService.genesFilter[`${this.defaultPrefix}featureType`] = currValue;
                    this.ngbGenesTableService.scheduleFilterGenes();
                }
                break;
            }
        }
    }
}
