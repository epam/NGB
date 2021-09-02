export function getDivider(options = {}) {
    if (!getDivider.index)
        getDivider.index = 0;
    return {
        label: `%divider${getDivider.index++}%`, /*dirty hack for track by, track by is required as material menu is lagging on state change*/
        name: 'divider',
        ...options,
        type: 'divider'
    };
}
