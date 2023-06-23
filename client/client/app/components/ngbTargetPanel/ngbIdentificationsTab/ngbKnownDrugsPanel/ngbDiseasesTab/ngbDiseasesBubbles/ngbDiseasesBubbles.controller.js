import * as d3 from 'd3';
import * as d3hierarchy from 'd3-hierarchy';
import ngbDiseasesChartControllerBase from '../ngbDiseasesCharts/ngbDiseases.chart.controller.base';
import {
    circlePath
} from '../ngbDiseasesCharts/utilities';

export default class ngbDiseasesBubblesController extends ngbDiseasesChartControllerBase {
    static get UID() {
        return 'ngbDiseasesBubblesController';
    }

    constructor(
        $scope,
        $timeout,
        $element,
        dispatcher,
        ngbDiseasesChartService
    ) {
        super(
            $scope,
            $timeout,
            $element,
            dispatcher,
            ngbDiseasesChartService
        );
    }

    get results() {
        if (this.ngbDiseasesChartService) {
            return this.ngbDiseasesChartService.results;
        }
        return null;
    }

    draw() {
        const svg = this.prepareSVG();
        if (!svg || !this.results) {
            return;
        }
        const width = svg.attr('width');
        const height = svg.attr('height');
        const data = {
            children: this.results.map((item) => ({
                ...item,
                area: true,
                children: (item.diseases || [])
                    .filter((disease) => disease.score >= this.scoreFilter)
                    .map((disease) => ({
                        ...disease,
                        value: disease.score,
                        disease: true
                    }))
            }))
                .filter((area) => area.children.length > 0)
        };

        // Create a pack layout
        const pack = d3hierarchy.pack()
            .size([width, height])
            .padding((d) => {
                if (d.data && (d.data.area || d.data.disease)) {
                    return 5;
                }
                return 30;
            });

        const root = d3hierarchy.hierarchy(data)
            .sum((d) => d.value);

        const nodes = pack(root).descendants();
        const getColor = (d) => {
            if (d.data && d.data.disease) {
                return this.ngbDiseasesChartService.getColorForScore(d.data.score);
            }
            return undefined;
        }
        const getFill = (d) => {
            if (d.data && d.data.area) {
                return '#ffffff';
            }
            return 'transparent';
        }
        const getStroke = (d) => {
            if (d.data && (d.data.area || d.data.disease)) {
                return '#eeeeee';
            }
            return 'transparent';
        }
        const getNodeID = (d, prefix) => {
            if (d.data && d.data.area) {
                return `${prefix}-${d.data.id}`;
            }
            if (
                d.data &&
                d.data.disease &&
                d.parent &&
                d.parent.data &&
                d.parent.data.area
            ) {
                return `${prefix}-${d.parent.data.id}-${d.data.id}`;
            }
            return undefined;
        }
        const blocks = svg
            .selectAll('circle')
            .data(nodes)
            .enter()
            .append('g')
            .attr('transform', (d) => `translate(${d.x}, ${d.y})`);
        blocks
            .append('path')
            .attr('class', 'node-image')
            .attr('id', d=> getNodeID(d, 'path'))
            .attr('d', d => circlePath(d.r))
            .attr('fill', (d) => getColor(d) || getFill(d))
            .attr('stroke', (d) => getColor(d) || getStroke(d));
        const getYShift = (idx, total) => `${-(total / 2.0) + idx + 1.0}em`;
        const showInfo = this.showInfo.bind(this);
        const hideInfo = this.hideInfo.bind(this);
        blocks.each(function (d) {
            if (d.data && d.data.disease) {
                const self = d3.select(this);
                self
                    .on('mouseover', (node) => showInfo(node.data, node.x, node.y))
                    .on('mouseout', hideInfo);
            }
            if (d.data && d.data.disease && d.data.name && d.r > 10) {
                const self = d3.select(this);
                const getUrl = d => {
                    const id = getNodeID(d, 'clip');
                    if (id) {
                        return `url(#${id})`;
                    }
                    return undefined;
                }
                self
                    .append('clipPath')
                    .attr('id', d => getNodeID(d, 'clip'))
                    .append('circle')
                    .attr('cx', 0)
                    .attr('cy', 0)
                    .attr('r', d => d.r);
                const text = self
                    .append('text')
                    .attr('pointer-events', 'none')
                    .attr('text-anchor', 'middle')
                    .attr('font-size', '8')
                    .attr('fill', 'white')
                    .attr('clip-path', getUrl);
                d.data.name.split(/[\s,;.!?()]/)
                    .forEach((line, idx, total) => {
                        text
                            .append('tspan')
                            .text(line)
                            .attr('x', 0)
                            .attr('y', getYShift(idx, total.length));
                    })
            } else if (d.data.area) {
                d3.select(this)
                    .append('text')
                    .attr('pointer-events', 'none')
                    .attr('text-anchor', 'middle')
                    .attr('font-size', '10')
                    .attr('fill', '#666666')
                    .append('textPath')
                    .attr('startOffset', '50%')
                    .attr('href', d => getNodeID(d, '#path'))
                    .text(d => d.data.name);
            }
        });
    }
}
