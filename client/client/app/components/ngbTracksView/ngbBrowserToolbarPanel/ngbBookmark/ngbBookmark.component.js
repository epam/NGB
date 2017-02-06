export template from './ngbBookmark.html';
import  baseController from '../../../../shared/baseController';

export const controller =  class NgbBookmarkController extends baseController {
    isClicked = false;
    bookmarkName = '';

    projectContext;
    localDataService;

    constructor($scope, $element, $timeout, bookmarkDataService, genomeDataService, localDataService, projectDataService, projectContext, dispatcher) {
        super();

        Object.assign(this, {
            $scope,
            $element,
            $timeout,
            bookmarkDataService,
            genomeDataService,
            localDataService,
            projectContext,
            projectDataService,
            dispatcher
        });
        this.input = $element.find('input');

        this.initEvents();

    }

    events = {
        'viewport:position': ::this.resetButtonState
    };

    onClick() {
        this.isClicked = true;
        this.$timeout(::this.input.focus, 200);

    }

    onBlur() {
        this.isClicked = false;
    }

    onKeyDown(event) {
        if (event.key === 'Enter') {
            this.saveBookmark();
            this.isClicked = false;
        }
    }

    resetButtonState() {
        this.isSaveSuccess = undefined;
    }

    getterSetterName(value) {
        return arguments.length ? this.bookmarkName = value : this.bookmarkName;
    }

    saveBookmark() {
        const ruler = {
            end: this.projectContext.viewport.end,
            start: this.projectContext.viewport.start
        };
        const chromosome = this.projectContext.currentChromosome;
        const name = this.bookmarkName.substring(0, 100) + (this.bookmarkName.length >= 100 ? '...' : '')
            || `${chromosome.name  }:${  this.projectContext.viewport.start  }-${  this.projectContext.viewport.end}`;
        const tracks = this.projectContext.tracksState;
        const layout = this.projectContext.layout;
        const vcfColumns = this.projectContext.vcfColumns;
        const query = new Bookmark(name, ruler, chromosome, tracks, layout, vcfColumns);
        try {
            const savingResult = this.localDataService.saveBookmark(query);
            this.isSaveSuccess = true;
            this.dispatcher.emitSimpleEvent('bookmark:save', {bookmarkId: savingResult.id});
        } catch (exception) {
            this.isSaveSuccess = false;
        }
        this.bookmarkName = '';
    }
};

function Bookmark(name, ruler, chromosome, tracks, layout, vcfColumns) {
    this.name = name;
    this.startIndex = parseInt(ruler.start);
    this.endIndex = parseInt(ruler.end);
    this.chromosome = {
        id: chromosome.id,
        name: chromosome.name
    };
    const mapFn = function(track) {
        return {
            bioDataItemId: track.bioDataItemId,
            height: track.height,
            projectId: track.projectId,
            state: track.state
        };
    };
    this.tracks = (tracks || []).map(mapFn);
    this.layout = layout;
    this.vcfColumns = vcfColumns;
}
