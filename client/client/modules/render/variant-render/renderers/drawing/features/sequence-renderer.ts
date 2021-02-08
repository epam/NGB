import PIXI from 'pixi.js';
import {BaseViewport, drawingConfiguration} from '../../../../core';
import {PixiTextSize} from '../../../../utilities';

const Math = window.Math;

export default class SequenceRenderer{

    _config = null;

    get config() { return this._config; }

    constructor (config){
        this._config = config;
    }

    renderSequence(sequence: Array, viewport: BaseViewport, container: PIXI.Container, topOffset): PIXI.Graphics {
        const rangeStart = viewport.chromosome.start;
        const rangeEnd = viewport.chromosome.end;

        const graphics = new PIXI.Graphics();
        container.addChild(graphics);

        const bpLength = viewport.factor - 2 * this._config.nucleotide.margin.x;
        const labelSize = PixiTextSize.getTextSize('W', this._config.nucleotide.label);
        const height = this._config.nucleotide.size.height;
        const shouldDisplayLabels = labelSize.width <= bpLength;

        for (let n = 0; n < sequence.length; n++) {
            const nucleotide = sequence[n];
            if (nucleotide.text === null || nucleotide.position < rangeStart || nucleotide.position > rangeEnd)
                continue;
            const x = viewport.project.brushBP2pixel(nucleotide.position);
            if (x + bpLength >= viewport.canvas.start && x - bpLength <= viewport.canvas.end) {
                const color = this._config.nucleotide.colors[nucleotide.text.toUpperCase()];
                graphics.beginFill(color, 1)
                    .drawRect(x - bpLength / 2, topOffset + this.config.nucleotide.margin.y, bpLength, height)
                    .endFill();
                if (shouldDisplayLabels) {
                    const label = new PIXI.Text(nucleotide.text, this._config.nucleotide.label);
                    label.resolution = drawingConfiguration.resolution;
                    label.x = Math.round(x - label.width / 2);
                    label.y = Math.round(topOffset + this.config.nucleotide.margin.y + height / 2 - label.height / 2);
                    container.addChild(label);
                }
            }
        }
    }
}
