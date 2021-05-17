import $ from 'jquery';
import Tether from 'tether';

import { WholeGenomeRenderer } from '../../../../modules/render/whole-genome-render';
import config from '../../../../modules/render/whole-genome-render/whole-genome-config';

export default class ngbBlastGenomeViewController {

    static get UID() {
        return 'ngbBlastGenomeViewController';
    }

    _genomeRendererDiv;
    _genomeRenderer;
    _scope;
    _timeout;
    _tooltipTarget;
    _tooltipElement;
    _tooltipContent = null;
    _tetherElement;

    constructor($scope, $element, $timeout, dispatcher) {

        this._scope = $scope;
        this._element = $element;
        this._timeout = $timeout;
        this._genomeRenderer = null;
        this._genomeRendererDiv = null;
        this._selectedSpecies = $scope.$parent.blastCurrentSpecies;

        (async () => {
            await new Promise(resolve => $timeout(resolve));
            this._genomeRendererDiv = $($element[0]).find('.whole-genome-view-container')[0];
            this._tooltipElement = $($element[0]).find('.chromosome-hit-tooltip')[0];
            this._tooltipTarget = $($element[0]).find('.whole-genome-view-container')[0];
            this._errorElement = $($element[0]).find('.no-species-selected')[0];
            if (this._selectedSpecies) {
                this._genomeRenderer = new WholeGenomeRenderer(
                    this.genomeRendererDiv,
                    this.getMaxChromosomeSize(this.chromosomes),
                    this.chromosomes,
                    this.blastResult,
                    this.displayTooltip.bind(this),
                );
            } else {
                this.displayErrorMessage();
            }
            const onDestroy = (selectedItem) => this.setSelectedSpeciesAndRenderCanvas(selectedItem);
            dispatcher.on('blast:select:species', onDestroy);
            $scope.$on('$destroy', () => {
                if (this._genomeRenderer){
                    this._genomeRenderer.destroy();
                }
                dispatcher.removeListener('blast:select:species', onDestroy);
            });
        })()
    }

    setSelectedSpeciesAndRenderCanvas(selectedItem) {
        $(this._errorElement).addClass('hidden');
        if (selectedItem && selectedItem !== this._selectedSpecies) {
            this._selectedSpecies = selectedItem;
            if (this._genomeRenderer) {
                this._genomeRenderer.destroy();
            }
            this._genomeRenderer = new WholeGenomeRenderer(
                this.genomeRendererDiv,
                this.getMaxChromosomeSize(this.chromosomes),
                this.chromosomes,
                this.blastResult,
                this.displayTooltip.bind(this),
            );
        }
    }

    get genomeRenderer() {
        return this._genomeRenderer;
    }
    get genomeRendererDiv() {
        return this._genomeRendererDiv;
    }
    getMaxChromosomeSize(chrArray) {
        return chrArray.reduce((max, chr) => Math.max(chr.size, max), 0);
    }
    displayTooltip(position, content) {
        if (content) {
            const offsetX = position.x;
            const offsetY = config.start.topMargin + position.y;

            this._tooltipContent = this.formatBlastTooltipContent(content);
            this._scope.$apply();

            if ($(this._tooltipElement).hasClass('hidden')) {
                $(this._tooltipElement).removeClass('hidden');
            }
            if (!this._tetherElement) {
                this._tetherElement = new Tether({
                    target: this._tooltipTarget,
                    element: this._tooltipElement,
                    attachment: 'top left',
                    targetAttachment: 'top left',
                    offset: `-${offsetY} -${offsetX}`,
                    constraints: [{
                        to: 'scrollParent',
                        pin: true
                    }],
                });
            }
            this._tetherElement.position();

        } else {
            this._tooltipContent = null;
            this._scope.$apply();
            if (!$(this._tooltipElement).hasClass('hidden')) {
                $(this._tooltipElement).addClass('hidden');
            }
            if (this._tetherElement) {
                this._tetherElement.destroy();
                this._tetherElement = null;
            }
        }
    }
    displayErrorMessage(){
        $(this._errorElement).removeClass('hidden');
    }
    formatBlastTooltipContent(content) {
        const blastResultsProps = ['score', 'strand', 'match', 'chromosome'];
        const contentForTooltip = {
            ...content
        };
        for (let key in contentForTooltip) {
            if (contentForTooltip.hasOwnProperty(key)) {
                if (!blastResultsProps.includes(key)) {
                    delete contentForTooltip[key];
                }
            }
        }
        return contentForTooltip;
    }
}
