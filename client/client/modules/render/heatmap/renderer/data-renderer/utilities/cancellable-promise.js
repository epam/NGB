export default function cancellablePromise(promiseFn, previousCancel) {
    let cancelRequested = false;
    let finished = false;
    const before = typeof previousCancel === 'function' ? previousCancel() : Promise.resolve();
    before
        .then(() => {
            if (cancelRequested) {
                throw new Error('cancelled');
            }
        })
        .then(() => promiseFn(() => cancelRequested))
        .catch(() => {})
        .then(() => {
            finished = true;
        });
    return function cancel() {
        cancelRequested = true;
        return new Promise((resolve) => {
            const checkFinished = () => {
                if (finished) {
                    resolve();
                }
                setTimeout(checkFinished, 0);
            };
            checkFinished();
        });
    };
}
