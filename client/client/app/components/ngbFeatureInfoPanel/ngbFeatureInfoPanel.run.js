export default function run($mdDialog, dispatcher) {


    const displayFeatureInfoCallback = (data)=> {
        $mdDialog.show({

            template: require('./ngbFeatureInfoPanelDlg.tpl.html'),
            controller: function ($scope) {
                $scope.properties = data.properties;
                $scope.referenceId = data.referenceId;
                $scope.chromosomeId = data.chromosomeId;
                $scope.startIndex = data.startIndex;
                $scope.endIndex = data.endIndex;
                $scope.read = data.read;
                if (data.read && data.read.name) {
                    $scope.name = data.read.name;
                }
                else if (data.name) {
                    $scope.name = data.name;
                }
                $scope.close = () => {
                    $mdDialog.hide();
                };
            },
            clickOutsideToClose: true
        });

    };

    dispatcher.on('feature:info:select', displayFeatureInfoCallback);

}