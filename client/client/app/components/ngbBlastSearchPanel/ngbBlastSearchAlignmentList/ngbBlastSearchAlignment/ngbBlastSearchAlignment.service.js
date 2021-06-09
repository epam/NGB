export default class ngbBlastSearchAlignmentService {
    static instance(blastContext, projectContext, dispatcher) {
        return new ngbBlastSearchAlignmentService(blastContext, projectContext, dispatcher);
    }
    constructor(blastContext, projectContext, dispatcher) {
        this.blastContext = blastContext;
        this.projectContext = projectContext;
        this.dispatcher = dispatcher;
    }

    getNavigationInfo (alignment, search) {
        if (!alignment || !search) {
            return null;
        }
        const {tool} = search;
        if (/^(blastp|blastx)$/i.test(tool)) {
            return null;
        }
        const {
            sequenceStart,
            sequenceEnd,
            sequenceId,
            sequenceTaxId
        } = alignment;
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
        return {
            start: sequenceStart,
            end: sequenceEnd,
            chromosome: sequenceId,
            referenceId
        };
    }

    navigationAvailable (alignment, search) {
        return !!this.getNavigationInfo(alignment, search);
    }

    navigateToTracks (alignment, search) {
        const navigationInfo = this.getNavigationInfo(alignment, search);
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
                this.blastContext.setAlignment(alignment, search);
            });
        }
    }
}
