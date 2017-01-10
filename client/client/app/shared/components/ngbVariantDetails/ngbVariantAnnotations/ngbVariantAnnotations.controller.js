import {ngbVariantDetailsController} from '../ngbVariantDetails.controller';

export default class ngbVariantAnnotationsController extends ngbVariantDetailsController{
    static get UID(){
        return 'ngbVariantAnnotationsController';
    }

    annotationsNotFoundMessage = null;

    _variantAnnotations = null;

    /* @ngInject */
    constructor($scope, vcfDataService, constants) {
        super($scope, vcfDataService, constants);
        this.annotationsNotFoundMessage = this._constants.errorMessages.variantAnnotationsNotFound;
        this.isLoading = false;
        this.INIT();
    }

    get variantAnnotations() { return this._variantAnnotations; }
    set variantAnnotations(annotations) { this._variantAnnotations = annotations; }

    INIT(){
        this.variantAnnotations = [];
        // [
        //     {
        //         databaseName: 'dbStructural',
        //         variantImpact: 'high'
        //     },
        //     {
        //         databaseName: 'ClinVar',
        //         variantImpact: 'moderate'
        //     },
        //     {
        //         databaseName: 'dbStructural',
        //         variantImpact: 'low'
        //     }
        //
        // ];
    }

}
