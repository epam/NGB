import {DataService} from '../data-service';

const MOCK_COLUMNS = 100;
const MOCK_ROWS = 100;
const MOCK_DATA_MIN = -50;
const MOCK_DATA_MAX = 50;

const DATA_TYPE_NUMBER = true;
const MISSING_DATA_THRESHOLD = 0.25;

function generateRandomItems(length, name) {
    return new Array(length)
        .fill(name)
        .map((o, i) => `${o} ${i + 1}`);
}

function generateRandomTreeItems(length, name) {
    return generateRandomItems(length, name).map(item => ([item, Math.random()]));
}

function generateRandomTree(items = []) {
    if (items.length < 2) {
        return items;
    }
    const elements = items.slice();
    const popRandomElement = () => {
        const index = Math.floor(Math.random() * elements.length);
        const [e] = elements.splice(index, 1);
        return e;
    };
    const result = [];
    result.push([popRandomElement(), popRandomElement()]);
    while (elements.length >= 2) {
        const left = popRandomElement();
        const right = popRandomElement();
        result.push([left, right]);
    }
    if (elements.length === 1) {
        return generateRandomTree([generateRandomTree(result), elements.pop()]);
    }
    return generateRandomTree(result);
}

const MOCK = false;

/**
 * data service for heatmap files
 * @extends DataService
 */
export class HeatmapDataService extends DataService {
    loadHeatmapMetadata(id, projectId) {
        if (!MOCK) {
            return new Promise((resolve, reject) => {
                this.get(`heatmap/${id}?projectId=${projectId}`)
                    .then((data)=> {
                        if (data) {
                            resolve(data);
                        } else {
                            throw new Error('Heatmap Data Service: error loading heatmap metadata');
                        }
                    })
                    .catch(reject);
            });
        }
        // eslint-disable-next-line no-console
        console.log(`load heatmap metadata mock for #${id} file (project #${projectId})`);
        // eslint-disable-next-line no-unused-vars
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                const columns = generateRandomItems(MOCK_COLUMNS, 'column');
                const rows = generateRandomItems(MOCK_ROWS, 'row');
                resolve({
                    columns,
                    rows,
                    min: MOCK_DATA_MIN,
                    max: MOCK_DATA_MAX,
                    values: DATA_TYPE_NUMBER ? [] : ['Item #1', 'Item #2', 'Item #3'],
                    type: DATA_TYPE_NUMBER ? 'number' : 'string'
                });
            }, 1000);
        });
    }

    loadHeatmapTree(id, projectId) {
        // eslint-disable-next-line no-console
        console.log(`load heatmap tree mock for #${id} file (project #${projectId})`);
        // eslint-disable-next-line no-unused-vars
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                const columns = generateRandomTreeItems(MOCK_COLUMNS, 'column');
                const rows = generateRandomTreeItems(MOCK_ROWS, 'row');
                const columnsTree = generateRandomTree(columns);
                const rowsTree = generateRandomTree(rows);
                resolve({
                    columns: columnsTree,
                    rows: rowsTree
                });
            }, 1000);
        });
    }

    loadHeatmap(id, projectId) {
        if (!MOCK) {
            return new Promise((resolve, reject) => {
                this.get(`heatmap/${id}/content?projectId=${projectId}`)
                    .then((data)=> {
                        if (data) {
                            resolve(data);
                        } else {
                            throw new Error('Heatmap Data Service: error loading heatmap data');
                        }
                    })
                    .catch(reject);
            });
        }
        // eslint-disable-next-line no-console
        console.log(`load heatmap mock for #${id} file (project #${projectId})`);
        // eslint-disable-next-line no-unused-vars
        return new Promise((resolve, reject) => {
            setTimeout(() => {
                const data = [];
                // const l = 50;
                // const d = o => Math.abs((o - Math.floor(o / l) * l) - l / 2.0) / l * 2.0;
                for (let r = 0; r < MOCK_ROWS; r += 1) {
                    const row = [];
                    data.push(row);
                    for (let c = 0; c < MOCK_COLUMNS; c += 1) {
                        // eslint-disable-next-line no-unused-vars
                        const dataItem = Math.random() * (MOCK_DATA_MAX - MOCK_DATA_MIN) + MOCK_DATA_MIN;
                        // const dataItem2 = MOCK_DATA_MIN + Math.max(d(c), d(r)) * (MOCK_DATA_MAX - MOCK_DATA_MIN);
                        // const dataItem3 = c < MOCK_COLUMNS / 2.0 ? MOCK_DATA_MIN : MOCK_DATA_MAX;
                        const dataItem4 = (c / MOCK_COLUMNS) * (MOCK_DATA_MAX - MOCK_DATA_MIN) + MOCK_DATA_MIN;

                        if (Math.random() > MISSING_DATA_THRESHOLD) {
                            if (DATA_TYPE_NUMBER) {
                                row.push([dataItem4, `Annotation for [${c + 1}, ${r + 1}]`]);
                            } else {
                                row.push([`Item #${c % 2 + r % 2 + 1}`, `Annotation for [${c + 1}, ${r + 1}]`]);
                            }
                        } else {
                            row.push([]);
                        }
                    }
                }
                resolve(data);
            }, 1000);
        });
    }
}
