export default function run($mdDialog, dispatcher) {
    const displayTrackCoverageSettingsCallback = (state)=> {
        const {sources, config, options} = state;
        const {browserId} = options || {};
        let processed = false;
        $mdDialog.show({
            template: require('./ngbTrackCoverageSettings.dialog.tpl.html'),
            controller: function ($scope) {
                $scope.from = config && config.extremumFn !== undefined ? config.extremumFn().min : 0;
                $scope.to = config && config.extremumFn !== undefined ? config.extremumFn().max : 300;
                $scope.applyToCurrentTrack = true;
                $scope.applyToWIGTracks = false;
                $scope.applyToBAMTracks = false;
                $scope.isLogScale = config && config.isLogScale !== undefined ? config.isLogScale : false;
                $scope.close = () => {
                    $mdDialog.hide();
                };
                $scope.save = () => {
                    processed = true;
                    $mdDialog.hide();
                    dispatcher.emitSimpleEvent('tracks:coverage:manual:configure:done', {
                        sources,
                        cancel: false,
                        data: {
                            browserId,
                            from: $scope.from,
                            to: $scope.to,
                            applyToCurrentTrack: $scope.applyToCurrentTrack,
                            applyToWIGTracks: $scope.applyToWIGTracks,
                            applyToBAMTracks: $scope.applyToBAMTracks,
                            isLogScale: $scope.isLogScale
                        }
                    });
                };
            },
            clickOutsideToClose: true,
            onRemoving: () => {
                if (!processed) {
                    dispatcher.emitSimpleEvent('tracks:coverage:manual:configure:done', {
                        sources,
                        cancel: true
                    });
                }
            }
        });

    };

    dispatcher.on('tracks:coverage:manual:configure', displayTrackCoverageSettingsCallback);
}
