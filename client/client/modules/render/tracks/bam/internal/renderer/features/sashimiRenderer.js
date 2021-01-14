import WIGRenderer from '../../../../wig/wigRenderer';
import {renderSashimiPlot} from './spliceJunctions';

export class SashimiRenderer extends WIGRenderer {
    constructor(config, bamConfig) {
        super(config.coverage, bamConfig);
        this._globalConfig = config;
    }

    render(viewport, {coverage, spliceJunctions}, forseRedraw = false, drawingConfig): undefined {
        super.render(viewport, coverage, forseRedraw);
        const {graphics, labelsContainer, shouldRender, y} = drawingConfig || {};
        renderSashimiPlot(
            spliceJunctions,
            viewport,
            {
                config: this._globalConfig,
                graphics,
                height: this.height * 2.0,
                labelsContainer,
                sashimi: true,
                shouldRender,
                y
            }
        );
    }
}
