import ngbConstants from '../../../../constants';
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
    };

    blastHelpPath = '';

    toolTooltip = {};
    isProgressShown = true;
    dbList = [];
    algorithmList = [];
    errorMessage = null;
    defaultParams = {};
    additionalParams = {};
    searchRequest = {
        title: '',
        algorithm: '',
        organisms: [],
        db: [],
        tool: '',
        sequence: '',
        threshold: null,
        sequenceLimit: null
    };

    events = {
        'read:show:blast': this.onExternalChange.bind(this),
        'defaultSettings:change': this.setAdditionalParams.bind(this)
    };

    constructor($scope, $timeout, dispatcher, ngbBlastSearchService, ngbBlastSearchFormConstants, projectContext) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbBlastSearchService,
            ngbBlastSearchFormConstants,
            projectContext
        });

        this.initialize();
        this.initEvents();
    }

    async initialize() {
        this.blastHelpPath = ngbConstants.urlPrefix;
        if (this.blastHelpPath[this.blastHelpPath.length - 1] !== '/') {
            this.blastHelpPath += '/';
        }
        this.isProgressShown = true;
        await this.setSearchRequest();
    }

    onExternalChange(data) {
        this.ngbBlastSearchService.currentSearchId = null;
        this.ngbBlastSearchService.currentTool = data.tool;
        this.ngbBlastSearchService.isRepeat = false;
        this.additionalParams = {};
        this.setSearchRequest();
    }

    async setSearchRequest() {
        this.isProgressShown = true;
        const {request, error} = await this.ngbBlastSearchService.getCurrentSearch();
        this.searchRequest = request;
        this.setDefaultAlgorithms();
        this.setAdditionalParams();
        await this.getDBList();
        this.errorMessage = this.errorMessage || error;
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

    setAdditionalParams() {
        this.defaultParams = this.projectContext.getTrackDefaultSettings('blast_settings') || {};
        const defaultParamsKeyList = Object.keys((this.defaultParams || {}).options || {});
        const currentValues = {};
        const options = {};
        Object.keys(this.additionalParams).forEach(key => currentValues[key] = this.additionalParams[key].value);
        this.additionalParams = {};
        defaultParamsKeyList.forEach(key => {
            this.additionalParams[key] = this.preprocessAdditionalParam(
                this.defaultParams.options[key],
                currentValues[key],
                (this.searchRequest.parameters || {})[key],
                this.ngbBlastSearchService.isRepeat);
        });
        if (this.searchRequest) {
            Object.keys(this.searchRequest.parameters || {}).forEach(key => {
                if (!defaultParamsKeyList.includes(key)) {
                    options[key] = {
                        value: this.searchRequest.parameters[key]
                    };
                }
            });
            this.searchRequest.options = this.ngbBlastSearchService.stringifySearchOptions(options);
        }
    }

    async getDBList() {
        const data = await this.ngbBlastSearchService.getBlastDBList(this.ngbBlastSearchFormConstants.BLAST_TOOL_DB[this.searchRequest.tool]);
        if (data.error) {
            this.errorMessage = data.message;
        } else {
            this.errorMessage = null;
            this.dbList = data;
            this.searchRequest.db = this.searchRequest.db.filter(dbId => this.dbList.some(db => db.id === dbId));
        }
        this.$timeout(() => this.$scope.$apply());
    }

    getFilteredDBList(selectedDBs = []) {
        const selectedIds = selectedDBs.map(value => value.id);
        return this.dbList.filter(value => !selectedIds.includes(value.id));
    }

    onSearch() {
        this.ngbBlastSearchService.createSearchRequest(this.searchRequest, this.additionalParams)
            .then(data => {
                if (data.error) {
                    this.errorMessage = data.message;
                    this.$timeout(() => this.$scope.$apply());
                } else {
                    this.errorMessage = null;
                    this.changeState({state: 'HISTORY'});
                }
            });
    }

    preprocessAdditionalParam(param, current, fromSearchRequest, isRepeat) {
        const nonNumberTypes = ['string', 'boolean', 'flag'];
        const result = {
            ...param,
            isNumber: param.type && !nonNumberTypes.includes(param.type),
            isBoolean: param.type && param.type === 'boolean',
            isFlag: param.type && param.type === 'flag',
            isString: !param.type || param.type === 'string',
        };
        const rawValue = current || (isRepeat ? fromSearchRequest : param.defaultValue);
        switch (true) {
            case result.isNumber: {
                result.value = +rawValue;
                break;
            }
            case result.isBoolean:
            case result.isFlag: {
                result.value = rawValue === 'true';
                break;
            }
            default: {
                result.value = rawValue;
            }
        }
        return result;
    }
}
