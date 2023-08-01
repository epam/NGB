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
        projectContext,
        appLayout,
        ngbSequencesPanelService,
        ngbTargetPanelService
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            projectContext,
            appLayout,
            ngbSequencesPanelService,
            ngbTargetPanelService
        });
        this.getSequences();
    }

    get interest () {
        return (this.ngbTargetPanelService.identificationTarget || {}).interest || [];
    }

    get gene() {
        return this.interest[0].geneName;
    }

    get references () {
        const interestTaxId = this.interest[0].taxId;
        return this.projectContext.references.filter(r => (
            (r.species && r.species.taxId) ?
                r.species.taxId === interestTaxId : false
        ));
    }

    get data() {
        return this.ngbSequencesPanelService.seqienceResults;
    }

    get isRegistered() {
        return this.references.length;
    }

    async getSequences() {
        await this.ngbSequencesPanelService.getSequencesResults();
        this.$timeout(() => this.$scope.$apply());
    }

    panelAddBrowserPanel() {
        const layoutChange = this.appLayout.Panels.browser;
        layoutChange.displayed = true;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
    }

    onClickSequence() {
        if (!this.projectContext || !this.isRegistered) return;
        this.panelAddBrowserPanel();
        this.navigateToReference();
        this.navigateToGene();
        this.setGeneTrack();
    }

    setGeneTrack() {
        const tracks = this.projectContext.tracks;
        const tracksState = this.projectContext.tracksState;
        for (let i = 0; i < tracksState.length; i++) {
            const track = tracksState[i];
            if (track.format === 'GENE') {
                track.state = track.state || {};
                track.state.geneTranscript = 'expanded';
            }
        }
        this.projectContext.changeState({tracks, tracksState});
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
            !this.gene ||
            !this.projectContext ||
            !this.projectContext.references ||
            this.projectContext.references.length === 0
        ) {
            return Promise.resolve(false);
        }
        const request = this.gene.trim();
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
}
