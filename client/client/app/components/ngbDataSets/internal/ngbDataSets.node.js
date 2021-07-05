export class SearchInfo {
    passed: boolean;
    pre: string; // string before match
    match: string;
    post: string; // string after match
    constructor() {
        this.passed = true;
    }
    test(pattern: string, s: string) {
        if (!pattern || !pattern.length) {
            this.passed = true;
            this.pre = s || '';
            this.match = '';
            this.post = '';
        } else {
            const index = (s || '').toLowerCase().indexOf(pattern.toLowerCase());
            this.passed = index >= 0;
            if (index >= 0) {
                this.pre = s.substring(0, index);
                this.match = s.substring(index, index + pattern.length);
                this.post = s.substring(index + pattern.length);
            } else {
                this.pre = s || '';
                this.match = '';
                this.post = '';
            }
        }
        return this.passed;
    }
}

export class Node {
    __selected: Boolean;
    __expanded: Boolean;
    __previousSelectedState: Boolean;

    nestedProjects: Array<Node>;
    items: Array<Node>; // for treeview displaying
    _lazyItems: Array<Node>; // pre-build tree. Used for lazy-building treeview
    tracks: Array<Node>;

    id;
    bioDataItemId;
    duplicateId;
    format;
    name;
    prettyName;
    displayName;
    modifiedName;

    project;
    projectId;
    reference;

    isTrack: Boolean;
    isProject: Boolean;
    isEmpty: Boolean;
    isLocal: Boolean;

    isPlaceholder: Boolean;

    filesCount: number;
    datasetsCount: number;

    hint: string;

    searchInfo: SearchInfo;
    modifiedNameSearchInfo: SearchInfo;
    searchFilterPassed: Boolean;
    childrenFilterPassed: Boolean;

    roles: Array<String>;
}
