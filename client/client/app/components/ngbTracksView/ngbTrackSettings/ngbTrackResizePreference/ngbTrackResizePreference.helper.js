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

export default function helper($mdDialog, projectContext, options) {
    const filterCommonTracks = (allTracks, format) =>
        allTracks.filter(m => m.config.format === format);

    return function ($scope) {
        const {tracks, options: actionOptions, types} = options;
        const {group = false} = actionOptions || {};
        const multiple = tracks.length > 1;
        $scope.applyToAllTracks = false;
        $scope.applyToAllTracksOfType = false;
        $scope.applyToAllTracksVisible = !group;
        $scope.applyToAllTracksOfTypeVisible = !group;
        const formats = (types || []).slice();
        const commonFormat = formats[0];
        $scope.types = formats;
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
        $scope.height = multiple
            ? tracks
                .map(track => track._height)
                .reduce((r, c) => (r === undefined || r === c) ? c : '', undefined)
            : tracks[0]._height;

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
            if ($scope.applyToAllTracks) {
                const allTracks = (projectContext.trackInstances || []);
                allTracks.forEach(saveTrackHeight);
            } else if ($scope.applyToAllTracksOfType) {
                const allTracks = (projectContext.trackInstances || []);
                const commonFormatTracks = filterCommonTracks(allTracks, commonFormat);
                commonFormatTracks.forEach(saveTrackHeight);
            }
            $mdDialog.hide();
        };
    };
}
