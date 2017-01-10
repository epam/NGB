export class Node {
    __selected: Boolean;
    __expanded: Boolean;
    __previousSelectedState: Boolean;

    nestedProjects: Array<Node>;
    items: Array<Node>; // for treeview displaying
    _lazyItems: Array<Node>; // pre-build tree. Used for lazy-building treeview

    id;
    bioDataItemId;
    format;
    name;

    project;
    reference;

    isTrack: Boolean;
    isProject: Boolean;
    isEmpty: Boolean;

    isPlaceholder: Boolean;

    filesCount: number;
    datasetsCount: number;

    hint: string;

    searchFilterPassed: Boolean;
    childrenFilterPassed: Boolean;
}