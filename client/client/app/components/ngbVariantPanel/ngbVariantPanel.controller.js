import {ngbVariantDetailsController} from '../../shared/components/ngbVariantDetails/ngbVariantDetails.controller';

export default class ngbVariantPanelController extends ngbVariantDetailsController{
    static get UID() {
        return 'ngbVariantPanelController';
    }

    /* @ngInject */
    constructor($scope, $mdDialog) {
        super($scope);
        this.variant = this.getScopeObject($scope, 'variant');
        this.variantRequest = this.getScopeObject($scope, 'variantRequest');
        this.$mdDialog = $mdDialog;
    }

    close() {
        this.$mdDialog.hide();
    }

}