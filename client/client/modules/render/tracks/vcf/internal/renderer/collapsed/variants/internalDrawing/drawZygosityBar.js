const Math = window.Math;

export function drawZygosityBar(zygosity, graphics, config, bpLength) {
    const white = 0xFFFFFF;
    graphics.lineStyle(0, white, 0);
    switch (zygosity) {
        case 1: {
            // homozygous
            graphics
                .beginFill(config.zygosity.homozygousColor, 1)
                .drawRect(Math.floor(- bpLength / 2), Math.floor(- config.height), bpLength,
                    config.height);
        }
            break;
        case 2: {
            // heterozygous
            graphics
                .beginFill(config.zygosity.homozygousColor, 1)
                .drawRect(Math.floor(- bpLength / 2), Math.floor(- config.height), bpLength,
                    config.height / 2)
                .beginFill(config.zygosity.heterozygousColor, 1)
                .drawRect(Math.floor(- bpLength / 2), Math.floor(- config.height / 2), bpLength,
                    config.height / 2);
        }
            break;
        default: {
            graphics
                .beginFill(config.zygosity.unknownColor, 1)
                .drawRect(Math.floor(- bpLength / 2), Math.floor(- config.height), bpLength,
                    config.height);
        }
            break;
    }
    return {
        rect: {
            x1: Math.floor(- bpLength / 2),
            x2: Math.floor(bpLength / 2),
            y1: - config.height,
            y2: 0
        }
    };
}