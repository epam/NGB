function getMaxChromosomeSize(chrArray) {
    return chrArray.reduce((max, chr) => {
        return Math.max(chr.size, max);
    }, 0);
}

export function helper($mdDialog, projectContext) {

  const blastResults = projectContext.chromosomes.slice(0,8);
    return function($scope) {
        $scope.chromosomes = blastResults;
        $scope.maxChrSize = getMaxChromosomeSize(blastResults);
        $scope.close = () => {
            $mdDialog.hide();
        };
    };
}
