export default function run($mdDialog, dispatcher) {
    const displayWholeGenomeViewCallback = () => {
        $mdDialog.show({
            template: require('./ngbBlastWholeGenomeView.dialog.tpl.html'),
            controller: function ($scope) {
                $scope.close = () => {
                    $mdDialog.hide();
                };
            },
            clickOutsideToClose: true,
        });
    };
    dispatcher.on('blast:whole:genome:view', displayWholeGenomeViewCallback);
}