import {Node} from './ngbDataSets.node';

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
    let hasNestedProjects = false;
    if (node.nestedProjects) {
        hasNestedProjects = true;
        for (let i = 0; i < node.nestedProjects.length; i++) {
            _preprocessNode(node.nestedProjects[i], node);
        }
    }
    const [reference] = node.items.filter(track => track.format === 'REFERENCE');
    const isEmpty = reference && node.items.length === 1 && !hasNestedProjects;
    const mapTrackFn = function(track) {
        track.isTrack = true;
        track.project = node;
        track.reference = reference;
        track.hint = `${track.name}${reference ? `\r\nReference: ${reference.name}` : ''}`;
        track.searchFilterPassed = true;
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
    return node;
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
    const itemsSort = function(node1: Node, node2: Node) {
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
    const fn = function(item) {
        if (item instanceof Array) {
            item.sort(itemsSort).forEach(fn);
        } else {
            const node: Node = item;
            if (node.isProject) {
                let items = node.items;
                if (node._lazyItems) {
                    items = node._lazyItems;
                }
                items.sort(itemsSort);
                items
                    .filter(child => child.isProject)
                    .forEach(fn);
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
    if (node1.name.toLowerCase() === node2.name.toLowerCase()) {
        return 0;
    }
    return node1.name.toLowerCase() > node2.name.toLowerCase() ? -1 : 1;
}

export function updateTracksStateFn(tree, manager, opts, projectsPath) {
    if (!projectsPath || projectsPath.length === 0) {
        return null;
    }
    return function (item: Node) {
        if (item.isProject && projectsPath.indexOf(item.id) === -1) {
            manager.deselect(tree, item, opts);
            return false;
        } else if (item.isTrack && projectsPath[0] !== item.project.id) {
            manager.deselect(tree, item, opts);
        }
        return true;
    };
}

export function mapVisibleTrackFn(track: Node){
    return {
        bioDataItemId: track.bioDataItemId,
        hidden: false
    };
}

export function mapInvisibleTrackFn(track: Node){
    return {
        bioDataItemId: track.bioDataItemId,
        hidden: true
    };
}

export function findProject(datasets: Array<Node>, project) {
    const {id} = project;
    for (let i = 0; i < datasets.length; i++) {
        const dataset = datasets[i];
        if (!dataset.isProject)
            continue;
        if (dataset.id === id) {
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

export function expandNode(node: Node, manager, options) {
    if (node.items && node.items.length && node.items[0].isPlaceholder) {
        node.items = node._lazyItems;
        node._lazyItems = null;
        manager.validate(node, options);
    }
    if (node.project) {
        expandNode(node.project, manager, options);
    }
}

export function expandToProject(datasets: Array<Node>, project, manager, options) {
    const node = findProject(datasets, project);
    if (node) {
        expandNode(node, manager, options);
    }
}

export function filter(node: Node) {
    return node.format !== 'REFERENCE' && node.searchFilterPassed;
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
        item.searchFilterPassed = searchFilterPassed || item.name.toLowerCase().indexOf(pattern) === 0;
        result = result || item.searchFilterPassed;
    }
    return result;
}