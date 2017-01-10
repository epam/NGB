export function renderDownSampleIndicators(downsampleCoverage, viewport, drawingConfig) {
    const {config, graphics, shouldRender, y} = drawingConfig;
    graphics.clear();
    if (shouldRender) {
        graphics.beginFill(config.downSampling.indicator.color, 1);
        const offset = 0.5;
        for (let i = 0; i < downsampleCoverage.length; i++) {
            if (downsampleCoverage[i].value === 0)
                continue;
            const x1 = viewport.project.brushBP2pixel(downsampleCoverage[i].startIndex - offset) + 1;
            const x2 = viewport.project.brushBP2pixel(downsampleCoverage[i].endIndex + offset) - 1;
            graphics
                .drawRect(
                    x1,
                    y + (config.downSampling.area.height - config.downSampling.indicator.height) / 2,
                    x2 - x1,
                    config.downSampling.indicator.height
                );
        }
        graphics.endFill();
    }
}