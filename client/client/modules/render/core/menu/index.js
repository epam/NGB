import buildMenu from './buildMenu';

function attachTracks(menuEntry, tracks) {
    const result = {};
    for (const key of Object.keys(menuEntry)) {
        switch (true) {
            case Array.isArray(menuEntry[key]):
                result[key] = menuEntry[key].map(subMenuEntry => attachTracks(subMenuEntry, tracks));
                break;
            case menuEntry[key] instanceof Function: {
                result[key] = () => menuEntry[key](tracks);
            }
                break;
            default:
                result[key] = menuEntry[key];
        }
    }
    return result;
}

function attach(...tracks) {
    return this.map(menuEntry => attachTracks(menuEntry, tracks));
}

export default function Menu(configuration, options) {
    const instance = buildMenu(configuration, options);
    instance.attach = attach.bind(instance);
    return instance;
}
