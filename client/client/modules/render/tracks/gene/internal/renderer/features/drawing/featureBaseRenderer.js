import {getStrandArrowSize} from './strandDrawing';
export const ZONES_MANAGER_DEFAULT_ZONE_NAME = 'default';
const Math = window.Math;

export default class FeatureBaseRenderer{

    _config;
    _opts;
    _textureCoordinates;

    constructor(config, registerLabel, registerDockableElement, registerFeaturePosition, registerAttachedElement){
        this._config = config;
        this._registerLabel = registerLabel;
        this._registerDockableElement = registerDockableElement;
        this._registerFeaturePosition = registerFeaturePosition;
        this._registerAttachedElement = registerAttachedElement;
    }

    get config() { return this._config; }
    get registerLabel() { return this._registerLabel; }
    get registerDockableElement() { return this._registerDockableElement; }
    get registerFeaturePosition() { return this._registerFeaturePosition; }
    get registerAttachedElement() { return this._registerAttachedElement; }

    get textureCoordinates() {
        return this._textureCoordinates;
    }

    get strandIndicatorConfig () {
        return undefined;
    }

    initializeRenderingSession() {
        this._textureCoordinates = {
            x: null,
            y: null
        };
    }

    updateTextureCoordinates(coordinates) {
        const {x, y} = coordinates;
        if (!this._textureCoordinates.x || this._textureCoordinates.x > x) {
            this._textureCoordinates.x = x;
        }
        if (!this._textureCoordinates.y || this._textureCoordinates.y > y) {
            this._textureCoordinates.y = y;
        }
    }

    shouldRenderStrandIndicatorInsteadOfGraphics (x1, x2) {
        return this.config.renderStrandIndicatorOnLargeScale &&
            this.strandIndicatorConfig &&
            Math.abs(x2 - x1) - 2 * this.strandIndicatorConfig.arrow.margin < getStrandArrowSize(this.strandIndicatorConfig.arrow.height).width;
    }

    analyzeBoundaries(feature, viewport){
        if (feature.hasOwnProperty('startIndex') && feature.hasOwnProperty('endIndex')){
            const pixelsInBp = viewport.factor; // pixels in 1 bp.
            const x1 = Math.min(
                Math.max(viewport.project.brushBP2pixel(feature.startIndex), -viewport.canvasSize),
                2 * viewport.canvasSize
            ) - pixelsInBp / 2;
            let x2 = Math.max(
                Math.min(viewport.project.brushBP2pixel(feature.endIndex), 2 * viewport.canvasSize),
                -viewport.canvasSize
            ) + pixelsInBp / 2;
            if (this.shouldRenderStrandIndicatorInsteadOfGraphics(x1, x2)) {
                x2 = x1 + getStrandArrowSize(this.strandIndicatorConfig.arrow.height).width;
            }
            return {
                margin:{
                    marginX: 0,
                    marginY: 0
                },
                rect: {
                    x1: x1,
                    x2: x2,
                    y1: 0,
                    y2: 20
                }
            };
        }
        return null;
    }

    render(){
    }
}
