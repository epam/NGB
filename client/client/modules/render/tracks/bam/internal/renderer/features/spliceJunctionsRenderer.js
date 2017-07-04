const Math = window.Math;

export function renderSpliceJunctions(spliceJunctions, viewport, drawingConfig) {
    const {colors, config, graphics, shouldRender, y, hovered} = drawingConfig;
    graphics.clear();
    if (shouldRender) {
        graphics.lineStyle(config.border.thickness, config.border.stroke, 1);
        const centerLine = y + config.height / 2;
        if (!hovered) {
            graphics.moveTo(0, y + config.height);
            graphics.lineTo(viewport.canvasSize, y + config.height);
            graphics.lineStyle(config.divider.thickness, config.divider.stroke, 1);
            graphics.moveTo(0, centerLine);
            graphics.lineTo(viewport.canvasSize, centerLine);
        }
        let maxValue = 0;
        for (let i = 0; i < spliceJunctions.length; i++) {
            const spliceJunctionItem = spliceJunctions[i];
            if (spliceJunctionItem.end < viewport.brush.start || spliceJunctionItem.start > viewport.brush.end)
                continue;
            maxValue = Math.max(maxValue, Math.abs(spliceJunctionItem.count));
        }
        const arcMaxHeight = config.height / 2
            - config.arc.offset.top - config.arc.offset.bottom;
        const renderArc = (startPx, endPx, strand, value) => {
            const innerRadius = config.arc.offset.top;
            const outerRadius = config.arc.offset.top + arcMaxHeight * value;
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
            let color = colors.strand.forward;
            if (!strand) {
                color = colors.strand.reverse;
            }
            graphics.lineStyle(1, color, 1);
            graphics.beginFill(color, .5);
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
        const array = hovered ? [hovered] : spliceJunctions;
        for (let i = 0; i < array.length; i++) {
            const spliceJunctionItem = array[i];
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