import {mapTrackFn} from '../../../ngbDataSets/internal/utilities';
import {ProjectDataService} from '../../../../../dataServices';

const projectDataService = new ProjectDataService();

const MODEL = {
    TARGET: 'target',
    QUERY: 'query'
};

function findGeneByName(reference, gene) {
    if (!reference || !gene) {
        return Promise.resolve(undefined);
    }
    return new Promise((resolve) => {
        projectDataService.searchGenes(reference.id, gene.trim())
            .then(result => {
                if (result.entries && result.entries.length > 0) {
                    const [match] = result.entries
                        .filter(entry => (entry.featureName || '').toLowerCase() === gene.trim().toLowerCase());
                    if (match) {
                        resolve(match);
                        return;
                    }
                }
                resolve(undefined);
            })
            .catch(() => resolve(undefined));
    });
}

function findReferenceByName(name, projectContext) {
    if (!name || !projectContext || !projectContext.references || projectContext.references.length === 0) {
        return undefined;
    }
    const regExp = new RegExp(`^${name.trim()}$`, 'i');
    const [ref] = projectContext.references.filter(r => regExp.test((r.name || '').trim()));
    return ref;
}

function findReferenceById(id, projectContext) {
    if (!id || !projectContext || !projectContext.references || projectContext.references.length === 0) {
        return undefined;
    }
    return projectContext.references.filter(reference => reference.id === id).pop();
}

export default class ngbGenomicsPanelController {

    targetModel = {};
    queryModel = {};
    geneOptions = [];
    proteinOptions = {
        target: [],
        query: []
    };
    allProteins = [];

