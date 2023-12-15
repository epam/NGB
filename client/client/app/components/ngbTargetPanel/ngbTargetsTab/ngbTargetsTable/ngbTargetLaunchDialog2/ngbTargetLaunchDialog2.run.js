import {
    groupedBySpecies,
    createFilterFor,
} from '../utilities/autocompleteFunctions';

export default function run(
    $mdDialog,
    $timeout,
    dispatcher,
) {
    const displayLaunchDialog = async (target) => {
        $mdDialog.show({
            template: require('./ngbTargetLaunchDialog2.tpl.html'),
            controller: function ($scope) {
                $scope.name = target.name;
                $scope.searchText = '';
                $scope.interestModel = '';
                $scope.translationalModel = '';
                $scope.genesOfInterest = [];
                $scope.translationalGenes = [];

                $scope.genes = groupedBySpecies(target.species.value);

                function getGroupItems (item) {
                    const { genesOfInterest = [], translationalGenes = [] } = $scope;
                    return $scope.genes
                        .filter(s => s.item
                            && s.speciesName === item.speciesName
                            && !genesOfInterest.includes(s)
                            && !translationalGenes.includes(s));
                }

                function filterGroupHead() {
                    return (gene) => {
                        if (gene.group) {
                            const items = getGroupItems(gene);
                            return items.length;
                        }
                        return true;
                    };
                }

                $scope.getFilteredGenes = (text) => {
                    const genes = $scope.genes
                        .filter(s => !$scope.genesOfInterest.includes(s))
                        .filter(s => !$scope.translationalGenes.includes(s))
                        .filter(filterGroupHead());
                    if (!text) return genes;
                    return genes.filter(createFilterFor(text));
                }

                $scope.identifyDisabled = () => (
                    false
                );

                $scope.identify = () => console.log('identify');

                $scope.close = () => {
                    $mdDialog.hide();
                };
                
            },
            clickOutsideToClose: true
        });
    };
    dispatcher.on('target:launch:identification', displayLaunchDialog);
}
