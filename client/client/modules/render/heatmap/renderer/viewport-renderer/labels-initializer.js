/**
 * Creates text sprites for labels
 * @param {LabelsManager} labelsManager
 * @param {PIXI.TextStyle} config
 * @param {Array<string>} labels
 * @return {Promise<PIXI.Sprite[]>}
 */
export default function labelsInitializer(labelsManager, config, labels = []) {
    if (!labelsManager || !config || labels.length === 0) {
        return Promise.resolve([]);
    }
    const maxIterationsPerFrame = 50;
    return new Promise((resolve) => {
        const result = [];
        const _iterate = (index = 0) => {
            const nextIterationIndex = index + maxIterationsPerFrame;
            for (let i = index; i < nextIterationIndex && i < labels.length; i++) {
                result.push(labelsManager.getLabel(labels[i], config));
            }
            if (nextIterationIndex >= labels.length) {
                resolve(result);
            } else {
                requestAnimationFrame(() => _iterate(nextIterationIndex));
            }
        };
        requestAnimationFrame(() => _iterate());
    });
}
