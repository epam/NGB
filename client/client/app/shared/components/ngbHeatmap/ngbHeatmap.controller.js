import Heatmap from '../../../../modules/render/heatmap';

class ngbHeatmapController {
    static get UID() {
        return 'ngbHeatmapController';
    }
    constructor(dispatcher, $scope, $element) {
        this.dispatcher = dispatcher;
        this.$scope = $scope;
        /**
         * @type {HTMLElement}
         */
        this.container = $element.find('.ngb-heatmap-container')[0];
        $scope.$on('$destroy', () => {
            if (this.heatmap) {
                this.heatmap.destroy();
                this.heatmap = undefined;
            }
        });
        this.onHeatmapSourceChanged();
        $scope.$watch('$ctrl.id', this.onHeatmapSourceChanged.bind(this));
        $scope.$watch('$ctrl.projectId', this.onHeatmapSourceChanged.bind(this));
    }

    onHeatmapSourceChanged() {
        if (this.heatmapId !== this.id || this.heatmapProjectId !== this.projectId) {
            this.heatmapId = this.id;
            this.heatmapProjectId = this.projectId;
            if (!this.heatmap) {
                this.heatmap = new Heatmap(
                    this.container,
                    this.dispatcher,
                    undefined,
                    this.checkResize
                );
            }
            this.heatmap.setDataConfig(this.heatmapId, this.heatmapProjectId);
        }
    }
}

export default ngbHeatmapController;
