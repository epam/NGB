const Math = window.Math;

export function renderCenterLine(viewport, drawingConfig) {
    const {config, graphics, height, shouldRender} = drawingConfig;
    graphics.clear();
    if (shouldRender) {
        const dashesCount = height / (2 * config.centerLine.dash.length);
        const length = config.centerLine.dash.length;
        const thickness = config.centerLine.dash.thickness;
        const color = config.centerLine.dash.fill;
        const drawVerticalDashLine = (x) => {
            graphics.lineStyle(thickness, color, 1);
            for (let i = 0; i < dashesCount; i++) {
                graphics
                    .moveTo(x, (2 * i) * length)
                    .lineTo(x, (2 * i + 1) * length);
            }
        };
        const center = Math.round(viewport.centerPosition);
        if (viewport.factor > 2) {
            drawVerticalDashLine(viewport.project.brushBP2pixel(center) - viewport.factor / 2);
            drawVerticalDashLine(viewport.project.brushBP2pixel(center) + viewport.factor / 2);
        }
        else {
            drawVerticalDashLine(viewport.project.brushBP2pixel(center));
        }
    }
}