import Tether from 'tether';
import angular from 'angular';

const tooltipElementContainer: HTMLElement = document.body;

const defaultStyle = `
width: auto;
min-width: 200px;
height: auto;
z-index: 1000;
margin: 2px;
pointer-events: none;
background-color: white;`;

const defaultTargetStyle = `
width: 0;
height: 0;
pointer-events: none;
position: absolute;
left: 0;
top: 0;
`;

const defaultTetherOpts = {
    attachment: 'top left',
    constraints: [
        {
            attachment: 'element',
            pin: true,
            to: 'window'
        }
    ],
    targetAttachment: 'right bottom'
};

export default function tooltipFactory(track) {
    const tooltipElement = document.createElement('track-tooltip');
    track.style.position = 'relative';
    const tooltipTargetElement = document.createElement('div');
    tooltipElementContainer.appendChild(tooltipTargetElement);
    tooltipElement.setAttribute('style', defaultStyle);
    tooltipTargetElement.setAttribute('style', defaultTargetStyle);
    const opts = {
        ...defaultTetherOpts,
        element: tooltipElement,
        target: tooltipTargetElement
    };
    const tether = new Tether(opts);
    tether.opts = opts;
    return {
        correctPosition(position) {
            const boundingRect = track.getBoundingClientRect();
            const bodyHeight = angular.element(document.getElementsByTagName('body'))[0].clientHeight,
                tooltipHeight = angular.element(tooltipElement).find('table')[0].clientHeight;
            const pos = {
                left: boundingRect.left + position.x,
                top: boundingRect.top + position.y
            };
            if (pos.top + tooltipHeight > bodyHeight) {
                pos.top -= tooltipHeight;
            }
            tooltipTargetElement.style.left = `${pos.left}px`;
            tooltipTargetElement.style.top = `${pos.top}px`;
            tether.position();
        },
        destructor() {
            tooltipElementContainer.removeChild(tooltipElement);
            tooltipElement.innerHTML = '';
        },
        hide() {
            if (this.delayedFn) {
                clearTimeout(this.delayedFn);
                this.delayedFn = null;
            }
            if (this.isShown)
                tooltipElementContainer.removeChild(tooltipElement);
            this.isShown = false;
        },
        isShown: false,
        move(position){
            this.hide();
            this.show(position);
            this.correctPosition(position);
        },
        setContent(content) {

            const mapLineElements = function({colspan, content}, index) {
                return `<td colspan='${colspan}' style='${index === 0 ? 'text-align:left' : 'text-align:right'}'>
                        ${content}</td>`;
            };

            const mapLine = function(line) {
                return `<tr>${line.map(mapLineElements).join('')}</tr>`;
            };

            tooltipElement.innerHTML = `<div class='md-open-menu-container md-whiteframe-z2 md-active md-clickable' style='width:100%'>
                <table style='width:100%'>${normalizeTooltipData(content).map(mapLine).join('')}</table></div>`;
        },
        show(position) {
            if (this.delayedFn) {
                clearTimeout(this.delayedFn);
                this.delayedFn = null;
            }
            const self = this;
            const showDelayed = function() {
                if (!self.isShown) {
                    self.isShown = true;
                    tooltipElementContainer.appendChild(tooltipElement);
                    if (position) {
                        self.correctPosition(position);
                    }
                }
            };
            const delayMs = 1000;
            this.delayedFn = setTimeout(showDelayed, delayMs);
        }
    };
}


function normalizeTooltipData(_tooltipData) {
    const tooltipData = Array.isArray(_tooltipData) ? _tooltipData : [_tooltipData];

    if (tooltipData) {
        return tooltipData
            .map(line => Array.isArray(line) ? line : (line !== undefined ? [line] : []))
            .map(line =>
                line.length === 1
                    ? [{colspan: 2, content: line[0]}]
                    : line.reduce((acc, content) =>
                    (acc.length === 0 && !content)
                        ? acc
                        : [...acc, content], [])
                    .map(content => ({colspan: 1, content})));
    } else {
        return [];
    }
}
