export default class ngbBlastGenomeViewController {
    
    static get UID() {
        return 'ngbBlastGenomeViewController';
    }
    constructor($scope, $mdDialog) {
        Object.assign(this, {
            $mdDialog,
            $scope,
        });
    }
}