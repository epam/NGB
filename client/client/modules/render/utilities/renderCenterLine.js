import './dash-line';
const Math = window.Math;

export default function renderCenterLine(viewport, drawingConfig) {
    const {config, graphics, height, shouldRender} = drawingConfig;
    graphics.clear();
    if (shouldRender) {
        const thickness = config.centerLine.dash.thickness;
        const color = config.centerLine.dash.fill;
        const drawVerticalDashLine = (x) => {
            graphics
                .lineStyle(thickness, color, 1)
                .drawDashLine(
                    Math.floor(x) + thickness / 2.0,
                    0,
                    Math.floor(x) + thickness / 2.0,
                    height,
                    config.centerLine.dash.length
                );
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
