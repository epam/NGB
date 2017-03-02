const variantsTableColumnAction = 'variantsTableColumn';
const closeAllTracksAction = 'closeAllTracks';

export default {
    actions: {
        closeAllTracks: closeAllTracksAction,
        variantsTableColumn: variantsTableColumnAction
    },
    viewActions: {
        ngbBrowser: [closeAllTracksAction],
        ngbVariantsTablePanel: [variantsTableColumnAction]
    }
};