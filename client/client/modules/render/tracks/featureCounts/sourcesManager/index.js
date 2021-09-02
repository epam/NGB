import colors from './colors';
import grayScaleColors from './grayScaleColors';

export default class Index {
    static instance (dispatcher) {
        return new Index(dispatcher);
    }

    constructor(dispatcher) {
        this.dispatcher = dispatcher;
        this.clearCache();
        dispatcher.on('chromosome:change', this.clearCache.bind(this));
    }

    get sources () {
        return Object.keys(this.colors || {});
    }

    clearCache () {
        this.colors = {};
    }

    registerSource (...source) {
        source.forEach(s => this.getColorConfiguration(s));
    }

    getColorConfiguration (source, options = {}) {
        const {
            grayScale = false,
            colors: array = (grayScale ? grayScaleColors : colors),
            singleColor = false,
            hovered = false,
            border = false
        } = options;
        const getColor = (colorIndex = 0) => {
            const configuration = array[colorIndex];
            if (border) {
                return configuration.border;
            }
            if (hovered) {
                return configuration.hovered;
            }
            return configuration.color;
        };
        if (singleColor) {
            return getColor();
        }
        if (!this.colors.hasOwnProperty(source)) {
            const all = Object.values(this.colors);
            const [best] = array
                .map((color, index) => ({index, weight: all.filter(c => c === index).length}))
                .sort((a, b) => a.weight - b.weight);
            this.colors[source] = best.index;
        }
        return getColor(this.colors[source]);
    }
}
