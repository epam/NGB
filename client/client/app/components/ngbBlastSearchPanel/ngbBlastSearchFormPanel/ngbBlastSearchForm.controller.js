import baseController from '../../../shared/baseController';

export default class ngbBlastSearchFormController extends baseController {
    static get UID() {
        return 'ngbBlastSearchFormController';
    }

    isProgressShown = true;
    dbList = [];
    algorithmList = [];
    errorMessageList = [];
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
    };

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
        await this.setSearchRequest();
        this.getDBList();
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
        } else {
            this.searchRequest.algorithm = '';
        }
    }

    clearOrganisms() {
        this.searchRequest.organisms = [];
    }

    onSearchToolChange() {
        this.setDefaultAlgorithms();
        this.getDBList();
    }

    getDBList() {
        this.ngbBlastSearchService.getBlastDBList(this.ngbBlastSearchFormConstants.BLAST_TOOL_DB[this.searchRequest.tool]).then(data => {
            if (data.error) {
                this.errorMessageList.push(data.message);
            } else {
                this.errorMessageList = [];
                this.dbList = data;
                if (!~this.dbList.indexOf(this.searchRequest.db)) {
                    this.searchRequest.db = null;
                }
            }
            this.$timeout(::this.$scope.$apply);
        });
    }

    onSearch() {
        this.ngbBlastSearchService.createSearchRequest(this.searchRequest)
            .then(data => {
                if (data.error) {
                    this.errorMessageList.push(data.message);
                    this.$timeout(::this.$scope.$apply);
                } else {
                    this.errorMessageList = [];
                    this.changeState({state: 'HISTORY'});
                }
            });
    }
}
