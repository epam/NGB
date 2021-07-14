const MAX_ITEMS_TO_DISPLAY = 100;
const Math = window.Math;

export default class ListElements {

    listFn;
    onSearchStartCallback;
    onSearchFinishedCallback;
    _isLoading = false;
    preLoadedList = [];
    fullList = null;
    token;

    constructor(listFn, callbacks) {
        this.listFn = listFn;
        if (listFn instanceof Array) {
            const list = listFn;
            this.listFn = (searchCriteria) => {
                if (!searchCriteria || !searchCriteria.length) {
                    return list;
                }
                return list.filter(item => item.toLowerCase().indexOf(searchCriteria.toLowerCase()) === 0);
            };
        }
        const {onSearchStartCallback, onSearchFinishedCallback} = callbacks;
        this.onSearchStartCallback = onSearchStartCallback;
        this.onSearchFinishedCallback = onSearchFinishedCallback;
        this.refreshList();
    }

    refreshList(searchString) {
        this._isLoading = true;
        const token = this.token = {};
        if (this.onSearchStartCallback) {
            this.onSearchStartCallback(searchString);
        }
        (async() => {
            let items;
            let shouldUpdateScope = true;
            if ((!searchString || !searchString.length) && this.fullList) {
                items = this.fullList;
                shouldUpdateScope = false;
            } else {
                if (!this.fullList || this.fullList.length === 0) {
                    items = await this.listFn(searchString);
                } else if (searchString && searchString.length > 0) {
                    if (this.fullList && this.fullList.length) {
                        items = this.fullList.filter(i => i.toLowerCase().indexOf(searchString.toLowerCase()) === 0);
                        shouldUpdateScope = false;
                    } else {
                        items = await this.listFn(searchString);
                        shouldUpdateScope = true;
                    }
                } else {
                    items = this.fullList;
                    shouldUpdateScope = false;
                }
                if (!searchString || !searchString.length) {
                    this.fullList = items;
                }
            }
            if (token === this.token) {
                this.token = null;
                this._isLoading = false;
                this.preLoadedList = [];
                if (items.length < MAX_ITEMS_TO_DISPLAY) {
                    this.preLoadedList = items;
                } else {
                    this.preLoadedList = [{
                        placeholder: true,
                        message: `Too much results (${items.length}). Top ${MAX_ITEMS_TO_DISPLAY} are shown`
                    }];
                    this.preLoadedList.push({divider: true});
                    for (let i = 0; i < Math.min(items.length, MAX_ITEMS_TO_DISPLAY); i++) {
                        this.preLoadedList.push(items[i]);
                    }
                }
                if (this.onSearchFinishedCallback) {
                    this.onSearchFinishedCallback(searchString, shouldUpdateScope);
                }
            }
        })();
    }

    get isLoading() {
        return this._isLoading;
    }

    getItemAtIndex(index) {
        if (index >= this.preLoadedList.length) {
            return null;
        }
        return this.preLoadedList[index];
    }

    getLength() {
        return this.preLoadedList.length;
    }

}
