export default function run($mdDialog, dispatcher) {
    const displaySashimiPlot = ({config, cacheService})=> {
        $mdDialog.show({
            clickOutsideToClose: true,
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
            multiple: true,
            skipHide: true,
            template: require('./ngbSashimiPlot.dialog.tpl.html'),
        });
    };

    dispatcher.on('bam:sashimi', displaySashimiPlot);
}
