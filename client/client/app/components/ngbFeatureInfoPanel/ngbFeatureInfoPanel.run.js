export default function run($mdDialog, dispatcher, ngbFeatureInfoPanelService) {

    const displayFeatureInfoCallback = (data) => {
        $mdDialog.show({

            template: require('./ngbFeatureInfoPanelDlg.tpl.html'),
            controller: function ($scope) {
                $scope.editable = data.editable;
                $scope.properties = data.properties;
                $scope.referenceId = data.referenceId;
                $scope.chromosomeId = data.chromosomeId;
                $scope.startIndex = data.startIndex;
                $scope.endIndex = data.endIndex;
                $scope.geneId = data.geneId;
                $scope.read = data.read;
                $scope.fileId = data.fileId;
                $scope.feature = data.feature;
                $scope.uuid = data.uuid;
                if (data.read && data.read.name) {
                    $scope.name = data.read.name;
                } else if (data.name) {
                    $scope.name = data.name;
                }
                $scope.infoForRead = data.infoForRead;
                $scope.panelTitle = data.title;
                $scope.close = () => {
                    if (ngbFeatureInfoPanelService.editMode) {
                        return;
                    }
                    ngbFeatureInfoPanelService.editMode = false;
                    $mdDialog.hide();
                };
                $scope.disableCloseBtn = false;
            },
            clickOutsideToClose: false
        });

    };

    dispatcher.on('feature:info:select', displayFeatureInfoCallback);

}
