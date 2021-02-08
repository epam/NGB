import {ngbVariantDetailsController} from '../ngbVariantDetails.controller';


export default class ngbVariantDbSnpController extends ngbVariantDetailsController{
    static get UID(){
        return 'ngbVariantDbSnpController';
    }

    isLoading = false;
    hasError = false;
    errorMessage = null;
    _variantDbSnpService = null;
    _variantDBSnpInfo = null;
    _scope;
    _rsId;

    /* @ngInject */
    constructor($scope, ngbVariantDbSnpService, vcfDataService, constants) {
        super($scope, vcfDataService, constants);
        this._scope = $scope;
        this._variantDbSnpService = ngbVariantDbSnpService;
        this.INIT();
    }

    get rsId() { return this._rsId; }
    set rsId(rsId) { this._rsId = rsId; this.INIT(); }

    get variantDbSnp() { return this._variantDBSnpInfo; }
    set variantDbSnp(info) { this._variantDBSnpInfo = info; }

    INIT(){
        const hasRsId = !(this.rsId === undefined || this.rsId === null);

        this.isLoading = false;
        this.hasError = false;

        if (hasRsId && this._variantDbSnpService !== null && this._variantDbSnpService !== undefined) {
            this.isLoading = true;
            this._variantDbSnpService
                .loadVariantSnpInfo(this.rsId,
                    (variantSnpInfo) => {
                        this.variantDbSnp = variantSnpInfo;

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
