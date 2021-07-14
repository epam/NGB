export default class ngbBlastSearchAlignmentService {
    static instance(blastContext, projectContext, genomeDataService, dispatcher) {
        return new ngbBlastSearchAlignmentService(blastContext, projectContext, genomeDataService, dispatcher);
    }
    constructor(blastContext, projectContext, genomeDataService, dispatcher) {
        this.blastContext = blastContext;
        this.projectContext = projectContext;
        this.genomeDataService = genomeDataService;
        this.dispatcher = dispatcher;
        this.chromosomesCache = new Map();
    }

    setAlignments (searchResult, search, featureCoords) {
        this.blastContext.setAlignments((searchResult || {}).alignments || [], search, featureCoords);
    }

    async fetchChromosomes (referenceId) {
        if (!this.chromosomesCache.has(+referenceId)) {
            const promise = new Promise((resolve) => {
                this.genomeDataService.loadAllChromosomes(referenceId)
                    .then(chromosomes => {
                        if (chromosomes && chromosomes.length) {
                            resolve(chromosomes);
                        } else {
                            resolve([]);
                        }
                    })
                    .catch((e) => {
                        // eslint-disable-next-line
                        console.warn('Error fetching chromosomes:', e.message);
                        resolve([]);
                    });
            });
            this.chromosomesCache.set(+referenceId, promise);
        }
        return await this.chromosomesCache.get(+referenceId);
    }

    async getNavigationInfo (alignment, search, featureCoords) {
        if (!alignment || !search) {
            return null;
        }
        const {
            sequenceStart,
            sequenceEnd,
            sequenceAccessionVersion: sequenceId,
            sequenceTaxId
        } = alignment;
        if (featureCoords) {
            const {
                start,
                end,
                referenceId,
                chromosomeId,
            } = featureCoords;
            const chromosomes = await this.fetchChromosomes(referenceId);
            const [chromosome] = chromosomes.filter(chr => chr.id === chromosomeId);
            if (!chromosome) {
                return null;
            }
            return {
                start: start,
                end: end,
                chromosome: chromosome.name,
                referenceId: referenceId,
            };
        } else {
            const [reference] = (this.projectContext.references || [])
                .filter(reference => reference.species && +(reference.species.taxId) === +sequenceTaxId);
            const referenceId = reference ? reference.id : undefined;
            if (
                !sequenceStart ||
                !sequenceEnd ||
                !sequenceTaxId ||
                !referenceId ||
                !sequenceId
            ) {
                return null;
            }
            const chromosomes = await this.fetchChromosomes(referenceId);
            const [chromosome] = chromosomes.filter(chr => chr.name.toLowerCase() === `${sequenceId}`.toLowerCase());
            if (!chromosome) {
                return null;
            }
            return {
                start: sequenceStart,
                end: sequenceEnd,
                chromosome: sequenceId,
                referenceId,
            };
        }
    }

    async navigationAvailable (alignment, search, featureCoords) {
        const info = await this.getNavigationInfo(alignment, search, featureCoords);
        return !!info;
    }

    async navigateToTracks (alignment, searchResult, search, featureCoords) {
        const navigationInfo = await this.getNavigationInfo(alignment, search, featureCoords);
        if (navigationInfo) {
            const {
                start,
                end,
                chromosome: chromosomeName,
                referenceId
            } = navigationInfo;
            const range = Math.abs(end - start);
            const rangeStart = Math.min(start, end) - range;
            const rangeEnd = Math.max(start, end) + range;
            const tracksOptions = {};
            const [reference] = (this.projectContext.references || [])
                .filter(r => r.id === referenceId);
            if (!reference) {
                // eslint-disable-next-line no-console
                console.warn(`Reference ${referenceId} not found`);
                return;
            }
            const blastTrack = {
                name: 'Search results',
                format: 'BLAST',
                isLocal: true,
                projectId: '',
                bioDataItemId: 'Search results',
                id: 0,
                reference,
                referenceId,
            };
            const referenceTrackState = {
                referenceShowForwardStrand: true,
                referenceShowReverseStrand: true,
                referenceShowTranslation: false
            };
            if (referenceId !== this.projectContext.referenceId || !this.projectContext.reference) {
                tracksOptions.reference = reference;
                tracksOptions.shouldAddAnnotationTracks = true;
                tracksOptions.tracks = [reference, blastTrack].map(track => ({
                    ...track,
                    projectId: '',
                    isLocal: true
                }));
                tracksOptions.tracksState = [
                    {...reference, state: referenceTrackState},
                    blastTrack
                ].map(track => ({
                    bioDataItemId: track.name,
                    duplicateId: track.duplicateId,
                    projectId: '',
                    format: track.format,
                    isLocal: true,
                    state: track.state
                }));
            } else if (
                (this.projectContext.tracks || [])
                    .filter(track => track.format === 'BLAST').length === 0
            ) {
                tracksOptions.tracks = (this.projectContext.tracks || []);
                tracksOptions.tracksState = (this.projectContext.tracksState || []);
                const [existingReferenceTrackState] = tracksOptions.tracksState
                    .filter(track => track.format === 'REFERENCE');
                if (existingReferenceTrackState) {
                    existingReferenceTrackState.state = {
                        ...(existingReferenceTrackState.state || {}),
                        ...referenceTrackState
                    };
                }
                const referenceTrackStateIndex = tracksOptions
                    .tracksState.indexOf(existingReferenceTrackState);
                tracksOptions.tracks.push(blastTrack);
                tracksOptions.tracksState.splice(referenceTrackStateIndex + 1, 0, {
                    bioDataItemId: blastTrack.name,
                    duplicateId: blastTrack.duplicateId,
                    projectId: blastTrack.projectId,
                    isLocal: true,
                    format: blastTrack.format
                });
            }
            const [currentReferenceTrack] = this.projectContext.getActiveTracks()
                .filter(track => track.format === 'REFERENCE');
            if (currentReferenceTrack && currentReferenceTrack.instance) {
                currentReferenceTrack.instance.state.referenceShowForwardStrand = true;
                currentReferenceTrack.instance.state.referenceShowReverseStrand = true;
                currentReferenceTrack.instance.reportTrackState(true);
                currentReferenceTrack.instance.requestRender();
            }
            this.projectContext.changeState({
                viewport: {
                    start: rangeStart,
                    end: rangeEnd
                },
                chromosome: {
                    name: chromosomeName
                },
                keepBLASTTrack: true,
                ...tracksOptions
            }, false, () => {
                this.setAlignments(searchResult, search, featureCoords);
            });
        }
    }

    async getNavigationToChromosomeInfo(searchResult) {
        const {
            taxId,
            sequenceAccessionVersion: sequenceId
        } = searchResult;
        if (
            !searchResult.alignments ||
            !searchResult.alignments.length ||
            !searchResult.alignments[0].sequenceLength
        ) {
            return null;
        }
        const end = searchResult.alignments[0].sequenceLength;
        if (!taxId) {
            return null;
        }

        const [reference] = (this.projectContext.references || [])
            .filter(reference => reference.species &&
                Number(reference.species.taxId) === Number(taxId));
        if (!reference) {
            return null;
        }

        const referenceId = reference ? reference.id : undefined;
        if (!referenceId) {
            return null;
        }

        const chromosomes = await this.fetchChromosomes(referenceId);
        const [chromosome] = chromosomes.filter(chr => chr === `${sequenceId}`.toLowerCase());
        if (!chromosome) {
            return null;
        }

        return {
            start: 1,
            end,
            chromosome: sequenceId,
            referenceId
        };
    }

    async navigationToChromosomeAvailable (searchResult) {
        const info = await this.getNavigationToChromosomeInfo(searchResult);
        return Boolean(info);
    }

    async navigateToChromosome (searchResult, search) {
        const navigationInfo = await this.getNavigationToChromosomeInfo(searchResult);
        if (navigationInfo) {
            const {
                start,
                end,
                chromosome: chromosomeName,
                referenceId
            } = navigationInfo;
            const tracksOptions = {};
            const [reference] = (this.projectContext.references || [])
                .filter(r => r.id === referenceId);
            if (!reference) {
                // eslint-disable-next-line no-console
                console.warn(`Reference ${referenceId} not found`);
                return;
            }
            const blastTrack = {
                name: 'Search results',
                format: 'BLAST',
                isLocal: true,
                projectId: '',
                bioDataItemId: 'Search results',
                id: 0,
                reference,
                referenceId,
            };
            const referenceTrackState = {
                referenceShowForwardStrand: true,
                referenceShowReverseStrand: true,
                referenceShowTranslation: false
            };
            if (referenceId !== this.projectContext.referenceId || !this.projectContext.reference) {
                tracksOptions.reference = reference;
                tracksOptions.shouldAddAnnotationTracks = true;
                tracksOptions.tracks = [reference, blastTrack].map(track => ({
                    ...track,
                    projectId: '',
                    isLocal: true
                }));
                tracksOptions.tracksState = [
                    {...reference, state: referenceTrackState},
                    blastTrack
                ].map(track => ({
                    bioDataItemId: track.name,
                    duplicateId: track.duplicateId,
                    projectId: '',
                    format: track.format,
                    isLocal: true,
                    state: track.state
                }));
            } else if (
                (this.projectContext.tracks || [])
                    .filter(track => track.format === 'BLAST').length === 0
            ) {
                tracksOptions.tracks = (this.projectContext.tracks || []);
                tracksOptions.tracksState = (this.projectContext.tracksState || []);
                const [existingReferenceTrackState] = tracksOptions.tracksState
                    .filter(track => track.format === 'REFERENCE');
                if (existingReferenceTrackState) {
                    existingReferenceTrackState.state = {
                        ...(existingReferenceTrackState.state || {}),
                        ...referenceTrackState
                    };
                }
                const referenceTrackStateIndex = tracksOptions
                    .tracksState.indexOf(existingReferenceTrackState);
                tracksOptions.tracks.push(blastTrack);
                tracksOptions.tracksState.splice(referenceTrackStateIndex + 1, 0, {
                    bioDataItemId: blastTrack.name,
                    duplicateId: blastTrack.duplicateId,
                    projectId: blastTrack.projectId,
                    isLocal: true,
                    format: blastTrack.format
                });
            }
            const [currentReferenceTrack] = this.projectContext.getActiveTracks()
                .filter(track => track.format === 'REFERENCE');
            if (currentReferenceTrack && currentReferenceTrack.instance) {
                currentReferenceTrack.instance.state.referenceShowForwardStrand = true;
                currentReferenceTrack.instance.state.referenceShowReverseStrand = true;
                currentReferenceTrack.instance.reportTrackState(true);
                currentReferenceTrack.instance.requestRender();
            }
            this.projectContext.changeState({
                viewport: {
                    start,
                    end
                },
                chromosome: {
                    name: chromosomeName
                },
                keepBLASTTrack: true,
                ...tracksOptions
            }, false, () => {
                this.setAlignments(searchResult, search);
            });
        }
    }
}
