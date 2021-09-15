import ListElements from './ngbFilterList.elements';

export default class ngbBookmarksFilterListController {
    listIsDisplayed = false;
    hideListTimeout = null;
    input;
    selectedItems = [];
    displayText = '';
    listElements = null;
    _hideListIsPrevented = false;

    constructor($scope, dispatcher, $element, ngbBookmarksTableService, projectContext) {
        Object.assign(
            this,
            {$scope, dispatcher, ngbBookmarksTableService, projectContext}
        );
        this.input = $element.find('.ngb-filter-input')[0];
    }

    static get UID() {
        return 'ngbBookmarksFilterListController';
    }

    $onChanges(changes) {
        if (!!changes.list && changes.list.currentValue
            && (changes.list.previousValue !== changes.list.currentValue)) {
            this.onListChanged(changes.list.currentValue);
        }
    }

    onListChanged(newListValue) {
        this.listElements = new ListElements(newListValue, {
            onSearchFinishedCallback: ::this.searchFinished
        });
        switch (this.field.field) {
            case 'chromosome.name': {
                if (this.ngbBookmarksTableService.bookmarksFilter.chromosome) {
                    this.selectedItems = [];
                    if (newListValue && newListValue.view) {
                        newListValue.view.forEach(item => {
                            const filterIndex = this.ngbBookmarksTableService.bookmarksFilter.chromosome.indexOf(item.id);
                            if (~filterIndex) {
                                this.selectedItems.push(item);
                            }
                        });
                    }
                    this.displayText = [...this.selectedItems.map(i => i.name)].join(', ');
                }
                break;
            }
            case 'reference.name': {
                if (this.ngbBookmarksTableService.bookmarksFilter.reference) {
                    this.selectedItems = this.projectContext.references
                        .filter(ref => this.ngbBookmarksTableService.bookmarksFilter.reference.indexOf(ref.id) >= 0)
                        .map(ref => ref.name);
                    this.displayText = [...this.selectedItems].join(', ');
                }
                break;
            }
        }
    }

    searchFinished(searchString, shouldUpdateScope) {
        const parts = this.displayText.split(',').map(part => part.trim().toLowerCase());
        let last = '';
        if (parts.length) {
            last = parts[parts.length - 1];
            parts.splice(parts.length - 1, 1);
        }
        if (this.listElements.fullList && this.listElements.fullList.model.length > 0) {
            switch (this.field.field) {
                case 'chromosome.name': {
                    this.selectedItems = [];
                    this.listElements.fullList.model.forEach((item, index) => {
                        if (parts.indexOf(item.toLowerCase()) >= 0 || item.toLowerCase() === last.toLowerCase()) {
                            this.selectedItems.push(this.listElements.fullList.view[index]);
                        }
                    });
                    break;
                }
                default: {
                    this.selectedItems = this.listElements.fullList.model.filter(item => parts.indexOf(item.toLowerCase()) >= 0);
                    const [fullMatch] = this.listElements.fullList.model.filter(item => item.toLowerCase() === last.toLowerCase());
                    if (fullMatch) {
                        this.selectedItems.push(fullMatch);
                    }
                    break;
                }
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
        let result;
        switch (this.field.field) {
            case 'chromosome.name': {
                result = this.selectedItems.filter(listItem => listItem.name.toLowerCase() === item.toLowerCase()).length > 0;
                break;
            }
            default: {
                result = this.selectedItems.filter(listItem => listItem.toLowerCase() === item.toLowerCase()).length > 0;
            }
        }
        return result;
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
        let index;
        switch (this.field.field) {
            case 'chromosome.name': {
                index = this.selectedItems.findIndex(i => i.name === item.name && i.refName === item.refName);
                break;
            }
            default: {
                index = this.selectedItems.indexOf(item.name);
            }
        }
        if (~index) {
            this.selectedItems.splice(index, 1);
        } else {
            this.selectedItems.push(item);
        }
        if (this.selectedItems.length) {
            this.displayText = [...this.selectedItems.map(i => i.name), ''].join(', ');
        } else {
            this.displayText = '';
        }
        this.listElements.refreshList(null);
    }

    apply() {
        const parts = this.displayText.split(',').map(part => part.trim().toLowerCase());
        switch (this.field.field) {
            case 'chromosome.name': {
                if (this.listElements.fullList && this.listElements.length > 0) {
                    this.listElements.fullList.model.forEach((item, index) => {
                        if (parts.indexOf(item.toLowerCase()) >= 0) {
                            this.selectedItems.push(this.listElements.fullList.view[index]);
                        }
                    });
                }
                this.displayText = this.selectedItems.map(i => i.name).join(', ');
                break;
            }
            default: {
                if (this.listElements.fullList && this.listElements.length > 0) {
                    this.selectedItems = this.listElements.fullList.model.filter(item => parts.indexOf(item.toLowerCase()) >= 0);
                } else {
                    this.selectedItems = parts.filter(part => part !== '');
                }
                this.displayText = this.selectedItems.join(', ');
            }
        }
        this.listIsDisplayed = false;
        if (!this.ngbBookmarksTableService.canScheduleFilterBookmarks()) {
            return;
        }
        switch (this.field.field) {
            case 'chromosome.name': {
                const prevValue = this.ngbBookmarksTableService.bookmarksFilter.chromosome || [];
                prevValue.sort();
                const prevValueStr = JSON.stringify(prevValue).toUpperCase();
                const currValue = [];
                this.selectedItems.forEach(item => {
                    const index = this.listElements.fullList.view.findIndex(viewItem =>
                        viewItem.name === item.name && viewItem.refName === item.refName);
                    if (~index) {
                        currValue.push(this.listElements.fullList.view[index].id);
                    }
                });
                currValue.sort();
                const currValueStr = JSON.stringify(currValue).toUpperCase();
                if (currValueStr !== prevValueStr) {
                    this.ngbBookmarksTableService.bookmarksFilter.chromosome = currValue;
                    this.ngbBookmarksTableService.scheduleFilterBookmarks();
                }
                break;
            }
            case 'reference.name': {
                const selectedItemsLowerCase = this.selectedItems.map(i => i.toLowerCase());
                const prevValue = this.ngbBookmarksTableService.bookmarksFilter.reference || [];
                prevValue.sort();
                const prevValueStr = JSON.stringify(prevValue).toUpperCase();
                const currValue = this.projectContext.references
                    .filter(ref => selectedItemsLowerCase.indexOf(ref.name.toLowerCase()) >= 0).map(ref => ref.id);
                currValue.sort();
                const currValueStr = JSON.stringify(currValue).toUpperCase();
                if (currValueStr !== prevValueStr) {
                    this.ngbBookmarksTableService.bookmarksFilter.reference = currValue;
                    this.ngbBookmarksTableService.scheduleFilterBookmarks();
                }
                this.dispatcher.emit('bookmarks:filter:change', {
                    key: this.field.field,
                    value: this.ngbBookmarksTableService.bookmarksFilter.reference
                });
                break;
            }
        }
    }
}
