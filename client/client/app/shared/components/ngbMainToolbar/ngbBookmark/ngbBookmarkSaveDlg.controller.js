export default class ngbBookmarkSaveDlgController {

    isLoading = false;
    bookmarkName;
    bookmarkDescription;
    error;

    constructor(dispatcher, $mdDialog, projectContext, $scope, trackNamingService, bookmarkDataService) {
        Object.assign(this, {
            dispatcher,
            $scope,
            $mdDialog,
            projectContext,
            trackNamingService,
            bookmarkDataService
        });
    }

    static get UID() {
        return 'ngbBookmarkSaveDlgController';
    }

    getterSetterName(value) {
        return arguments.length ? this.bookmarkName = value : this.bookmarkName;
    }

    getterSetterDesc(value) {
        return arguments.length ? this.bookmarkDescription = value : this.bookmarkDescription;
    }

    save(event) {
        this.isLoading = true;
        const ruler = {
            end: this.projectContext.viewport.end,
            start: this.projectContext.viewport.start
        };
        const chromosome = this.projectContext.currentChromosome;
        const name = this.bookmarkName.substring(0, 100) + (this.bookmarkName.length >= 100 ? '...' : '')
            || `${chromosome.name}:${this.projectContext.viewport.start}-${this.projectContext.viewport.end}`;
        const description = this.bookmarkDescription;
        const tracks = this.projectContext.tracksState;
        const layout = this.projectContext.layout;
        const vcfColumns = this.projectContext.vcfColumns;
        const customNames = this.trackNamingService.customNames;
        const reference = this.projectContext.reference ? this.projectContext.reference : {};
        const sessionValue = new Bookmark(name, description, reference, ruler, chromosome, tracks, layout, vcfColumns, customNames);
        const params = {
            owner: '',
            chromosome: sessionValue.chromosome.name,
            start: sessionValue.startIndex,
            description: sessionValue.description,
            sessionValue: JSON.stringify(sessionValue),
            referenceId: sessionValue.reference.id,
            name: sessionValue.name,
            end: sessionValue.endIndex
        };
        this.bookmarkDataService.saveBookmark(params)
            .then(data => {
                if (data.error) {
                    this.error = 'Error during save happened';
                } else {
                    this.dispatcher.emitSimpleEvent('bookmarks:save');
                    this.close();
                }
                this.isLoading = false;
                this.$scope.$apply();
            });
        event.stopImmediatePropagation();
        return false;
    }

    close() {
        this.$mdDialog.hide();
    }
}

function Bookmark(name, description, reference, ruler, chromosome, tracks, layout, vcfColumns, customNames) {
    this.name = name;
    this.description = description;
    this.reference = {
        name: reference.name,
        id: reference.id
    };
    this.customNames = customNames || {};
    this.startIndex = parseInt(ruler.start);
    this.endIndex = parseInt(ruler.end);
    this.chromosome = {
        id: chromosome.id,
        name: chromosome.name
    };
    const mapFn = function (track) {
        return {
            bioDataItemId: track.bioDataItemId,
            duplicateId: track.duplicateId,
            height: track.height,
            projectId: track.projectId,
            state: track.state,
            index: track.index,
            format: track.format,
            isLocal: track.isLocal
        };
    };
    this.tracks = (tracks || []).map(mapFn);
    this.layout = layout;
    this.vcfColumns = vcfColumns;
}
