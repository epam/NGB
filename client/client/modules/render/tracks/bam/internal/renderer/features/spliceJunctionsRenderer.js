const Math = window.Math;

export function renderSpliceJunctions(spliceJunctions, viewport, drawingConfig) {
    const {colors, config, graphics, shouldRender, y} = drawingConfig;
    graphics.clear();
    if (shouldRender) {
        graphics.lineStyle(config.spliceJunctions.border.thickness, config.spliceJunctions.border.stroke, 1);
        const centerLine = y + config.spliceJunctions.height / 2;
        graphics.moveTo(0, y + config.spliceJunctions.height);
        graphics.lineTo(viewport.canvasSize, y + config.spliceJunctions.height);
        graphics.lineStyle(config.spliceJunctions.divider.thickness, config.spliceJunctions.divider.stroke, 1);
        graphics.moveTo(0, centerLine);
        graphics.lineTo(viewport.canvasSize, centerLine);
        let maxValue = 0;
        for (let i = 0; i < spliceJunctions.length; i++) {
            const spliceJunctionItem = spliceJunctions[i];
            if (spliceJunctionItem.end < viewport.brush.start || spliceJunctionItem.start > viewport.brush.end)
                continue;
            maxValue = Math.max(maxValue, Math.abs(spliceJunctionItem.count));
        }
        const arcMaxHeight = config.spliceJunctions.height / 2
            - config.spliceJunctions.arc.offset.top - config.spliceJunctions.arc.offset.bottom;
        const renderArc = (startPx, endPx, strand, value) => {
            const innerRadius = config.spliceJunctions.arc.offset.top;
            const outerRadius = config.spliceJunctions.arc.offset.top + arcMaxHeight * value;
            const xRadius = (endPx - startPx) / 2;
            const centerPoint = {
                x: (endPx + startPx) / 2,
                y: centerLine
            };
            let steps = (endPx - startPx) / 2;
            if (steps > viewport.canvasSize) {
                steps = viewport.canvasSize;
            }
            else if (steps < 100) {
                steps = 100;
            }
            const step = Math.PI / steps;
            let color = colors.strandDirection.r;
            if (!strand) {
                color = colors.strandDirection.l;
            }
            graphics.beginFill(color, .5);
            graphics.lineStyle(1, color, 1);
            const multiplier = !strand ? 1 : -1;
            for (let s = 0; s <= steps; s++) {
                if (s === 0) {
                    graphics.moveTo(centerPoint.x + xRadius * Math.cos(s * step), centerPoint.y + multiplier * innerRadius * Math.sin(s * step));
                }
                graphics.lineTo(centerPoint.x + xRadius * Math.cos(s * step), centerPoint.y + multiplier * innerRadius * Math.sin(s * step));
            }
            for (let s = steps; s >= 0; s--) {
                graphics.lineTo(centerPoint.x + xRadius * Math.cos(s * step), centerPoint.y + multiplier * outerRadius * Math.sin(s * step));
            }
            graphics.endFill();
        };
        for (let i = 0; i < spliceJunctions.length; i++) {
            const spliceJunctionItem = spliceJunctions[i];
            if (spliceJunctionItem.end < viewport.brush.start || spliceJunctionItem.start > viewport.brush.end)
                continue;
            const value = spliceJunctionItem.count / maxValue;
            renderArc(
                viewport.project.brushBP2pixel(spliceJunctionItem.start),
                viewport.project.brushBP2pixel(spliceJunctionItem.end),
                spliceJunctionItem.strand,
                value);
        }
    }
}