export default function run($mdDialog, dispatcher) {
    const displayWigColorSettingsCallback = ()=> {
        $mdDialog.show({
            template: require('./ngbWigColorPreference.dialog.tpl.html'),
            controller: function ($scope) {
                $scope.applyToCurrentTrack = true;
                $scope.close = () => $mdDialog.hide();
                $scope.save = () => $mdDialog.hide();
            },
            clickOutsideToClose: true,
        });
    };

    dispatcher.on('wig:color:configure', displayWigColorSettingsCallback);
}
