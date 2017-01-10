import Clipboard from 'clipboard';
import angular from 'angular';
import baseController from '../../../baseController';

export default class ngbShareLinkController extends baseController {
    static get UID() {
        return 'ngbShareLinkController';
    }

    events = {};

    constructor($scope, dispatcher,  $mdDialog) {
        super();

        Object.assign(this, {$scope, dispatcher,  $mdDialog});
    }


    onClick() {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ($scope, $mdDialog) => {
                new Clipboard('.copy-to-clipboard-button');
                $scope.copyLink = $mdDialog.cancel;
                $scope.close = $mdDialog.cancel;
            },
            parent: angular.element(document.body),
            template: require('./ngbShareLinkDialog.tpl.html')
        });
    }

}