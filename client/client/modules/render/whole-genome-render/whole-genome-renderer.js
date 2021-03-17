import PIXI from 'pixi.js';
import $ from 'jquery';

import { getRenderer } from '../core';
import { ScaleRenderer } from './partials/scale/scaleRenderer';
import { ScaleTransformer } from './partials/scale/scaleTransformer';

export default class WholeGenomeRenderer {

    constructor(canvas, displayTooltip = null) {
        
        this._ticksCount = 50;
        this._range = 500000;

        this._canvas = canvas;
        this._width = canvas.clientWidth;
        this._height = canvas.clientHeight;

        this._displayTooltipFn = displayTooltip;
        this._container = new PIXI.Container();

        this._pixiRenderer = getRenderer({
            width: this.width,
            height: this.height
          }, 
          { backgroundColor: 0xffffff});

        this._scaleRenderer = new ScaleRenderer(
          this.container,
          {
            width: $(this.canvasElement).width(),
            height: $(this.canvasElement).height()
          },
          {
            start: { x: 10, y:30 },
          },
          {
            topMargin: 20
          },
           this._range
          );

        this.canvasElement.appendChild(this.renderer.view);
        this.scaleRenderer.init(
            ScaleTransformer.buildTicks(this._range, this._ticksCount)
        );
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

    get width() {
        return this._width;
    }

    get height() {
        return this._height;
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
