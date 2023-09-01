export default class ngbGenomicsPanelService {

    _loadingData = false;
    _failedResult = false;
    _errorMessageList = null;
    _alignment = null;

    get alignment() {
        return this._alignment;
    }

    get loadingData() {
        return this._loadingData;
    }
    set loadingData(value) {
        this._loadingData = value;
    }
    get failedResult() {
        return this._failedResult;
    }
    get errorMessageList() {
        return this._errorMessageList;
    }

    static instance (dispatcher, ngbTargetPanelService, targetDataService, genomeDataService) {
        return new ngbGenomicsPanelService(dispatcher, ngbTargetPanelService, targetDataService, genomeDataService);
    }

    constructor(dispatcher, ngbTargetPanelService, targetDataService, genomeDataService) {
        Object.assign(this, {ngbTargetPanelService, targetDataService, genomeDataService});
        dispatcher.on('target:identification:changed', this.resetData.bind(this));
        this.getHomologene();
    }

    get genesIds() {
        return this.ngbTargetPanelService.genesIds;
    }

    setAlignment(data) {
        const [target, query] = data;
        const getName = (item) => {
            return item.split(' ')[0];
        };
        this._alignment = {
            targetName: getName(target.name),
            targetTooltip: target.name,
            targetSequence: target.baseString,
            targetStart: 1,
            targetEnd: target.baseString.length,
            queryName: getName(query.name),
            queryTooltip: query.name,
            querySequence: query.baseString,
            queryStart: 1,
            queryEnd: query.baseString.length,
        };
    }

    getTargetAlignment(targetId, sequenceIds) {
        return new Promise(resolve => {
            this.targetDataService.getTargetAlignment(targetId, sequenceIds)
                .then(data => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    if (data && data.length === 2) {
                        this.setAlignment(data);
                    } else {
                        this._alignment = null;
                    }
                    this._loadingData = false;
                    resolve(true);
                })
                .catch(err => {
                    this._failedResult = true;
                    this._errorMessageList = [err.message];
                    this._alignment = null;
                    this._loadingData = false;
                    resolve(false);
                });
        });
    }

    async getData() {
        await this.getAllHomologs();
        await this.getAllHomologenes();
    }

    getAllHomologs() {
        return Promise.all(
            this.genesIds.map(async (id) => (
                await this.getHomologs(id)
            )))
                .then(values => (values.some(v => v)));
    }

    async getHomologs(id) {
        if (!id) {
            return new Promise.resolve(true);
        }
        const request = {
            geneId: id,
            page: 1,
            pageSize: 10
        }
        return new Promise(resolve => {
            this.genomeDataService.getOrthoParaLoad(request)
                .then(data => {
                    console.log(data)
                    resolve(true);
                })
                .catch(err => {
                    console.log(err)
                    resolve(false);
                });
        });
    }

    async getAllHomologenes() {
        const {
            interest = [],
            translational = []
        } = this.ngbTargetPanelService.identificationTarget || {};
        const allGenes = [...interest, ...translational];
        return Promise.all(
            allGenes.map(async (gene) => (
                await this.getHomologene(gene.geneName)
            )))
                .then(values => (values.some(v => v)));
    }

    async getHomologene(name) {
        if (!name) {
            return Promise.resolve(true);
        }
        const request = {
            query: name,
            page: 1,
            pageSize: 10
        }
        return new Promise(resolve => {
            this.genomeDataService.getHomologeneLoad(request)
                .then(data => {
                    console.log(data)
                    resolve(true);
                })
                .catch(err => {
                    console.log(err)
                    resolve(false);
                });
        });
    }

    resetData() {
        this._loadingData = false;
        this._failedResult = false;
        this._errorMessageList = null;
        this._alignment = null;
    }
}
