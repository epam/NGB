export default function run($mdDialog, dispatcher) {
    const displaySashimiPlot = ({config, cacheService})=> {
        $mdDialog.show({
            template: require('./ngbSashimiPlot.dialog.tpl.html'),
            controller: function ($scope) {
                $scope.chromosomeName = config.chromosome ? config.chromosome.name : undefined;
                $scope.referenceId = config.referenceId;
                $scope.track = {
                    bioDataItemId: config.bioDataItemId,
                    format: config.format
                };
                $scope.cacheService = cacheService;
                $scope.close = () => {
                    $mdDialog.hide();
                };
            },
            clickOutsideToClose: true
        });

    };

    dispatcher.on('bam:sashimi', displaySashimiPlot);
}
