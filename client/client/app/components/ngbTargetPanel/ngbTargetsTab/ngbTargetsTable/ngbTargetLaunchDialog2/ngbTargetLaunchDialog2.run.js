export default function run(
    $mdDialog,
    $timeout,
    dispatcher,
) {
    const displayLaunchDialog = async (target) => {
        $mdDialog.show({
            template: require('./ngbTargetLaunchDialog2.tpl.html'),
            controller: function ($scope) {
                $scope.name = target.name;

                $scope.identifyDisabled = () => (
                    false
                );

                $scope.identify = () => console.log('identify');

                $scope.close = () => {
                    $mdDialog.hide();
                };
                
            },
            clickOutsideToClose: true
        });
    };
    dispatcher.on('target:launch:identification', displayLaunchDialog);
}
