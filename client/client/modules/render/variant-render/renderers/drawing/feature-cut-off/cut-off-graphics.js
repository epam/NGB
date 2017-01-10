import PIXI from 'pixi.js';
const Math = window.Math;

export default class CutOffGraphics{

    _mask: PIXI.Graphics;
    _cutOffGraphics: PIXI.Graphics;
    _maskingContainer: PIXI.Container;
    _topOffset: number;
    _config = null;

    get config () { return this._config; }
    get mask (): PIXI.Graphics { return this._mask; }
    get cutOffGraphics (): PIXI.Graphics { return this._cutOffGraphics; }
    get maskingContainer (): PIXI.Container { return this._maskingContainer; }
    get topOffset (): number { return this._topOffset; }

    constructor(config, container: PIXI.Container){
        if (container.parent === null || container.parent === undefined){
            throw new Error('`Container` must be added to parent before drawing cutting-off graphics.');
        }
        this._config = config;
        this._maskingContainer = container;
        this._mask = new PIXI.Graphics();
        this._cutOffGraphics = new PIXI.Graphics();
        this._topOffset = this._getTotalTopOffset(container);
    }

    cutOff(boundaries, clipping = {rightClip: true, leftClip: true}): CutOffGraphics{
        const { rightClip, leftClip } = clipping;
        const { x, y, width, height } = boundaries;
        this.mask.beginFill(0x00FF00, 1);
        if (leftClip) {
            this.mask.moveTo(Math.round(x + this._breakCurve(0)), Math.round(y + this.topOffset));
            for (let _y = 0; _y <= height; _y++) {
                this.mask.lineTo(Math.round(x + this._breakCurve(_y)), Math.round(_y + y + this.topOffset));
            }
        }
        else {
            this.mask.moveTo(x, Math.round(y + this.topOffset));
            this.mask.lineTo(x, Math.round(y + this.topOffset + height));
        }
        if (rightClip) {
            for (let _y = height; _y >= 0; _y--) {
                this.mask.lineTo(Math.round(x + width - this._breakCurve(_y)), Math.round(_y + y + this.topOffset));
            }
            this.mask.lineTo(Math.round(x + this._breakCurve(0)), Math.round(y + this.topOffset));
        }
        else {
            this.mask.lineTo(x + width, Math.round(y + this.topOffset + height));
            this.mask.lineTo(x + width, Math.round(y + this.topOffset));
            this.mask.lineTo(x, Math.round(y + this.topOffset));
        }

        this.mask.endFill();

        this.cutOffGraphics.beginFill(0xFFFFFF, 0);
        const step = 1;

        this.cutOffGraphics.lineStyle(this.config.breaks.thickness, this.config.breaks.stroke, 1);
        if (leftClip) {
            for (let _y = step + this.config.callout.margin; _y <= height - this.config.callout.margin; _y += step) {
                this.cutOffGraphics.moveTo(Math.round(x + this._breakCurve(_y - step)) + this.config.breaks.thickness, Math.round(_y + y - step));
                this.cutOffGraphics.lineTo(Math.round(x + this._breakCurve(_y)) + this.config.breaks.thickness, Math.round(_y + y));
            }
        }
        if (rightClip) {
            for (let _y = step + this.config.callout.margin; _y <= height - this.config.callout.margin; _y += step) {
                this.cutOffGraphics.moveTo(Math.round(x + width - this._breakCurve(_y - step)) - this.config.breaks.thickness, Math.round(_y + y - step));
                this.cutOffGraphics.lineTo(Math.round(x + width - this._breakCurve(_y)) - this.config.breaks.thickness, Math.round(_y + y));
            }
        }
        return this;
    }

    endCuttingOff(): void{
        this.maskingContainer.mask = this.mask;
        this.maskingContainer.addChild(this.cutOffGraphics);
    }

    _breakCurve(localY){
        const d = localY - Math.floor(localY / (4 * this.config.breaks.amplitude.x)) * (4 * this.config.breaks.amplitude.x);
        let dX = 0;
        if (d < this.config.breaks.amplitude.x) {
            dX = d;
        }
        else if (d < 3 * this.config.breaks.amplitude.x) {
            dX = 2 * this.config.breaks.amplitude.x - d;
        }
        else {
            dX = d - 4 * this.config.breaks.amplitude.x;
        }

        return this.config.breaks.margin.x + this.config.breaks.amplitude.x + dX;
    }

    _getTotalTopOffset(container: PIXI.Container){
        if (container.parent === null || container.parent === undefined)
            return container.y;
        return container.y + this._getTotalTopOffset(container.parent);
    }
}