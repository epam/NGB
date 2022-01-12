const MOTIFS_SEARCH_PARAMS_COLUMNS = ['name', 'motif', 'search type'];
const MOTIFS_SEARCH_RESULTS_COLUMNS = ['chromosome', 'start', 'end', 'strand'];
const WHOLE_GENOME = 'WHOLE_GENOME';
const PAGE_SIZE = 100;

export default class ngbMotifsTableService {

    _currentParams = {};

    static instance(
        dispatcher,
        projectContext,
        motifsContext,
        ngbMotifsPanelService
    ) {
        return new ngbMotifsTableService(
            dispatcher,
            projectContext,
            motifsContext,
            ngbMotifsPanelService
        );
    }

    constructor(
        dispatcher,
        projectContext,
        motifsContext,
        ngbMotifsPanelService
    ) {
        Object.assign(this, {
            dispatcher,
            projectContext,
            motifsContext,
            ngbMotifsPanelService
        });
    }

    get isShowParamsTable () {
        return this.ngbMotifsPanelService.isShowParamsTable;
    }

    get motifsSearchParamsColumns() {
        return MOTIFS_SEARCH_PARAMS_COLUMNS;
    }

    get motifsSearchResultsColumns() {
        return MOTIFS_SEARCH_RESULTS_COLUMNS;
    }

    get positiveStrand () {
        return this.ngbMotifsPanelService.positive.toLowerCase();
    }

    get negativeStrand () {
        return this.ngbMotifsPanelService.negative.toLowerCase();
    }

    get wholeGenomeType () {
        return WHOLE_GENOME;
    }

    get pageSize () {
        return PAGE_SIZE;
    }

    get currentParams () {
        return this._currentParams;
    }

    set currentParams (params) {
        if (JSON.stringify(params) === '{}') {
            this._currentParams = {};
        } else {
            const referenceId = this.projectContext.reference.id;
            const setSearchType = (type) => {
                return type === this.ngbMotifsPanelService.referenceType ?
                    this.wholeGenomeType : type;
            };
            this._currentParams = {
                referenceId,
                motif: params.motif,
                name: params.name || params.motif,
                searchType: setSearchType(params['search type']),
                pageSize: this.pageSize,
            };
        }
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
            name: searchName,
            motif,
            searchType
        } = this.currentParams;
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

        const strand = this.ngbMotifsPanelService.getStrand(row.strand).toLowerCase();
        const name = (strand) => `${searchName || motif}_${strand}`;
        const reference = this.projectContext.reference;

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
                id: strand === this.positiveStrand ? trackId : trackId + 1,
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
                    motifStrand: strand,
                    motif
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
            !motifsTracksNames.includes(name(this.positiveStrand)) &&
            !motifsTracksNames.includes(name(this.negativeStrand))
        ) {
            positiveTrackPosition = 1;
            negativeTrackPosition = 2;
        } else {
            if (!motifsTracksNames.includes(name(strand))) {
                positiveTrackPosition = strand === this.positiveStrand ? 1 : 0;
                negativeTrackPosition = strand === this.negativeStrand ? 1 : 0;
            }
        }
        if (positiveTrackPosition !== 0) {
            tracksOptions.tracks.push(motifsTrackPattern(this.positiveStrand));
            tracksOptions.tracksState.splice(
                referenceTrackStateIndex + positiveTrackPosition,
                0,
                motifsTrackStatePattern(this.positiveStrand)
            );
        }
        if (negativeTrackPosition !== 0) {
            tracksOptions.tracks.push(motifsTrackPattern(this.negativeStrand));
            tracksOptions.tracksState.splice(
                referenceTrackStateIndex + negativeTrackPosition,
                0,
                motifsTrackStatePattern(this.negativeStrand)
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
