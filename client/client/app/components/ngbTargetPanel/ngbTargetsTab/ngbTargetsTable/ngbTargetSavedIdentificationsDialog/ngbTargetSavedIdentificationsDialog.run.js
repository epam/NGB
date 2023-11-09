import {
    groupedBySpecies,
    createFilterFor,
} from '../utilities/autocompleteFunctions';

const COLUMN_LIST = ['name', 'genes of interest', 'translational genes'];

export default function run(
    $mdDialog,
    $timeout,
    dispatcher,
) {
    const displaySavedIdentificationsDialog = async (target) => {
        $mdDialog.show({
            template: require('./ngbTargetSavedIdentificationsDialog.tpl.html'),
            controller: function ($scope) {
                $scope.columnList = COLUMN_LIST;
                $scope.name = target.name;
                $scope.genes = groupedBySpecies(target.species.value);
                $scope.searchText = null;
                
                $scope.identificationsModel = target.identifications.map(t => {
                    const {name, genesOfInterest, translationalGenes} = t;
                    const getGeneById = (id) => {
                        const genes = $scope.genes.filter(gene => gene.geneId === id);
                        return genes.length ? genes[0] : undefined;
                    };
                    return {
                        name: name,
                        genesOfInterest: genesOfInterest
                            .map(geneId => getGeneById(geneId))
                            .filter(i => i),
                        translationalGenes: translationalGenes
                            .map(geneId => getGeneById(geneId))
                            .filter(i => i)
                    }
                })

                function getGroupItems(item, model) {
                    const { genesOfInterest = [], translationalGenes = [] } = model;
                    return $scope.genes.filter(s => s.item
                        && s.speciesName === item.speciesName
                        && !genesOfInterest.includes(s)
                        && !translationalGenes.includes(s));
                }

                $scope.getFilteredGenes = (text, model) => {
                    const filterGroupHead = (model) => {
                        return (gene) => {
                            if (gene.group) {
                                const items = getGroupItems(gene, model);
                                return items.length;
                            }
                            return true;
                        };
                    };
                    const genes = $scope.genes
                        .filter(s => !model.genesOfInterest.includes(s))
                        .filter(s => !model.translationalGenes.includes(s))
                        .filter(filterGroupHead(model));
                    if (!text) return genes;
                    return genes.filter(createFilterFor(text));
                }

                $scope.genesOfInterestChanged = (item, model) => {
                    if (item && item.group) {
                        const items = getGroupItems(item, model);
                        for (let i = 0; i < items.length; i++) {
                            model.genesOfInterest.push(items[i]);
                        }
                        $timeout(() => {
                            const i = model.genesOfInterest.indexOf(item);
                            if (i) {
                                model.genesOfInterest.splice(i, 1);
                            }
                        });
                    }
                    document.activeElement.blur();
                };

                $scope.translationalGenesChanged = (item, model) => {
                    if (item && item.group) {
                        const items = getGroupItems(item, model);
                        for (let i = 0; i < items.length; i++) {
                            model.translationalGenes.push(items[i]);
                        }
                        $timeout(() => {
                            const index = model.translationalGenes.indexOf(item);
                            if (index) {
                                model.translationalGenes.splice(index, 1);
                            }
                        });
                    }
                    document.activeElement.blur();
                };

                $scope.onClickSave = (index) => {}

                $scope.onClickDelete = (index) => {}

                $scope.onClickLaunch = (index) => {}

                $scope.close = () => {
                    $mdDialog.hide();
                };
            },
            clickOutsideToClose: true
        });
    };
    dispatcher.on('target:show:saved:identifications', displaySavedIdentificationsDialog);
}
