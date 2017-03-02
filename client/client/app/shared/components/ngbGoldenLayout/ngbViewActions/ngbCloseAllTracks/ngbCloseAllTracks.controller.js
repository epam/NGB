export default class ngbCloseAllTracksController {

    static get UID() {
        return 'ngbCloseAllTracksController';
    }

    mdDialog;
    projectContext;

    constructor($mdDialog, projectContext) {
        this.mdDialog = $mdDialog;
        this.projectContext = projectContext;
    }

    closeAllTracks() {
        const closeFn = () => {
            this.projectContext.changeState({reference: null, tracks: null, tracksState: null});
        };
        const confirm = this.mdDialog.confirm()
            .title('Closing all tracks')
            .textContent('All opened tracks will be closed.')
            .ok('OK')
            .cancel('Cancel');
        this.mdDialog.show(confirm).then(closeFn, function() {});
    }

}