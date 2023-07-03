import ngbDiseasesChartControllerBase from '../ngbDiseasesCharts/ngbDiseases.chart.controller.base';
import makeGraph from './make.graph';
import {
    circlePath,
    rectanglePath,
    getTreeUp,
    getTreeDown,
    getCurve
} from '../ngbDiseasesCharts/utilities';

const MIN_RADIUS = 3.0;
const MAX_RADIUS = 5.0;

const MAX_DATA_TO_SHOW_LINKS = 100;

export default class ngbDiseasesGraphController extends ngbDiseasesChartControllerBase {
    static get UID() {
        return 'ngbDiseasesGraphController';
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
        this._highlightedTreeNode = undefined;
    }

    get results() {
        if (this.ngbDiseasesChartService) {
            return this.ngbDiseasesChartService.diseases;
        }
        return null;
    }

    get ontology() {
        if (this.ngbDiseasesChartService) {
            return this.ngbDiseasesChartService.getCurrentOntology();
        }
        return [];
    }

    get highlightedTreeNode() {
        return this._highlightedTreeNode;
    }

    set highlightedTreeNode(node) {
        if (this._highlightedTreeNode !== node) {
            this._highlightedTreeNode = node;
            this.highlightCurrentHoveredNode();
        }
    }

    highlightCurrentHoveredNode() {
        if (!this.svg) {
            return;
        }
        if (this.highlightedTreeNode) {
            const children = getTreeUp(this.highlightedTreeNode, this.nodes || []);
            const parents = getTreeDown(this.highlightedTreeNode, this.nodes || []);
            const ids = [...children, ...parents];
            const getOpacity = (d) => {
                if (d.data && ids.includes(d.data.id)) {
                    return 1;
                }
                return 0.1;
            };
            const getFontWeight = (d) => {
                if (d.data && this.highlightedTreeNode.id === d.data.id) {
                    return 'bold';
                }
                return 'normal';
            };
            this.svg
                .selectAll('.node-image')
                .attr('fill-opacity', getOpacity)
                .attr('stroke-opacity', getOpacity);
            this.svg
                .selectAll('.node-title')
                .attr('fill-opacity', getOpacity)
                .attr('font-weight', getFontWeight);
            this.svg
                .selectAll('.node-link')
                .attr('stroke-opacity', (edge) => {
                    const {
                        child,
                        parent
                    } = edge;
                    if (
                        children.includes(parent.data.id) ||
                        parents.includes(child.data.id)
                    ) {
                        return 1;
                    }
                    return 0.1;
                });
        } else {
            this.svg
                .selectAll('.node-link')
                .attr('stroke-opacity', 0.25);
            this.svg
                .selectAll('.node-image')
                .attr('fill-opacity', 1)
                .attr('stroke-opacity', 1);
            this.svg
                .selectAll('.node-title')
                .attr('fill-opacity', 1)
                .attr('font-weight', 'normal');
        }
    }

