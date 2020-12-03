function wrapStateFn(fn) {
    return tracks => tracks
        .map(track => fn(track.state))
        .reduce((r, c) => r && c, true);
}

function wrapIndeterminateStateFn(fn) {
    return tracks => [...(new Set(tracks.map(track => fn(track.state))))].length > 1;
}

function wrapDisplayFn(fn) {
    return (tracks) => {
        const display = [...(new Set(tracks.map(track => fn(track.state))))];
        if (display.length === 1) {
            return display[0];
        }
        return undefined;
    };
}

// beforeFn(tracks) - called before all tracks state mutation
// preFn(track) - called before each track's state mutation
// postFn(track) - called after each track's state mutation
// afterFn(tracks) - called after all tracks state mutation
function wrapStateMutatorFn(fn, key, preFn, postFn, beforeFn, afterFn) {
    const singleTrackMutatorFn = (track) => {
        const prePayload = typeof preFn === 'function'
            ? preFn(track)
            : {};
        fn(track.state);
        if (typeof postFn === 'function') {
            postFn(track, key, prePayload);
        }
    };
    return (tracks) => {
        if (typeof beforeFn === 'function') {
            beforeFn(tracks);
        }
        tracks.forEach(singleTrackMutatorFn);
        if (typeof afterFn === 'function') {
            afterFn(tracks, key);
        }
    };
}

// beforeFn(tracks) - called before all tracks action
// preFn(track) - called before each track's action
// postFn(track) - called after each track's action
// afterFn(tracks) - called after all tracks action
function wrapPerformFn(fn, key, preFn, postFn, beforeFn, afterFn) {
    return (tracks) => {
        if (typeof beforeFn === 'function') {
            beforeFn(tracks);
        }
        const prePayloads = tracks.reduce((r, track) => ({
            ...r,
            [`${track.bioDataItemId}`]: typeof preFn === 'function'
                ? preFn(track)
                : {}
        }), {});
        fn(tracks);
        tracks.forEach(track => {
            if (typeof postFn === 'function') {
                postFn(track, key, prePayloads[`${track.bioDataItemId}`]);
            }
        });
        if (typeof afterFn === 'function') {
            afterFn(tracks, key);
        }
    };
}

function processMenuEntry(menuEntry, options) {
    const result = {};
    const {
        beforePerformFn,
        prePerformFn,
        postPerformFn,
        afterPerformFn,
        beforeStateMutatorFn,
        preStateMutatorFn,
        postStateMutatorFn,
        afterStateMutatorFn
    } = options;
    for (const key of Object.keys(menuEntry)) {
        switch (true) {
            case Array.isArray(menuEntry[key]):
                result[key] = menuEntry[key].map(subMenuEntry => processMenuEntry(subMenuEntry, options));
                break;
            case menuEntry[key] instanceof Function: {
                switch (true) {
                    case key.startsWith('is'):
                        result[key] = wrapStateFn(menuEntry[key]);
                        result[`${key}Indeterminate`] = wrapIndeterminateStateFn(menuEntry[key]);
                        break;
                    case key.startsWith('display'):
                        result[key] = wrapDisplayFn(menuEntry[key]);
                        break;
                    case key === 'perform':
                        result[key] = wrapPerformFn(
                            menuEntry[key],
                            menuEntry.name,
                            prePerformFn,
                            postPerformFn,
                            beforePerformFn,
                            afterPerformFn
                        );
                        break;
                    default:
                        result[key] = wrapStateMutatorFn(
                            menuEntry[key],
                            menuEntry.name,
                            preStateMutatorFn,
                            postStateMutatorFn,
                            beforeStateMutatorFn,
                            afterStateMutatorFn
                        );
                        break;
                }
            }
                break;
            default:
                result[key] = menuEntry[key];
        }
    }

    return result;
}

export default function buildMenu(menu, options) {
    return menu.map(menuEntry => processMenuEntry(menuEntry, options));
}
