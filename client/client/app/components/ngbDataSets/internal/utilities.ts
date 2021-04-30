import {Node, SearchInfo} from './ngbDataSets.node';

export function preprocessNode(node: Node) {
    return _preprocessNode(node, null);
}

const __tracks_formats = [
    'BAM',
    'BED',
    'GENE',
    'MAF',
    'SEG',
    'VCF',
    'WIG'
];

function _preprocessNode(node: Node, parent: Node = null) {
    node.isProject = true;
    node.project = parent;
    node.searchFilterPassed = true;
    node.childrenFilterPassed = true;
    node.roles = ['ROLE_ADMIN'];
    node.displayName = node.prettyName || node.name;
    node.searchInfo = new SearchInfo();
    node.modifiedNameSearchInfo = new SearchInfo();
    let hasNestedProjects = false;
    if (node.nestedProjects) {
        hasNestedProjects = true;
        for (let i = 0; i < node.nestedProjects.length; i++) {
            _preprocessNode(node.nestedProjects[i], node);
        }
    }
    const [reference] = node.items.filter(track => track.format === 'REFERENCE');
    const isEmpty = reference && node.items.length === 1 && !hasNestedProjects;
    const mapTrackFn = function (track) {
        track.isTrack = true;
        track.project = node;
        track.projectId = node.name;
        track.reference = reference;
        track.hint = `${getTrackFileName(track)}${reference ? `\r\nReference: ${reference.name}` : ''}`;
        track.searchFilterPassed = true;
        track.roles = ['ROLE_ADMIN'];
        track.displayName = getTrackFileName(track);
        return track;
    };
    node.hint = _getProjectHint(node, reference);
    const tracks = (node.items || []).map(mapTrackFn);
    node._lazyItems = [...(node.nestedProjects || []), ...tracks];
    node.items = [{isPlaceholder: true, searchFilterPassed: false}];
    node.isEmpty = isEmpty;
    node.filesCount = tracks.length - (reference ? 1 : 0);
    node.datasetsCount = (node.nestedProjects || []).length;
    node.reference = reference;
    node.tracks = tracks;
    return node;
}

function getTrackFileName(track) {
    if (!track.isLocal) {
        return track.prettyName || track.name;
    } else {
        const fileName = track.name;
        if (!fileName || !fileName.length) {
            return null;
        }
        return fileName.split('/').pop().split('\\').pop();
    }
}

function _getProjectHint(project, reference) {
    let tracksFormats = '';
    for (let i = 0; i < __tracks_formats.length; i++) {
        const count = project.items.filter(track => track.format === __tracks_formats[i]).length;
        if (count) {
            tracksFormats += `\r\n${count} ${__tracks_formats[i]} ${count === 1 ? 'file' : 'files'}`;
        }
    }
    return `${project.name}${reference ? `\r\nReference: ${reference.name}` : ''}${tracksFormats}`;
}

export function sortDatasets(content, sortFn) {
    if (!sortFn || !(sortFn instanceof Function)) {
        sortFn = sortByNameAsc;
    }
    const itemsSort = function (node1: Node, node2: Node) {
        if ((node1.isTrack && node2.isTrack) || (node1.isProject && node2.isProject)) {
            return sortFn(node1, node2);
        } else if (node1.isProject) {
            // projects should be above tracks
            return -1;
        } else {
            // node2.isProject
            return 1;
        }
    };
    const fn = function (item) {
        if (item instanceof Array) {
            item.sort(itemsSort).forEach(fn);
        } else {
            const node: Node = item;
            if (node.isProject) {
                let items = node.items;
                if (node._lazyItems) {
                    items = node._lazyItems;
                }
                if (items) {
                    items.sort(itemsSort);
                    items
                        .filter(child => child.isProject)
                        .forEach(fn);
                }
            }
        }
    };
    fn(content);
}

export function sortByNameAsc(node1: Node, node2: Node) {
    if (!node1.name) {
        return -1;
    } else if (!node2.name) {
        return 1;
    }
    if (node1.isLocal !== node2.isLocal) {
        if (node1.isLocal) {
            return -1;
        }
        if (node2.isLocal) {
            return 1;
        }
    }
    if (node1.name.toLowerCase() === node2.name.toLowerCase()) {
        return 0;
    }
    return node1.name.toLowerCase() > node2.name.toLowerCase() ? 1 : -1;
}

export function sortByNameDesc(node1: Node, node2: Node) {
    if (!node1.name) {
        return -1;
    } else if (!node2.name) {
        return 1;
    }
    if (node1.isLocal !== node2.isLocal) {
        if (node1.isLocal) {
            return -1;
        }
        if (node2.isLocal) {
            return 1;
        }
    }
    if (node1.name.toLowerCase() === node2.name.toLowerCase()) {
        return 0;
    }
    return node1.name.toLowerCase() > node2.name.toLowerCase() ? -1 : 1;
}

