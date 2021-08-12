export default function run($mdDialog, dispatcher, projectContext, ngbMotifsPanelService) {
    const displayMotifsDialog = () => {
        $mdDialog.show({
            template: require('./ngbMotifsDialog.tpl.html'),
            controller: function ($scope) {
                $scope.isSearchInCurrent = ngbMotifsPanelService.searchInCurrent;
                $scope.close = () => {
                    $mdDialog.hide();
                };
                $scope.search = () => {
                    const request = {
                        pattern: $scope.$ctrl.motif,
                        title: $scope.$ctrl.title || null,
                        inCurrent: $scope.isSearchInCurrent,
                        reference: projectContext._reference
                    };
                    ngbMotifsPanelService.motifsRequest(request);
                    $mdDialog.hide();
                };
                $scope.cancel = () => {
                    $mdDialog.hide();
                };
                $scope.change = () => {
                    $scope.isSearchInCurrent = !$scope.isSearchInCurrent;
                };
            },
            clickOutsideToClose: true
        });
    };
    dispatcher.on('reference:search:motifs:open', displayMotifsDialog);
}
