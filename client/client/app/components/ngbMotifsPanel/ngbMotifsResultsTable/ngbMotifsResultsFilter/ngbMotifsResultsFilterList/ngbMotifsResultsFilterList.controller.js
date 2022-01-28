import ListElements from './ngbFilterList.elements';

export default class ngbMotifsResultsFilterListController {

    listIsDisplayed = false;
    hideListTimeout = null;
    selectedItems = [];
    displayText = '';
    listElements = null;
    _hideListIsPrevented = false;

    get searchMotifFilter () {
        return this.ngbMotifsPanelService.searchMotifFilter;
    }

    static get UID() {
        return 'ngbMotifsResultsFilterListController';
    }

    get chromosomeType () {
        const currentSearchType = this.ngbMotifsPanelService.currentParams.searchType;
        const chromosomeType = this.ngbMotifsPanelService.chromosomeType;
        return ( currentSearchType === chromosomeType &&
            this.field.field === chromosomeType.toLowerCase());
    }

    constructor($scope, $element, dispatcher, projectContext, ngbMotifsPanelService) {
        Object.assign(this, {
            $scope,
            $element,
            dispatcher,
            projectContext,
            ngbMotifsPanelService
        });
        this.input = $element.find('.ngb-filter-input')[0];
        this.listElements = new ListElements(this.list,  {
            onSearchFinishedCallback: ::this.searchFinished
        });
        this.$scope.$watch('$ctrl.list', () => {
            this.listElements = new ListElements(this.list,  {
                onSearchFinishedCallback: ::this.searchFinished
            });
        });
        this.initializeFilters();
        const initializeFilters = this.initializeFilters.bind(this);
        this.dispatcher.on('initialize:motif:filters', initializeFilters);
        this.$scope.$on('$destroy', () => {
            this.dispatcher.removeListener('initialize:motif:filters', initializeFilters);
        });
    }

    initializeFilters () {
        if (this.searchMotifFilter) {
            switch (this.field.field) {
                case 'chromosome': {
                    if (this.searchMotifFilter.chromosome) {
                        this.selectedItems = this.projectContext.chromosomes
                            .filter(chr => this.searchMotifFilter.chromosome.indexOf(chr.name) >= 0)
                            .map(chr => chr.name);
                        this.displayText = [...this.selectedItems].join(', ');
                    }
                    break;
                }
                case 'strand': {
                    if (this.searchMotifFilter.strand) {
                        this.selectedItems = (this.searchMotifFilter.strand || []).map(t => t);
                        this.displayText = [...this.selectedItems].join(', ');
                    } else {
                        this.resetFilter();
                    }
                    break;
                }
                case 'gene': {
                    if (this.searchMotifFilter.gene) {
                        this.selectedItems = (this.searchMotifFilter.gene || []).map(t => t);
                        this.displayText = [...this.selectedItems].join(', ');
                    } else {
                        this.resetFilter();
                    }
                    break;
                }
            }
        } else {
            this.resetFilter();
        }
    }

    resetFilter () {
        this.selectedItems = [];
        this.displayText = '';
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
        this.$scope.$apply();
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
        const mutliple = ['strand'].includes(this.field.field);
        if (this.field.field === 'strand') {
            if (index >= 0) {
                this.selectedItems = [];
            } else {
                this.selectedItems = [item];
            }
        } else {
            if (index >= 0) {
                this.selectedItems.splice(index, 1);
            } else {
                this.selectedItems.push(item);
            }
        }
        if (this.selectedItems.length) {
            this.displayText = [...this.selectedItems, mutliple ? '' : false]
                .filter(Boolean)
                .join(', ');
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
        if (!this.ngbMotifsPanelService.canScheduleFilterResults()) {
            return;
        }
        switch (this.field.field) {
            case 'chromosome': {
                const selectedItemsLowerCase = this.selectedItems.map(i => i.toLowerCase());
                const prevValue = (this.searchMotifFilter || {}).chromosome || [];
                prevValue.sort();
                const prevValueStr = JSON.stringify(prevValue).toUpperCase();
                const currValue = this.projectContext.chromosomes
                    .filter(chr => selectedItemsLowerCase.indexOf(chr.name.toLowerCase()) >= 0)
                    .map(chr => chr.name);
                currValue.sort();
                const currValueStr = JSON.stringify(currValue).toUpperCase();
                if (currValueStr !== prevValueStr) {
                    this.ngbMotifsPanelService.setFilter('chromosome', currValue);
                    this.ngbMotifsPanelService.scheduleFilterResults();
                }
                break;
            }
            case 'strand': {
                this.selectedItems = (this.selectedItems || [])
                    .map(item => (item || '').trim())
                    .filter(item => /^(\+|\-)$/.test(item));
                const selectedStrands = [...(new Set(this.selectedItems))];
                if (selectedStrands.length >= 2) {
                    this.selectedItems = [];
                } else {
                    this.selectedItems = selectedStrands;
                }
                this.displayText = this.selectedItems.join(', ');
                const prevValue = (this.searchMotifFilter || {}).strand || [];
                const prevValueStr = JSON.stringify(prevValue);
                const currValue = (this.selectedItems || []).filter(item => (
                    this.listElements.fullList.includes(item)
                ));
                const currValueStr = JSON.stringify(currValue);
                if (currValueStr !== prevValueStr) {
                    this.ngbMotifsPanelService.setFilter('strand', currValue);
                    this.ngbMotifsPanelService.scheduleFilterResults();
                }
                break;
            }
            case 'gene': {
                const prevValue = (this.searchMotifFilter || {}).gene || [];
                prevValue.sort();
                const prevValueStr = JSON.stringify(prevValue).toUpperCase();
                const currValue = (this.selectedItems || []);
                currValue.sort();
                const currValueStr = JSON.stringify(currValue).toUpperCase();
                if (currValueStr !== prevValueStr) {
                    this.ngbMotifsPanelService.setFilter('gene', currValue);
                    this.ngbMotifsPanelService.scheduleFilterResults();
                }
                break;
            }
        }
        this.listElements.refreshList(null);
    }
}
