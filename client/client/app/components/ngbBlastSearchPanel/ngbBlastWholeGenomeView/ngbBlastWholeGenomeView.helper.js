export function helper($mdDialog, dispatcher, projectContext, { data, speciesList }) {
    const currentChromosomesNames = data.map((obj) => obj.chromosome);
    const currentChromosomes = projectContext.chromosomes.filter((chr) =>
        currentChromosomesNames.includes(chr.name),
    );
    return function ($scope) {
        $scope.chromosomes = currentChromosomes;
        $scope.species = speciesList;
        $scope.blastCurrentSpecies = projectContext.reference.id;
        $scope.result = data;
        $scope.close = () => $mdDialog.hide();
        $scope.selectSpecies = (selectedItemId) => {
            $scope.blastCurrentSpecies = selectedItemId;
            dispatcher.emitSimpleEvent('blast:select:species', selectedItemId);
        }
    };
}