    static get UID() {
        return 'ngbGenomicsPanelController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        appLayout,
        ngbGenomicsPanelService,
        ngbTargetPanelService,
        ngbSequencesPanelService,
        projectContext,
        targetContext
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            appLayout,
            ngbGenomicsPanelService,
            ngbTargetPanelService,
            ngbSequencesPanelService,
            projectContext,
            targetContext
        });
        dispatcher.on('target:identification:sequences:results:updated', this.setAllProteinOptions.bind(this));
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:identification:sequences:results:updated', this.setAllProteinOptions.bind(this));
        });
    }

    get loadingData() {
        return this.ngbGenomicsPanelService.loadingData;
    }
    set loadingData(value) {
        this.ngbGenomicsPanelService.loadingData = value;
    }
    get failedResult() {
        return this.ngbGenomicsPanelService.failedResult;
    }
    get errorMessageList() {
        return this.ngbGenomicsPanelService.errorMessageList;
    }

    get genesIds() {
        return this.ngbTargetPanelService.genesIds;
    }

    get allSequences () {
        return this.ngbSequencesPanelService.allSequences;
    }

    get targetId() {
        const {target} = this.ngbTargetPanelService.identificationTarget || {};
        return target.id;
    }

    getChipByGeneId (id) {
        return this.ngbTargetPanelService.getChipByGeneId(id);
    }

    get taxId () {
        if (!this.targetModel.gene) return undefined;
        const taxIds = this.ngbTargetPanelService.allGenes
            .filter(g => g.geneId.toLowerCase() === this.targetModel.gene.toLowerCase())
            .map(g => g.taxId);
        return taxIds.length ? taxIds[0] : undefined;
    }

    get references () {
        return this.projectContext.references.filter(r => (
            (r.species && r.species.taxId) ?
                r.species.taxId === this.taxId : false
        ));
    }

    get isRegistered() {
        return this.references.length;
    }

    get isTrackButtonDisabled() {
        return !this.targetModel.protein || !this.queryModel.protein || !this.isRegistered || !this.alignment;
    }

    get tableResults() {
        return this.ngbGenomicsPanelService.tableResults;
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        if (this.genesIds) {
            this.geneOptions = this.genesIds.map(id => ({
                geneId: id.toLowerCase(),
                chip: this.getChipByGeneId(id)
            }));
            this.setAllProteinOptions();
        }
    }

    get targetGeneOptions() {
        return this.geneOptions.filter(o => this.queryModel.gene !== o.geneId);
    }

    get queryGeneOptions() {
        return this.geneOptions.filter(o => this.targetModel.gene !== o.geneId);
    }

    get alignment() {
        return this.ngbGenomicsPanelService.alignment;
    }

    get geneChips() {
        return [...this.ngbTargetPanelService.allChips];
    }

    isProteinOptionDisabled(geneId) {
        return !this.allProteins[geneId] || !this.allProteins[geneId].length;
    }

    getProteins(geneId) {
        if (!this.allSequences) return;
        const data = this.allSequences[geneId.toLowerCase()] || {};
        const getName = (protein = {}) => {
            if (protein.name && protein.name !== ' ' && protein.id) {
                return `${protein.name} (${protein.id})`;
            }
            return protein.id;
        };
        const proteins = (data.sequences || [])
            .map(s => ({
                name: getName(s.protein),
                value: (s.protein || {}).id
            }))
            .filter(p => p.value);
        return proteins;
    }

    setAllProteinOptions() {
        this.allProteins = this.geneOptions.reduce((acc, gene) => {
            const {geneId} = gene;
            acc[geneId.toLowerCase()] = this.getProteins(geneId);
            return acc;
        }, {});
        this.$timeout(() => this.$scope.$apply());
    }

    onChangeGene(name, geneId) {
        if (name === MODEL.TARGET) {
            this.targetModel.protein = undefined;
        }
        if (name === MODEL.QUERY) {
            this.queryModel.protein = undefined;
        }
        if (!geneId) {
            this.proteinOptions[name] = [];
        } else {
            this.proteinOptions[name] = this.allProteins[geneId.toLowerCase()];
        }
    }

    get targetProteinOptions() {
        return this.proteinOptions.target.filter(o => o !== this.queryModel.protein);
    }

    get queryProteinOptions() {
        return this.proteinOptions.query.filter(o => o !== this.targetModel.protein);
    }

    async alignComparison() {
        this.loadingData = true;
        const targetProtein = this.targetModel.protein;
        const queryProtein = this.queryModel.protein;
        if (!this.targetId || !targetProtein || !queryProtein) {
            this._loadingData = false;
            return;
        }
        const sequenceIds = {
            firstSequenceId: targetProtein,
            secondSequenceId: queryProtein
        };
        await this.ngbGenomicsPanelService.getTargetAlignment(this.targetId, sequenceIds);
        this.dispatcher.emit('target:identification:alignment:updated', true);
        this.$timeout(() => this.$scope.$apply());
    }

    viewOnTrack() {
        if (!this.projectContext || !this.isRegistered) return;
        this.panelAddBrowserPanel();
        this.navigateToReference();
        this.navigateToGene();
        this.setGeneTrack();
    }

    panelAddBrowserPanel() {
        const layoutChange = this.appLayout.Panels.browser;
        layoutChange.displayed = true;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
    }

    navigateToReference() {
        const referenceObj = this.references[0];
        const payload = this.getOpenReferencePayload(referenceObj);
        if (payload) {
            setTimeout(() => {
                this.projectContext.changeState(payload);
            }, 0);
        }
    }

    getOpenReferencePayload(referenceObj) {
        if (referenceObj && this.projectContext.datasets) {
            // we'll open first dataset of this reference
            const tree = this.projectContext.datasets || [];
            const find = (items = []) => {
                const projects = items.filter(item => item.isProject);
                const [dataset] = projects.filter(item => item.reference && item.reference.id === referenceObj.id);
                if (dataset) {
                    return dataset;
                }
                for (const project of projects) {
                    const nested = find(project.nestedProjects);
                    if (nested) {
                        return nested;
                    }
                }
                return undefined;
            };
            const dataset = find(tree);
            if (dataset) {
                const referenceTrackState = {
                    referenceShowForwardStrand: true,
                    referenceShowReverseStrand: true,
                    referenceShowTranslation: true
                };
                const tracks = [dataset.reference];
                const tracksState = [{...mapTrackFn(dataset.reference), state: referenceTrackState}];
                return {
                    tracks,
                    tracksState,
                    reference: dataset.reference,
                    shouldAddAnnotationTracks: true
                };
            }
        }
        return undefined;
    }

    navigateToGene() {
        if (
            !this.targetModel.gene ||
            !this.projectContext ||
            !this.projectContext.references ||
            this.projectContext.references.length === 0
        ) {
            return Promise.resolve(false);
        }
        const request = this.ngbTargetPanelService.allGenes
            .filter(gene => gene.geneId.toLowerCase() === this.targetModel.gene.toLowerCase())
            .map(gene => gene.geneName)[0].trim();
        const refGeneGroups = /^(.*):(.*)$/.exec(request);
        const findPromise = (reference, gene) => new Promise((resolve) => {
            if (!reference) {
                resolve();
            } else {
                findGeneByName(reference, gene)
                    .then(gene => gene ? resolve({gene, reference}) : resolve());
            }
        });
        return new Promise((resolve) => {
            const [, referenceName, geneName] = refGeneGroups || [];
            findPromise(findReferenceByName(referenceName), geneName)
                .then(result => {
                    if (result) {
                        return Promise.resolve(result);
                    }
                    const reference = findReferenceById(this.references[0].id, this.projectContext);
                    return findPromise(reference, request);
                })
                .then(geneInfo => {
                    if (geneInfo) {
                        const {
                            reference,
                            gene: feature = {}
                        } = geneInfo;
                        const {
                            startIndex,
                            endIndex,
                            chromosome = {}
                        } = feature;
                        if (
                            reference &&
                            chromosome &&
                            startIndex !== undefined &&
                            endIndex !== undefined
                        ) {
                            this.navigateToCoordinates(`${reference.name}:${chromosome.name}:${startIndex}-${endIndex}`)
                                .then(resolve);
                            this.targetContext.featureCoords = {startIndex, endIndex};
                            this.setArtificialTrack();
                            return;
                        }
                    }
                    resolve(false);
                });
        });
    }

    navigateToCoordinates(coordinates) {
        if (!coordinates || !this.projectContext) {
            return Promise.resolve(false);
        }
        let reference;
        // eslint-disable-next-line prefer-const
        let [ref, chr, coords] = coordinates.split(':').map(o => o.trim());
        if (!coords) {
            // we're parsing "chr: start - end" format
            coords = chr;
            chr = ref;
            reference = findReferenceById(this.references[0].id, this.projectContext);
        } else {
            reference = findReferenceByName(ref, this.projectContext);
        }
        if (reference && chr && coords) {
            const [startIndex, endIndex] = coords.split('-').map(o => Number(o.trim()));
            const referenceChanged = !this.projectContext.reference ||
                this.projectContext.reference.id !== reference.id;
            const payload = referenceChanged
                ? this.getOpenReferencePayload(reference)
                : {};
            payload.chromosome = {name: chr};
            if (!Number.isNaN(startIndex) && !Number.isNaN(endIndex)) {
                payload.viewport = {
                    start: startIndex,
                    end: endIndex
                };
            } else if (!Number.isNaN(startIndex)) {
                payload.position = startIndex;
            }
            setTimeout(() => {
                this.projectContext.changeState(payload);
            }, 0);
            return Promise.resolve(true);
        }
        return Promise.resolve(false);
    }

    setGeneTrack() {
        const tracks = this.projectContext.tracks;
        const tracksState = this.projectContext.tracksState;
        if (tracksState && tracksState.length) {
            for (let i = 0; i < tracksState.length; i++) {
                const track = tracksState[i];
                if (track.format === 'GENE') {
                    track.state = track.state || {};
                    track.state.geneTranscript = 'expanded';
                }
            }
            this.projectContext.changeState({tracks, tracksState});
        }
    }

    setArtificialTrack() {
        const tracks = this.projectContext.tracks;
        const tracksState = this.projectContext.tracksState;
        if (tracksState && tracksState.length) {
            let comparisonTrackIndex;
            const comparisonTrack = (tracksState || []).filter((track, index) => {
                if (track.format === 'COMPARISON') {
                    comparisonTrackIndex = index;
                    return track;
                }
            });
            const name = this.getChipByGeneId(this.queryModel.gene);
            const trackPattern = {
                name: name,
                format: 'COMPARISON',
                isLocal: true,
                projectId: '',
                bioDataItemId: 'Comparison results',
                id: 0,
                reference: 'reference',
                referenceId: '1'
            };
            const trackStatePattern = {
                bioDataItemId: name,
                duplicateId: undefined,
                projectId: '',
                format: 'COMPARISON',
                isLocal: true,
                state: {}
            }
            if (!comparisonTrack.length) {
                tracks.push(trackPattern);
                tracksState.push(trackStatePattern);
            } else {
                tracks.splice(comparisonTrackIndex, 0, trackPattern);
                tracksState.splice(comparisonTrackIndex, 0, trackStatePattern);
            }
            setTimeout(() => {
                this.projectContext.changeState({tracks, tracksState},
                false,
                () => this.targetContext.setAlignments([this.ngbGenomicsPanelService.alignment]));
            }, 0);
            
        }
    }

    exportResults() {
        this.loadingData = true;
        this.ngbGenomicsPanelService.exportResults()
            .then(data => {
                const linkElement = document.createElement('a');
                try {
                    const blob = new Blob([data], {type: 'application/csv'});
                    const url = window.URL.createObjectURL(blob);

                    linkElement.setAttribute('href', url);
                    linkElement.setAttribute('download',
                        `${this.geneChips.join('_')}-homologues.csv`);

                    const clickEvent = new MouseEvent('click', {
                        'view': window,
                        'bubbles': true,
                        'cancelable': false
                    });
                    linkElement.dispatchEvent(clickEvent);
                    this.loadingData = false;
                } catch (ex) {
                    // eslint-disable-next-line no-console
                    console.error(ex);
                    this.loadingData = false;
                }
                this.$timeout(() => this.$scope.$apply());
            });
    }
}
