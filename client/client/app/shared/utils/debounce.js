export class Debounce {
    timers = {};
    constructor() {
        this.debounce = this.debounce.bind(this);
    }
    debounce(context, func, ms) {
        return (...args) => {
            const later = () => {
                func.apply(context, args);
                delete this.timers[func.name];
            };
            clearTimeout(this.timers[func.name]);
            this.timers[func.name] = setTimeout(later, ms);
        };
    }
}
