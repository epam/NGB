import baseController from '../../../shared/baseController';

export default class ngbBlastSearchFormController extends baseController {
    static get UID() {
        return 'ngbBlastSearchFormController';
    }
    blastToolDescription = {
        blastn: {
            description: 'Four different algorithms are supported:',
            algorithms: {
                'megablast': 'for very similar sequences (e.g., sequencing errors)',
                'discontiguous megablast': 'for more dissimilar sequences, typically used for inter-species comparisons',
                'blastn': 'the traditional program used for inter-species comparisons',
                'blastn-short': 'optimized for sequences less than 30 nucleotides'
            }
        },
        blastp: {
            description: 'Three different algorithms are supported:',
            algorithms: {
                'blastp': 'for standard protein-protein comparisons',
                'blastp-short': 'optimized for query sequences shorter than 30 residues',
                'blastp-fast': 'a faster version that uses a larger word-size'
            },
        },
        blastx: {
            description: 'Two different algorithms are supported:',
            algorithms: {
                'blastx': 'for standard translated nucleotide-protein comparison',
                'blastx-fast': 'a faster version that uses a larger word-size'
            },
        },
        tblastn: {
            description: 'Two different algorithms are supported:',
            algorithms: {
                'tblastn': 'for a standard protein-translated nucleotide comparison',
                'tblastn-fast': 'for a faster version with a larger word-size'
            },
        }
    }
    blastHelpPath = '/server/catgenome/src/main/resources/static/'

    toolTooltip = {};
    isProgressShown = true;
    dbList = [];
    algorithmList = [];
    errorMessageList = [];
    searchRequest = {
        title: '',
        algorithm: '',
        organisms: [],
        db: 0,
        dbName: '',
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
        if (this.algorithmList) {
            if (!this.algorithmList.includes(this.searchRequest.algorithm)) {
                this.searchRequest.algorithm = this.algorithmList[0] || '';
            }
        } else {
            this.searchRequest.algorithm = '';
        }
        this.toolTooltip = this.blastToolDescription[this.searchRequest.tool];
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
                if (this.dbList.filter(db => db.id === this.searchRequest.db).length === 0) {
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
