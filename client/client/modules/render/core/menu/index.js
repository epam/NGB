import buildMenu from './buildMenu';

function attachTracks(menuEntry, tracks, options) {
    const result = {};
    for (const key of Object.keys(menuEntry)) {
        switch (true) {
            case Array.isArray(menuEntry[key]):
                result[key] = menuEntry[key].map(subMenuEntry => attachTracks(subMenuEntry, tracks, options));
                break;
            case menuEntry[key] instanceof Function: {
                result[key] = () => menuEntry[key](tracks, options);
            }
                break;
            default:
                result[key] = menuEntry[key];
        }
    }
    if (result.type === 'submenu' && typeof result.dynamicFields === 'function') {
        const hash = fields => (fields || [])
            .map(field => field.hash ? field.hash() : '')
            .filter(Boolean)
            .map(o => `${o}`)
            .join('|');
        Reflect.defineProperty(
            result,
            'fields',
            {
                get: function () {
                    const fields = this.dynamicFields();
                    if (!this.dynamicFieldsCache || hash(this.dynamicFieldsCache) !== hash(fields)) {
                        this.dynamicFieldsCache = fields;
                    }
                    return this.dynamicFieldsCache;
                }
            });
    }
    return result;
}

function attach(tracks, options) {
    const convertToArray = o => Array.isArray(o) ? o : [o];
    return this.map(menuEntry => attachTracks(menuEntry, convertToArray(tracks), options));
}

export default function Menu(configuration, options) {
    const instance = buildMenu(configuration, options);
    instance.attach = attach.bind(instance);
    return instance;
}
