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
                $scope.genesOfInterest = [];
                $scope.translationalGenes = [];

                $scope.genes = groupedBySpecies(target.species.value);

                function getGroupItems (item) {
                    const { genesOfInterest = [], translationalGenes = [] } = $scope;
                    return $scope.genes
                        .filter(s => (s.item
                            && s.speciesName === item.speciesName
                            && !genesOfInterest.includes(s)
                            && !translationalGenes.includes(s)));
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

                $scope.genesOfInterestChanged = (item) => {
                    if (item) {
                        if (item.group) {
                            const items = getGroupItems(item);
                            for (let i = 0; i < items.length; i++) {
                                $scope.genesOfInterest.push(items[i]);
                            }
                            $timeout(() => {
                                const index = $scope.genesOfInterest.indexOf(item);
                                if (index !== -1) {
                                    $scope.genesOfInterest.splice(index, 1);
                                }
                            });
                        } else {
                            $scope.genesOfInterest.push(item);
                        }
                    }
                };

                $scope.translationalGenesChanged = (item) => {
                    if (item) {
                        if (item.group) {
                            const items = getGroupItems(item);
                            for (let i = 0; i < items.length; i++) {
                                $scope.translationalGenes.push(items[i]);
                            }
                            $timeout(() => {
                                const index = $scope.translationalGenes.indexOf(item);
                                if (index !== -1) {
                                    $scope.translationalGenes.splice(index, 1);
                                }
                            });
                        } else {
                            $scope.translationalGenes.push(item);
                        }
                    }
                };

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
