import {ngbVariantDetailsController} from '../../shared/components/ngbVariantDetails/ngbVariantDetails.controller';

export default class ngbVariantPanelController extends ngbVariantDetailsController{
    static get UID() {
        return 'ngbVariantPanelController';
    }

    _rsId = null;

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

    get rsId() {
        let ids = this.variant.id.split(/[;,|\s]/)
                                .filter(
                                    id => id.match(/^rs\d+$/)
                                );

        if (ids[0]) {
            this._rsId = ids[0];
        } else {
            this._rsId = null;
        }

        return this._rsId;
    }

}