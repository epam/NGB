const hasTracks = (context) => context.tracks && context.tracks.length;
const variantsTableDownloadAction = {
    name: 'variantsTableDownload',
    isDefault: false,
    isVisible: (context, appearance) => context.containsVcfFiles &&
        !appearance.embedded && appearance.vcfDownload
};
const variantsTableColumnAction = {
    name: 'variantsTableColumn',
    isDefault: false,
    isVisible: (context, appearance) => !appearance.embedded && appearance.vcfColumns
};
const variantsTablePaginationAction = {
    name: 'variantsTablePagination',
    liStyle: {
        width: 'auto'
    },
    isDefault: false
};
const variantsLoadingIndicatorAction = {
    name: 'variantsLoadingIndicator',
    liStyle: {
        width: 'auto'
    },
    isDefault: false
};
const closeAllTracksAction = {
    name: 'closeAllTracks',
    isDefault: false,
    isVisible:(context, appearance) => !appearance.embedded && appearance.closeTracks
};
const fitAllTracksAction = {
    name: 'fitAllTracks',
    isDefault: true,
    event: 'tracks:fit:height',
    icon: 'format_line_spacing',
    label: 'Fit tracks heights',
    isVisible: (context, appearance) => (
        hasTracks(context) && context.currentChromosome &&
        !appearance.embedded && appearance.fitTracks
    )
};
const variantsResetFilterActions = {
    name: 'variantsResetFilter',
    isDefault: true,
    event: 'variants:reset:filter',
    icon: 'delete_sweep',
    label: 'Reset variants filter',
    isVisible: (context) => !context.vcfFilterIsDefault
};
const organizeTracksAction = {
    name: 'organizeTracks',
    isDefault: true,
    event: 'tracks:organize',
    icon: 'sort_by_alpha',
    label: 'Organize tracks',
    isVisible: (context, appearance) => (
        !appearance.embedded && appearance.organizeTracks &&
        hasTracks(context) && context.currentChromosome
    )
};

const genomeAnnotationsAction = {
    name: 'genomeAnnotations',
    isDefault: false,
    liStyle: {
        width: 'auto'
    },
    isVisible: (context, appearance) => hasTracks(context) && !appearance.embedded && appearance.genomeAnnotations
};

const projectInfoSectionsAction = {
    name: 'projectInfoSections',
    isDefault: false,
    liStyle: {
        width: 'auto'
    },
    isVisible: (context, appearance) => hasTracks(context) && !appearance.embedded && appearance.projectInfo
};


const tracksSelectionAction = {
    liStyle: {
        width: 'auto'
    },
    name: 'tracksSelection',
    isVisible: (context, appearance) => (
        hasTracks(context) && context.currentChromosome &&
        !appearance.embedded && appearance.tracksSelection
    )
};

const genesTableDownloadAction = {
    name: 'genesTableDownload',
    isDefault: false,
    isVisible: (context, appearance) => !!context.reference &&
        !appearance.embedded && appearance.genesDownload
};
const genesTableColumnAction = {
    name: 'genesTableColumn',
    isDefault: false,
    isVisible: (context, appearance) => !appearance.embedded && appearance.genesColumns
};
const genesResetFilterActions = {
    name: 'genesResetFilter',
    isDefault: true,
    event: 'genes:reset:filter',
    icon: 'delete_sweep',
    label: 'Reset genes filter',
    isVisible: (context) => !context.genesFilterIsDefault
};
const bookmarksTablePaginationAction = {
    name: 'bookmarksTablePagination',
    liStyle: {
        width: 'auto'
    },
    isDefault: false
};

const bookmarksResetFilterActions = {
    name: 'bookmarksResetFilter',
    isDefault: true,
    event: 'bookmarks:reset:filter',
    icon: 'delete_sweep',
    label: 'Reset sessions filter',
    isVisible: (context) => !context.bookmarksFilterIsDefault
};

const coverageTableActions = {
    name: 'coverageTableActions',
    isDefault: true,
    isVisible: (context, appearance) => !appearance.embedded && appearance.coverageActions
};
const coverageResetFilters = {
    name: 'coverageResetFilters',
    isDefault: true,
    event: 'coverage:filters:reset',
    icon: 'delete_sweep',
    label: 'Reset coverage filters',
    isVisible: (context, appearance, bamCoverageContext) => (
        bamCoverageContext && !bamCoverageContext.isFiltersDefault
    )
};

export default {
    actions: {
        closeAllTracks: closeAllTracksAction,
        fitAllTracks: fitAllTracksAction,
        genomeAnnotations: genomeAnnotationsAction,
        projectInfoSections: projectInfoSectionsAction,
        organizeTracks: organizeTracksAction,
        tracksSelection: tracksSelectionAction,
        variantsLoadingIndicator: variantsLoadingIndicatorAction,
        variantsTableDownloadAction: variantsTableDownloadAction,
        variantsResetFilter: variantsResetFilterActions,
        variantsTableColumn: variantsTableColumnAction,
        variantsTablePagination: variantsTablePaginationAction,
        genesTableDownloadAction: genesTableDownloadAction,
        genesTableColumn: genesTableColumnAction,
        genesResetFilter: genesResetFilterActions,
        bookmarksTablePagination: bookmarksTablePaginationAction,
        bookmarksResetFilter: bookmarksResetFilterActions,
        coverageTableActions: coverageTableActions,
        coverageResetFilters: coverageResetFilters
    },
    viewActions: {
        ngbBrowser: [projectInfoSectionsAction, genomeAnnotationsAction, tracksSelectionAction, fitAllTracksAction, organizeTracksAction, closeAllTracksAction],
        ngbVariantsTablePanel: [variantsTablePaginationAction, variantsLoadingIndicatorAction, variantsTableDownloadAction, variantsResetFilterActions, variantsTableColumnAction],
        ngbGenesTablePanel: [genesTableDownloadAction, genesResetFilterActions, genesTableColumnAction],
        ngbBookmarksPanel: [bookmarksTablePaginationAction, bookmarksResetFilterActions],
        ngbCoveragePanel: [coverageResetFilters, coverageTableActions]
    }
};
