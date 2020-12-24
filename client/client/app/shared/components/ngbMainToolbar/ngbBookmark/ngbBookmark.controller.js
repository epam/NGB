import  baseController from '../../../baseController';

export default class ngbBookmarkController extends baseController {

    static get UID() {
        return 'ngbBookmarkController';
    }

    isClicked = false;
    bookmarkName = '';

    projectContext;
    localDataService;
    showContentMenu;
    trackNamingService;
    $mdMenu;

    constructor(
        $scope,
        $element,
        $timeout,
        $mdMenu,
        bookmarkDataService,
        genomeDataService,
        localDataService,
        projectDataService,
        projectContext,
        dispatcher,
        trackNamingService
    ) {
        super();

        Object.assign(this, {
            $scope,
            $element,
            $timeout,
            $mdMenu,
            bookmarkDataService,
            genomeDataService,
            localDataService,
            projectContext,
            projectDataService,
            dispatcher,
            trackNamingService
        });
        this.input = $element.find('input');

        this.initEvents();

    }

    openMenu($mdOpenMenu, ev) {
        this.showContentMenu = !this.showContentMenu;
        $mdOpenMenu(ev);
    }

    events = {
        'viewport:position': ::this.resetButtonState
    };

    onClick() {
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
        this.isClicked = false;
    }

    getterSetterName(value) {
        return arguments.length ? this.bookmarkName = value : this.bookmarkName;
    }

    saveBookmark() {

        if (!this.bookmarkName || !this.bookmarkName.length) {
            return;
        }

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
        const customNames = this.trackNamingService.customNames;
        const query = new Bookmark(name, ruler, chromosome, tracks, layout, vcfColumns, customNames);
        try {
            const savingResult = this.localDataService.saveBookmark(query);
            this.isSaveSuccess = true;
            this.dispatcher.emitSimpleEvent('bookmark:save', {bookmarkId: savingResult.id});
            this.$mdMenu.hide();
        } catch (exception) {
            this.isSaveSuccess = false;
        }
        this.bookmarkName = '';
    }
};

function Bookmark(name, ruler, chromosome, tracks, layout, vcfColumns, customNames) {
    this.name = name;
    this.customNames = customNames || {};
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