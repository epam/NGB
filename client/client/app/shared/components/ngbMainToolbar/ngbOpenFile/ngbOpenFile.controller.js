const MODE_URL = 'url';
const MODE_NGB = 'ngb';

export default class ngbOpenFileController {
    static get UID() {
        return 'ngbOpenFileController';
    }

    $mdDialog;
    projectContext;

    /* @ngInject */
    constructor($mdDialog, projectContext) {
        this.showContentMenu = false;
        this.projectContext = projectContext;
        Object.assign(this, {
            $mdDialog
        });
    }

    openMenu($mdOpenMenu, ev) {
        this.showContentMenu = !this.showContentMenu;
        $mdOpenMenu(ev);
    }

    openFileFromNGBServer() {
        this._openFileDialog(MODE_NGB);
    }

    openFileFromURL() {
        this._openFileDialog(MODE_URL);
    }

    _openFileDialog(mode) {
        const self = this;
        let template = null;
        switch (mode) {
            case MODE_URL:
                template = require('./ngbOpenFileFromUrl/ngbOpenFileFromUrl.dialog.tpl.html');
                break;
            case MODE_NGB:
                template = require('./ngbOpenFileFromNGBServer/ngbOpenFileFromNGBServer.dialog.tpl.html');
                break;
        }
        if (!template) {
            return;
        }
        this.$mdDialog.show({
            template: template,
            controller: function ($scope, $mdDialog) {
                $scope.tracks = [];
                $scope.close = () => $mdDialog.hide();
                $scope.loadingDisabled = () => $scope.tracks.length === 0;
                $scope.tracksCount = () => $scope.tracks.length;
                $scope.load = () => {
                    self.loadTracks($scope.tracks);
                    $mdDialog.hide();
                };
            },
            clickOutsideToClose: true
        });
    }

    loadTracks(selectedFiles) {
        const mapFn = (selectedFile) => {
            return {
                openByUrl: true,
                isLocal: true,
                format: selectedFile.format,
                id: selectedFile.path,
                bioDataItemId: selectedFile.path,
                referenceId: selectedFile.reference.id,
                reference: selectedFile.reference,
                name: selectedFile.path,
                indexPath: selectedFile.index,
                projectId: selectedFile.reference.name
            };
        };
        const mapTrackStateFn = (t) => {
            return {
                bioDataItemId: t.name,
                name: t.name,
                index: t.indexPath,
                isLocal: true,
                format: t.format,
                projectId: t.projectId
            };
        };
        let tracks = selectedFiles.map(mapFn).filter(
            track => this.projectContext.tracks.filter(t => t.isLocal && t.name.toLowerCase() === track.name.toLowerCase()).length === 0);

        if (tracks.length === 0) {
            return;
        }

        for (let i = 0; i < tracks.length; i++) {
            this.projectContext.addLastLocalTrack(tracks[i]);
        }

        const [reference] = tracks.map(t => t.reference);
        reference.projectId = reference.name;
        reference.isLocal = true;

        if (!this.projectContext.reference || this.projectContext.reference.bioDataItemId !== reference.bioDataItemId) {
            const _tracks = [reference, ...tracks];
            const tracksState = _tracks.map(mapTrackStateFn);
            this.projectContext.changeState({reference, tracks: _tracks, tracksState, shouldAddAnnotationTracks: true});
        } else {
            const _tracks = [...this.projectContext.tracks, ...tracks];
            const tracksState = [...(this.projectContext.tracksState || []), ...tracks.map(mapTrackStateFn)];
            this.projectContext.changeState({reference: this.projectContext.reference, tracks: _tracks, tracksState});
        }
    }
}
