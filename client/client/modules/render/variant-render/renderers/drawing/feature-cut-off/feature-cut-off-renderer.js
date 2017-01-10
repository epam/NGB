import CutOffGraphics from './cut-off-graphics';

export default class FeatureCutOffRenderer{

    _config = null;

    constructor(config){
        this._config = config;
    }

    get config () { return this._config; }

    beginCuttingOff(container): CutOffGraphics{
        return new CutOffGraphics(this.config, container);
    }
}