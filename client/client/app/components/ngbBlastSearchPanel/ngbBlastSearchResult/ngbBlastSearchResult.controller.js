import angular from 'angular';
import baseController from '../../../shared/baseController';

export default class ngbBlastSearchResult extends baseController {

    static get UID() {
        return 'ngbBlastSearchResult';
    }

    searchResult = {};
    isProgressShown = true;

    constructor(dispatcher, $scope, $timeout, $mdDialog, ngbBlastSearchService) {
        super(dispatcher);

        Object.assign(this, {
            $scope,
            $timeout,
            $mdDialog,
            dispatcher,
            ngbBlastSearchService
        });

        this.initEvents();
        this.initialize();
    }

    async initialize() {
        this.searchResult = await this.ngbBlastSearchService.getCurrentSearch();
        this.isProgressShown = false;
    }

    openQueryInfo() {
        this.$mdDialog.show({
            clickOutsideToClose: true,
            controller: ['sequence', function(sequence) {
                this.sequence = sequence;
            }],
            controllerAs: 'ctrl',
            parent: angular.element(document.body),
            template: `<div>
                        {{ctrl.sequence}}
                       </div>`,
            locals: {
                sequence: this.searchResult.sequence
            }
        });
    }
}
