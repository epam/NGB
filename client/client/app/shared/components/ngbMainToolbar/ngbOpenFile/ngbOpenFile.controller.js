
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
            case MODE_URL: template = require('./ngbOpenFileFromUrl/ngbOpenFileFromUrl.dialog.tpl.html'); break;
            case MODE_NGB: template = require('./ngbOpenFileFromNGBServer/ngbOpenFileFromNGBServer.dialog.tpl.html'); break;
        }
        if (!template) {
            return;
        }
        this.$mdDialog.show({
            template: template,
            controller: function($scope, $mdDialog) {
                $scope.tracks = [];
                $scope.close = () => $mdDialog.hide();
                $scope.loadingDisabled = () => $scope.tracks.length === 0;
                $scope.load = () => {
                    self.loadTracks($scope.tracks);
                    $mdDialog.hide();
                };
            },
            clickOutsideToClose: true
        });
    }

    loadTracks(selectedFiles) {
        const openedByUrlProjectName = this.projectContext.openedByUrlProjectName;
        const mapFn = (selectedFile) => {
            return {
                openByUrl: true,
                format: selectedFile.format,
                id: selectedFile.path,
                bioDataItemId: selectedFile.path,
                referenceId: selectedFile.reference.id,
                reference: selectedFile.reference,
                name: selectedFile.path,
                indexPath: selectedFile.index,
                projectId: openedByUrlProjectName
            }
        };
        const mapTrackStateFn = (t) => {
            return {
                bioDataItemId: t.name,
                name: t.name,
                index: t.indexPath,
                format: t.format,
                projectId: openedByUrlProjectName
            };
        };
        let tracks = selectedFiles.map(mapFn);
        tracks.forEach(t => t.projectId = openedByUrlProjectName);
        const [reference] = tracks.map(t => t.reference);
        reference.projectId = openedByUrlProjectName;
        if (!this.projectContext.reference || this.projectContext.reference.bioDataItemId !== reference.bioDataItemId) {
            if (reference.geneFile) {
                reference.geneFile.projectId = openedByUrlProjectName;
                tracks = [reference.geneFile, ...tracks];
            }
            const _tracks = [reference, ...tracks];
            const tracksState = _tracks.map(mapTrackStateFn);
            this.projectContext.changeState({reference, tracks: _tracks, tracksState});
        } else {
            const _tracks = [...this.projectContext.tracks, ...tracks];
            const tracksState = [...(this.projectContext.tracksState || []), ...tracks.map(mapTrackStateFn)];
            this.projectContext.changeState({reference: this.projectContext.reference, tracks: _tracks, tracksState});
        }
    }

}
