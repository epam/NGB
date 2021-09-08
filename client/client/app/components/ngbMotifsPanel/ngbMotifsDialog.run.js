export default function run($mdDialog, dispatcher, ngbMotifsPanelService) {
    const displayMotifsDialog = () => {
        $mdDialog.show({
            template: require('./ngbMotifsDialog.tpl.html'),
            controller: function ($scope) {
                $scope.inReference = false;
                $scope.close = () => {
                    $mdDialog.hide();
                };
                $scope.search = () => {
                    const params = {
                        pattern: $scope.$ctrl.motif,
                        title: $scope.$ctrl.title,
                        inReference: $scope.inReference,
                    };
                    ngbMotifsPanelService.searchMotif(params);
                    $mdDialog.hide();
                };
                $scope.cancel = () => {
                    $mdDialog.hide();
                };
                $scope.change = () => {
                    $scope.inReference = !$scope.inReference;
                };
            },
            clickOutsideToClose: true
        });
    };
    dispatcher.on('reference:search:motifs:open', displayMotifsDialog);
}
