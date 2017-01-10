export class ngbVariantDetailsController {

    _variant;
    _variantRequest;
    _dataService = null;
    _constants = null;

    /* @ngInject */
    constructor($scope, dataService, constants) {
        this._constants = constants;
        this._dataService = dataService;
    }

    get variant() { return this._variant; }
    set variant(variant) { this._variant = variant; this.INIT(); }
    get variantRequest() { return this._variantRequest; }
    set variantRequest(request) { this._variantRequest = request; this.INIT(); }
    get constants() { return this._constants; }

    getScopeObject(_scope_, objectName){
        if (_scope_.hasOwnProperty(objectName)){
            return _scope_[objectName];
        }
        else if (_scope_.$parent !== null){
            return this.getScopeObject(_scope_.$parent, objectName);
        }
        return undefined;
    }

    INIT(){

    }
}

export function capitalizeFilter() {
    return function (input) {
        return input ? input.charAt(0).toUpperCase() + input.substr(1).toLowerCase() : '';
    };
}