import $ from 'jquery';

import { WholeGenomeRenderer } from '../../../../modules/render/whole-genome-render';

export default class ngbBlastGenomeViewController {
    
    static get UID() {
        return 'ngbBlastGenomeViewController';
    }

    _genomeRendererDiv;
    _genomeRenderer;
    _scope;
    _timeout;

    constructor($scope, $element, $timeout) {

        this._scope = $scope;
        this._timeout = $timeout;
        this._genomeRenderer = null;
        this._genomeRendererDiv = null;

        (async() => {
            await new Promise(resolve => $timeout(resolve));
            this._genomeRendererDiv = $($element[0]).find('#whole-genome-view-container')[0];
            this._genomeRenderer = new WholeGenomeRenderer(
                this.genomeRendererDiv,
                this.getMaxChromosomeSize(this.chromosomes),
                this.chromosomes,
                this.blastResult,
                null
            );

            $scope.$on('$destroy', () => {
                this.genomeRenderer.destroy();
            });

        })();
    }

    get genomeRenderer() {
        return this._genomeRenderer;
    }
    get genomeRendererDiv(){
        return this._genomeRendererDiv;
    }
    
    getMaxChromosomeSize(chrArray) {
        return chrArray.reduce((max, chr) => {
            return Math.max(chr.size, max);
        }, 0);
    };
}
