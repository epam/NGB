const INDEX_FILE_FORMAT = {
    'bam': ['bai'],
    'vcf': ['tbi', 'idx'],
    'bed': ['tbi', 'idx'],
    'gene': ['tbi', 'idx']
};

function getDisplayDirectory(root, currentFolder, pathSeparator) {
    if (root && currentFolder) {
        let displayDirectory = currentFolder;
        if (displayDirectory.toLowerCase().indexOf(root.toLowerCase()) === 0) {
            displayDirectory = displayDirectory.slice(root.length);
            if (pathSeparator && displayDirectory.startsWith(pathSeparator)) {
                displayDirectory = displayDirectory.slice(pathSeparator.length);
            }
        }
        return displayDirectory;
    }
    return null;
}

export default class ngbOpenFileFromNGBServerController {
    static get UID() {
        return 'ngbOpenFileFromNGBServerController';
    }

    projectContext;
    utilsDataService;
    references;
    referenceId;
    switchingReference = false;
    isLoading = false;

    items;
    rootFolder;
    pathSeparator = '/';

    _lockSelection = false;

    gridOptions = {
        enableFiltering: false,
        enableGridMenu: false,
        enableHorizontalScrollbar: 0,
        enablePinning: false,
        enableRowHeaderSelection: false,
        enableRowSelection: true,
        headerRowHeight: 20,
        height: '100%',
        multiSelect: true,
        rowHeight: 48,
        showHeader: true,
        treeRowHeaderAlwaysVisible: false
    };

    previousDirectory = {
        isPrevious: true,
        name: '...',
        type: 'DIRECTORY'
    };
    currentDirectory = null;
    displayDirectory = null;
    displayDirectoryPath = [];
    selectedItems = [];
    _isDirectoryTyping = false;

    static lastCurrentDirectory = null;

