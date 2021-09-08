export default function run($mdDialog, dispatcher, ngbHeatmapColorSchemePreferenceConstants) {
    const displayHeatmapColorSchemePreferenceCallback = (arg) => {
        const {
            config = {}
        } = arg || {};
        const {
            id,
            scheme
        } = config;
        $mdDialog.show({
            clickOutsideToClose: true,
            controller: function ($scope) {
                $scope.scheme = scheme;
                $scope.schemeForm = {};
                $scope.constants = ngbHeatmapColorSchemePreferenceConstants;
                $scope.close = function () {
                    $mdDialog.hide();
                };
                $scope.save = function () {
                    dispatcher.emit(`heatmap:colorscheme:configure:done:${id}`, $scope.scheme.copy({
                        colorFormat: ngbHeatmapColorSchemePreferenceConstants.colorFormats.number
                    }));
                    $scope.scheme = null;
                    $mdDialog.hide();
                };
            },
            template: require('./ngbHeatmapColorSchemePreference.dialog.tpl.html'),
        });
    };

    dispatcher.on('heatmap:colorscheme:configure', displayHeatmapColorSchemePreferenceCallback);
}