    draw() {
        const svg = this.prepareSVG();
        if (!svg || !this.results) {
            return;
        }
        const width = svg.attr('width');
        const height = svg.attr('height');
        const data = makeGraph(
            this.results || [],
            this.scoreFilter,
            this.ontology,
        );
        const maxLevel = Math.max(...data.map((item) => item.x));
        const maxSize = Math.max(...data.map((item) => item.levelSize), 1);
        const offset = {
            top: 30 + MAX_RADIUS,
            bottom: 10 + MAX_RADIUS,
            left: 10 + MAX_RADIUS,
            right: 10 + MAX_RADIUS
        };
        const widthCorrected = width - offset.left - offset.right;
        const heightCorrected = height - offset.bottom - offset.top;
        const dY = Math.max(
            2.0 * (MIN_RADIUS + 1.0),
            Math.min(heightCorrected / maxSize, 4.0 * MAX_RADIUS)
        );
        const dX = widthCorrected / (maxLevel + 1);
        const cY = offset.top + heightCorrected / 2.0;
        const maxAbsoluteYPosition = (heightCorrected / 2.0) / dY;
        const filtered = data.filter((item) => Math.abs(item.y) < maxAbsoluteYPosition);
        const hidden = data
            .filter((item) => Math.abs(item.y) >= maxAbsoluteYPosition)
            .reduce((levels, item) => {
                let level = levels.find((aLevel) => aLevel.id === item.level);
                if (!level) {
                    level = {
                        id: item.level,
                        items: []
                    };
                    levels.push(level);
                }
                level.items.push(item);
                return levels;
            }, []);
        const more = [];
        hidden.forEach((level) => {
            if (level.items.length === 1) {
                filtered.push(...level.items);
            } else {
                more.push({
                    x: level.id,
                    y: Math.ceil(maxAbsoluteYPosition) + 0.5,
                    data: {
                        id: `level-${level.id}`,
                        name: `+${level.items.length} diseases`,
                        items: level.items,
                        more: true
                    }
                });
            }
        });
        this.nodes = filtered.map((item) => item.data).filter(Boolean);
        const r = Math.max(1, Math.floor(dY / 2.0 - 1.0));
        const getNodeClassName = (node) => {
            if (node.data && node.data.area) {
                return 'area';
            }
            if (node.data && node.data.disease) {
                return 'disease';
            }
            return undefined;
        }
        const getX = (node) => offset.left + dX * (node.x || 0);
        const getY = (node) => cY + dY * (node.y || 0);
        const aCircle = circlePath(r);
        const aRectangle = rectanglePath(2.0 * r);
        const getLinkedNode = (linkId) => filtered.find((o) => o.data && o.data.id === linkId);
        const getPath = (node) => {
            if (node.data && node.data.area) {
                return aRectangle;
            }
            if (node.data && node.data.disease) {
                return aCircle;
            }
            return '';
        };
        const getStroke = () => '#0f6496';
        const getFill = (node) => {
            if (node.data && node.data.area) {
                return 'white';
            }
            if (node.data && node.data.score !== undefined) {
                return this.ngbDiseasesChartService.getColorForScore(node.data.score) || 'white';
            }
            return 'transparent';
        };
        const getNodeId = (node, prefix) => {
            if (node.data && node.data.area) {
                return `${prefix}area-${node.data.id}`;
            }
            if (node.data && node.data.disease) {
                return `${prefix}disease-${node.data.id}`;
            }
            if (node.data && node.data.more) {
                return `${prefix}more-${node.data.id}`;
            }
            return undefined;
        };
        const showInfo = this.showInfo.bind(this);
        const hideInfo = this.hideInfo.bind(this);
        const edges = [];
        if (filtered.length < MAX_DATA_TO_SHOW_LINKS) {
            filtered.forEach((d) => {
                const node = d.data;
                if (node) {
                    const {
                        links = []
                    } = node;
                    links.forEach((linkId) => {
                        const linkNode = getLinkedNode(linkId);
                        if (linkNode) {
                            edges.push({
                                child: d,
                                parent: linkNode
                            });
                        }
                    });
                }
            });
        }
        svg
            .selectAll('.node-link')
            .data(edges)
            .enter()
            .append('path')
            .attr('class', 'node-link')
            .attr('d', (d) => {
                const x1 = getX(d.parent);
                const y1 = getY(d.parent);
                const x2 = getX(d.child);
                const y2 = getY(d.child);
                return getCurve(x1, y1, x2, y2, r);
            })
            .attr('stroke', '#90c5e5')
            .attr('fill', 'transparent')
            .attr('stroke-opacity', 0.25)
            .attr('stroke-width', 1);
        const moreLabels = this.svg
            .selectAll('.more-label')
            .data(more)
            .enter()
            .append('g')
            .attr('class', 'more-label')
            .attr('transform', (d) => `translate(${getX(d)}, ${getY(d)})`);
        const nodes = this.svg
            .selectAll('.node')
            .data(filtered)
            .enter()
            .append('g')
            .attr('class', (d) => ['node', getNodeClassName(d)].filter(Boolean).join(' '))
            .attr('transform', (d) => `translate(${getX(d)}, ${getY(d)})`)
            .on('mouseover', (node) => {
                if (node.data) {
                    showInfo(node.data, getX(node), getY(node));
                }
                this.highlightedTreeNode = node.data;
            })
            .on('mouseout', () => {
                hideInfo();
                this.highlightedTreeNode = undefined;
            });
        nodes
            .append('path')
            .attr('class', 'node-image')
            .attr('id', (d) => getNodeId(d, 'node-image-'))
            .attr('d', getPath)
            .attr('stroke', getStroke)
            .attr('fill', getFill);
        const getNodeClipPathId = (node) => getNodeId(node, 'clip');
        const getNodeClipPathUrl = (node) => {
            const id = getNodeId(node, 'clip');
            if (id) {
                return `url(#${id})`;
            }
            return undefined;
        };
        moreLabels
            .append('clipPath')
            .attr('id', getNodeClipPathId)
            .append('rect')
            .attr('x', 0)
            .attr('y', -dY / 2.0)
            .attr('width', dX - r)
            .attr('height', dY);
        moreLabels
            .append('text')
            .attr('class', 'node-title')
            .attr('x', 2)
            .attr('y', 2)
            .attr('fill', '#333333')
            .attr('pointer-events', 'none')
            .attr('text-anchor', 'start')
            .attr('alignment-baseline', 'middle')
            .attr('font-size', Math.min(2.0 * r, 10))
            .attr('font-style', 'italic')
            .text((node) => {
                if (node.data && node.data.name) {
                    return node.data.name;
                }
                return undefined;
            })
            .attr('clip-path', getNodeClipPathUrl);
        nodes
            .append('clipPath')
            .attr('id', getNodeClipPathId)
            .append('rect')
            .attr('x', r)
            .attr('y', -dY / 2.0)
            .attr('width', dX - 3.0 * r)
            .attr('height', dY);
        nodes
            .append('text')
            .attr('class', 'node-title')
            .attr('x', r + 2)
            .attr('y', 0)
            .attr('fill', '#333333')
            .attr('text-anchor', 'start')
            .attr('alignment-baseline', 'middle')
            .attr('font-size', Math.min(2.0 * r, 10))
            .text((node) => {
                if (node.data && node.data.name) {
                    return node.data.name;
                }
                return undefined;
            })
            .attr('clip-path', getNodeClipPathUrl);
        const legendSize = {
            width: 200,
            height: offset.top
        };
        const legend = svg
            .append('g')
            .attr('transform', `translate(${width / 2.0 - legendSize.width / 2.0}, 0)`);
        legend
            .append('rect')
            .attr('x', 0)
            .attr('y', 0)
            .attr('width', legendSize.width)
            .attr('height', legendSize.height)
            .attr('stroke', 'transparent')
            .attr('fill', 'rgba(255.0,255.0,255.0,50%)');
        legend
            .append('text')
            .attr('x', 10.0)
            .attr('y', legendSize.height / 2.0)
            .attr('fill', '#333333')
            .attr('pointer-events', 'none')
            .attr('text-anchor', 'start')
            .attr('alignment-baseline', 'middle')
            .attr('font-size', 11)
            .attr('font-weight', 'bold')
            .text('General');
        legend
            .append('text')
            .attr('x', legendSize.width - 10)
            .attr('y', legendSize.height / 2.0)
            .attr('fill', '#333333')
            .attr('pointer-events', 'none')
            .attr('text-anchor', 'end')
            .attr('alignment-baseline', 'middle')
            .attr('font-size', 11)
            .attr('font-weight', 'bold')
            .text('Specific');
        const arrowX1 = 60;
        const arrowX2 = legendSize.width - 60;
        const arrowWidth = arrowX2 - arrowX1;
        const arrow = legend
            .append('g')
            .attr('transform', `translate(${arrowX1}, ${legendSize.height / 2.0})`);
        arrow
            .append('path')
            .attr('d', `M0 0 L${arrowWidth} 0`)
            .attr('stroke', '#333333')
            .attr('fill', 'transparent');
        arrow
            .append('path')
            .attr('d', `M${arrowWidth} 0 L${arrowWidth - 5} -3 L${arrowWidth - 5} 3`)
            .attr('stroke', '#333333')
            .attr('fill', '#333333');
    }
}
