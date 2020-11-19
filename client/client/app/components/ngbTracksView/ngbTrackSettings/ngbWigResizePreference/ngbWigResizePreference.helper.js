export default function helper($mdDialog, projectContext, config) {
    const filterWigTracks = (tracks) =>
        tracks.filter(
            (m) =>
                m.format === 'WIG' &&
                m.projectId.toLowerCase() === config.projectId.toLowerCase(),
        );

    return function ($scope) {
        $scope.applyToWIGTracks = false;

        const tracksState = projectContext.tracksState || [];
        const [tracksSettings] = tracksState.filter(
            (m) =>
                m.bioDataItemId.toLowerCase() === config.name.toLowerCase() &&
                m.projectId.toLowerCase() === config.projectId.toLowerCase(),
        );
        $scope.height = tracksSettings.height;

        $scope.close = () => $mdDialog.hide();
        $scope.save = () => {
            const tracks = projectContext.tracks;

            if ($scope.applyToWIGTracks) {
                const wigTracks = filterWigTracks(tracks);
                wigTracks.forEach((track) =>
                    track.instance.changeTrackHeight($scope.height),
                );

                const wigTracksSettings = filterWigTracks(tracksState);
                wigTracksSettings.forEach(
                    (tracksSettings) => (tracksSettings.height = $scope.height),
                );
            } else {
                const [currentWigTrack] = tracks.filter(
                    (m) =>
                        m.bioDataItemId === config.bioDataItemId &&
                        m.projectId.toLowerCase() ===
                            config.projectId.toLowerCase(),
                );
                currentWigTrack.instance.changeTrackHeight($scope.height);

                tracksSettings.height = $scope.height;
            }

            projectContext.changeState({ tracksState });
            $mdDialog.hide();
        };
    };
}
