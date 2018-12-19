import angular from 'angular';
import ngbPermissionsFormController from '../ngbPermissionsForm/ngbPermissionsForm.controller';

export default function($scope, $mdDialog) {
    $scope.openPermissions = function (event) {
        event.preventDefault();
        event.stopPropagation();
        $scope.closeContextMenu();
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
