export default class ngbProjectSummaryController {
    static get UID() {
        return 'ngbProjectSummaryController';
    }

    projectContext;
    showTrackOriginalName = true;

    /**
     * @constructor
     * @param {$scope} scope
     * @param {projectDataService} dataService
     * @param {dispatcher} dispatcher
     */
    /** @ngInject */
    constructor(
        $scope,
        projectDataService,
        dispatcher,
        projectContext,
        trackNamingService,
        localDataService
    ) {
        const __dispatcher = this._dispatcher = dispatcher;
        this._dataService = projectDataService;
        this.trackNamingService = trackNamingService;
        this.projectContext = projectContext;
        this.localDataService = localDataService;
        this.$scope = $scope;
        this.showTrackOriginalName = this.localDataService.getSettings().showTrackOriginalName;

        this.INIT();
        const reloadPanel = this.INIT.bind(this);
        const self = this;
        const globalSettingsChangedHandler = (state) => {
            self.showTrackOriginalName = state.showTrackOriginalName;
        };
        this._dispatcher.on('tracks:state:change', reloadPanel);
        this._dispatcher.on('settings:change', globalSettingsChangedHandler);
        // We must remove event listener when component is destroyed.
        $scope.$on('$destroy', () => {
            __dispatcher.removeListener('tracks:state:change', reloadPanel);
            __dispatcher.removeListener('settings:change', globalSettingsChangedHandler);
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
            const customName = this.getCustomName(item) || '';
            for (const file of files) {
                if (file.type === item.format) {
                    if (!file.names.some((nameObj) => nameObj.name === name)) {
                        file.names.push({customName, name});
                    }
                    added = true;
                    break;
                }
            }
            if (!added) {
                files.push({
                    names: [{customName, name}],
                    type: item.format,
                });
            }
        }
        files.forEach(f => {
            f.names.sort((a, b) => {
                const {customName: aCustomName, name: aName} = a;
                const {customName: bCustomName, name: bName} = b;
                const aDisplayName = (aCustomName || aName).toLowerCase();
                const bDisplayName = (bCustomName || bName).toLowerCase();
                if (aDisplayName > bDisplayName) {
                    return 1;
                }
                if (aDisplayName < bDisplayName) {
                    return -1;
                }
                return 0;
            });
        });
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

    getCustomName(track) {
        return this.trackNamingService.getCustomName(track);
    }
}
