function sortProperties(a, b) {
    return a[0] > b[0] ? 1 : -1;
}
function extractProperties(o, except = []){
    return Object.entries(o || {})
    .map(([key, value]) => {
        if (
            o.hasOwnProperty(key) &&
            typeof o[key] !== 'object' &&
            except.indexOf(key) === -1 &&
            o[key] !== undefined
        ) {
            return [key, value, false];
        }
        return undefined;
    })
    .filter(Boolean)
    .sort(sortProperties);
}

export default function run($mdDialog, dispatcher, ngbFeatureInfoPanelService) {
    const displayFeatureInfoCallback = async (data) => {
        let result;
        let formattedResult;
        const {
            fileId,
            uuid,
            chromosomeId,
            seqName,
            referenceId
         } = data;
        if (uuid && fileId) {
            result = await ngbFeatureInfoPanelService.getGeneInfo(fileId, uuid);
            formattedResult = (result && !result.error) ? {
                projectId: undefined,
                chromosomeId,
                startIndex: result.startIndex,
                endIndex: result.endIndex,
                name: result.featureName,
                geneId: result.featureId,
                properties: [
                    ['chromosome', seqName, false],
                    ['start', result.startIndex, false],
                    ['end', result.endIndex, false],
                    ...extractProperties(result, [
                        'start',
                        'end',
                        'chromosome',
                        'startIndex',
                        'endIndex'
                    ]),
                    ...Object
                    .entries(result.attributes || {})
                    .map(([key, value]) => ([
                        key, value, true
                    ]))
                    .sort(sortProperties)
                ],
                referenceId,
                title: result.feature,
                fileId,
                feature: result,
                uuid
            } : null;
        } else {
            result = data;
            formattedResult = data;
        }

        $mdDialog.show({
            template: require('./ngbFeatureInfoPanelDlg.tpl.html'),
            controller: function ($scope) {
                if (formattedResult) {
                    $scope.properties = formattedResult.properties;
                    $scope.referenceId = formattedResult.referenceId;
                    $scope.chromosomeId = formattedResult.chromosomeId;
                    $scope.startIndex = formattedResult.startIndex;
                    $scope.endIndex = formattedResult.endIndex;
                    $scope.geneId = formattedResult.geneId;
                    $scope.read = formattedResult.read;
                    $scope.fileId = formattedResult.fileId;
                    $scope.feature = formattedResult.feature;
                    $scope.uuid = formattedResult.uuid;
                    if (formattedResult.read && formattedResult.read.name) {
                        $scope.name = formattedResult.read.name;
                    } else if (formattedResult.name) {
                        $scope.name = formattedResult.name;
                    }
                    $scope.infoForRead = formattedResult.infoForRead;
                    $scope.panelTitle = formattedResult.title;
                } else {
                    $scope.error = 'Network error';
                }
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
