const interpolate = (bit, ratio, min, max) =>
    min[bit] + (max[bit] - min[bit]) * ratio;

export default class ngbDiseasesChartService {
    static instance (
        dispatcher,
        ngbTargetPanelService,
        ngbKnownDrugsPanelService,
        targetDataService
    ) {
        return new ngbDiseasesChartService(
            dispatcher,
            ngbTargetPanelService,
            ngbKnownDrugsPanelService,
            targetDataService
        )
    }

    constructor(
        dispatcher,
        ngbTargetPanelService,
        ngbKnownDrugsPanelService,
        targetDataService
    ) {
        this.dispatcher = dispatcher;
        this.ngbTargetPanelService = ngbTargetPanelService;
        this.ngbKnownDrugsPanelService = ngbKnownDrugsPanelService;
        this.targetDataService = targetDataService;
        this._loading = false;
        this._error = false;
        this._errorMessageList = null;
        this._diseasesResults = null;
        this._diseases = null;
        this._ontologyRequests = {};
        this._ontology = {};
        this.minScore = 0;
        this.maxScore = 1;
        this.scoreStep = 0.01;
        this.scoreFilter = 0;
        this.minColor = {r: 246, g: 252, b: 255};
        this.maxColor = {r: 15, g: 100, b: 150};
    }

    get identificationTarget() {
        return this.ngbTargetPanelService.identificationTarget || {};
    }

    get geneIds() {
        const {interest, translational} = this.identificationTarget;
        return [...interest.map(i => i.geneId), ...translational.map(t => t.geneId)];
    }

    get loading() {
        return this._loading;
    }

    get error() {
        return this._error;
    }

    get errorMessageList() {
        return this._errorMessageList;
    }

    get results() {
        return this._diseasesResults;
    }

    get diseases() {
        return this._diseases;
    }

    getCurrentOntology() {
        if (
            !this.ngbKnownDrugsPanelService.sourceModel ||
            !this._ontology
        ) {
            return [];
        }
        const source = this.ngbKnownDrugsPanelService.sourceModel.name;
        return this._ontology[source] || [];
    }

    getColorForScore(score, minColor = this.minColor, maxColor = this.maxColor) {
        if (score === undefined) {
            return undefined;
        }
        const ratio = (score - this.minScore) / (this.maxScore - this.minScore);
        const r = interpolate('r', ratio, minColor, maxColor);
        const g = interpolate('g', ratio, minColor, maxColor);
        const b = interpolate('b', ratio, minColor, maxColor);
        return `rgb(${r}, ${g}, ${b})`;
    }

    getRequest() {
        return {
            page: 1,
            pageSize: 10,
            geneIds: this.geneIds,
        };
    }

    setDiseasesResult(result) {
        const formatted = (result || [])
            .filter((item) => !!item.disease && !item.disease.therapeuticArea)
            .map((item) => {
                const {
                    targetId,
                    disease = {},
                    scores = {}
                } = item;
                const {
                    OVERALL: score = 0
                } = scores;
                return {
                    targetId,
                    ...disease,
                    score,
                };
            })
            .filter((item) => item.score > 0 &&
                (item.therapeuticAreas || []).length > 0 &&
                item.id &&
                item.name
            )
            .sort((a, b) => b.score - a.score);
        const diseaseIds = formatted.reduce((map, disease) => ({
            ...map,
            [disease.id]: disease
        }), {});
        formatted.forEach((disease) => {
            disease.parents = (disease.parents || []).filter((id) => !!diseaseIds[id]);
        });
        const scores = formatted.map((disease) => disease.score);
        if (scores.length === 0) {
            scores.push(0, 1);
        }
        this.minScore = Math.min(...scores);
        this.maxScore = Math.max(...scores);
        const therapeuticAreas = [];
        this.scoreStep = (this.maxScore - this.minScore) / 100.0;
        this.scoreFilter = Math.min(this.maxScore, Math.max(this.minScore, this.scoreFilter || 0));
        formatted.forEach((disease) => {
            const {
                therapeuticAreas: areas = [],
                ...diseaseInfo
            } = disease;
            areas.forEach((diseaseArea) => {
                let area = therapeuticAreas.find((item) => item.id === diseaseArea.id);
                if (!area) {
                    area = {
                        ...diseaseArea,
                        diseases: []
                    };
                    therapeuticAreas.push(area);
                }
                area.diseases.push(diseaseInfo);
            });
        });
        this._diseasesResults = therapeuticAreas;
        this._diseases = formatted;
    }

    getDiseasesOntology() {
        if (!this.ngbKnownDrugsPanelService.sourceModel) {
            return Promise.resolve();
        }
        const source = this.ngbKnownDrugsPanelService.sourceModel.name;
        if (!this._ontologyRequests) {
            this._ontologyRequests = {};
        }
        if (!this._ontologyRequests[source]) {
            this._ontologyRequests[source] = new Promise((resolve, reject) => {
                this.targetDataService.getOntology(source)
                    .then((data) => {
                        this._ontology[source] = data || [];
                        resolve(this._ontology[source]);
                    })
                    .catch(reject);
            });
        }
        return this._ontologyRequests[source];
    }

    getDiseases() {
        const request = this.getRequest();
        if (
            !request.geneIds ||
            !request.geneIds.length ||
            !this.ngbKnownDrugsPanelService.sourceModel
        ) {
            return new Promise(resolve => {
                this._loading = false;
                resolve(true);
            });
        }
        const source = this.ngbKnownDrugsPanelService.sourceModel.name;
        this._loading = true;
        return new Promise(resolve => {
            Promise.all([
                this.getDiseasesOntology(),
                this.targetDataService.getAllDiseasesResults(request, source),
            ])
                .then(([, data]) => {
                    this._error = false;
                    this._errorMessageList = null;
                    this.setDiseasesResult(data);
                    this._loading = false;
                    resolve(true);
                })
                .catch((error) => {
                    console.warn(error.message);
                    this._error = true;
                    this._errorMessageList = [error.message];
                    this._loading = false;
                    resolve(false);
                });
        });
    }

    resetDiseasesData() {
        this._diseasesResults = null;
        this._diseases = null;
        this._ontology = {};
        this._ontologyRequests = {};
        this._ontology = {};
        this._loading = false;
        this._error = false;
        this._errorMessageList = null;
        this.minScore = 0;
        this.maxScore = 1;
        this.scoreFilter = 0;
        this.scoreStep = 0.01;
    }
}
