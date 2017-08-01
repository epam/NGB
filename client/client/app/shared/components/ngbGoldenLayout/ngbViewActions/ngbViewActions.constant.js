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
    isVisible: (context) => {
        return context.tracks && context.tracks.length && context.currentChromosome;
    }
};
const variantsResetFilterActions = {
    name: 'variantsResetFilter',
    isDefault: true,
    event: 'variants:reset:filter',
    icon: 'delete_sweep',
    label: 'Reset variants filter',
    isVisible: (context) => {
        return !context.vcfFilterIsDefault;
    }
};
const organizeTracksAction = {
    name: 'organizeTracks',
    isDefault: true,
    event:'tracks:organize',
    icon: 'sort_by_alpha',
    label: 'Organize tracks',
    isVisible: (context) => {
        return context.tracks && context.tracks.length && context.currentChromosome;
    }
};

const genomeAnnotationsAction = {
    name: 'genomeAnnotations',
    isDefault: false,
    liStyle: {
        width: 'auto'
    },
    isVisible: (context) => {
        return context.tracks && context.tracks.length;
    }
};

export default {
    actions: {
        closeAllTracks: closeAllTracksAction,
        fitAllTracks: fitAllTracksAction,
        genomeAnnotations: genomeAnnotationsAction,
        organizeTracks: organizeTracksAction,
        variantsLoadingIndicator: variantsLoadingIndicatorAction,
        variantsResetFilter: variantsResetFilterActions,
        variantsTableColumn: variantsTableColumnAction,
        variantsTablePagination: variantsTablePaginationAction
    },
    viewActions: {
        ngbBrowser: [genomeAnnotationsAction, fitAllTracksAction, organizeTracksAction, closeAllTracksAction],
        ngbVariantsTablePanel: [variantsTablePaginationAction, variantsLoadingIndicatorAction, variantsResetFilterActions, variantsTableColumnAction]
    }
};