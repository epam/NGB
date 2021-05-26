import baseController from '../../../shared/baseController';

export default class ngbBlastSearchFormController extends baseController {
    static get UID() {
        return 'ngbBlastSearchFormController';
    }

    isProgressShown = true;
    setVariants = {
        CUSTOM: 'custom',
        DB: 'db'
    };
    organismList = [];
    dbList = [];
    selectedSet = this.setVariants[0];
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
        'read:show:blast': ::this.initialize
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

        this.initialize({});
        this.initEvents();
    }

    async initialize(data) {
        this.isProgressShown = true;
        this.organismList = await this.ngbBlastSearchService.getOrganismList('human*[orgn]');
        this.dbList = await this.ngbBlastSearchService.getBlastDBList();
        const currentSearch = await this.ngbBlastSearchService.getCurrentSearch();
        currentSearch.tool = currentSearch.tool || data.tool || '';
        Object.assign(this.searchRequest, currentSearch);
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
