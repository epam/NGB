import angular from 'angular';
import baseController from '../../../shared/baseController';

export default class ngbBlastSearchResult extends baseController {

    static get UID() {
        return 'ngbBlastSearchResult';
    }

    searchResult = {};
    isProgressShown = true;

    constructor($mdDialog, ngbBlastSearchService) {
        super();

        Object.assign(this, {
            $mdDialog,
            ngbBlastSearchService
        });
        this.initialize();
    }

    async initialize() {
        this.searchResult = await this.ngbBlastSearchService.getCurrentSearchResult();
        this.searchResult.organisms = (this.searchResult.organisms || []).map(o => o.taxid);
        this.isProgressShown = false;
    }

    openQueryInfo() {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ['sequence', '$mdDialog', function(sequence, $mdDialog) {
                this.sequence = sequence;
                this.close = $mdDialog.hide;
            }],
            controllerAs: 'ctrl',
            parent: angular.element(document.body),
            template: require('./ngbBlastSearchQueryDlg.tpl.html'),
            locals: {
                sequence: this.searchResult.sequence
            }
        });
    }
}
