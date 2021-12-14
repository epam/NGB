import ListElements from './../../../../../shared/filter/filterList/ngbFilterList.elements';

export default class ngbVariantsFilterListController {
    static get UID() {
        return 'ngbVariantsFilterListController';
    }

    projectContext;
    listIsDisplayed = false;
    hideListTimeout = null;
    scope;
    input;
    selectedItems = [];
    displayText = '';

    listElements = null;

    constructor($scope, $element, projectContext) {
        this.scope = $scope;
        this.projectContext = projectContext;
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
            case 'variationType': {
                if (this.projectContext.vcfFilter.selectedVcfTypes) {
                    this.selectedItems = (this.projectContext.vcfFilter.selectedVcfTypes || []).map(t => t);
                    this.displayText = [...this.selectedItems].join(', ');
                }
            }
                break;
            case 'chrName': {
                if (this.projectContext.vcfFilter.chromosomeIds) {
                    this.selectedItems = this.projectContext.chromosomes.filter(chr => this.projectContext.vcfFilter.chromosomeIds.indexOf(chr.id) >= 0).map(chr => chr.name);
                    this.displayText = [...this.selectedItems].join(', ');
                }
            }
                break;
            case 'geneNames': {
                if (this.projectContext.vcfFilter.selectedGenes) {
                    this.selectedItems = (this.projectContext.vcfFilter.selectedGenes || []).map(t => t);
                    this.displayText = [...this.selectedItems].join(', ');
                }
            }
                break;
            case 'sampleNames': {
                if (this.projectContext.vcfFilter.sampleNames) {
                    this.selectedItems = (this.projectContext.vcfFilter.sampleNames || []).map(t => t);
                    this.displayText = [...this.selectedItems].join(', ');
                }
            }
                break;
        }
    }

    searchFinished(searchString, shouldUpdateScope) {
        const parts = this.displayText.split(',').map(part => part.trim().toLowerCase());
        let last = '';
        if (parts.length) {
            last = parts[parts.length - 1];
            parts.splice(parts.length - 1, 1);
        }
        if (this.listElements.fullList && this.listElements.fullList.length > 0) {
            this.selectedItems = this.listElements.fullList.filter(item => parts.indexOf(item.toLowerCase()) >= 0);
            const [fullMatch] = this.listElements.fullList.filter(item => item.toLowerCase() === last.toLowerCase());
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

    _hideListIsPrevented = false;

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
        const parts = this.displayText.split(',').map(part => part.trim());
        if (this.listElements.fullList && this.listElements.length > 0) {
            const lowerCasedParts = parts.map(o => o.toLowerCase());
            this.selectedItems = this.listElements.fullList
                .filter(item => lowerCasedParts.indexOf(item.toLowerCase()) >= 0);
        } else {
            this.selectedItems = parts.filter(part => part !== '');
        }
        this.displayText = this.selectedItems.join(', ');
        this.listIsDisplayed = false;
        if (!this.projectContext.canScheduleFilterVariants()) {
            return;
        }
        switch (this.field.field) {
            case 'variationType': {
                const prevValue = (this.projectContext.vcfFilter.selectedVcfTypes || []);
                prevValue.sort();
                const prevValueStr = JSON.stringify(prevValue).toUpperCase();
                const currValue = (this.selectedItems || []);
                currValue.sort();
                const currValueStr = JSON.stringify(currValue).toUpperCase();
                if (currValueStr !== prevValueStr) {
                    this.projectContext.vcfFilter.selectedVcfTypes = this.selectedItems.map(i => i.toUpperCase());
                    this.projectContext.scheduleFilterVariants();
                }
            }
                break;
            case 'chrName': {
                const selectedItemsLowerCase = this.selectedItems.map(i => i.toLowerCase());
                const prevValue = (this.projectContext.vcfFilter.chromosomeIds || []);
                prevValue.sort();
                const prevValueStr = JSON.stringify(prevValue).toUpperCase();
                const currValue = this.projectContext.chromosomes.filter(chr => selectedItemsLowerCase.indexOf(chr.name.toLowerCase()) >= 0).map(chr => chr.id);
                currValue.sort();
                const currValueStr = JSON.stringify(currValue).toUpperCase();
                if (currValueStr !== prevValueStr) {
                    this.projectContext.vcfFilter.chromosomeIds = currValue;
                    this.projectContext.scheduleFilterVariants();
                }
            }
                break;
            case 'geneNames': {
                const prevValue = (this.projectContext.vcfFilter.selectedGenes || []);
                prevValue.sort();
                const prevValueStr = JSON.stringify(prevValue).toUpperCase();
                const currValue = (this.selectedItems || []);
                currValue.sort();
                const currValueStr = JSON.stringify(currValue).toUpperCase();
                if (currValueStr !== prevValueStr) {
                    this.projectContext.vcfFilter.selectedGenes = this.selectedItems;
                    this.projectContext.scheduleFilterVariants();
                }
            }
                break;
            case 'sampleNames': {
                const prevValue = (this.projectContext.vcfFilter.sampleNames || []);
                prevValue.sort();
                const prevValueStr = JSON.stringify(prevValue).toUpperCase();
                const currValue = (this.selectedItems || []);
                currValue.sort();
                const currValueStr = JSON.stringify(currValue).toUpperCase();
                if (currValueStr !== prevValueStr) {
                    this.projectContext.vcfFilter.sampleNames = this.selectedItems;
                    this.projectContext.scheduleFilterVariants();
                }
            }
                break;
        }
    }
}
