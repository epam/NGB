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

    constructor($scope, $element,$timeout, projectContext) {

        this._scope = $scope;
        this._timeout = $timeout;
        this._genomeRenderer = null;
        this._genomeRendererDiv = null;
        this.projectContext = projectContext;

        (async() => {
            await new Promise(resolve => $timeout(resolve));
            this.getMaxChromosomeSize();
            this._genomeRendererDiv = $($element[0]).find('#cnv')[0];
            //this._genomeRenderer = new WholeGenomeRenderer(this.genomeRendererDiv, this.getMaxChromosomeSize(), projectContext.chromosomes.slice(1,5), null);
            this._genomeRenderer = new WholeGenomeRenderer(this.genomeRendererDiv, this.getMaxChromosomeSize(), projectContext.chromosomes.slice(0,8), null);

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
    getMaxChromosomeSize() {
        //const maxSize = this.projectContext.chromosomes.reduce((max, chr) => {
        const maxSize = this.projectContext.chromosomes.slice(0,8).reduce((max, chr) => {
            max = Math.max(chr.size, max);
            return max;
        }, 0);
        return maxSize;
    }
}