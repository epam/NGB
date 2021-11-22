import baseController from '../../shared/baseController';

function heatmapNameSorter(a, b) {
    const aName = (a.prettyName || a.name || '').toLowerCase();
    const bName = (b.prettyName || b.name || '').toLowerCase();
    if (!aName) {
        return 1;
    }
    if (!bName) {
        return -1;
    }
    if (aName < bName) {
        return -1;
    }
    if (aName > bName) {
        return 1;
    }
    return 0;
}

export default class ngbHeatmapPanelController extends baseController {
    static get UID() {
        return 'ngbHeatmapPanelController';
    }

    sessionLoadStarted = false;

    events = {
        'session:load:started': this.onSessionLoad.bind(this),
    };

    constructor($scope, appLayout, dispatcher, heatmapContext) {
        super();
        Object.assign(this, {
            $scope,
            appLayout,
            dispatcher,
            heatmapContext
        });
        this.onHeatmapNavigationCallback = this.onHeatmapNavigation.bind(this);
        this.onHeatmapOptionsChangeCallback = this.onHeatmapOptionsChange.bind(this);
        this.heatmapContext.selectFirst();
        $scope.$on('$destroy', () => {
            if (!this.sessionLoadStarted) {
                this.heatmapContext.heatmap = undefined;
            }
        });
        this.initEvents();
    }

    get loading() {
        return !this.heatmapContext || !this.heatmapContext.initialized;
    }

    get heatmaps() {
        return this.heatmapContext.heatmaps || [];
    }

    get previousHeatmap() {
        return this.heatmaps
            .filter(heatmap =>
                !heatmap.isTrack
                && !heatmap.isAnnotation
                && heatmap.id !== this.heatmapId)
            .pop();
    }

    get heatmapsFromTrack() {
        return this.heatmaps.filter(heatmap => heatmap.isTrack).sort(heatmapNameSorter);
    }

    get heatmapsFromAnnotations() {
        return this.heatmaps.filter(heatmap => heatmap.isAnnotation).sort(heatmapNameSorter);
    }

    get heatmap() {
        return this.heatmapContext.heatmap;
    }

    set heatmap(heatmap) {
        this.heatmapContext.heatmap = heatmap;
        this.heatmapContext.heatmapNavigationOccurred = false;
        this.heatmapContext.refreshHeatmapsList();
    }

    get heatmapId() {
        if (this.heatmap) {
            return this.heatmap.id;
        }
        return undefined;
    }

    set heatmapId(heatmapId) {
        const [heatmap] = this.heatmaps.filter(h => h.id === heatmapId);
        this.heatmap = heatmap;
    }

    get heatmapProjectId() {
        if (this.heatmap) {
            return this.heatmap.project ? this.heatmap.project.id : this.heatmap.projectIdNumber;
        }
        return undefined;
    }

    get heatmapReferenceId() {
        if (this.heatmap) {
            return this.heatmap.referenceId;
        }
        return undefined;
    }

    onHeatmapNavigation() {
        this.heatmapContext.heatmapNavigationOccurred = true;
    }

    /**
     *
     * @param {HeatmapViewOptions} options
     */
    onHeatmapOptionsChange(options) {
        if (
            options &&
            this.heatmapContext &&
            this.heatmapContext.heatmap &&
            options.dataConfig &&
            Number(options.dataConfig.id) === Number(this.heatmapContext.heatmap.id)
        ) {
            this.heatmapContext.setCurrentState(options.serialize());
        }
    }

    onSessionLoad() {
        this.sessionLoadStarted = true;
    }
}
