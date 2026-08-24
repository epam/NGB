export default class ngbBibliographyPanelController {

    searchText = '';

    get genesDisplay() {
        const selected = this.selectedGeneIds.length;
        const all = this.genes.length;
        return `${selected} gene${selected === 1 ? '' : 's'} selected of ${all}`;
    }

    static get UID() {
        return 'ngbBibliographyPanelController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbBibliographyPanelService,
        ngbTargetPanelService
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            ngbBibliographyPanelService,
            ngbTargetPanelService
        });
        const refresh = this.refresh.bind(this);
        dispatcher.on('target:identification:publications:page:changed', refresh);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:identification:publications:page:changed', refresh);
        });
    }

    get publications() {
        return this.ngbBibliographyPanelService.publications;
    }

    get keyWords() {
        return this.ngbBibliographyPanelService.keyWords;
    }
    set keyWords(value) {
        this.ngbBibliographyPanelService.keyWords = value;
    }

    refresh() {
        this.$timeout(() => this.$scope.$apply());
    }

    get loadingPublications() {
        return this.ngbBibliographyPanelService.loadingPublications;
    }
    get failedPublications() {
        return this.ngbBibliographyPanelService.failedPublications;
    }
    get publicationsError() {
        return this.ngbBibliographyPanelService.publicationsError;
    }
    get emptyPublications() {
        return this.ngbBibliographyPanelService.emptyPublications;
    }

    get totalPages() {
        return this.ngbBibliographyPanelService.totalPages;
    }
    get currentPage() {
        return this.ngbBibliographyPanelService.currentPage;
    }
    set currentPage(value) {
        this.ngbBibliographyPanelService.currentPage = value;
    }

    get genes() {
        return this.ngbTargetPanelService.allGenes || [];
    }
    get selectedGeneIds() {
        return this.ngbBibliographyPanelService.selectedGeneIds;
    }
    set selectedGeneIds(value) {
        this.ngbBibliographyPanelService.selectedGeneIds = value;
    }
    get searchedGeneIds() {
        return this.ngbBibliographyPanelService.searchedGeneIds;
    }

    $onInit() {
        (this.refresh)();
    }

    async searchPublications() {
        await this.ngbBibliographyPanelService.getDataOnPage(1);
        this.refresh();
    }

    onBlur () {
        if (this.searchText !== this.keyWords) {
            this.keyWords = this.searchText;
            this.searchPublications();
        }
    }

    onKeyPress (event) {
        switch ((event.code || '').toLowerCase()) {
            case 'enter':
                this.onBlur();
                break;
            default:
                break;
        }
    }

    onClickClear() {
        this.searchText = '';
        this.onBlur();
    }

    onCloseSelector() {
        if (this.selectedGeneIds.length) {
            const genesChanged = (selected, searched) => {
                if (selected.length !== searched.length) return true;
                selected = [...selected].sort();
                searched = [...searched].sort();
                return selected.some((id, index) => id !== searched[index]);
            }
            if (genesChanged(this.selectedGeneIds, this.searchedGeneIds)) {
                this.searchPublications();
            }
        }
    }
}
