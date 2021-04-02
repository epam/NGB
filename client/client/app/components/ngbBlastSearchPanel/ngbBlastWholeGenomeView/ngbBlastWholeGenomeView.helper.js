export function helper($mdDialog, projectContext) {

  const blastResults = projectContext.chromosomes.slice(0,8);
    return function($scope) {
        $scope.chromosomes = blastResults;
        $scope.close = () => {
            $mdDialog.hide();
        };
    };
}
