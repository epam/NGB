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
