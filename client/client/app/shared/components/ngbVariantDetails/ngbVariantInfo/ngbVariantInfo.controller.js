import {ngbVariantDetailsController} from '../ngbVariantDetails.controller';


export default class ngbVariantInfoController extends ngbVariantDetailsController{
    static get UID(){
        return 'ngbVariantInfoController';
    }

    isLoading = false;
    hasError = false;
    errorMessage = null;
    _variantInfoService = null;
    _variantInfo = null;
    _scope;

    /* @ngInject */
    constructor($scope, ngbVariantInfoService, vcfDataService, constants) {
        super($scope, vcfDataService, constants);
        this._scope = $scope;
        this._variantInfoService = ngbVariantInfoService;
        this.INIT();
    }

    get variantInfo() { return this._variantInfo; }
    set variantInfo(info) { this._variantInfo = info; }

    INIT(){
        const hasVariantRequest = !(this.variantRequest === undefined || this.variantRequest === null);

        this.isLoading = false;
        this.hasError = false;

        if (hasVariantRequest && this._variantInfoService !== null && this._variantInfoService !== undefined) {
            this.isLoading = true;
            this._variantInfoService
                .loadVariantInfo(this.variantRequest,
                    (variantInfo) => {
                        this.variantInfo = variantInfo;
                        this.isLoading = false;
                        if (this._scope !== null && this._scope !== undefined) {
                            this._scope.$apply();
                        }
                    },
                    this._handleError.bind(this));
        }
        else{
            if (this._constants !== null && this._constants !== undefined)
                this._handleError(this._constants.errorMessages.errorLoadingVariantInfo);
        }
    }

    
    _handleError(message){
        this.isLoading = false;
        this.hasError = true;
        this.errorMessage = message;
    }
}
