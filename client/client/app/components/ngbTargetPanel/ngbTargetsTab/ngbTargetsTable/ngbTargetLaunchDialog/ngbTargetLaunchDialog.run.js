export default function run($mdDialog, dispatcher, ngbTargetsTabService, ngbTargetPanelService) {
    const displayLaunchDialog = async (target) => {
        $mdDialog.show({
            template: require('./ngbTargetLaunchDialog.tpl.html'),
            controller: function ($scope) {
                $scope.name = target.name;
                $scope.species = [...target.species.value];
                $scope.speciesOfInterest = [];
                $scope.translationalSpecies = [];
                $scope.searchText = null;

                $scope.identifyDisabled = () => (
                    !$scope.speciesOfInterest.length || !$scope.translationalSpecies.length
                );

                function createFilterFor(text) {
                    return (specie) => specie.name.toLowerCase().includes(text.toLowerCase());
                }
                
                $scope.getSpeciesOfInterestList = (text) => text
                    ? $scope.species
                        .filter(s => $scope.speciesOfInterest.indexOf(s))
                        .filter(createFilterFor(text))
                    : [];

                $scope.getTranslationalSpecies = (text) => text
                    ? $scope.species
                        .filter(s => $scope.translationalSpecies.indexOf(s))
                        .filter(createFilterFor(text))
                    : [];

                function getIdentificationData(scope) {
                    const params = {
                        targetId: target.id,
                        speciesOfInterest:  scope.speciesOfInterest.map(s => s.taxId),
                        translationalSpecies: scope.translationalSpecies.map(s => s.taxId)
                    };
                    const info = {
                        targetName: target.name,
                        interest:  scope.speciesOfInterest.map(s => s.name),
                        translational: scope.translationalSpecies.map(s => s.name)
                    };
                    ngbTargetsTabService.getIdentificationData(params, info);
                }

                $scope.identify = () => {
                    if (ngbTargetPanelService.identificationData && ngbTargetPanelService.identificationParams) {
                        const self = $scope;
                        $mdDialog.show({
                            template: require('./ngbTargetLaunchConfirmDialog.tpl.html'),
                            controller: function($scope, $mdDialog) {
                                $scope.launch = function () {
                                    getIdentificationData(self);
                                    $mdDialog.hide();
                                    $mdDialog.hide();
                                };
                                $scope.cancel = function () {
                                    $mdDialog.hide();
                                };
                            },
                            preserveScope: true,
                            autoWrap: true,
                            skipHide: true,
                        });
                        return;
                    }
                    getIdentificationData($scope);
                    $mdDialog.hide();
                };

                $scope.close = () => {
                    $mdDialog.hide();
                };
                $scope.change = () => {
                    $scope.inReference = !$scope.inReference;
                };
            },
            clickOutsideToClose: true
        });
    };
    dispatcher.on('target:launch:identification', displayLaunchDialog);
}
