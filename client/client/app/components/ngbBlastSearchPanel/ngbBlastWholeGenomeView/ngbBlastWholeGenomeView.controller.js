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

    constructor($scope, $element,$timeout) {

        this._scope = $scope;
        this._timeout = $timeout;
        this._genomeRenderer = null;
        this._genomeRendererDiv = null;

        (async() => {
            await new Promise(resolve => $timeout(resolve));

            this._genomeRendererDiv = $($element[0]).find('#cnv')[0];
            this._genomeRenderer = new WholeGenomeRenderer(this.genomeRendererDiv);

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
}