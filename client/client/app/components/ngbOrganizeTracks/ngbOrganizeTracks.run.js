export default function run($mdDialog, dispatcher) {

    const displayOrganizeTracksCallback = (state)=> {

        $mdDialog.show({

            template: require('./ngbOrganizeTracksDialog.tpl.html'),
            controller: ($scope, $mdDialog, projectContext) => {
                $scope.close = $mdDialog.cancel;
                $scope.organizeTracks = projectContext.getActiveTracks();
                $scope.$mdDialog = $mdDialog;
                $scope.projectContext = projectContext;

                $scope.save = () => {
                    const tracksState = $scope.projectContext.tracksState;
                    const organizeTracks = [];

                    $scope.organizeTracks.forEach(function(val) {
                        const track = tracksState.filter(t => t.bioDataItemId === val.name && t.projectId === val.projectId);
                        if(track && track.length > 0) {
                            organizeTracks.push(track[0]);
                        }
                    });

                    const referenceName = $scope.projectContext.reference.name.toLowerCase();
                    if (organizeTracks.filter(t => t.bioDataItemId.toLowerCase() !== referenceName).length === 0) {
                        $scope.projectContext.changeState({reference: null});
                    } else {
                        $scope.projectContext.changeState({tracksState: organizeTracks});
                    }

                    $scope.$mdDialog.cancel();
                };
            },
            clickOutsideToClose: true

        });

    };

    dispatcher.on('tracks:organize', displayOrganizeTracksCallback);
}
