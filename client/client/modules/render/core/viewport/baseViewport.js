import {Subject} from 'rx';

interface Range {
    start: number;
    end: number;
}

export default class BaseViewport {
    canvasChangeSubject = new Subject();

    brush: Range;
    canvas: Range;
    chromosome: Range;

    get canvasSize(): number {
        return this.canvas.end - this.canvas.start;
    }

    get chromosomeSize(): number {
        return this.chromosome.end - this.chromosome.start + 1;
    }

    get brushSize(): number {
        return this.brush.end - this.brush.start;
    }

    get factor() {
        return this.canvasSize / this.brushSize;
    }

    get chromosomeFactor() {
        return this.canvasSize / this.chromosomeSize;
    }

    types = {
        brushBP: {
            from: x => x / (this.brushSize + 1),
            offset: () => { const pbOffset = 0.5; return this.brush.start - pbOffset; },
            to: x => x * (this.brushSize + 1)
        },
        chromoBP: {
            from: x => x / (this.chromosomeSize + 1),
            offset: () => { const pbOffset = 0.5; return this.chromosome.start - pbOffset; },
            to: x => x * (this.chromosomeSize + 1)
        },
        convert: (typeFrom, typeTo, x) => typeTo.to(typeFrom.from(x)),
        pixel: {
            from: x => x / this.canvasSize,
            offset: () => this.canvas.start,
            to: x => x * this.canvasSize
        },
        project: (typeFrom, typeTo, x) => this.types.convert(typeFrom, typeTo, x - typeFrom.offset(x)) + typeTo.offset(x),
    };

    get convert() {
        return this._convert;
    }

    get project() {
        return this._project;
    }

    _convert = {
        brushBP2pixel: this.types.convert.bind(this, this.types.brushBP, this.types.pixel),
        chromoBP2pixel: this.types.convert.bind(this, this.types.chromoBP, this.types.pixel),
        pixel2brushBP: this.types.convert.bind(this, this.types.pixel, this.types.brushBP),
        pixel2chromoBP: this.types.convert.bind(this, this.types.pixel, this.types.chromoBP),
    };

    _project = {
        brushBP2pixel: this.types.project.bind(this, this.types.brushBP, this.types.pixel),
        chromoBP2pixel: this.types.project.bind(this, this.types.chromoBP, this.types.pixel),
        pixel2brushBP: this.types.project.bind(this, this.types.pixel, this.types.brushBP),
        pixel2chromoBP: this.types.project.bind(this, this.types.pixel, this.types.chromoBP)
    };

    constructor({chromosome, brush, canvas, factor}) {
        this.initialize({chromosome, brush, canvas, factor});
    }

    initialize({chromosome, brush, canvas, factor}) {
        if (!chromosome) {
            throw new Error('Viewport instance should be initialized at least with `chromosomeSize`');
        }
        this.chromosome = chromosome;
        if (factor !== undefined && factor !== null && canvas !== undefined && canvas !== null) {
            this.canvas = canvas;
            const brushStartDefined = brush.start !== null && brush.start !== undefined;
            const brushEndDefined = brush.end !== null && brush.end !== undefined;
            const brushSize = this.canvasSize / factor;
            if (brushStartDefined && !brushEndDefined) {
                brush.end = brush.start + brushSize;
            }
            else if (!brushStartDefined && brushEndDefined) {
                brush.start = brush.end - brushSize;
            }
            else {
                throw new Error('Viewport instance should be initialized by `factor` only with `brush.start` or `brush.end`');
            }
            this.brush = brush;
        }
        else {
            this.brush = brush;
            this.canvas = canvas;
        }

    }

    resize(newSize) {
        const oldSize = this.canvas.end - this.canvas.start;
        if (oldSize !== newSize) {
            this.canvas.end = this.canvas.start + newSize;
            this.canvasChangeSubject.onNext(this);
        }
    }
}
