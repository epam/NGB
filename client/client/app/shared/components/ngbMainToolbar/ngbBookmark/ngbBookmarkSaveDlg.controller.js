export default class ngbBookmarkSaveDlgController {

    isLoading = false;
    bookmarkName;
    bookmarkDescription;
    error;

    constructor(
        dispatcher,
        $mdDialog,
        projectContext,
        miewContext,
        heatmapContext,
        $scope,
        trackNamingService,
        ngbStrainLineageService,
        ngbPathwaysService,
        bookmarkDataService
    ) {
        Object.assign(this, {
            dispatcher,
            $scope,
            $mdDialog,
            projectContext,
            miewContext,
            heatmapContext,
            trackNamingService,
            bookmarkDataService,
            ngbStrainLineageService,
            ngbPathwaysService
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

    getLineageState() {
        if (this.ngbStrainLineageService) {
            try {
                const {
                    selectedTree,
                    layout = {}
                } = this.ngbStrainLineageService.loadState() || {};
                if (selectedTree) {
                    const {id} = selectedTree;
                    if (layout.hasOwnProperty(id)) {
                        return {
                            selectedTree,
                            layout: {[id]: layout[id]}
                        };
                    }
                }
            } catch (_) {
                return undefined;
            }
        }
        return undefined;
    }

    getPathwaysState() {
        if (this.ngbPathwaysService) {
            return this.ngbPathwaysService.getSessionState();
        }
        return undefined;
    }

    save(event) {
        this.isLoading = true;
        const ruler = this.projectContext.viewport
            ? {
                end: this.projectContext.viewport.end,
                start: this.projectContext.viewport.start
            }
            : undefined;
        const chromosome = this.projectContext.currentChromosome;
        const name = this.bookmarkName.substring(0, 100) + (this.bookmarkName.length >= 100 ? '...' : '')
            || `${chromosome.name}:${this.projectContext.viewport.start}-${this.projectContext.viewport.end}`;
        const description = this.bookmarkDescription;
        const tracks = this.projectContext.tracksState;
        const layout = this.projectContext.layout;
        const vcfColumns = this.projectContext.vcfColumns;
        const customNames = this.trackNamingService.customNames;
        const reference = this.projectContext.reference ? this.projectContext.reference : {};
        const sessionValue = new Bookmark(
            name,
            description,
            reference,
            ruler,
            chromosome,
            tracks,
            layout,
            vcfColumns,
            customNames,
            this.miewContext.routeInfo,
            this.heatmapContext.routeInfo,
            this.getLineageState(),
            this.getPathwaysState()
        );
        const params = {
            owner: '',
            chromosome: sessionValue.chromosome ? sessionValue.chromosome.name : undefined,
            start: sessionValue.startIndex,
            description: sessionValue.description,
            sessionValue: JSON.stringify(sessionValue),
            referenceId: sessionValue.reference ? sessionValue.reference.id : undefined,
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

function Bookmark(
    name,
    description,
    reference,
    ruler,
    chromosome,
    tracks,
    layout,
    vcfColumns,
    customNames,
    miew,
    heatmap,
    lineage,
    pathways
) {
    this.name = name;
    this.description = description;
    this.reference = reference
        ? {
            name: reference.name,
            id: reference.id
        }
        : undefined;
    this.customNames = customNames || {};
    this.startIndex = ruler ? parseInt(ruler.start) : undefined;
    this.endIndex = ruler ? parseInt(ruler.end) : undefined;
    this.chromosome = chromosome
        ? {
            id: chromosome.id,
            name: chromosome.name
        }
        : undefined;
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
    this.miew = miew;
    this.heatmap = heatmap;
    this.lineage = lineage;
    this.pathways = pathways;
}
