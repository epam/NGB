const colors = [
    0xff0000, // red
    0x007500, // green
    0x0000ff, // blue
    0xf76f00, // yellow
    0x009471, // another green
    0x004094, // dark blue
    0x940094 // violet
];

export default class FCSourcesManager {
    static instance (dispatcher) {
        return new FCSourcesManager(dispatcher);
    }

    constructor(dispatcher) {
        this.dispatcher = dispatcher;
        this.clearCache();
        dispatcher.on('chromosome:change', this.clearCache.bind(this));
    }

    clearCache () {
        this.colors = {};
    }

    getColorConfiguration (source, singleColor = false) {
        if (singleColor) {
            return colors[0];
        }
        if (!this.colors[source]) {
            const all = Object.values(this.colors);
            const [best] = colors
                .map(color => ({color, weight: all.filter(c => c === color).length}))
                .sort((a, b) => a.weight - b.weight);
            this.colors[source] = best.color;
        }
        return this.colors[source];
    }
}
