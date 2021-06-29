function getFilter (o) {
    if (o.exclude && o.exclude.length > 0) {
        return track => o.exclude.indexOf(track.format) === -1;
    } else if (o.include && o.include.length > 0) {
        return track => o.include.indexOf(track.format) >= 0;
    }
    return () => true;
}

function wrapStateFn(fn, o) {
    return (tracks, options) => tracks
        .filter(getFilter(o))
        .map(track => fn(track.state, tracks.filter(getFilter(o)), track, options))
        .reduce((r, c) => r && c, true);
}

function wrapIndeterminateStateFn(fn, o) {
    return (tracks, options) => [
        ...(new Set(
            tracks
                .filter(getFilter(o))
                .map(track => fn(track.state, tracks.filter(getFilter(o)), track, options))
        ))
    ].length > 1;
}

function wrapDisplayFn(fn, o) {
    return (tracks, options) => {
        const display = [
            ...(new Set(
                tracks
                    .filter(getFilter(o))
                    .map(track => fn(track.state, tracks.filter(getFilter(o)), track, options))
            ))
        ];
        if (display.length === 1) {
            return display[0];
        }
        return undefined;
    };
}

function wrapDynamicFields(fn, o) {
    return (tracks, options) => tracks
        .filter(getFilter(o))
        .map(track => fn(track.state, tracks.filter(getFilter(o)), track, options))
        .reduce((r, c) => ([...r, ...c]), []);
}

// beforeFn(tracks) - called before all tracks state mutation
// preFn(track) - called before each track's state mutation
// postFn(track) - called after each track's state mutation
// afterFn(tracks) - called after all tracks state mutation
function wrapStateMutatorFn(fn, key, preFn, postFn, beforeFn, afterFn, o) {
    const singleTrackMutatorFn = (options) => (track, idx, tracks) => {
        const prePayload = typeof preFn === 'function'
            ? preFn(track, options)
            : {};
        fn(track.state, tracks, track, options);
        if (typeof postFn === 'function') {
            postFn(track, key, prePayload, options);
        }
    };
    return (tracks, options) => {
        if (typeof beforeFn === 'function') {
            beforeFn(tracks.filter(getFilter(o)), options);
        }
        tracks.filter(getFilter(o)).forEach(singleTrackMutatorFn(options));
        if (typeof afterFn === 'function') {
            afterFn(tracks.filter(getFilter(o)), key, options);
        }
    };
}

// beforeFn(tracks) - called before all tracks action
// preFn(track) - called before each track's action
// postFn(track) - called after each track's action
// afterFn(tracks) - called after all tracks action
function wrapPerformFn(fn, key, preFn, postFn, beforeFn, afterFn, o) {
    return (tracks, options) => {
        if (typeof beforeFn === 'function') {
            beforeFn(tracks.filter(getFilter(o)), options);
        }
        const prePayloads = tracks
            .filter(getFilter(o))
            .reduce((r, track) => ({
                ...r,
                [`${track.bioDataItemId}`]: typeof preFn === 'function'
                    ? preFn(track, options)
                    : {}
            }), {});
        fn(tracks.filter(getFilter(o)), options);
        tracks
            .filter(getFilter(o))
            .forEach(track => {
                if (typeof postFn === 'function') {
                    postFn(track, key, prePayloads[`${track.bioDataItemId}`], options);
                }
            });
        if (typeof afterFn === 'function') {
            afterFn(tracks.filter(getFilter(o)), key, options);
        }
    };
}

function wrapGetFn(fn, o) {
    return (tracks, options) => fn(tracks.filter(getFilter(o)), options);
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
    } = options || {};
    for (const key of Object.keys(menuEntry)) {
        switch (true) {
            case Array.isArray(menuEntry[key]):
                result[key] = menuEntry[key].map(subMenuEntry => processMenuEntry(subMenuEntry, options));
                break;
            case menuEntry[key] instanceof Function: {
                switch (true) {
                    case key.startsWith('is'):
                        result[key] = wrapStateFn(menuEntry[key], result);
                        result[`${key}Indeterminate`] = wrapIndeterminateStateFn(menuEntry[key], result);
                        break;
                    case key.startsWith('display'):
                        result[key] = wrapDisplayFn(menuEntry[key], result);
                        break;
                    case key === 'dynamicFields':
                        result[key] = wrapDynamicFields(menuEntry[key], result);
                        break;
                    case key === 'perform':
                        result[key] = wrapPerformFn(
                            menuEntry[key],
                            menuEntry.name,
                            prePerformFn,
                            postPerformFn,
                            beforePerformFn,
                            afterPerformFn,
                            result
                        );
                        break;
                    case key === 'get': {
                        result[key] = wrapGetFn(
                            menuEntry[key],
                            result
                        );
                        break;
                    }
                    default:
                        result[key] = wrapStateMutatorFn(
                            menuEntry[key],
                            menuEntry.name,
                            preStateMutatorFn,
                            postStateMutatorFn,
                            beforeStateMutatorFn,
                            afterStateMutatorFn,
                            result
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
