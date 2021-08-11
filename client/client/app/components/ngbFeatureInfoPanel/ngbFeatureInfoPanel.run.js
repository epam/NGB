export default function run($mdDialog, dispatcher, ngbFeatureInfoPanelService) {
    const displayFeatureInfoCallback = (data) => {
        $mdDialog.show({

            template: require('./ngbFeatureInfoPanelDlg.tpl.html'),
            controller: function ($scope) {
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
                    if (ngbFeatureInfoPanelService.editMode &&
                        ngbFeatureInfoPanelService.unsavedChanges($scope.properties)
                    ) {
                        $mdDialog.show({
                            template: require('./ngbFeatureInfoConfirmCloseDlg.tpl.html'),
                            controller: function($scope, $mdDialog, dispatcher) {
                                $scope.yes = function () {
                                    dispatcher.emitSimpleEvent('feature:info:changes:cancel');
                                    $mdDialog.hide();
                                    $mdDialog.hide();
                                };
                                $scope.no = function () {
                                    $mdDialog.hide();
                                };
                                $scope.close = function () {
                                    $mdDialog.hide();
                                };
                            },
                            preserveScope: true,
                            autoWrap: true,
                            skipHide: true,
                        });
                        return;
                    }
                    ngbFeatureInfoPanelService.editMode = false;
                    $mdDialog.hide();
                };
            },
            clickOutsideToClose: false
        });

    };

    dispatcher.on('feature:info:select', displayFeatureInfoCallback);

}
