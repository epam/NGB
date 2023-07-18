
function getNodeLinks(
    id,
    data = [],
    areas = [],
    ontology = [],
    cache = {},
) {
    if (ontology.length === 0) {
        return {
            links: [],
            getLevel: () => 0
        };
    }
    const {
        parents: rawParents = []
    } = ontology.find((o) => o.id === id) || {};
    const result = [];
    const parents = rawParents
        .filter((parent) => (parent || '').trim().length > 0);
    if (parents.length === 0) {
        return {
            links: [],
            getLevel: () => 0
        };
    }
    const getLevels = [];
    parents.forEach((parent) => {
        const area = areas.find((o) => o.id === parent);
        const disease = data.find((o) => o.id === parent);
        if (area) {
            area.hit = true;
            result.push(parent);
            getLevels.push(() => 1);
        } else if (disease) {
            result.push(parent);
            getLevels.push(() => (typeof disease.getLevel === 'function' ? disease.getLevel() : 0) + 1);
        } else {
            if (!cache[parent]) {
                cache[parent] = getNodeLinks(parent, data, areas, ontology, cache);
            }
            getLevels.push(cache[parent].getLevel);
            result.push(...cache[parent].links);
        }
    });
    let _level = undefined;
    const getLevel = () => {
        if (_level !== undefined) {
            return _level;
        }
        _level = Math.max(0, ...getLevels.map((fn) => fn()));
        return _level;
    };
    return {
        links: [...new Set(result)],
        getLevel
    };
}

function buildNodeLinks(node, data = [], areas = [], ontology = [], cache = {}) {
    if (node.linksProcessed) {
        return;
    }
    node.linksProcessed = true;
    const {
        links,
        getLevel
    } = getNodeLinks(node.id, data, areas, ontology, cache);
    node.links = links;
    node.getLevel = getLevel;
}

function buildTree(data = [], ontology = []) {
    const processed = data.map((item) => ({
            ...item,
            disease: true
        }));
    const areas = processed.reduce((areasList, root) => {
        const result = [...areasList];
        (root.therapeuticAreas || []).forEach((area) => {
            if (!result.find((existing) => existing.id === area.id)) {
                result.push({...area});
            }
        });
        return result;
    }, []).map((area) => ({
        ...area,
        level: 0,
        area: true,
        hit: false,
        links: []
    }));
    const cache = {};
    processed
        .forEach((node) => buildNodeLinks(node, processed, areas, ontology, cache));
    processed
        .forEach((node) => {
            if (typeof node.getLevel === 'function') {
                node.level = node.getLevel();
            }
        });
    return [
        ...areas.filter((area) => area.hit),
        ...processed
    ];
}

export default function makeGraph(data = [], scoreFilter = 0.0, ontology = []) {
    const filtered = data
        .filter((item) => item.score >= scoreFilter);
    const formatted = buildTree(filtered, ontology)
        .map((item) => {
            const {
                level = 0
            } = item;
            return {
                data: item,
                level
            };
        });
    const levels = formatted
        .reduce((levelsResult, item) => {
            const {
                level
            } = item;
            const result = [...levelsResult];
            let levelItem = result.find((aLevel) => aLevel.id === level);
            if (!levelItem) {
                levelItem = {
                  id: level,
                  items: []
                };
                result.push(levelItem);
            }
            levelItem.items.push(item);
            return result;
        }, [])
        .sort((a, b) => a.level - b.level);
    levels.forEach((level) => {
        level.items.forEach((item, idx) => {
            const d = idx % 2 === 0 ? 1 : -1;
            const offset = Math.floor(idx / 2.0) + 0.5;
            item.x = level.id;
            item.y = offset * d;
            item.levelSize = level.items.length;
        });
    })
    return formatted;
}