export function updateTracksStateFn(item: Node, reference) {
    if (!reference) {
        return;
    }
    if (item.isTrack && item.reference && item.reference.name !== reference.name) {
        item.__selected = false;
    }
    return true;
}

export function selectRecursively(item: Node, isSelected) {
    item.__selected = isSelected;
    if (item.isProject) {
        for (let i = 0; i < item.items.length; i++) {
            selectRecursively(item.items[i], isSelected);
        }
    }
}

export function mapTrackFn(track: Node) {
    return {
        bioDataItemId: track.name,
        projectId: track.projectId,
        isLocal: track.isLocal,
        format: track.format
    };
}

export function findProject(datasets: Array<Node>, project) {
    if (!datasets) {
        return null;
    }
    const {name} = project;
    for (let i = 0; i < datasets.length; i++) {
        const dataset = datasets[i];
        if (!dataset.isProject)
            continue;
        if (dataset.name === name) {
            return dataset;
        }
        let items = dataset.items;
        if (items && items.length && items[0].isPlaceholder) {
            items = dataset._lazyItems;
        }
        const result = findProject(items, project);
        if (result) {
            return result;
        }
    }
    return null;
}

export function findProjectReference(project: Node) {
    const tracks = project.items || project._lazyItems;
    let reference = null;
    if (tracks.filter(t => t.isTrack && !t.isPlaceholder).length) {
        [reference] = tracks.filter(t => t.isTrack && !t.isPlaceholder).map(t => t.reference);
    } else {
        const datasets = tracks.filter(t => t.isProject);
        for (let i = 0; i < datasets.length; i++) {
            const dataset = datasets[i];
            reference = findProjectReference(dataset);
            if (reference) {
                break;
            }
        }
    }
    return reference;
}

export function expandNode(node: Node) {
    if (node.items && node.items.length && node.items[0].isPlaceholder) {
        node.items = node._lazyItems;
        node._lazyItems = null;
    }
    if (node.project) {
        expandNode(node.project);
    }
}

export function expandNodeWithChilds(node: Node) {
    if (node.items && node.items.length && node.items[0].isPlaceholder) {
        node.items = node._lazyItems;
        node._lazyItems = null;
    }

    if (node.isProject) {
        node.items.forEach(item => {
            expandNodeWithChilds(item);
        });
    }
}

export function expandToProject(datasets: Array<Node>, project) {
    const node = findProject(datasets, project);
    if (node) {
        expandNode(node);
    }
}

export function treeFilter(node: Node) {
    return node.format !== 'REFERENCE' && node.searchFilterPassed;
}

export function getGenomeFilter(genome) {
    if (!genome) {
        return function () {
            return true;
        };
    } else {
        return function (node: Node) {
            return !node.reference || node.reference.name.toLowerCase() === genome.toLowerCase();
        };
    }
}

export function search(pattern, items: Array<Node>) {
    let result = false;
    for (let i = 0; i < items.length; i++) {
        const item = items[i];
        let searchFilterPassed = !pattern || !pattern.length;
        if (item.isProject) {
            let projectItems = item.items;
            if (projectItems && projectItems.length && projectItems[0].isPlaceholder) {
                projectItems = item._lazyItems;
            }
            item.childrenFilterPassed = search(pattern, projectItems);
            searchFilterPassed = searchFilterPassed || item.childrenFilterPassed;
        } else if (item.isTrack && item.format === 'REFERENCE') {
            item.searchFilterPassed = false;
            continue;
        }
        if (!item.searchInfo) {
            item.searchInfo = new SearchInfo();
        }
        if (!item.modifiedNameSearchInfo) {
            item.modifiedNameSearchInfo = new SearchInfo();
        }
        item.searchInfo.test(pattern, item.displayName);
        item.modifiedNameSearchInfo.test(pattern, item.modifiedName);
        item.searchFilterPassed = searchFilterPassed || item.searchInfo.passed || item.modifiedNameSearchInfo.passed;
        if (item.childrenFilterPassed && item.isProject && pattern.length > 0) {
            expandNodeWithChilds(item);
            item.__expanded = true;
        }
        result = result || item.searchFilterPassed;
    }
    return result;
}

export function toPlainList(items, namingService, ident = 0) {
    const result = [];
    if (items && items.length) {
        const sortedItems = items.map(item => {
            if (!item.isProject && namingService.nameChanged(item)) {
                item.modifiedName = namingService.getCustomName(item);
            } else {
                item.modifiedName = undefined;
            }
            return item;
        }).sort((a, b) => {
            const aName = (a.modifiedName || a.displayName || '').toLowerCase();
            const bName = (b.modifiedName || b.displayName || '').toLowerCase();
            if (aName > bName) {
                return 1;
            }
            if (aName < bName) {
                return -1;
            }
            return 0;
        });
        for (let i = 0; i < sortedItems.length; i++) {
            const item = sortedItems[i];
            item.ident = ident;
            result.push(item);
            if (item.isProject && item.__expanded) {
                result.push(...toPlainList(item.items, namingService, ident + 1));
            }
        }
    }
    return result;
}
