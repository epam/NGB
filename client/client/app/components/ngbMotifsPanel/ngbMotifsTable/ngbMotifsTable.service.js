const MOTIFS_FIRST_LEVEL_COLUMNS = ['name', 'motif', 'matches'];
const MOTIFS_SECOND_LEVEL_COLUMNS = ['reference', 'chromosome', 'start', 'end', 'strand'];

export default class ngbMotifsTableService {

    _isLevelFirst = true;

    static instance(
        dispatcher,
        uiGridConstants,
        projectContext,
        motifsContext,
        ngbMotifsPanelService
    ) {
        return new ngbMotifsTableService(
            dispatcher,
            uiGridConstants.dispatcher,
            projectContext,
            motifsContext,
            ngbMotifsPanelService
        );
    }

    constructor(
        dispatcher,
        uiGridConstants,
        projectContext,
        motifsContext,
        ngbMotifsPanelService
    ) {
        Object.assign(this, {
            dispatcher,
            uiGridConstants,
            projectContext,
            motifsContext,
            ngbMotifsPanelService
        });
    }

    get isLevelFirst () {
        return this._isLevelFirst;
    }

    set isLevelFirst (isFirst) {
        this._isLevelFirst = isFirst;
    }

    get motifsFirstLevelColumns() {
        return MOTIFS_FIRST_LEVEL_COLUMNS;
    }

    get motifsSecondLevelColumns() {
        return MOTIFS_SECOND_LEVEL_COLUMNS;
    }

    getMotifsGridColumns() {
        const headerCells = require('./ngbMotifsTable_header.tpl.html');

        const result = [];
        const columnsList = this.isLevelFirst ?
            this.motifsFirstLevelColumns : this.motifsSecondLevelColumns;
        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            const column = columnsList[i];
            columnSettings = {
                enableHiding: false,
                enableFiltering: true,
                enableSorting: true,
                field: column,
                headerCellTemplate: headerCells,
                headerTooltip: column,
                minWidth: 40,
                displayName: column,
                width: '*'
            };
            if (columnSettings) {
                result.push(columnSettings);
            }
        }
        return result;
    }

    addTracks (currentMatch) {
        const allMatches = this.ngbMotifsPanelService.getData(currentMatch.id);

        const {
            motif,
            start,
            end,
            chromosome
        } = currentMatch;
        const strand = currentMatch.strand.toLowerCase();
        const reference = this.ngbMotifsPanelService.reference;
        const referenceId = reference.id;
        const tracksOptions = {};
        const motifsTracks = (this.projectContext.tracks || [])
            .filter(track => track.format === 'MOTIFS');
        const trackId = motifsTracks.length;

        const name = (strand) => `${motif}_${strand}`;
        const motifsTrackPattern = (strand) => {
            return ({
                name: name(strand),
                format: 'MOTIFS',
                isLocal: true,
                projectId: '',
                bioDataItemId: name(strand),
                id: strand === 'positive' ? trackId : trackId + 1,
                reference,
                referenceId
            });
        };
        const motifsTrackStatePattern = (strand) => {
            return ({
                bioDataItemId: name(strand),
                duplicateId: undefined,
                projectId: '',
                isLocal: true,
                format: 'MOTIFS',
                state: {
                    motifStrand: strand
                }
            });
        };

        const referenceTrackState = {
            referenceShowForwardStrand: true,
            referenceShowReverseStrand: true,
            referenceShowTranslation: false
        };
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

        const motifsTracksNames = (motifsTracks || []).map(track => track.name);
        let positiveTrackPosition = 0;
        let negativeTrackPosition = 0;

        if (
            !motifsTracksNames.includes(name('positive')) &&
            !motifsTracksNames.includes(name('negative'))
        ) {
            positiveTrackPosition = 1;
            negativeTrackPosition = 2;
        } else {
            if (!motifsTracksNames.includes(name(strand))) {
                positiveTrackPosition = strand === 'positive' ? 1 : 0;
                negativeTrackPosition = strand === 'negative' ? 1 : 0;
            }
        }
        if (positiveTrackPosition !== 0) {
            tracksOptions.tracks.push(motifsTrackPattern('positive'));
            tracksOptions.tracksState.splice(
                referenceTrackStateIndex + positiveTrackPosition,
                0,
                motifsTrackStatePattern('positive')
            );
        }
        if (negativeTrackPosition !== 0) {
            tracksOptions.tracks.push(motifsTrackPattern('negative'));
            tracksOptions.tracksState.splice(
                referenceTrackStateIndex + negativeTrackPosition,
                0,
                motifsTrackStatePattern('negative')
            );
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
                start: Math.min(start, end),
                end: Math.max(start, end)
            },
            chromosome: {
                name: chromosome
            },
            ...tracksOptions,
            keepMotifTrack: true
        }, false, () => {
            this.setMotifs(currentMatch, allMatches);
        });
    }

    setMotifs (currentMatch, allMatches) {
        this.motifsContext.setMotifs(currentMatch, allMatches);
    }
}
