import {
    groupedBySpecies,
    createFilterFor,
} from '../utilities/autocompleteFunctions';

const COLUMN_LIST = ['name', 'genes of interest', 'translational genes'];

export default function run(
    $mdDialog,
    $timeout,
    dispatcher,
    targetDataService
) {
    const displaySavedIdentificationsDialog = async (target) => {
        $mdDialog.show({
            template: require('./ngbTargetSavedIdentificationsDialog.tpl.html'),
            controller: function ($scope) {
                $scope.columnList = COLUMN_LIST;
                $scope.actionFailed = false;
                $scope.errorMessageList = null;
                $scope.name = target.name;
                $scope.genes = groupedBySpecies(target.species.value);
                $scope.searchText = null;
                
                $scope.identificationsModel = target.identifications.map(t => {
                    const {id, name, genesOfInterest, translationalGenes} = t;
                    const getGeneById = (id) => {
                        const genes = $scope.genes.filter(gene => !gene.group && gene.geneId === id);
                        console.log(id, genes)
                        return genes.length ? genes[0] : undefined;
                    };
                    return {
                        id,
                        name,
                        genesOfInterest: genesOfInterest
                            .map(geneId => getGeneById(geneId))
                            .filter(i => i),
                        translationalGenes: translationalGenes
                            .map(geneId => getGeneById(geneId))
                            .filter(i => i),
                        actionLoading: false,
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

                function deleteTarget(id) {
                    return new Promise((resolve) => {
                        targetDataService.deleteIdentification(id)
                            .then(() => {
                                $scope.errorMessageList = null;
                                $scope.actionFailed = false;
                                resolve(true);
                            })
                            .catch(error => {
                                $scope.errorMessageList = [error.message];
                                $scope.actionFailed = true;
                                resolve(false);
                            });
                    });
                }

                $scope.onClickDelete = async (index) => {
                    const id = $scope.identificationsModel[index].id;
                    if (!id) return;
                    $scope.identificationsModel[index].actionLoading = true;
                    const result = await deleteTarget(id);
                    if (result) {
                        $scope.identificationsModel = $scope.identificationsModel
                            .filter((item, i) => i !== index);
                        if (!$scope.identificationsModel.length) {
                            $scope.close();
                            dispatcher.emit('target:table:update');
                        }
                        $timeout(() => $scope.$apply());
                    }
                }

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
