import {EventGeneInfo} from '../utils/events';

const keysMapper = {
    geneTracks: 'gt',
    camera: 'v',
    geneId: 'g',
    transcriptId: 't',
    startIndex: 's',
    endIndex: 'e',
    pdb: 'p',
    highlight: 'h',
    mode: 'm',
    color: 'c',
    chain: 'ch',
    trackId: 'i',
    id: 'ti',
    projectId: 'pi',
    chromosomeId: 'ci'
};

const keysReverseMapper = Object
    .keys(keysMapper)
    .map(key => ({[keysMapper[key]]: key}))
    .reduce((r, c) => ({...r, ...c}), {});

const map = (o, mapper) => {
    const result = {};
    const keys = Object.keys(o || {});
    for (let k = 0; k < keys.length; k++) {
        const key = keys[k];
        if (Array.isArray(o[key])) {
            result[mapper[key] || key] = o[key].map(e => map(e, mapper));
        } else if (typeof o[key] === 'object') {
            result[mapper[key] || key] = map(o[key], mapper);
        } else {
            result[mapper[key] || key] = o[key];
        }
    }
    return result;
};

class MiewContext {
    static instance (dispatcher) {
        return new MiewContext(dispatcher);
    }
    constructor(dispatcher) {
        this.dispatcher = dispatcher;
        this._info = undefined;
        this.dispatcher.on('miew:clear:structure', () => {
            this.update();
        });
    }

    get info() {
        return this._info;
    }

    get routeInfo() {
        if (!this.info) {
            return undefined;
        }
        const info = this.info;
        const filtered = Object
            .keys(info)
            .filter(key => info.hasOwnProperty(key) && info[key] !== null && info[key] !== undefined)
            .map(key => ({[key]: info[key]}))
            .reduce((r, c) => ({...r, ...c}), {});
        return JSON.stringify(map(filtered, keysMapper));
    }

    set routeInfo(info) {
        try {
            this.update(map(JSON.parse(info), keysReverseMapper), {route: false, emit: true});
        } catch (e) {
            // eslint-disable-next-line no-console
            console.warn('Error parsing miew info from route:', e.message);
        }
    }

    update (opts, events = {}) {
        const {
            route = true,
            emit = false
        } = events;
        if (!opts) {
            this._info = undefined;
        } else {
            this._info = {
                ...(this._info || {}),
                ...opts
            };
            this._info.geneTracks = (this._info.geneTracks || []).map(track => ({
                id: track.id,
                chromosomeId: track.chromosomeId,
                projectId: track.projectId
            }));
        }
        if (this._info) {
            const {
                pdb,
                geneTracks,
                geneId,
                startIndex,
                endIndex
            } = this._info;
            if (
                !pdb ||
                !geneTracks ||
                geneTracks.length === 0 ||
                !geneId ||
                !startIndex ||
                !endIndex
            ) {
                this._info = undefined;
            }
        }
        if (route) {
            this.dispatcher.emitGlobalEvent('route:change');
        }
        if (emit) {
            this.emit();
        }
        this.dispatcher.emit('miew:structure:change', this.info);
    }

    emit() {
        if (this.info) {
            this.dispatcher.emitGlobalEvent(
                'miew:show:structure',
                new EventGeneInfo(this.info)
            );
        }
    }
}

export default MiewContext;
