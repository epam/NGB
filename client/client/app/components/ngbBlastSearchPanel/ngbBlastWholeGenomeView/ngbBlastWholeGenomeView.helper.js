export function helper($mdDialog, dispatcher, projectContext, { data, speciesList }) {
    const currentChromosomesNames = data.map((obj) => obj.chromosome);
    const currentChromosomes = projectContext.chromosomes.filter((chr) =>
        currentChromosomesNames.includes(chr.name),
    );
    return function ($scope) {
        $scope.chromosomes = currentChromosomes;
        $scope.species = speciesList;
        $scope.blastCurrentSpecies = speciesList[0].taxid;
        $scope.result = data.filter(item => item.taxid === speciesList[0].taxid);
        $scope.close = () => $mdDialog.hide();
        $scope.selectSpecies = (selectedItemId) => {
            $scope.result = data.filter(item => item.taxid === selectedItemId);
            dispatcher.emitSimpleEvent('blast:select:species', selectedItemId);
        }
    };
}
