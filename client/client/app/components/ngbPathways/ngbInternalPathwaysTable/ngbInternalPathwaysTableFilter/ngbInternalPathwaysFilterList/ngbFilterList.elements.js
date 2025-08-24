import {deepSearch} from '../../../../../shared/utils/deepSearch';

const MAX_ITEMS_TO_DISPLAY = 100;
const Math = window.Math;

export default class ListElements {

    listFn;
    onSearchStartCallback;
    onSearchFinishedCallback;
    preLoadedList = {
        model: [],
        view: []
    };
    fullList = null;
    token;

    constructor(listFn, callbacks) {
        this.listFn = listFn;
        let list;
        if (listFn instanceof Array) {
            list = listFn;
            this.listFn = (searchCriteria) => {
                if (!searchCriteria || !searchCriteria.length) {
                    return {
                        model: list,
                        view: list
                    };
                }
                const filteredList = list.filter(item => item.toLowerCase().indexOf(searchCriteria.toLowerCase()) === 0);
                return {
                    model: filteredList,
                    view: filteredList
                };
            };
        } else if(listFn.hasOwnProperty('model')) {
            list = {
                model: listFn.model
            };
            if (listFn.hasOwnProperty('view')) {
                list.view = listFn.view;
            } else {
                list.view = listFn.model;
            }
            this.listFn = (searchCriteria) => {
                if (!searchCriteria || !searchCriteria.length) {
                    return {
                        model: list,
                        view: list
                    };
                }
                const result = {
                    model: [],
                    view: []
                };
                list.model.forEach((item, index) => {
                    if (item.toLowerCase().indexOf(searchCriteria.toLowerCase()) === 0) {
                        result.model.push(item);
                        result.view.push(list.view[index]);
                    }
                });
                return result;
            };
        }
        const {onSearchStartCallback, onSearchFinishedCallback} = callbacks;
        this.onSearchStartCallback = onSearchStartCallback;
        this.onSearchFinishedCallback = onSearchFinishedCallback;
        this.refreshList();
    }

    _isLoading = false;

    get isLoading() {
        return this._isLoading;
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
                if (!this.fullList || this.fullList.model.length === 0) {
                    items = await this.listFn(searchString);
                } else if (searchString && searchString.length > 0) {
                    if (this.fullList && this.fullList.model.length) {
                        items = {
                            model: [],
                            view: []
                        };
                        this.fullList.model.forEach((model, index) => {
                            if (model.toLowerCase().indexOf(searchString.toLowerCase()) === 0
                                || deepSearch(this.fullList.view[index], searchString.toLowerCase(), [])) {
                                items.model.push(model);
                                items.view.push(this.fullList.view[index]);
                            }
                        });
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
                this.preLoadedList = {
                    model: [],
                    view: []
                };
                if (items.model.length < MAX_ITEMS_TO_DISPLAY) {
                    this.preLoadedList.model = items.model;
                    this.preLoadedList.view = items.view;
                } else {
                    this.preLoadedList = {
                        model: [{
                            placeholder: true,
                            message: `Too much results (${items.model.length}). Top ${MAX_ITEMS_TO_DISPLAY} are shown`
                        }],
                        view: [{placeholder: true}]
                    };
                    this.preLoadedList.model.push({divider: true});
                    this.preLoadedList.view.push({divider: true});
                    for (let i = 0; i < Math.min(items.model.length, MAX_ITEMS_TO_DISPLAY); i++) {
                        this.preLoadedList.model.push(items.model[i]);
                        this.preLoadedList.view.push(items.view[i]);
                    }
                }
                if (this.onSearchFinishedCallback) {
                    this.onSearchFinishedCallback(searchString, shouldUpdateScope);
                }
            }
        })();
    }

    getItemAtIndex(index) {
        if (index >= this.preLoadedList.model.length) {
            return null;
        }
        return this.preLoadedList.model[index];
    }

    getLength() {
        return this.preLoadedList.model.length;
    }

}
