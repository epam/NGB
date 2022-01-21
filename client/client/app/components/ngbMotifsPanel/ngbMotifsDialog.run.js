export default function run($mdDialog, dispatcher, ngbMotifsPanelService) {
    const displayMotifsDialog = async (data) => {
        let sequence;
        if (data) {
            sequence = await data.getSequence()
                .then(sequence => sequence);
        }
        $mdDialog.show({
            template: require('./ngbMotifsDialog.tpl.html'),
            controller: function ($scope) {
                $scope.sequence = sequence || '';
                $scope.inReference = false;
                $scope.close = () => {
                    $mdDialog.hide();
                };
                $scope.search = () => {
                    const params = {
                        pattern: $scope.sequence,
                        title: $scope.title,
                        inReference: $scope.inReference,
                    };
                    ngbMotifsPanelService.panelAddMotifsPanel(params);
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
    dispatcher.on('search:motifs:open', displayMotifsDialog);
}
