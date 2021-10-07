import HeatmapEventDispatcher from '../utilities/heatmap-event-dispatcher';
import HeatmapNavigationType from './types';
import {ProjectDataService} from '../../../../dataServices';
import events from '../utilities/events';
import {mapTrackFn} from '../../../../app/components/ngbDataSets/internal/utilities';

const projectDataService = new ProjectDataService();

const MAXIMUM_TRACKS_TO_OPEN = 20;

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

export {HeatmapNavigationType};
export default class HeatmapNavigation extends HeatmapEventDispatcher {
    /**
     *
     * @param {projectContext} projectContext
     * @param {HeatmapData} data
     */
    constructor(projectContext, data) {
        super();
        this.projectContext = projectContext;
        this.data = data;
    }

    /**
     * Navigates to dataset, track, coordinates, gene etc...
     * @param {HeatmapNavigationType} type
     * @param {string} navigation
     */
    navigate(type, navigation) {
        if (navigation) {
            let navigationPromise;
            switch (type) {
                case HeatmapNavigationType.dataset:
                    navigationPromise = this.navigateToDatasets.bind(this);
                    break;
                case HeatmapNavigationType.gene:
                    navigationPromise = this.navigateToGene.bind(this);
                    break;
                case HeatmapNavigationType.coordinates:
                    navigationPromise = this.navigateToCoordinates.bind(this);
                    break;
                case HeatmapNavigationType.reference:
                    navigationPromise = this.navigateToReference.bind(this);
                    break;
            }
            if (typeof navigationPromise === 'function') {
                navigationPromise(navigation)
                    .then(navigated => {
                        if (navigated) {
                            this.emit(events.navigate);
                        }
                    });
            }
        }
    }

    /**
     * Navigates to datasets
     * @param {string} datasets - comma-separated dataset names, e.g. "Dataset 1, Dataset 2"
     * @returns {Promise<boolean>}
     */
    navigateToDatasets(datasets) {
        if (!datasets || !this.projectContext) {
            return Promise.resolve(false);
        }
        const datasetNames = datasets.split(',')
            .map(o => o.trim())
            .map(datasetName => new RegExp(`^${datasetName}$`, 'i'));
        if (datasetNames.length === 0) {
            return Promise.resolve(false);
        }
        const nameMatches = name => datasetNames.filter(o => o.test(name)).length > 0;
        const tree = this.projectContext.datasets || [];
        const find = (items = []) => {
            const projects = items.filter(item => item.isProject);
            let match = projects.filter(item => nameMatches(item.name));
            for (const project of projects) {
                match = match.concat(find(project.nestedProjects));
            }
            return match;
        };
        const raw = find(tree);
        const [referenceDataset] = raw
            .filter(dataset => dataset.reference);
        if (referenceDataset) {
            const reference = referenceDataset.reference;
            const datasetsToNavigate = raw
                .filter(dataset => dataset.reference && dataset.reference.id === reference.id);
            let tracks = [reference];
            let tracksState = [mapTrackFn(reference)];
            const addDatasetItems = (dataset) => {
                const items = dataset._lazyItems || dataset.items;
                for (const item of items) {
                    if (item.isProject) {
                        addDatasetItems(item);
                    } else if (item.format !== 'REFERENCE') {
                        tracks.push(item);
                        tracksState.push(mapTrackFn(item));
                    }
                }
            };
            for (const dataset of datasetsToNavigate) {
                addDatasetItems(dataset);
            }
            tracks = tracks.slice(0, MAXIMUM_TRACKS_TO_OPEN);
            tracksState = tracksState.slice(0, MAXIMUM_TRACKS_TO_OPEN);
            setTimeout(() => {
                this.projectContext.changeState({
                    tracks,
                    tracksState,
                    reference
                });
            }, 0);
            return Promise.resolve(true);
        }
        return Promise.resolve(false);
    }

    /**
     * Navigates to gene
     * @param {string} gene - "Reference: gene" or "gene"
     * @returns {Promise<boolean>}
     */
    navigateToGene(gene) {
        if (
            !gene ||
            !this.projectContext ||
            !this.projectContext.references ||
            this.projectContext.references.length === 0
        ) {
            return Promise.resolve(false);
        }
        const request = gene.trim();
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
                    const reference = findReferenceById(this.data.referenceId, this.projectContext);
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
                const tracks = [dataset.reference];
                const tracksState = [mapTrackFn(dataset.reference)];
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

    /**
     * Navigates to reference. Opens reference track in first dataset of such reference
     * @param {string} reference - reference name
     * @returns {Promise<boolean>}
     */
    navigateToReference(reference) {
        if (!reference || !this.projectContext) {
            return Promise.resolve(false);
        }
        const referenceObj = findReferenceByName(reference, this.projectContext);
        const payload = this.getOpenReferencePayload(referenceObj);
        if (payload) {
            setTimeout(() => {
                this.projectContext.changeState(payload);
            }, 0);
            return Promise.resolve(true);
        }
        return Promise.resolve(false);
    }

    /**
     * Navigates to coordinates
     * @param {string} coordinates - one of: "reference: chromosome: start - end", "chromosome: start - end"
     * @returns {Promise<boolean>}
     */
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
            reference = findReferenceById(this.data.referenceId, this.projectContext);
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

    onNavigated(callback) {
        this.addEventListener(events.navigate, callback);
    }
}
