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
const closeAllTracksAction = {
    name: 'closeAllTracks',
    isDefault: false
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

export default {
    actions: {
        closeAllTracks: closeAllTracksAction,
        variantsTableColumn: variantsTableColumnAction,
        variantsTablePagination: variantsTablePaginationAction,
        variantsResetFilter: variantsResetFilterActions
    },
    viewActions: {
        ngbBrowser: [closeAllTracksAction],
        ngbVariantsTablePanel: [variantsTablePaginationAction, variantsResetFilterActions, variantsTableColumnAction]
    }
};