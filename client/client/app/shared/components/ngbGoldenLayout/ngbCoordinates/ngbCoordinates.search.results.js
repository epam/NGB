export default class SearchResults {

    _items;
    _hints;

    constructor(hints, items) {
        const mapHintFn = function(hint) {
            return {
                hint,
                __type__: 'hint'
            };
        };
        const mapItemFn = function(item) {
            item.__type__ = 'item';
            return item;
        };
        this._hints = (hints || []).map(mapHintFn);
        this._items = (items || []).map(mapItemFn);
    }

    getItemAtIndex(index) {
        let result = null;
        if (index < this._hints.length) {
            result = this._hints[index];
        } else {
            result = this._items[index - this._hints.length];
        }
        return result;
    }

    getLength() {
        return this._items.length + this._hints.length;
    }

}