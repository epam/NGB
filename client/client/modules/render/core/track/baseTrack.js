import {Viewport} from '../viewport/viewport';
import {extractFeaturesForTrack} from '../configuration';

export default class BaseTrack {
    viewport: Viewport;

    destructor() {

    }

    get stateKeys() {
        return [];
    }

    state = {};

    constructor(opts) {
        const {viewport, config, dataItemClicked, changeTrackVisibility, changeTrackHeight, ...configRest} = opts;
        const state = extractFeaturesForTrack(Object.assign(opts.defaultFeatures || {}, opts.state || {}), this.stateKeys);
        const getTrackConfigFn = () => {
            const trackDefaultConfig = typeof this.constructor.getTrackDefaultConfig === 'function'
                ? this.constructor.getTrackDefaultConfig()
                : {};
            if (configRest.projectContext) {
                const serverConfig = configRest.projectContext.getTrackDefaultSettings(configRest.format);
                if (serverConfig) {
                    return Object.assign(
                        {fitHeightFactor: 1},
                        BaseTrack.recoverConfig(serverConfig, trackDefaultConfig)
                    );
                }
            }
            return Object.assign({fitHeightFactor: 1}, trackDefaultConfig);
        };

        Reflect.defineProperty(this, 'trackConfig', {
            value: getTrackConfigFn()
        });

        Reflect.defineProperty(this, 'viewport', {
            value: viewport
        });

        Reflect.defineProperty(this, 'dataItemClicked', {
            value: dataItemClicked
        });

        Reflect.defineProperty(this, 'changeTrackVisibility', {
            value: changeTrackVisibility
        });

        Reflect.defineProperty(this, 'changeTrackHeight', {
            value: changeTrackHeight
        });

        const mapDefaultHeightFn = () => ({
            height: (typeof this.trackConfig.height === 'function')
                ? this.trackConfig.height(state, this.trackConfig)
                : this.trackConfig.height
        });

        const defaultConfig = {
            header: {
                fontSize: '12px',
            },
            maxHeight: 500,
            minHeight: 20,
            height: 50
        };

        Reflect.defineProperty(this, 'config', {
            value: Object.freeze(
                [config, configRest, mapDefaultHeightFn(), defaultConfig].reduceRight((acc, config) => ({...acc, ...config})))
        });
        this.state = state;
    }

    static recoverConfig(serverConfig, localConfig) {
        if (serverConfig !== null && serverConfig !== undefined) {
            for (const key in localConfig) {
                if (localConfig.hasOwnProperty(key) && localConfig[key] !== undefined &&
                    serverConfig.hasOwnProperty(key) && serverConfig[key] !== undefined && serverConfig[key] !== null) {
                    switch ((typeof localConfig[key]).toLowerCase()) {
                        case 'object':
                            localConfig[key] = BaseTrack.recoverConfig(serverConfig[key], localConfig[key]);
                            break;
                        default:
                            localConfig[key] = serverConfig[key];
                            break;
                    }
                }
            }
        }
        return localConfig;
    }
}
