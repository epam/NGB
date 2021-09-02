export default class ngbMotifsTablePagination {

    firstChromosome;
    lastChromosome;
    lastPosition;

    static get UID() {
        return 'ngbMotifsTablePagination';
    }

    constructor($scope, dispatcher, projectContext, ngbMotifsPanelService) {
        Object.assign(this, {$scope, dispatcher, projectContext, ngbMotifsPanelService});
    }

    $onInit () {
        const type = this.ngbMotifsPanelService.currentParams.searchType;
        if (type === 'CHROMOSOME') {
            this.firstChromosome = this.projectContext.currentChromosome.id;
            this.lastChromosome = this.projectContext.currentChromosome.id;
            this.lastPosition = this.projectContext.currentChromosome.size;
        } else if (type === 'WHOLE_GENOME') {
            const last = this.projectContext.chromosomes.length - 1;
            this.firstChromosome = this.projectContext.chromosomes[0].id;
            this.lastChromosome = this.projectContext.chromosomes[last].id;
            this.lastPosition = this.projectContext.chromosomes[last].size;
        }
    }

    get currentChromosome () {
        return this.ngbMotifsPanelService.searchStopOnChromosome;
    }

    get currentPosition () {
        return this.ngbMotifsPanelService.searchStopOnPosition;
    }

    get isFirstPage () {
        return (
            this.currentChromosome === this.firstChromosome &&
            this.currentPosition === 0
        );
    }

    get isLastPage () {
        return (
            this.currentChromosome === this.lastChromosome &&
            this.currentPosition <= this.lastPosition
        );
    }

    getNextPage () {
        this.dispatcher.emitSimpleEvent('motifs:pagination:next');
    }

    getPreviousPage () {
        this.dispatcher.emitSimpleEvent('motifs:pagination:previous');
    }
}
