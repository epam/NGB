function getFilter (o) {
    if (o.exclude && o.exclude.length > 0) {
        return track => o.exclude.indexOf(track.format) === -1;
    } else if (o.include && o.include.length > 0) {
        return track => o.include.indexOf(track.format) >= 0;
    }
    return () => true;
}

function wrapStateFn(fn, o) {
    return tracks => tracks
        .filter(getFilter(o))
        .map(track => fn(track.state, tracks.filter(getFilter(o)), track))
        .reduce((r, c) => r && c, true);
}

function wrapIndeterminateStateFn(fn, o) {
    return tracks => [
        ...(new Set(
            tracks
                .filter(getFilter(o))
                .map(track => fn(track.state, tracks.filter(getFilter(o)), track))
        ))
    ].length > 1;
}

function wrapDisplayFn(fn, o) {
    return (tracks) => {
        const display = [
            ...(new Set(
                tracks
                    .filter(getFilter(o))
                    .map(track => fn(track.state, tracks.filter(getFilter(o)), track))
            ))
        ];
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
function wrapStateMutatorFn(fn, key, preFn, postFn, beforeFn, afterFn, o) {
    const singleTrackMutatorFn = (track, idx, tracks) => {
        const prePayload = typeof preFn === 'function'
            ? preFn(track)
            : {};
        fn(track.state, tracks, track);
        if (typeof postFn === 'function') {
            postFn(track, key, prePayload);
        }
    };
    return (tracks) => {
        if (typeof beforeFn === 'function') {
            beforeFn(tracks.filter(getFilter(o)));
        }
        tracks.filter(getFilter(o)).forEach(singleTrackMutatorFn);
        if (typeof afterFn === 'function') {
            afterFn(tracks.filter(getFilter(o)), key);
        }
    };
}

// beforeFn(tracks) - called before all tracks action
// preFn(track) - called before each track's action
// postFn(track) - called after each track's action
// afterFn(tracks) - called after all tracks action
function wrapPerformFn(fn, key, preFn, postFn, beforeFn, afterFn, o) {
    return (tracks) => {
        if (typeof beforeFn === 'function') {
            beforeFn(tracks.filter(getFilter(o)));
        }
        const prePayloads = tracks
            .filter(getFilter(o))
            .reduce((r, track) => ({
                ...r,
                [`${track.bioDataItemId}`]: typeof preFn === 'function'
                    ? preFn(track)
                    : {}
            }), {});
        fn(tracks.filter(getFilter(o)));
        tracks
            .filter(getFilter(o))
            .forEach(track => {
                if (typeof postFn === 'function') {
                    postFn(track, key, prePayloads[`${track.bioDataItemId}`]);
                }
            });
        if (typeof afterFn === 'function') {
            afterFn(tracks.filter(getFilter(o)), key);
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
