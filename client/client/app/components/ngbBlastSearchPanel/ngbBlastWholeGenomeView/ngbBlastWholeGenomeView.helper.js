export function helper($mdDialog, projectContext, { data }) {
  const currentChromosomesNames = data.map((obj) => obj.chromosome)
  const currentChromosomes = projectContext.chromosomes.filter((chr) =>
    currentChromosomesNames.includes(chr.name),
  )
  return function ($scope) {
    $scope.chromosomes = currentChromosomes
    $scope.result = data
    $scope.close = () => {
      $mdDialog.hide()
    }
  }
}
