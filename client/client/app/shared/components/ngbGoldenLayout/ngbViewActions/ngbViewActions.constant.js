const variantsTableDownloadAction = {
    name: 'variantsTableDownload',
    isDefault: false
};
const variantsTableColumnAction = {
    name: 'variantsTableColumn',
    isDefault: false
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
    isDefault: false
};
const fitAllTracksAction = {
    name: 'fitAllTracks',
    isDefault: true,
    event:'tracks:fit:height',
    icon: 'format_line_spacing',
    label: 'Fit tracks heights',
    isVisible: context => context.tracks && context.tracks.length && context.currentChromosome
};
const variantsResetFilterActions = {
    name: 'variantsResetFilter',
    isDefault: true,
    event: 'variants:reset:filter',
    icon: 'delete_sweep',
    label: 'Reset variants filter',
    isVisible: context => !context.vcfFilterIsDefault
};
const organizeTracksAction = {
    name: 'organizeTracks',
    isDefault: true,
    event:'tracks:organize',
    icon: 'sort_by_alpha',
    label: 'Organize tracks',
    isVisible: context => context.tracks && context.tracks.length && context.currentChromosome
};

const genomeAnnotationsAction = {
    name: 'genomeAnnotations',
    isDefault: false,
    liStyle: {
        width: 'auto'
    },
    isVisible: context => context.tracks && context.tracks.length
};

const projectInfoSectionsAction = {
    name: 'projectInfoSections',
    isDefault: false,
    liStyle: {
        width: 'auto'
    },
    isVisible: context => context.tracks && context.tracks.length && !context.currentChromosome,
};

const tracksSelectionAction = {
    liStyle: {
        width: 'auto'
    },
    name: 'tracksSelection',
    isVisible: context => context.tracks && context.tracks.length && context.currentChromosome,
};

const genesTableDownloadAction = {
    name: 'genesTableDownload',
    isDefault: false
};
const genesTableColumnAction = {
    name: 'genesTableColumn',
    isDefault: false
};
const genesResetFilterActions = {
    name: 'genesResetFilter',
    isDefault: true,
    event: 'genes:reset:filter',
    icon: 'delete_sweep',
    label: 'Reset genes filter',
    isVisible: context => !context.genesFilterIsDefault
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
        genesResetFilter: genesResetFilterActions
    },
    viewActions: {
        ngbBrowser: [projectInfoSectionsAction, genomeAnnotationsAction, tracksSelectionAction, fitAllTracksAction, organizeTracksAction, closeAllTracksAction],
        ngbVariantsTablePanel: [variantsTablePaginationAction, variantsLoadingIndicatorAction, variantsTableDownloadAction, variantsResetFilterActions, variantsTableColumnAction],
        ngbGenesTablePanel: [genesTableDownloadAction, genesResetFilterActions, genesTableColumnAction]
    }
};