    constructor($scope, projectContext, utilsDataService) {
        this.utilsDataService = utilsDataService;
        this.currentDirectory = ngbOpenFileFromNGBServerController.lastCurrentDirectory;
        const headerCells = require('./explorer/ngbExplorer.item.header.tpl.html');
        const typeCells = require('./explorer/ngbExplorer.item.type.tpl.html');
        const nameCells = require('./explorer/ngbExplorer.item.name.tpl.html');
        this.scope = $scope;
        const columnDefs = [
            {
                cellTemplate: typeCells,
                enableColumnMenu: false,
                enableSorting: false,
                enableMove: false,
                field: 'type',
                headerCellTemplate: headerCells,
                maxWidth: 16,
                minWidth: 16,
                name: ''
            },
            {
                enableHiding: false,
                cellTemplate: nameCells,
                field: 'name',
                headerCellTemplate: headerCells,
                minWidth: 50,
                name: 'Name',
                width: '*'
            },
            {
                enableHiding: false,
                field: 'format',
                headerCellTemplate: headerCells,
                minWidth: 50,
                name: 'Type',
                width: '100'
            },
            {
                enableHiding: false,
                field: 'size',
                headerCellTemplate: headerCells,
                minWidth: 50,
                name: 'Size',
                width: '100',
                cellFilter: 'sizeFilter'
            }
        ];
        Object.assign(this.gridOptions, {
            appScopeProvider: $scope,
            columnDefs: columnDefs,
            rowHeight: 20,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.selection.on.rowSelectionChanged(this.scope, ::this.rowClick);
            }
        });
        this.projectContext = projectContext;
        this.references = this.projectContext.references;
        this.referenceId = this.projectContext.reference ? +this.projectContext.reference.id : null;
        if (!this.referenceId && this.references && this.references.length > 0) {
            this.referenceId = this.references[0].id;
        }
        this.clearSelectionHandler = () => {
            this._lockSelection = true;
            for (let i = 0; i < this.selectedItems.length; i++) {
                const [item] = this.gridOptions.data.filter(d => d.name === this.selectedItems[i].track.name && d.type === this.selectedItems[i].track.type && d.format === this.selectedItems[i].track.format);
                if (item) {
                    this.gridApi.selection.unSelectRow(item);
                }
            }
            this._lockSelection = false;
            this.selectedItems = [];
            this.check();
        };
        $scope.$watch('$ctrl.referenceId', ::this.check);
        (async() => {
            await this.loadCurrentDirectory();
            await this.projectContext.refreshReferences(true);
            this.references = this.projectContext.references;
            if (!this.referenceId && this.references && this.references.length > 0) {
                this.referenceId = this.references[0].id;
            }
        })();
    }

    get isDirectoryTyping() {
        return this._isDirectoryTyping;
    }

    async didTypeDirectory() {
        let directory = this.displayDirectory || '';
        if (directory.indexOf(this.rootFolder) !== 0) {
            if (directory.length === 0) {
                directory = null;
            } else if (directory.indexOf(this.pathSeparator) === 0) {
                directory = `${this.rootFolder}${directory}`;
            } else {
                directory = `${this.rootFolder}${this.pathSeparator}${directory}`;
            }
        }
        this.currentDirectory = directory;
        ngbOpenFileFromNGBServerController.lastCurrentDirectory = directory;
        await this.loadCurrentDirectory();
        this._isDirectoryTyping = false;
    }

    updateDirectoryPath() {
        let paths = [];
        if (this.displayDirectory) {
            paths = this.displayDirectory.split(this.pathSeparator);
        }
        const separator = this.pathSeparator;
        const root = this.rootFolder;
        const getFullPath = (index) => {
            let path = root;
            for (let i = 0; i <= index; i++) {
                path = `${path}${separator}${paths[i]}`;
            }
            return path;
        };
        const result = [{
            name: 'NGB',
            value: null
        }];
        for (let i = 0; i < paths.length; i++) {
            result.push({
                name: paths[i],
                value: getFullPath(i)
            });
        }
        paths = result;
        this.displayDirectoryPath = paths;
    }

    async didClickOnPath(path, event) {
        event.stopImmediatePropagation();
        this.currentDirectory = path;
        ngbOpenFileFromNGBServerController.lastCurrentDirectory = path;
        await this.loadCurrentDirectory();
    }

    startDirectoryTyping() {
        this._isDirectoryTyping = true;
    }

    discardDirectoryTyping() {
        if (this.currentDirectory && this.rootFolder) {
            this.displayDirectory = getDisplayDirectory(this.rootFolder, this.currentDirectory, this.pathSeparator);
        }
        this.updateDirectoryPath();
        this._isDirectoryTyping = false;
    }

    check() {
        this.switchingReference = this.projectContext.reference && this.projectContext.reference.id !== +this.referenceId;
        const [reference] = this.references.filter(r => r.id === +this.referenceId);
        if (reference) {
            this.tracks = (this.selectedItems || []).map(f => this.mapTrack(f, reference));
        } else {
            this.tracks = [];
        }
    }

    static trackNeedsIndexFile(track) {
        return INDEX_FILE_FORMAT[track.format.toLowerCase()] !== undefined;
    }

    static trackIsIndexFile(track) {
        return track.format.toLowerCase().indexOf('_index') !== -1;
    }

    static getIndexFileForTrack(track, files) {
        const indexExtensions = INDEX_FILE_FORMAT[track.format.toLowerCase()];
        if (indexExtensions) {
            for (let i = 0; i < indexExtensions.length; i++) {
                const extension = indexExtensions[i];
                const nameVersion1 = `${track.name}.${extension}`.toLowerCase();
                let nameVersion2 = null;
                if (track.format.toLowerCase() === 'bam') {
                    const nameParts = track.name.split('.');
                    nameVersion2 = '';
                    for (let i = 0; i < nameParts.length - 1; i ++) {
                        nameVersion2 += `${nameParts[i]}.`;
                    }
                    nameVersion2 += `${extension}`;
                    nameVersion2 = nameVersion2.toLowerCase();
                }
                const format = `${track.format.toLowerCase()}_index`;
                const [indexFile] = files.filter(t => t.track.format.toLowerCase() === format &&
                (
                    t.track.name.toLowerCase() === nameVersion1 ||
                    (nameVersion2 !== null && t.track.name.toLowerCase() === nameVersion2)));
                if (indexFile) {
                    return indexFile;
                }
            }
        }
        return null;
    }

    mapTrack(file, reference) {
        const {track} = file;
        if (track.indexFile) {
            return {
                index: `${track.indexFile.folder}${this.pathSeparator}${track.indexFile.track.name}`,
                path: `${file.folder}${this.pathSeparator}${track.name}`,
                reference: reference,
                format: track.format,
                name: track.name
            };
        } else {
            return {
                index: null,
                path: `${file.folder}${this.pathSeparator}${track.name}`,
                reference: reference,
                format: track.format,
                name: track.name
            };
        }
    }

    rowClick(cell) {
        if (this._lockSelection) {
            return;
        }
        if (cell.entity.type === 'DIRECTORY') {
            cell.isSelected = false;
            if (cell.entity.isPrevious) {
                const pathes = this.currentDirectory.split(this.pathSeparator);
                pathes.splice(pathes.length - 1, 1);
                this.currentDirectory = pathes.reduce((path, entry) =>
                    (!entry || !entry.length) ? path : path + this.pathSeparator + entry, '');
                ngbOpenFileFromNGBServerController.lastCurrentDirectory = this.currentDirectory;
                this.loadCurrentDirectory();
            } else {
                this.currentDirectory = cell.entity.path;
                ngbOpenFileFromNGBServerController.lastCurrentDirectory = this.currentDirectory;
                this.loadCurrentDirectory();
            }
        } else if (cell.isSelected) {
            if (!cell.entity.canSelect) {
                cell.isSelected = false;
            } else {
                this.selectedItems.push({
                    folder: this.currentDirectory || this.rootFolder || '',
                    track: cell.entity
                });
            }
        } else {
            const [item] = this.selectedItems
                .filter(i => i.track.name === cell.entity.name);
            if (item) {
                const index = this.selectedItems.indexOf(item);
                if (index >= 0) {
                    this.selectedItems.splice(index, 1);
                }
            }
        }
        this.check();
    }

    async loadCurrentDirectory() {
        this.selectedItems = [];
        this.isLoading = true;
        this.gridOptions.data = [];
        const sortFn = (item1, item2) => {
            if (item1.type === item2.type) {
                if (item1.name.toLowerCase() > item2.name.toLowerCase()) {
                    return 1;
                } else if (item1.name.toLowerCase() < item2.name.toLowerCase()) {
                    return -1;
                }
                return 0;
            }
            if (item1.type === 'FILE') {
                return 1;
            }
            return -1;
        };
        let {files, root, separator} = await this.utilsDataService.getFiles(this.currentDirectory);
        if (root) {
            this.rootFolder = root;
        }
        if (separator) {
            this.pathSeparator = separator;
        }
        const mapFn = (item) => {
            if (item.type === 'DIRECTORY') {
                const parts = item.path.split(this.pathSeparator);
                item.name = parts[parts.length - 1];
            }
            return item;
        };
        files = this.preprocessFiles((files || []).map(mapFn).sort(sortFn));
        const isRoot = !this.currentDirectory || !this.currentDirectory.length || this.currentDirectory.toLowerCase() === this.rootFolder;
        if (this.currentDirectory && this.rootFolder) {
            this.displayDirectory = getDisplayDirectory(this.rootFolder, this.currentDirectory, this.pathSeparator);
        } else {
            this.displayDirectory = null;
        }
        this.updateDirectoryPath();
        this.gridOptions.data = isRoot ? files : [this.previousDirectory, ...files];
        this.isLoading = false;
        this.scope.$apply();
    }

    preprocessFiles(files) {
        const result = [];
        const mapFn = (item) => {
            return {
                folder: this.currentDirectory || this.rootFolder || '',
                track: item
            }
        };
        const allTracks = files.filter(i => i.type === 'FILE').map(mapFn);
        for (let i = 0; i < files.length; i++) {
            if (files[i].type === 'DIRECTORY') {
                result.push(files[i]);
            } else if (ngbOpenFileFromNGBServerController.trackIsIndexFile(files[i])){

            } else if (!ngbOpenFileFromNGBServerController.trackNeedsIndexFile(files[i])) {
                files[i].indexRequired = false;
                result.push(files[i]);
            } else {
                files[i].indexRequired = true;
                files[i].indexFile = ngbOpenFileFromNGBServerController.getIndexFileForTrack(files[i], allTracks);
                result.push(files[i]);
            }
        }
        result.forEach(t => {
            if (t.type === 'FILE') {
                t.canSelect = !t.indexRequired || t.indexFile;
            } else {
                t.canSelect = true;
            }
        });
        return result;
    }

}
