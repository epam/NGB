const TRACKS_MIN_HEIGHT = 20;

function correctTrackHeight(track, height) {
    const maxHeight =
        typeof track._maxHeight === 'function'
            ? track._maxHeight(track.state, track.trackConfig)
            : track._maxHeight;
    const minHeight =
        typeof track._minHeight === 'function'
            ? track._minHeight(track.state, track.trackConfig)
            : track._minHeight;
    return Math.max(Math.min(height, maxHeight || Infinity), minHeight || TRACKS_MIN_HEIGHT);
}

export default function helper($mdDialog, projectContext, tracks) {
    const filterCommonTracks = (allTracks, format) =>
        allTracks.filter(
            m =>
                m.format === format &&
                m.projectId.toLowerCase() ===
                    tracks[0].config.projectId.toLowerCase(),
        );

    return function ($scope) {
        const multiple = tracks.length > 1;
        $scope.multiple = multiple;
        $scope.applyToAllTracks = multiple;
        const formats = [...(new Set((tracks || []).map(track => track.config.format)))];
        const commonFormat = formats.length === 1 ? formats[0] : '';
        $scope.commonFormat = commonFormat;
        $scope.applyToAllTracksTitle = formats.length === 1
            ? `Apply fo all ${formats[0]} tracks`
            : 'Apply to all tracks';
        if (!multiple) {
            tracks.forEach(track => {
                $scope.maxHeight =
                    typeof track._maxHeight === 'function'
                        ? track._maxHeight(track.state, track.trackConfig)
                        : track._maxHeight;
                $scope.minHeight =
                    typeof track._minHeight === 'function'
                        ? track._minHeight(track.state, track.trackConfig)
                        : track._minHeight;
            });
        }

        $scope.minHeight = $scope.minHeight || 0;
        $scope.maxHeight = $scope.maxHeight || Infinity;
        $scope.height = multiple ? '' : tracks[0]._height;

        $scope.close = () => $mdDialog.hide();
        $scope.save = () => {
            const saveTrackHeight = (track) => {
                if (track.trackIsResizable) {
                    const newHeight = correctTrackHeight(track, $scope.height);
                    track.changeTrackHeight(newHeight);
                    track.reportTrackState();
                }
            };
            tracks.forEach(saveTrackHeight);
            if ($scope.applyToAllTracks && !multiple) {
                const allTracks = (projectContext.tracks || []);
                const commonFormatTracks = filterCommonTracks(allTracks, commonFormat)
                    .map(track => track.instance);
                commonFormatTracks.forEach(saveTrackHeight);
            }
            $mdDialog.hide();
        };
    };
}
