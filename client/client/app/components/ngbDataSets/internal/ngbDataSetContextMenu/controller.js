export default function($scope, $mdDialog) {
    $scope.openPermissions = function (event) {
        event.preventDefault();
        event.stopPropagation();
        $scope.closeContextMenu();
        // todo: open mdDialog
    };
}
