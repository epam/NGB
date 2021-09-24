export default class HeatmapEventDispatcher {
    constructor() {
        this.listeners = [];
    }

    addEventListener(event, listener) {
        const [existing] = this.listeners.filter(l => l.event === event && l.listener === listener);
        if (!existing && typeof listener === 'function') {
            this.listeners.push({
                event,
                listener
            });
        }
    }

    removeEventListener(event, listener) {
        const [existing] = this.listeners.filter(l => l.event === event && l.listener === listener);
        if (existing) {
            const index = this.listeners.indexOf(existing);
            this.listeners.splice(index, 1);
        }
    }

    removeEventListeners(...listeners) {
        this.listeners = this.listeners.filter(listener => !listeners.includes(listener));
    }

    emit(event, payload) {
        const listeners = this.listeners.filter(config => config.event === event);
        listeners.forEach(config => config.listener(this, payload));
    }

    destroy () {
        this.destroyed = true;
        this.listeners = [];
    }
}
