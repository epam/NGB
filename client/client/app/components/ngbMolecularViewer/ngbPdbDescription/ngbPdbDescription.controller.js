import ngbMolecularViewerService from '../ngbMolecularViewer.service';

export default class ngbPdbDescriptionController {
    static get UID() {
        return 'ngbPdbDescriptionController';
    }

    service: ngbMolecularViewerService;

    constructor($scope, $element, ngbMolecularViewerService, ngbMolecularViewerConstant) {
        this.ngbMolecularViewerConstant = ngbMolecularViewerConstant;
        this.$scope = $scope;
        this.$element = $element;
        this.service = ngbMolecularViewerService;

        this.pdb && this.pdbChange(this.pdb);
        this.$scope.$watch('$ctrl.pdb', (newVal)=> {
            this.pdbChange(newVal);
        });

    }

    async pdbChange(pdb) {
        this.errorMessageList = [];
        this.description = null;
        this.loadDone = false;

        const [firstChain] = this.pdbList.filter(m=>m.id === pdb.id);
        const chainLetter = firstChain.position && firstChain.position.substring(0, 1);

        try {
            this.description = await  this.service.loadPdbDescription(pdb.id);
            let chain;

            if (chainLetter) {
                [chain] = this.description.entities.filter(m => m.chainId === chainLetter);
            }
            if (!chain) {
                [chain] = this.description.entities;
            }

            if (chain) {
                this.chainId = chain.chainId;
            }

        } catch (exception) {
            this.errorMessageList.push((exception && exception.message)
                ? exception.message
                : this.ngbMolecularViewerConstant.unhandledEx);
        }

        this.loadDone = true;
        this.$scope.$apply();
    }


}
