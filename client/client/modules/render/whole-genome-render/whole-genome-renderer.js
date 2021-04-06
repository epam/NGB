import PIXI from 'pixi.js';
import $ from 'jquery';

import { getRenderer } from '../core';
import { ScaleRenderer } from './partials/scale/scaleRenderer';
import { ScaleTransformer } from './partials/scale/scaleTransformer';
import { ChromosomeColumnRenderer } from './partials/chromosomeColumn/chromosomeColumnRenderer';
import drawingConfig from './whole-genome-config';

export default class WholeGenomeRenderer {

    constructor(canvas, maxChromosomeSize, chromosomes, blastResult, displayTooltip = null) {
        
        this._ticksCount = drawingConfig.ticks;
        this._range = maxChromosomeSize;
        this._chromosomes = chromosomes;
        this._blastResult = blastResult;

        this._canvas = canvas;
        this._width = canvas.clientWidth;
        this._height = canvas.clientHeight;

        this._displayTooltipFn = displayTooltip;
        this._container = new PIXI.Container();

        this._pixiRenderer = getRenderer({
            width: this.width,
            height: this.height
            }, 
            { backgroundColor: 0xffffff });

        this._scaleRenderer = new ScaleRenderer(this.renderParams);

        this._chromosomeRenderer = new ChromosomeColumnRenderer(this.renderParams);

        this.canvasElement.appendChild(this.renderer.view);
        this.scaleRenderer.init(
            ScaleTransformer.buildTicks(this._range, this._ticksCount)
        );
        this.chromosomeRenderer.init();
        this.resizeRenderer();

        $(window).on('resize', () => {
            this.resizeRenderer();
        });

        requestAnimationFrame(::this.render);
    }

    get canvasElement() {
        return this._canvas;
    }

    get container() {
        return this._container;
    }

    get renderer() {
        return this._pixiRenderer;
    }

    get scaleRenderer() {
      return this._scaleRenderer;
    }

    get chromosomeRenderer() {
        return this._chromosomeRenderer;
    }

    get width() {
        return this._width;
    }

    get height() {
        return this._height;
    }
    
    get range() {
        return this._range;
    }

    get chromosomes(){
        return this._chromosomes;
    }
    get hits(){
        return this._blastResult;
    }

    get flags() {
        const flags = {
            widthChanged: false,
            reset: false
        };
        if (this.canvasElement !== null &&
            this.width !== $(this.canvasElement).width()
        ) {
            this._width = $(this.canvasElement).width();
            flags.widthChanged = true;
        };
        return flags;
    }

    get renderParams() {
        return {
            container: this.container,
            canvasSize: {
                width: $(this.canvasElement).width(),
                height: $(this.canvasElement).height()
            },
            chromosomes: this.chromosomes,
            range: this.range,
            hits: this.hits,
        };
    }

    render() {
        if (this.renderer !== null &&
            this.renderer !== undefined
        ) { 
            const flags = this.flags;
            if (flags.widthChanged) {
                this.resizeRenderer();
            }
            this.renderer.render(this.container);
        };
    }

    resizeRenderer() {
        if (this.renderer) {
            this.renderer.resize(this.width, this.height);

            Object.assign(this.renderer.view.style, {
                width: `${this.width}px`,
                height: `${this.height}px`
            });
            this.render();
        };
    }

    destroy() {
        this._destroyed = true;
        if (this.renderer && this.renderer.view) {
            this.container.removeChildren();
            this.canvasElement.removeChild(this.renderer.view);
            this.renderer.destroy(true);
            this._pixiRenderer = null;
        };
    }
}
