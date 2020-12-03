export default function run($mdDialog, dispatcher) {
    const displaySashimiPlot = (tracks)=> {
        const [anyTrack] = tracks;
        if (anyTrack) {
            const {config} = anyTrack;
            $mdDialog.show({
                template: require('./ngbSashimiPlot.dialog.tpl.html'),
                controller: function ($scope) {
                    $scope.chromosomeName = config.chromosome ? config.chromosome.name : undefined;
                    $scope.referenceId = config.referenceId;
                    $scope.tracks = tracks;
                    $scope.close = () => {
                        $mdDialog.hide();
                    };
                },
                clickOutsideToClose: true,
                multiple: true,
                skipHide: true,
            });
        }
    };

    dispatcher.on('bam:sashimi', displaySashimiPlot);
}
