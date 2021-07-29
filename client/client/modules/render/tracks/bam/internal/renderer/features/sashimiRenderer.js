import WIGRenderer from '../../../../wig/wigRenderer';
import {renderSashimiPlot} from './spliceJunctions';

export class SashimiRenderer extends WIGRenderer {
    constructor(config, bamConfig, track) {
        super(config.coverage, bamConfig, track);
        this._globalConfig = config;
    }

    render(viewport, {coverage, spliceJunctions}, forseRedraw = false, drawingConfig): undefined {
        super.render(viewport, coverage, forseRedraw);
        const {
            graphics,
            labelsContainer,
            shouldRender,
            y,
            spliceJunctionsFiltering
        } = drawingConfig || {};
        renderSashimiPlot(
            spliceJunctions,
            viewport,
            {
                config: this._globalConfig,
                graphics,
                height: this.height * 2.0,
                spliceJunctionsFiltering,
                labelsContainer,
                sashimi: true,
                shouldRender,
                y
            },
            this.labelsManager
        );
    }
}
