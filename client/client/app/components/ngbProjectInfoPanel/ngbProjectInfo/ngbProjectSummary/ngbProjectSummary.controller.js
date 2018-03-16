export default class ngbProjectSummaryController {
    static get UID() {
        return 'ngbProjectSummaryController';
    }

    projectContext;

    /**
     * @constructor
     * @param {$scope} scope
     * @param {projectDataService} dataService
     * @param {dispatcher} dispatcher
     */
    /** @ngInject */
    constructor($scope, projectDataService, dispatcher, projectContext) {
        const __dispatcher = this._dispatcher = dispatcher;
        this._dataService = projectDataService;
        this.projectContext = projectContext;
        this.$scope = $scope;

        this.INIT();
        const reloadPanel = ::this.INIT;
        this._dispatcher.on('tracks:state:change', reloadPanel);
        // We must remove event listener when component is destroyed.
        $scope.$on('$destroy', () => {
            __dispatcher.removeListener('tracks:state:change', reloadPanel);
        });
    }

    INIT() {
        if (!this.projectContext.tracks)
            return;

        const files = [];
        const items = this.projectContext.tracks;
        for (const item of items) {
            let added = false;
            const name = this.getTrackFileName(item);
            for (const file of files) {
                if (file.type === item.format) {
                    if (file.names.indexOf(name) === -1) {
                        file.names.push(name);
                    }
                    added = true;
                    break;
                }
            }
            if (!added) {
                files.push({names: [name], type: item.format});
            }
        }
        this.files = files;
    }

    getTrackFileName(track) {
        if (!track.isLocal) {
            return track.prettyName || track.name;
        } else {
            const fileName = track.name;
            if (!fileName || !fileName.length) {
                return null;
            }
            let list = fileName.split('/');
            list = list[list.length - 1].split('\\');
            return list[list.length - 1];
        }
    }
}