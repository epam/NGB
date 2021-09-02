export default function run($mdDialog, dispatcher, ngbMotifsPanelService) {
    const displayMotifsDialog = () => {
        $mdDialog.show({
            template: require('./ngbMotifsDialog.tpl.html'),
            controller: function ($scope) {
                $scope.chromosomeOnly = true;
                $scope.close = () => {
                    $mdDialog.hide();
                };
                $scope.search = () => {
                    const params = {
                        pattern: $scope.$ctrl.motif,
                        title: $scope.$ctrl.title || null,
                        chromosomeOnly: $scope.chromosomeOnly,
                    };
                    ngbMotifsPanelService.searchMotif(params);
                    $mdDialog.hide();
                };
                $scope.cancel = () => {
                    $mdDialog.hide();
                };
                $scope.change = () => {
                    $scope.chromosomeOnly = !$scope.chromosomeOnly;
                };
            },
            clickOutsideToClose: true
        });
    };
    dispatcher.on('reference:search:motifs:open', displayMotifsDialog);
}
