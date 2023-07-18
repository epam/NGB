import * as d3 from 'd3';
import ngbDiseasesControllerBase from '../ngbDiseases.controler.base';

const CONTROLS_ROW_HEIGHT = 40;
const TOOLTIP_SHOW_TIMEOUT_MS = 1000;
const TOOLTIP_HIDE_TIMEOUT_MS = 250;

export default class ngbDiseasesChartControllerBase extends ngbDiseasesControllerBase {
    constructor(
        $scope,
        $timeout,
        $element,
        dispatcher,
        ngbDiseasesChartService
    ) {
        super($scope, $timeout, dispatcher);
        this.ngbDiseasesChartService = ngbDiseasesChartService;
        this.element = $element[0];
        this.svgContainer = $element.find('.ngb-diseases-chart-svg-container')[0];
        if (this.svgContainer) {
            $(this.svgContainer).attr('top', CONTROLS_ROW_HEIGHT);
        }
        this.tooltip = $element.find('.ngb-disease-tooltip')[0];
        this.hoveredDisease = undefined;
        this.startResizeRAF();
    }

    get loading() {
        return this.ngbDiseasesChartService.loading;
    }

    get error() {
        return this.ngbDiseasesChartService.error;
    }

    get dataLoading() {
        return this.loading && !this.results;
    }

    get results() {
        if (this.ngbDiseasesChartService) {
            return this.ngbDiseasesChartService.results;
        }
        return null;
    }

    get minScore() {
        return this.ngbDiseasesChartService.minScore;
    }

    get maxScore() {
        return this.ngbDiseasesChartService.maxScore;
    }

    get scoreStep() {
        return this.ngbDiseasesChartService.scoreStep;
    }

    get scoreFilter() {
        return this.ngbDiseasesChartService.scoreFilter;
    }

    set scoreFilter(value) {
        if (this.ngbDiseasesChartService.scoreFilter !== value) {
            this.ngbDiseasesChartService.scoreFilter = value;
            this.draw();
        }
    }

    async initialize() {
        await super.initialize();
        await this.loadData();
    }

    $onDestroy() {
        this.cancelResizeRAF();
        this.clearTimeouts();
    }

    startResizeRAF() {
        let width, height;
        const animationFrameFn = () => {
            if (
                this.element && (
                    width !== this.element.clientWidth ||
                    height !== this.element.clientHeight
                )
            ) {
                width = this.element.clientWidth;
                height = this.element.clientHeight;
                this.draw();
            }
            this.raf = requestAnimationFrame(animationFrameFn);
        };
        animationFrameFn();
    }

    cancelResizeRAF() {
        cancelAnimationFrame(this.raf);
    }

    async sourceChanged() {
        await this.loadData();
    }

    async loadData() {
        const success = await this.ngbDiseasesChartService.getDiseases();
        if (success) {
            this.draw();
        }
    }

    prepareSVG() {
        if (!this.svgContainer || !this.element) {
            return undefined;
        }
        let {
            clientWidth: width = 0,
            clientHeight: height = 0
        } = this.element;
        if (!width || !height) {
            return undefined;
        }
        height = Math.max(height - CONTROLS_ROW_HEIGHT, 100);
        if (!this.svg) {
            this.svg = d3.select(this.svgContainer)
                .append('svg');
        }
        this.svg.selectAll('*').remove();
        this.svg
            .attr('width', width)
            .attr('height', height);
        this.$timeout(() => this.$scope.$apply());
        return this.svg;
    }

    clearTimeouts() {
        clearTimeout(this.hideInfoTimeout);
        clearTimeout(this.showInfoTimeout);
    }

    hideInfo() {
        this.clearTimeouts();
        this.hideInfoTimeout = setTimeout(() => {
            this.hoveredDisease = undefined;
            this.$scope.$apply();
        }, TOOLTIP_HIDE_TIMEOUT_MS);
    }

    showInfo(disease, x, y) {
        this.clearTimeouts();
        clearTimeout(this.showInfoTimeout);
        this.showInfoTimeout = setTimeout(() => {
            this.hoveredDisease = disease;
            this.$scope.$apply();
            if (this.tooltip && this.svg) {
                const width = this.svg.attr('width');
                const height = this.svg.attr('height');
                if (y > height / 2.0) {
                    this.tooltip.style.top = '';
                    this.tooltip.style.bottom = `${height - y + 30}px`;
                } else {
                    this.tooltip.style.top = `${y + CONTROLS_ROW_HEIGHT + 30}px`;
                    this.tooltip.style.bottom = '';
                }
                if (x > width / 2.0) {
                    this.tooltip.style.left = '';
                    this.tooltip.style.right = `${width - x}px`;
                } else {
                    this.tooltip.style.left = `${x}px`;
                    this.tooltip.style.right = '';
                }
            }
        }, this.hoveredDisease
            ? Math.min(100, TOOLTIP_SHOW_TIMEOUT_MS)
            : TOOLTIP_SHOW_TIMEOUT_MS
        );
    }

    draw() {
        // to be overridden
    }
}
