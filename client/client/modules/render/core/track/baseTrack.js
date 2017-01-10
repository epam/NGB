import {Viewport} from '../viewport/viewport';


export default class BaseTrack {
    viewport: Viewport;
    static tracks = [];

    destructor() {
        BaseTrack.tracks.splice(BaseTrack.tracks.indexOf(this), 1);
    }

    constructor({viewport, config, dataItemClicked, ...configRest}) {
        BaseTrack.tracks.push(this);

        const configChain = [];
        let protoPointer = this;
        while (protoPointer && protoPointer instanceof BaseTrack) {
            configChain.push(protoPointer.constructor);
            protoPointer = protoPointer.__proto__;
        }

        Reflect.defineProperty(this, 'viewport', {
            value: viewport
        });

        Reflect.defineProperty(this, 'dataItemClicked', {
            value: dataItemClicked
        });

        const mapDefaultHeightFn = function(constructor) {
            return {
                height: constructor.trackDefaultHeight
            };
        };

        Reflect.defineProperty(this, 'config', {
            value: Object.freeze(
                [config, configRest, ...configChain.map(constructor => constructor.config).filter(a => a),
                    ...configChain.map(mapDefaultHeightFn).filter(a => a)]
                    .reduceRight((acc, config) => ({...acc, ...config})))
        });

    }
}