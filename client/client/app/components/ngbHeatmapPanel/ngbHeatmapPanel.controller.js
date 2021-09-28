export default class ngbHeatmapPanelController {
    static get UID() {
        return 'ngbHeatmapPanelController';
    }
    constructor($scope, ngbHeatmapPanelService) {
        this.service = ngbHeatmapPanelService;
    }

    get loading() {
        return !this.service || this.service.loading;
    }

    get heatmaps() {
        return this.service.heatmaps;
    }

    get heatmap() {
        return this.service.selectedHeatmap;
    }

    set heatmap(heatmap) {
        this.service.selectedHeatmap = heatmap;
    }

    get heatmapId() {
        if (this.heatmap) {
            return this.heatmap.id;
        }
        return undefined;
    }

    get heatmapProjectId() {
        if (this.heatmap) {
            return this.heatmap.project ? this.heatmap.project.id : this.heatmap.projectIdNumber;
        }
        return undefined;
    }
}
