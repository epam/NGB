import Heatmap from '../../../../modules/render/heatmap';

class ngbHeatmapController {
    static get UID() {
        return 'ngbHeatmapController';
    }
    constructor(dispatcher, projectContext, $scope, $element) {
        this.dispatcher = dispatcher;
        this.$scope = $scope;
        this.projectContext = projectContext;
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
        $scope.$watch('$ctrl.renderOnChange', () => {
            if (this.heatmap) {
                setTimeout(this.heatmap.render.bind(this.heatmap), 0);
            }
        });
    }

    onHeatmapSourceChanged() {
        if (this.heatmapId !== this.id || this.heatmapProjectId !== this.projectId) {
            this.heatmapId = this.id;
            this.heatmapProjectId = this.projectId;
            if (!this.heatmap) {
                this.heatmap = new Heatmap(
                    this.container,
                    this.dispatcher,
                    this.projectContext,
                    undefined,
                    this.checkResize
                );
                this.heatmap.onNavigated(this.onHeatmapNavigationCallback.bind(this));
            }
            this.heatmap.referenceId = this.referenceId;
            this.heatmap.setDataConfig(this.heatmapId, this.heatmapProjectId);
        }
    }

    onHeatmapNavigationCallback() {
        if (typeof this.onHeatmapNavigation === 'function') {
            this.onHeatmapNavigation();
        }
    }
}

export default ngbHeatmapController;
