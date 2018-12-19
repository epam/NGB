import angular from 'angular';
import ngbPermissionsFormController from '../ngbPermissionsForm/ngbPermissionsForm.controller';

export default function($scope, $mdDialog, ngbDataSetContextMenu) {
    $scope.openPermissions = function (event) {
        event.preventDefault();
        event.stopPropagation();
        if (ngbDataSetContextMenu.visible()) {
            ngbDataSetContextMenu.close();
        }
        $mdDialog.show({
            clickOutsideToClose: true,
            controller: ngbPermissionsFormController,
            controllerAs: 'ctrl',
            locals: {
                node: $scope.node
            },
            parent: angular.element(document.body),
            skipHide: true,
            template: require('../ngbPermissionsForm/ngbPermissionsForm.template.html'),
        });
    };
}
