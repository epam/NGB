import HeatmapEventDispatcher from './heatmap-event-dispatcher';
import events from './events';

/**
 *
 * @param {HeatmapEventDispatcher} target
 */
export function reportInitialized(target) {
    if (target instanceof  HeatmapEventDispatcher) {
        target.emit(events.initialized);
    }
}

/**
 * @this {HeatmapEventDispatcher}
 * @param {function|undefined} callback
 */
function onInitialized(callback) {
    if (this instanceof HeatmapEventDispatcher) {
        this.addEventListener(events.initialized, callback);
    }
}

/**
 * @typedef {Object} InitializableOptions
 * @property {function: boolean} [isInitialized]
 */

/**
 *
 * @param {HeatmapEventDispatcher} target
 * @param {InitializableOptions} [options]
 */
export default function makeInitializable(target, options = {}) {
    if (!target || !(target instanceof HeatmapEventDispatcher)) {
        return;
    }
    const {
        isInitialized = () => true
    } = options;
    Object.defineProperty(
        target,
        'reportInitialized',
        {
            enumerable: false,
            configurable: false,
            writable: false,
            value: reportInitialized.bind(target, target)
        }
    );
    Object.defineProperty(
        target,
        'onInitialized',
        {
            enumerable: false,
            configurable: false,
            writable: false,
            value: onInitialized.bind(target)
        }
    );
    Object.defineProperty(
        target,
        'initialized',
        {
            get() {
                return !!this._initialized && isInitialized.call(this);
            },
            set(initialized) {
                if (!!this._initialized !== !!initialized) {
                    this._initialized = !!initialized;
                    if (this._initialized) {
                        this.reportInitialized();
                    }
                }
            }
        }
    );
}
