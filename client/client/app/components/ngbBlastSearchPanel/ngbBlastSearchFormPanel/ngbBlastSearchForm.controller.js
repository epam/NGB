import baseController from '../../../shared/baseController';

export default class ngbBlastSearchFormController extends baseController {
    static get UID() {
        return 'ngbBlastSearchFormController';
    }

    isProgressShown = true;
    dbList = [];
    algorithmList = [];
    searchRequest = {
        title: '',
        algorithm: '',
        organisms: [],
        db: '',
        tool: '',
        sequence: '',
        threshold: null,
        sequenceLimit: null
    };

    events = {
        'read:show:blast': ::this.onExternalChange
    }

    constructor($scope, $timeout, dispatcher, ngbBlastSearchService, ngbBlastSearchFormConstants) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbBlastSearchService,
            ngbBlastSearchFormConstants
        });

        this.initialize();
        this.initEvents();
    }

    async initialize() {
        this.isProgressShown = true;
        this.dbList = await this.ngbBlastSearchService.getBlastDBList();
        await this.setSearchRequest();
    }

    onExternalChange(data) {
        this.ngbBlastSearchService.currentSearchId = null;
        this.ngbBlastSearchService.currentTool = data.tool;
        this.setSearchRequest();
    }

    async setSearchRequest() {
        this.isProgressShown = true;
        this.searchRequest = await this.ngbBlastSearchService.getCurrentSearch();
        this.setDefaultAlgorithms();
        this.$timeout(() => this.isProgressShown = false);
    }

    setDefaultAlgorithms() {
        this.algorithmList = this.ngbBlastSearchFormConstants.ALGORITHMS[this.searchRequest.tool];
        if (this.algorithmList && !this.algorithmList.includes(this.searchRequest.algorithm)) {
            this.searchRequest.algorithm = this.algorithmList[0] || '';
        }
    }

    onSearch() {
        this.ngbBlastSearchService.createSearchRequest(this.searchRequest);
            // .then(() => {});
        this.changeState({state: 'HISTORY'});
    }
}
