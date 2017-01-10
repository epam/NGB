import {BedItemFeatureRenderer} from './drawing';
import {FeatureRenderer} from '../../../../gene/internal/renderer/features';

export default class BEDFeatureRenderer extends FeatureRenderer {

    _bedItemFeatureRenderer = null;

    constructor(config) {
        super(config);
        this._bedItemFeatureRenderer = new BedItemFeatureRenderer(config,
            ::this.registerLabel,
            ::this.registerDockableElement,
            ::this.registerFeaturePosition);
    }

    prepareRenderers() {
        this._bedItemFeatureRenderer.initializeRenderingSession();
    }

    get textureCoordinates() {
        const coordinates = {
            x: null,
            y: null
        };
        if (this._bedItemFeatureRenderer.textureCoordinates) {
            if (this._bedItemFeatureRenderer.textureCoordinates.x !== null &&
                (!coordinates.x || coordinates.x > this._bedItemFeatureRenderer.textureCoordinates.x)) {
                coordinates.x = this._bedItemFeatureRenderer.textureCoordinates.x;
            }
            if (this._bedItemFeatureRenderer.textureCoordinates.y !== null &&
                (!coordinates.y || coordinates.y > this._bedItemFeatureRenderer.textureCoordinates.y)) {
                coordinates.y = this._bedItemFeatureRenderer.textureCoordinates.y;
            }
        }
        return coordinates;
    }

    get defaultRenderer() {
        return this._bedItemFeatureRenderer;
    }
}