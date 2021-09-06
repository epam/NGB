const MOTIFS_SEARCH_PARAMS_COLUMNS = ['search type', 'name', 'motif'];
const MOTIFS_SEARCH_RESULTS_COLUMNS = ['reference', 'chromosome', 'start', 'end', 'strand'];

export default class ngbMotifsTableService {

    _isShowParamsTable = true;

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

    get isShowParamsTable () {
        return this._isShowParamsTable;
    }

    set isShowParamsTable (value) {
        this._isShowParamsTable = value;
    }

    get motifsSearchParamsColumns() {
        return MOTIFS_SEARCH_PARAMS_COLUMNS;
    }

    get motifsSearchResultsColumns() {
        return MOTIFS_SEARCH_RESULTS_COLUMNS;
    }

    getMotifsGridColumns() {
        const headerCells = require('./ngbMotifsTable_header.tpl.html');

        const result = [];
        const columnsList = this.isShowParamsTable ?
            this.motifsSearchParamsColumns : this.motifsSearchResultsColumns;
        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            const column = columnsList[i];
            columnSettings = {
                enableHiding: false,
                enableFiltering: false,
                enableSorting: false,
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


    async addTracks (row) {
        const {
            chromosome,
            start,
            end,
        } = row;
        const {
            referenceId,
            motif,
            searchType,
        } = this.ngbMotifsPanelService.currentParams;
        const currentMatch = {
            chromosome,
            start,
            end,
            referenceId,
            motif,
            searchType
        };

        const range = Math.abs(end - start);
        const rangeStart = Math.min(start, end) - range;
        const rangeEnd = Math.max(start, end) + range;

        const strand = row.strand.toLowerCase();
        const name = (strand) => `${motif}_${strand}`;
        const reference = this.ngbMotifsPanelService.reference;

        const tracksOptions = {};
        const motifsTracks = (this.projectContext.tracks || [])
            .filter(track => track.format === 'MOTIFS');
        const trackId = this.ngbMotifsPanelService.requestNumber;

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
                start: rangeStart,
                end: rangeEnd
            },
            chromosome: {
                name: chromosome
            },
            ...tracksOptions,
            keepMotifTrack: true
        }, false, () => {
            this.setMotif(currentMatch);
        });
    }

    setMotif (currentMatch) {
        this.motifsContext.setMotif(currentMatch);
    }
}
