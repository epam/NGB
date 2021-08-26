export default function run($mdDialog, dispatcher) {
    const displayTrackCoverageSettingsCallback = (state)=> {
        const {sources, config, options} = state;
        const {
            browserId,
            formats
        } = options || {};
        let processed = false;
        console.log(sources, formats);
        $mdDialog.show({
            template: require('./ngbTrackCoverageSettings.dialog.tpl.html'),
            controller: function ($scope) {
                $scope.from = config && config.extremumFn !== undefined ? config.extremumFn().min : 0;
                $scope.to = config && config.extremumFn !== undefined ? config.extremumFn().max : 300;
                $scope.applyToCurrentTrack = true;
                $scope.applyToWIGTracks = false;
                $scope.applyToBAMTracks = false;
                $scope.applyToFeatureCountsTracks = false;
                $scope.hasFeatureCountsTracks = (formats || []).includes('FEATURE_COUNTS');
                $scope.hasCoverageTracks = (formats || []).filter(f => f !== 'FEATURE_COUNTS').length > 0;
                $scope.isLogScale = config && config.isLogScale !== undefined ? config.isLogScale : false;
                $scope.error = undefined;
                $scope.close = () => {
                    $mdDialog.hide();
                };
                $scope.save = () => {
                    const isEmpty = o => o === null || o === undefined || `${o}`.trim().length === 0;
                    if (isEmpty($scope.from) || Number.isNaN(Number($scope.from))) {
                        $scope.error = 'Incorrect "from" value';
                        return;
                    }
                    if (isEmpty($scope.to) || Number.isNaN(Number($scope.to))) {
                        $scope.error = 'Incorrect "to" value';
                        return;
                    }
                    if (+($scope.from) >= +($scope.to)) {
                        $scope.error = 'Incorrect "from" and "to" values';
                        return;
                    }
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
                            applyToFeatureCountsTracks: $scope.applyToFeatureCountsTracks,
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
