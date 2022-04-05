const MAX_ANNOTATIONS = 15;

export default class NgbPathwayAnnotationMarkController {
    static get UID () {
        return 'ngbPathwayAnnotationMarkController';
    }

    constructor($element, $scope, ngbPathwayAnnotationMarkService) {
        this.service = ngbPathwayAnnotationMarkService;
        this.element = $element;
        this.background = $element ? $element.find('.internal-pathway-annotation-background') : undefined;
        $scope.$watch('$ctrl.annotations', this.updateBackground.bind(this));
    }

    get collapsed() {
        return this.annotations && this.annotations.length > MAX_ANNOTATIONS;
    }

    get visible() {
        return this.annotations && this.annotations.length > 0;
    }

    get showBackground() {
        return this.visible && !this.collapsed;
    }

    buildAnnotationStyle() {
        const colors = this.annotations || [];
        let backgroundInfo = this.service.generateAnnotationBackground(
            colors,
            {
                stroked: true
            }
        );
        const EMPTY = {
            container: { marginLeft: 0, marginRight: 0 },
            background: { display: 'none', backgroundImage: 'unset' },
            tooltip: undefined
        };
        if (!this.visible || !backgroundInfo) {
            return EMPTY;
        }
        this.tooltipInfo = undefined;
        if (this.visible && this.collapsed) {
            const maxColumns = Math.max(1, Math.floor(Math.sqrt(Math.min(MAX_ANNOTATIONS, colors.length))));
            backgroundInfo = this.service.generateAnnotationBackground(
                colors.slice(0, maxColumns ** 2),
                {maxColumns, stroked: false}
            );
            this.tooltipInfo = this.service.generateAnnotationBackground(
                colors,
                {
                    maxColumns: Math.floor(Math.sqrt(colors.length)),
                    stroked: false,
                    minimizedCellSize: 1
                }
            );
        }
        const {
            backgroundImage,
            width: defaultWidth,
            height: defaultHeight
        } = backgroundInfo;
        const offset = 0.5;
        const unitSize = 1;
        const width = this.collapsed ? 1.5 : defaultWidth;
        const height = this.collapsed ? 1.5 : defaultHeight;
        const units = 'em';
        return {
            container: {
                marginLeft: `-${(width + offset) * unitSize}${units}`,
                marginRight: `${offset * unitSize}${units}`,
            },
            background: {
                display: 'block',
                backgroundImage,
                width: `${width * unitSize}${units}`,
                height: `${height * unitSize}${units}`
            }
        };
    }

    updateBackground() {
        if (this.background && this.element) {
            const {container, background} = this.buildAnnotationStyle();
            this.element.css(container);
            this.background.css(background);
        }
    }

    onMouseEnter(event) {
        if (!this.over && this.tooltipInfo) {
            this.over = true;
            if (event) {
                const rect = event.target.getBoundingClientRect();
                this.service.showTooltip(this.tooltipInfo, rect);
            }
        }
    }

    onMouseLeave() {
        this.over = false;
        this.service.hideTooltip();
    }

    onClick(event) {
        if (event) {
            event.preventDefault();
            event.stopPropagation();
        }
    }
}
