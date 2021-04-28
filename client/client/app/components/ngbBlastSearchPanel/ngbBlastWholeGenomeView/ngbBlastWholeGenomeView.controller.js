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

    constructor($scope, $element, $timeout) {

        this._scope = $scope;
        this._timeout = $timeout;
        this._genomeRenderer = null;
        this._genomeRendererDiv = null;

        (async () => {
            await new Promise(resolve => $timeout(resolve));
            this._genomeRendererDiv = $($element[0]).find('.whole-genome-view-container')[0];
            this._tooltipElement = $($element[0]).find('.chromosome-hit-tooltip')[0];
            this._tooltipTarget = $($element[0]).find('.whole-genome-view-container')[0];
            this._genomeRenderer = new WholeGenomeRenderer(
                this.genomeRendererDiv,
                this.getMaxChromosomeSize(this.chromosomes),
                this.chromosomes,
                this.blastResult,
                this.displayTooltip.bind(this),
            );

            $scope.$on('$destroy', () => {
                this.genomeRenderer.destroy();
            });

        })();
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
