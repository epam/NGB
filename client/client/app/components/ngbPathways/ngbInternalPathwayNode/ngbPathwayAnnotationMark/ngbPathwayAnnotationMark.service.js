const DEFAULT_MAX_COLUMNS = 5;
const DEFAULT_MINIMIZED_CELL_SIZE = 0.75;

const LOCAL_CELL_SIZE = 16;
const WINDOW_OFFSET = 10;

export default class NgbPathwayAnnotationMarkService {
    static instance() {
        return new NgbPathwayAnnotationMarkService();
    }
    constructor() {
        this.annotationTooltipContainer = document.createElement('div');
        this.annotationTooltipContainer.classList.add('ngb-pathway-annotation-tooltip-container');
        document.body.appendChild(this.annotationTooltipContainer);
    }

    generateAnnotationBackground(colors, options = {}) {
        if (!colors || colors.length === 0) {
            return undefined;
        }
        const {
            maxColumns = DEFAULT_MAX_COLUMNS,
            stroked = true,
            minimizedCellSize = DEFAULT_MINIMIZED_CELL_SIZE
        } = options;
        const columns = Math.min(colors.length, maxColumns);
        const rows = Math.ceil(colors.length / columns);
        const cellSize = rows >= 2 ? minimizedCellSize : 1;
        const getCoordinate = (index) => ({
            column: index % columns,
            row: Math.floor(index / columns)
        });
        let svg = `<svg
 xmlns="http://www.w3.org/2000/svg"
 width="${columns * LOCAL_CELL_SIZE}" height="${rows * LOCAL_CELL_SIZE}"
 ${stroked ? 'stroke="rgb(120, 120, 120)"' : ''}
>`;
        for (let i = 0; i < colors.length; i++) {
            const {column, row} = getCoordinate(i);
            svg = svg.concat(`
<rect
 x="${column * LOCAL_CELL_SIZE}"
 y="${row * LOCAL_CELL_SIZE}"
 width="${LOCAL_CELL_SIZE}"
 height="${LOCAL_CELL_SIZE}"
 fill="${colors[i]}"
/>`);
        }
        svg = svg.concat('</svg>');
        svg = `url(data:image/svg+xml;base64,${window.btoa(svg)})`;
        return {
            backgroundImage: svg,
            width: columns * cellSize,
            height: rows * cellSize
        };
    }

    showTooltip(info, position) {
        this.hideTooltip();
        if (!info || !position || !this.annotationTooltipContainer) {
            return;
        }
        const {
            left,
            top,
            width,
            height,
        } = position;
        const {
            width: columns,
            height: rows,
            backgroundImage
        } = info;
        const cellSize = 10;
        const tooltipWidth = columns * cellSize;
        const tooltipHeight = rows * cellSize;
        const offset = 20;
        let tooltipY = top + height / 2.0 - tooltipHeight / 2.0;
        if (tooltipY <= WINDOW_OFFSET) {
            tooltipY = WINDOW_OFFSET;
        }
        if (tooltipY + tooltipHeight >= window.innerHeight - WINDOW_OFFSET) {
            tooltipY = window.innerHeight - WINDOW_OFFSET - tooltipHeight;
        }
        let tooltipX = left - offset - tooltipWidth;
        if (tooltipX <= WINDOW_OFFSET) {
            tooltipX = left + width + offset;
        }
        this.tooltip = document.createElement('div');
        this.tooltip.classList.add('ngb-pathway-annotation-tooltip');
        this.annotationTooltipContainer.appendChild(this.tooltip);
        this.tooltip.style.width = `${tooltipWidth}px`;
        this.tooltip.style.height = `${tooltipHeight}px`;
        this.tooltip.style.backgroundImage = backgroundImage;
        this.tooltip.style.top = `${tooltipY}px`;
        this.tooltip.style.left = `${tooltipX}px`;
    }

    hideTooltip() {
        if (this.tooltip && this.annotationTooltipContainer) {
            this.annotationTooltipContainer.removeChild(this.tooltip);
        }
        this.tooltip = undefined;
    }
}
