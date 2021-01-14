export default function renderArea(viewport, drawingConfig) {
    const {config, graphics, shouldRender, y, hovered, height: predefinedHeight} = drawingConfig;
    const height = predefinedHeight || config.height;
    graphics.clear();
    const centerLine = y + height / 2;
    if (shouldRender) {
        graphics.lineStyle(config.border.thickness, config.border.stroke, 1);
        if (!hovered) {
            graphics.moveTo(0, y + height);
            graphics.lineTo(viewport.canvasSize, y + height);
            graphics.lineStyle(config.divider.thickness, config.divider.stroke, 1);
            graphics.moveTo(0, centerLine);
            graphics.lineTo(viewport.canvasSize, centerLine);
        }
    }
    return {centerLine, height};
}
