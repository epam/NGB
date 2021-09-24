export default class ngbProjectSummaryController {
    static get UID() {
        return 'ngbProjectSummaryController';
    }

    projectContext;
    showTrackOriginalName = true;
    datasets = [];

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
        const reloadPanel = ::this.INIT;
        const self = this;
        const globalSettingsChangedHandler = (state) => {
            self.showTrackOriginalName = state.showTrackOriginalName;
        };
        this._dispatcher.on('tracks:state:change', reloadPanel);
        this._dispatcher.on('metadata:change', reloadPanel);
        this._dispatcher.on('settings:change', globalSettingsChangedHandler);
        // We must remove event listener when component is destroyed.
        $scope.$on('$destroy', () => {
            __dispatcher.removeListener('tracks:state:change', reloadPanel);
            __dispatcher.removeListener('metadata:change', reloadPanel);
            __dispatcher.removeListener('settings:change', globalSettingsChangedHandler);
        });
    }

    INIT() {
        if (!this.projectContext.tracks)
            return;

        const files = [];
        const items = this.projectContext.tracks;
        const datasetsId = items.reduce((datasetsID, item) => {
            const id = item && item.project ? item.project.id: null;
            if (id) {
                if (!datasetsID.includes(id)) {
                    datasetsID.push(id);
                }
            }
            return datasetsID;
        }, []);
        this.datasets = this.projectContext.datasets.filter(project => datasetsId.includes(project.id));
        for (const item of items) {
            let added = false;
            const name = this.getTrackFileName(item);
            const customName = this.getCustomName(item) || '';
            const annotationFiles = JSON.parse(localStorage[`${this.projectContext.reference.name.toLowerCase()}-annotations`]);
            let metadata;
            if (
                item && item.name &&
                !annotationFiles.includes(item.name.toLowerCase())
            ) {
                metadata = item.metadata;
            } else if (
                this.projectContext &&
                this.projectContext.datasets &&
                this.projectContext.datasets.length > 0
            ) {
                metadata = this.datasets.reduce((result, dataset) => {
                    const [itemInfo] = dataset.items
                        .filter(dsItem => dsItem.name.toLowerCase() === item.name.toLowerCase() && dsItem.id=== item.id);
                    if (itemInfo) {
                        result = itemInfo.metadata;
                    }
                    return result;
                }, {});
            }
            for (const file of files) {
                if (file.type === item.format) {
                    if (!file.names.some((nameObj) => nameObj.name === name)) {
                        file.names.push({customName, name, metadata});
                    }
                    added = true;
                    break;
                }
            }
            if (!added) {
                files.push({
                    names: [{customName, name, metadata}],
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
