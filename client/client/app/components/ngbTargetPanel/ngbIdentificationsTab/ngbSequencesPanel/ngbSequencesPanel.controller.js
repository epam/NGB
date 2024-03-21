import {mapTrackFn} from '../../../../components/ngbDataSets/internal/utilities';
import {ProjectDataService} from '../../../../../dataServices';

const projectDataService = new ProjectDataService();

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

export default class ngbSequencesPanelController {

    static get UID() {
        return 'ngbSequencesPanelController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        appLayout,
        ngbSequencesPanelService,
        ngbTargetPanelService,
        projectContext
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            appLayout,
            ngbSequencesPanelService,
            ngbTargetPanelService,
            projectContext
        });
    }

    get genes() {
        return this.ngbSequencesPanelService.genes || [];
    }

    get selectedGeneId() {
        return this.ngbSequencesPanelService.selectedGeneId;
    }
    set selectedGeneId(id) {
        this.ngbSequencesPanelService.selectedGeneId = id;
    }
    get selectedGene() {
        return this.ngbSequencesPanelService.selectedGene;
    }

    get loadingData() {
        return this.ngbSequencesPanelService.loadingData;
    }
    set loadingData(value) {
        this.ngbSequencesPanelService.loadingData = value;
    }

    get sequencesReference() {
        return this.ngbSequencesPanelService.sequencesReference;
    }

    get taxId () {
        const taxIds = this.ngbTargetPanelService.allGenes
            .filter(g => g.geneId === this.selectedGeneId)
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

    get tableResults() {
        const results = this.ngbSequencesPanelService.sequencesResults;
        return results && results.length;
    }

    get geneChips() {
        return [...this.ngbTargetPanelService.allChips];
    }

    get includeLocal() {
        return this.ngbSequencesPanelService.includeLocal;
    }
    set includeLocal(value) {
        this.ngbSequencesPanelService.includeLocal = value;
    }

    get includeAdditionalGenes() {
        return this.ngbSequencesPanelService.includeAdditionalGenes;
    }
    set includeAdditionalGenes(value) {
        this.ngbSequencesPanelService.includeAdditionalGenes = value;
    }

    onChangeSetting() {
        this.dispatcher.emit('target:identification:sequence:setting:changed');
    }

    onChangeGene() {
        this.dispatcher.emit('target:identification:sequence:gene:changed');
    }

    panelAddBrowserPanel() {
        const layoutChange = this.appLayout.Panels.browser;
        layoutChange.displayed = true;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
    }

    onClickSequence() {
        if (!this.projectContext || !this.isRegistered) return;
        this.panelAddBrowserPanel();
        const reference = this.references[0];
        this.navigateToReference(reference);
        this.navigateToGene(reference.id);
        this.setGeneTrack();
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

    navigateToReference(reference) {
        const payload = this.getOpenReferencePayload(reference);
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

    navigateToGene(referenceId) {
        if (
            !this.selectedGene ||
            !this.projectContext ||
            !this.projectContext.references ||
            this.projectContext.references.length === 0
        ) {
            return Promise.resolve(false);
        }
        const request = this.selectedGene.geneName.trim();
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
                    const reference = findReferenceById(referenceId, this.projectContext);
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
                            this.navigateToCoordinates(`${reference.name}:${chromosome.name}:${startIndex}-${endIndex}`, referenceId)
                                .then(resolve);
                            return;
                        }
                    }
                    resolve(false);
                });
        });
    }

    navigateToCoordinates(coordinates, referenceId) {
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
            reference = findReferenceById(referenceId, this.projectContext);
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

    exportResults() {
        this.loadingData = true;
        this.ngbSequencesPanelService.exportResults()
            .then(data => {
                const linkElement = document.createElement('a');
                try {
                    const blob = new Blob([data], {type: 'application/csv'});
                    const url = window.URL.createObjectURL(blob);

                    linkElement.setAttribute('href', url);
                    linkElement.setAttribute('download',
                        `${this.geneChips.join('_')}-sequences.csv`);

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

    getLocalReference(referenceId) {
        return this.projectContext.references.find(r => r.id && referenceId);
    }

    onClickReference(referenceId) {
        if (!this.projectContext) return;
        this.panelAddBrowserPanel();
        const reference = this.getLocalReference(referenceId);
        this.navigateToReference(reference);
        this.navigateToGene(referenceId);
        this.setGeneTrack();
    }
}
