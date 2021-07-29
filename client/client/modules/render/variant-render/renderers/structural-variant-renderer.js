import * as PIXI from 'pixi.js';
import VariantBaseRenderer from './base-renderer';
import {BaseViewport, drawingConfiguration} from '../../core';
import {StructuralVariantConfig, CommonConfig} from '../configs';
import {PixiTextSize, ColorProcessor} from '../../utilities';
import {
    FeatureCutOffRenderer,
    StrandDirectionRenderer,
    DomainColorsManager
} from './drawing';
import {VariantZonesManager} from './zones';
import {DragManager} from './interaction';

const Math = window.Math;

const expandCollpaseButtonRadius = 7;
const emptyGeneSize = 20;

export default class StructuralVariantRenderer extends VariantBaseRenderer {

    _config = Object.assign(CommonConfig, StructuralVariantConfig);
    _domainColorsManager:DomainColorsManager = new DomainColorsManager(this._config);
    _featureCutOffRenderer:FeatureCutOffRenderer = new FeatureCutOffRenderer(this._config);
    _variantZonesManager:VariantZonesManager = new VariantZonesManager(this._config);
    _strandDirectionRenderer:StrandDirectionRenderer = new StrandDirectionRenderer(this._config);
    _dragManager:DragManager = null;
    _breakpointPositioning:Map = new Map();

    _legendExpanded = false;
    _transcriptExpandedStatus = null;

    _mainOffset = 0;
    _visualAreaOffset = 0;

    _altChromosomeNames = [];
    _altChromosomeVisualInfo = {};

    _exonPositions = [];
    _domainLegendPositions = [];

    _hoveredDomain = null;

    get config() {
        return this._config;
    }

    get featureCutOffRenderer():FeatureCutOffRenderer {
        return this._featureCutOffRenderer;
    }

    get variantZonesManager():VariantZonesManager {
        return this._variantZonesManager;
    }

    get dragManager():DragManager {
        return this._dragManager;
    }

    get height() {
        return this.variantZonesManager.getTotalHeight();
    }

    get strandDirectionRenderer():StrandDirectionRenderer {
        return this._strandDirectionRenderer;
    }

    get domainColorsManager():DomainColorsManager {
        return this._domainColorsManager;
    }

    constructor(variant, heightChanged, showTooltip, affectedGeneTranscriptChanged, updateSceneFn, reRenderScene) {
        super(variant, heightChanged, showTooltip, affectedGeneTranscriptChanged, updateSceneFn, reRenderScene);
        this._dragManager = new DragManager(this.config, this.container);
    }

    renderObjects(flags, config) {
        if (super.renderObjects(flags, config)) {
            if (this.variant !== null && this.variant !== undefined) {
                this._renderVariantEffect(config);
                if (this._height !== this.height) {
                    this._height = this.height;
                    this._heightChanged();
                }
            }
        }
    }

    manageViewports(config) {
        super.manageViewports(config);
        this.domainColorsManager.domainsInfo = this.variant.analysisResult.domainColors;
        this.domainColorsManager.genesInfo = this.variant.analysisResult.geneNames;
        const refZonez = this._manageReferenceViewports(config);
        const altZone = this._manageAlternativeViewports(config);
        const buffer = {name: 'buffer'};
        const legendZone = {name: 'legends', zones: [], expanded: this._legendExpanded};
        for (const legend in this.variant.analysisResult.domainColors) {
            if (this.variant.analysisResult.domainColors.hasOwnProperty(legend)) {
                legendZone.zones.push({name: legend, zones: [{name: 'legend'}]});
            }
        }
        this.variantZonesManager.configureZones([...refZonez, buffer, altZone, buffer, {name: 'legendsLabel'}, legendZone]);
    }

    _manageReferenceViewports(config) {
        this._breakpointPositioning.clear();
        if (!this._transcriptExpandedStatus) {
            this._transcriptExpandedStatus = {};
        }
        let maximumItemsLength = 0;
        let maximumLeftLength = 0;
        let maximumRightLength = 0;
        let maximumBreakpointsPerChromosome = 0;
        const chromosomeVisualInfo = {};
        const refZone = {name: 'reference', zones: []};
        let maxChromosomeLabelSize = 0;

        const geneTranscriptsZones = [];

        for (let i = 0; i < this.variant.analysisResult.chromosomes.length; i++) {
            const name = this._getChromosomeDisplayName(this.variant.analysisResult.chromosomes[i]);
            const size = PixiTextSize.getTextSize(name, this.config.chromosome.label);
            if (maxChromosomeLabelSize < size.width) {
                maxChromosomeLabelSize = size.width;
            }
        }
        for (let i = 0; i < this.variant.analysisResult.chromosomes.length; i++) {
            const subZone = {
                name: this.variant.analysisResult.chromosomes[i].toUpperCase(),
                zones: []
            };
            refZone.zones.push(subZone);
            const breakpointsAtChromosome = this.variant.analysisResult.breakpoints.filter(breakpoint => breakpoint.chromosome.name.toLowerCase() === this.variant.analysisResult.chromosomes[i]);
            let itemsLengthAtChromosome = 0;
            const genes = {};
            const geneNames = [];
            const visualBreakpoints = [];
            let prevGene = null;
            let prevPosition = null;
            let maxTranscriptsPerChromosome = 0;
            let emptyGenesLength = 0;
            for (let j = 0; j < breakpointsAtChromosome.length; j++) {
                const breakpoint = breakpointsAtChromosome[j];
                if (breakpoint.affectedGene !== undefined && breakpoint.affectedGene !== null) {
                    const affectedGene = breakpoint.affectedGene;
                    if (!affectedGene.empty) {
                        if (maxTranscriptsPerChromosome < affectedGene.transcripts.length) {
                            maxTranscriptsPerChromosome = affectedGene.transcripts.length;
                        }
                    }
                    if (prevGene && prevPosition && (prevGene.empty || affectedGene.empty || prevGene.name !== affectedGene.name)) {
                        const left = {
                            gene: prevGene,
                            range: {
                                start: prevPosition,
                                end: prevGene.empty ? emptyGeneSize : prevGene.totalExonsLength
                            }
                        };
                        const right = {
                            gene: affectedGene,
                            range: {
                                start: affectedGene.empty ? 0 : breakpoint.relativePositions[affectedGene.name],
                                end: affectedGene.empty ? emptyGeneSize / 2 : affectedGene.totalExonsLength
                            }
                        };
                        visualBreakpoints.push({
                            left,
                            right
                        });
                    }
                    const left = {
                        gene: affectedGene,
                        range: {
                            start: affectedGene.empty ? 0 : ((prevGene && prevGene.name === affectedGene.name) ? prevPosition : 0),
                            end: affectedGene.empty ? emptyGeneSize / 2 : breakpoint.relativePositions[breakpoint.affectedGene.name]
                        }
                    };
                    const right = {
                        gene: affectedGene,
                        range: {
                            start: affectedGene.empty ? emptyGeneSize / 2 : breakpoint.relativePositions[breakpoint.affectedGene.name],
                            end: affectedGene.empty ? emptyGeneSize : affectedGene.totalExonsLength
                        }
                    };
                    visualBreakpoints.push({
                        left,
                        right,
                        dataBreakpoint: breakpoint
                    });

                    prevGene = affectedGene;
                    prevPosition = affectedGene.empty ? emptyGeneSize / 2 : breakpoint.relativePositions[breakpoint.affectedGene.name];
                    if (affectedGene.empty) {
                        emptyGenesLength += emptyGeneSize;
                    }
                    else {
                        if (!genes.hasOwnProperty(affectedGene.name)) {
                            genes[affectedGene.name] = 0;
                            geneNames.push(affectedGene.name);
                        }
                        let itemsLengthAtGene = 0;
                        for (let k = 0; k < affectedGene.consensusExons.length; k++) {
                            itemsLengthAtGene += (affectedGene.consensusExons[k].end - affectedGene.consensusExons[k].start);
                        }
                        genes[affectedGene.name] = itemsLengthAtGene;
                    }
                }
            }
            subZone.zones.push({name: 'gene'});
            subZone.zones.push({name: 'upper', zones: [{name: 'edges'}]});
            subZone.zones.push({name: 'chromosome'});
            subZone.zones.push({name: 'down', zones: [{name: 'edges'}]});

            for (let j = 0; j < geneNames.length; j++) {
                itemsLengthAtChromosome += genes[geneNames[j]];
            }
            itemsLengthAtChromosome += emptyGenesLength;
            chromosomeVisualInfo[this.variant.analysisResult.chromosomes[i]] = {
                itemsLength: itemsLengthAtChromosome,
                visualBreakpoints: visualBreakpoints
            };
            if (maximumBreakpointsPerChromosome < visualBreakpoints.length) {
                maximumBreakpointsPerChromosome = visualBreakpoints.length;
            }
            if (maximumItemsLength < itemsLengthAtChromosome) {
                maximumItemsLength = itemsLengthAtChromosome;
            }
        }
        if (maximumBreakpointsPerChromosome === 1) {
            for (let i = 0; i < this.variant.analysisResult.chromosomes.length; i++) {
                const chromosomeName = this.variant.analysisResult.chromosomes[i];
                if (chromosomeVisualInfo[chromosomeName].visualBreakpoints.length === 1) {
                    const visualBreakpoint = chromosomeVisualInfo[chromosomeName].visualBreakpoints[0];
                    chromosomeVisualInfo[chromosomeName].leftPart = visualBreakpoint.left.range.end - visualBreakpoint.left.range.start;
                    if (visualBreakpoint.left.range.end - visualBreakpoint.left.range.start > maximumLeftLength) {
                        maximumLeftLength = visualBreakpoint.left.range.end - visualBreakpoint.left.range.start;
                    }
                    if (visualBreakpoint.right.range.end - visualBreakpoint.right.range.start > maximumRightLength) {
                        maximumRightLength = visualBreakpoint.right.range.end - visualBreakpoint.right.range.start;
                    }
                }
            }
            maximumItemsLength = maximumLeftLength + maximumRightLength;
        }
        this._visualAreaOffset = this.config.reference.width + this.config.reference.margin.x + this.config.chromosome.margin + this.config.breakpoint.width / 2;
        const offset = this._visualAreaOffset + maxChromosomeLabelSize;
        this._mainOffset = offset - 5;
        const usefulCanvasLength = config.width - maximumBreakpointsPerChromosome * this.config.breakpoint.width - 2 * offset;
        const factor = usefulCanvasLength / maximumItemsLength;

        const convertBpToPixels = (bp) => bp * factor;

        const registerViewport = (name, bpRange, canvasStart) => {
            const canvasParams = {
                start: canvasStart,
                end: canvasStart + convertBpToPixels(bpRange.end - bpRange.start)
            };
            const viewport = new BaseViewport({chromosome: bpRange, brush: bpRange, canvas: canvasParams});
            let viewports = this.viewports.get(name.toUpperCase());
            if (!viewports) {
                viewports = [];
            }
            viewports.push(viewport);
            this.viewports.set(name.toUpperCase(), viewports);
            return viewport;
        };

        const registerViewportByRange = (name, bpRange, canvasRange) => {
            const viewport = new BaseViewport({chromosome: bpRange, brush: bpRange, canvas: canvasRange});
            let viewports = this.viewports.get(name.toUpperCase());
            if (!viewports) {
                viewports = [];
            }
            viewports.push(viewport);
            this.viewports.set(name.toUpperCase(), viewports);
            return viewport;
        };

        for (let i = 0; i < this.variant.analysisResult.geneNames.length; i++) {
            const gene = this.variant.analysisResult.genes[this.variant.analysisResult.geneNames[i]].gene;
            if (this._transcriptExpandedStatus[gene.name.toUpperCase()] === undefined) {
                this._transcriptExpandedStatus[gene.name.toUpperCase()] = false;
            }
            const start = 0;
            let end = null;
            const zone = {
                name: `${gene.name} transcripts`,
                zones: [{name: 'transcriptLabel'}]
            };
            const geneTranscriptsZone = {
                name: 'transcriptsZone',
                zones: [],
                expanded: this._transcriptExpandedStatus[gene.name.toUpperCase()]
            };
            zone.zones.push(geneTranscriptsZone);
            geneTranscriptsZones.push(zone);
            for (let j = 0; j < gene.transcripts.length; j++) {
                const transcript = gene.transcripts[j];
                for (let t = 0; t < transcript.canonicalCds.length; t++) {
                    const cds = transcript.canonicalCds[t];
                    if (end === null || end < cds.positionFromStart.end) {
                        end = cds.positionFromStart.end;
                    }
                }
                geneTranscriptsZone.zones.push({
                    name: `${gene.name}_transcript_${j}`,
                    zones: [{name: 'transcriptName'}, {name: 'transcript'}]
                });
                registerViewportByRange(`${gene.name}_transcript_${j}`,
                    {start, end},
                    {
                        start: offset + this.config.transcript.radio.radius * 2 +
                        this.config.transcript.radio.margin * 2,
                        end: config.width - offset
                    });
            }
            geneTranscriptsZone.zones.push({name: 'buffer'});
        }

        for (let i = 0; i < this.variant.analysisResult.chromosomes.length; i++) {
            const chromosomeName = this.variant.analysisResult.chromosomes[i];
            const chromosomeWidthPx = convertBpToPixels(chromosomeVisualInfo[chromosomeName].itemsLength) + chromosomeVisualInfo[chromosomeName].visualBreakpoints.length * this.config.breakpoint.width;
            let startX = config.width / 2 - chromosomeWidthPx / 2;
            if (chromosomeVisualInfo[chromosomeName].leftPart !== undefined) {
                startX = offset + convertBpToPixels(maximumLeftLength - chromosomeVisualInfo[chromosomeName].leftPart);
            }
            for (let j = 0; j < chromosomeVisualInfo[chromosomeName].visualBreakpoints.length; j++) {
                const visualBreakpoint = chromosomeVisualInfo[chromosomeName].visualBreakpoints[j];
                if (j < chromosomeVisualInfo[chromosomeName].visualBreakpoints.length - 1) {
                    // left viewport
                    const viewportCanvasWidth = convertBpToPixels(visualBreakpoint.left.range.end - visualBreakpoint.left.range.start);
                    const viewport = registerViewport(`${chromosomeName}:${visualBreakpoint.left.gene.name}`, visualBreakpoint.left.range, startX);
                    startX += this.config.breakpoint.width + viewportCanvasWidth;
                    if (visualBreakpoint.dataBreakpoint) {
                        this._breakpointPositioning.set(visualBreakpoint.dataBreakpoint, {
                            start: viewport.canvas.end,
                            end: viewport.canvas.end + this.config.breakpoint.width
                        });
                    }
                }
                else {
                    // left & right viewports
                    const leftViewportCanvasWidth = convertBpToPixels(visualBreakpoint.left.range.end - visualBreakpoint.left.range.start);
                    const viewport = registerViewport(`${chromosomeName}:${visualBreakpoint.left.gene.name}`, visualBreakpoint.left.range, startX);
                    startX += this.config.breakpoint.width + leftViewportCanvasWidth;
                    if (visualBreakpoint.dataBreakpoint) {
                        this._breakpointPositioning.set(visualBreakpoint.dataBreakpoint, {
                            start: viewport.canvas.end,
                            end: viewport.canvas.end + this.config.breakpoint.width
                        });
                    }
                    const rightViewportCanvasWidth = convertBpToPixels(visualBreakpoint.right.range.end - visualBreakpoint.right.range.start);
                    registerViewport(`${chromosomeName}:${visualBreakpoint.right.gene.name}`, visualBreakpoint.right.range, startX);
                    startX += this.config.breakpoint.width + rightViewportCanvasWidth;
                }
            }
        }
        return [...geneTranscriptsZones, refZone];
    }

    _manageAlternativeViewports(config) {
        this._altChromosomeNames = [];
        this._altChromosomeVisualInfo = {};
        const altZone = {name: 'alternative', zones: []};
        const chromosomes = [];
        const chromosomeVisualInfo = {};
        let maximumItemsLength = 0;
        let maximumBreakpointsPerChromosome = 0;
        let maxChromosomeLabelSize = 0;
        for (let i = 0; i < this.variant.analysisResult.altInfos.length; i++) {
            const altInfo = this.variant.analysisResult.altInfos[i];
            if (altInfo.chromosome) {
                if (chromosomes.indexOf(altInfo.chromosome.name) === -1) {
                    chromosomes.push(altInfo.chromosome.name);
                    chromosomeVisualInfo[altInfo.chromosome.name] = {
                        length: 0,
                        infos: []
                    };
                }
                chromosomeVisualInfo[altInfo.chromosome.name].length += altInfo.totalExonsLength;
                chromosomeVisualInfo[altInfo.chromosome.name].infos.push(altInfo);

                if (maximumItemsLength < chromosomeVisualInfo[altInfo.chromosome.name].length) {
                    maximumItemsLength = chromosomeVisualInfo[altInfo.chromosome.name].length;
                }
                if (maximumBreakpointsPerChromosome < chromosomeVisualInfo[altInfo.chromosome.name].infos.length) {
                    maximumBreakpointsPerChromosome = chromosomeVisualInfo[altInfo.chromosome.name].infos.length;
                }
            }
            else {
                chromosomes.push(`mixed_chromosome_${i}`);
                chromosomeVisualInfo[`mixed_chromosome_${i}`] = {
                    length: altInfo.totalExonsLength,
                    infos: [altInfo],
                    isFake: true
                };
                if (maximumItemsLength < altInfo.totalExonsLength) {
                    maximumItemsLength = altInfo.totalExonsLength;
                }
            }
        }

        for (let i = 0; i < chromosomes.length; i++) {
            const name = this._getChromosomeDisplayName(chromosomes[i]);
            const size = PixiTextSize.getTextSize(name, this.config.chromosome.label);
            if (maxChromosomeLabelSize < size.width) {
                maxChromosomeLabelSize = size.width;
            }
        }

        const offset = this.config.reference.width + this.config.chromosome.margin + maxChromosomeLabelSize + this.config.breakpoint.width / 2;
        const usefulCanvasLength = config.width - maximumBreakpointsPerChromosome * this.config.breakpoint.width - 2 * offset;
        const factor = usefulCanvasLength / maximumItemsLength;

        const convertBpToPixels = (bp) => bp * factor;


        const registerViewport = (name, bpRange, canvasStart) => {
            const canvasParams = {
                start: canvasStart,
                end: canvasStart + convertBpToPixels(bpRange.end - bpRange.start)
            };
            const viewport = new BaseViewport({chromosome: bpRange, brush: bpRange, canvas: canvasParams});
            let viewports = this.viewports.get(name.toUpperCase());
            if (!viewports) {
                viewports = [];
            }
            viewports.push(viewport);
            this.viewports.set(name.toUpperCase(), viewports);
            return viewport;
        };

        for (let i = 0; i < chromosomes.length; i++) {
            const chromosomeName = chromosomes[i];
            altZone.zones.push({
                name: chromosomeName, zones: [
                    {name: 'gene'},
                    {
                        name: 'upper',
                        zones: [{name: 'edges'}]
                    },
                    {name: 'chromosome'},
                    {
                        name: 'down',
                        zones: [{name: 'edges'}]
                    }]
            });
            const chromosomeWidthPx = convertBpToPixels(chromosomeVisualInfo[chromosomeName].length) + (chromosomeVisualInfo[chromosomeName].infos.length - 1) * this.config.breakpoint.width;
            let startX = config.width / 2 - chromosomeWidthPx / 2;
            for (let j = 0; j < chromosomeVisualInfo[chromosomeName].infos.length; j++) {
                const info = chromosomeVisualInfo[chromosomeName].infos[j];
                const viewportCanvasWidth = convertBpToPixels(info.totalExonsLength);
                registerViewport(`alternative:${chromosomeName}:${j}`, {
                    start: 0,
                    end: info.totalExonsLength
                }, startX);
                startX += this.config.breakpoint.width + viewportCanvasWidth;
            }
        }
        this._altChromosomeNames = chromosomes;
        this._altChromosomeVisualInfo = chromosomeVisualInfo;
        return altZone;
    }

    _renderVariantEffect(config) {
        this._addInteractionArea(config);
        this._exonPositions = [];
        this._domainLegendPositions = [];
        for (let i = 0; i < this.variant.analysisResult.geneNames.length; i++) {
            const gene = this.variant.analysisResult.genes[this.variant.analysisResult.geneNames[i]].gene;
            const chromosome = this.variant.analysisResult.genes[this.variant.analysisResult.geneNames[i]].chromosome;
            this._renderGeneStructure(config, gene, chromosome);
        }
        this._renderBreakpointConnections();
        this._renderChromosomeNames(config);
        for (let i = 0; i < this.variant.analysisResult.altInfos.length; i++) {
            const altInfo = this.variant.analysisResult.altInfos[i];
            let chromosomeName = `mixed_chromosome_${i}`;
            if (altInfo.chromosome) {
                chromosomeName = altInfo.chromosome.name;
            }
            this._renderAltStructure(altInfo, i, chromosomeName);
        }
        this._renderLegends(config);
        this._renderReferenceLabel(this.container, config);
        this._renderAltLabel(this.container, config);
    }

    _renderReferenceLabel(container:PIXI.Container, config) {
        let style = this.config.reference.label;
        const label = new PIXI.Text('REF', style);
        label.resolution = drawingConfiguration.resolution;
        if (label.width + 5 > this.variantZonesManager.getHeight('reference')) {
            style = this.config.reference.smallLabel;
            label.style = style;
        }
        label.x = Math.round(this._visualAreaOffset / 2 - label.height / 2);
        label.y = Math.round(this.variantZonesManager.getCenter('reference') + label.width / 2);
        label.rotation = -Math.PI / 2;
        container.addChild(label);

        const labelRight = new PIXI.Text('REF', style);
        labelRight.resolution = drawingConfiguration.resolution;
        labelRight.x = Math.round(config.width - this._visualAreaOffset / 2 - labelRight.height / 2);
        labelRight.y = Math.round(this.variantZonesManager.getCenter('reference') + labelRight.width / 2);
        labelRight.rotation = -Math.PI / 2;
        container.addChild(labelRight);

        const graphics = new PIXI.Graphics();
        graphics.lineStyle(1, 0xcccccc, 1);

        const outlineLength = this.config.reference.outline;

        graphics
            .moveTo(this._visualAreaOffset / 2 + outlineLength - .5, this.variantZonesManager.getStartPosition('reference') - .5)
            .lineTo(this._visualAreaOffset / 2 - .5, this.variantZonesManager.getStartPosition('reference') - .5)
            .lineTo(this._visualAreaOffset / 2 - .5, this.variantZonesManager.getCenter('reference') - label.width / 2 - .5)
            .moveTo(this._visualAreaOffset / 2 - .5, this.variantZonesManager.getCenter('reference') + label.width / 2 - .5)
            .lineTo(this._visualAreaOffset / 2 - .5, this.variantZonesManager.getEndPosition('reference') - .5)
            .lineTo(this._visualAreaOffset / 2 + outlineLength - .5, this.variantZonesManager.getEndPosition('reference') - .5)

            .moveTo(config.width - this._visualAreaOffset / 2 - outlineLength - .5, this.variantZonesManager.getStartPosition('reference') - .5)
            .lineTo(config.width - this._visualAreaOffset / 2 - .5, this.variantZonesManager.getStartPosition('reference') - .5)
            .lineTo(config.width - this._visualAreaOffset / 2 - .5, this.variantZonesManager.getCenter('reference') - label.width / 2 - .5)
            .moveTo(config.width - this._visualAreaOffset / 2 - .5, this.variantZonesManager.getCenter('reference') + label.width / 2 - .5)
            .lineTo(config.width - this._visualAreaOffset / 2 - .5, this.variantZonesManager.getEndPosition('reference') - .5)
            .lineTo(config.width - this._visualAreaOffset / 2 - outlineLength - .5, this.variantZonesManager.getEndPosition('reference') - .5);
        container.addChild(graphics);
    }

    _renderAltLabel(container, config) {
        let style = this.config.reference.label;
        const label = new PIXI.Text('ALT', style);
        label.resolution = drawingConfiguration.resolution;
        if (label.width + 5 > this.variantZonesManager.getHeight('alternative')) {
            style = this.config.reference.smallLabel;
            label.style = style;
        }
        label.x = Math.round(this._visualAreaOffset / 2 - label.height / 2);
        label.y = Math.round(this.variantZonesManager.getCenter('alternative') + label.width / 2);
        label.rotation = -Math.PI / 2;
        container.addChild(label);

        const labelRight = new PIXI.Text('ALT', style);
        labelRight.resolution = drawingConfiguration.resolution;
        labelRight.x = Math.round(config.width - this._visualAreaOffset / 2 - labelRight.height / 2);
        labelRight.y = Math.round(this.variantZonesManager.getCenter('alternative') + labelRight.width / 2);
        labelRight.rotation = -Math.PI / 2;
        container.addChild(labelRight);

        const graphics = new PIXI.Graphics();
        graphics.lineStyle(1, 0xcccccc, 1);

        const outlineLength = this.config.reference.outline;

        graphics
            .moveTo(this._visualAreaOffset / 2 + outlineLength - .5, this.variantZonesManager.getStartPosition('alternative') - .5)
            .lineTo(this._visualAreaOffset / 2 - .5, this.variantZonesManager.getStartPosition('alternative') - .5)
            .lineTo(this._visualAreaOffset / 2 - .5, this.variantZonesManager.getCenter('alternative') - label.width / 2 - .5)
            .moveTo(this._visualAreaOffset / 2 - .5, this.variantZonesManager.getCenter('alternative') + label.width / 2 - .5)
            .lineTo(this._visualAreaOffset / 2 - .5, this.variantZonesManager.getEndPosition('alternative') - .5)
            .lineTo(this._visualAreaOffset / 2 + outlineLength - .5, this.variantZonesManager.getEndPosition('alternative') - .5)

            .moveTo(config.width - this._visualAreaOffset / 2 - outlineLength - .5, this.variantZonesManager.getStartPosition('alternative') - .5)
            .lineTo(config.width - this._visualAreaOffset / 2 - .5, this.variantZonesManager.getStartPosition('alternative') - .5)
            .lineTo(config.width - this._visualAreaOffset / 2 - .5, this.variantZonesManager.getCenter('alternative') - label.width / 2 - .5)
            .moveTo(config.width - this._visualAreaOffset / 2 - .5, this.variantZonesManager.getCenter('alternative') + label.width / 2 - .5)
            .lineTo(config.width - this._visualAreaOffset / 2 - .5, this.variantZonesManager.getEndPosition('alternative') - .5)
            .lineTo(config.width - this._visualAreaOffset / 2 - outlineLength - .5, this.variantZonesManager.getEndPosition('alternative') - .5);
        container.addChild(graphics);
    }

    _addInteractionArea(config) {
        const area = new PIXI.Graphics();
        area.beginFill(0xffffff, 0)
            .drawRect(0, 0, config.width, this.variantZonesManager.getTotalHeight())
            .endFill();
        area.interactive = true;
        area.on('mousemove', (event) => {
            if (event.target !== area) {
                return;
            }
            this._checkPosition(config, event.data.getLocalPosition(event.target.parent));
        })
            .on('mouseout', () => {
                this.displayTooltip(null, null);
            });
        this.container.addChild(area);
    }

    _checkPosition(config, position) {
        if (position.x >= 0 && position.x <= config.width && position.y >= 0 && position.y <= this.variantZonesManager.getTotalHeight()) {
            let hoveredExon = null;
            for (let i = 0; i < this._exonPositions.length; i++) {
                const testExon = this._exonPositions[i];
                if (testExon.boundaries.x1 <= position.x && testExon.boundaries.x2 >= position.x && testExon.boundaries.y1 <= position.y && testExon.boundaries.y2 >= position.y) {
                    hoveredExon = testExon.exon;
                    break;
                }
            }
            if (hoveredExon) {
                const content = [
                    {key: 'Gene', value: hoveredExon.geneName},
                    {key: 'Exon', value: `#${(hoveredExon.index + 1)}`}
                ];
                if (hoveredExon.id) {
                    content.push({key: 'Id', value: hoveredExon.id});
                }
                if (hoveredExon.domains && hoveredExon.domains.length > 0) {
                    if (hoveredExon.domains.length === 1) {
                        content.push({key: 'Domain', value: hoveredExon.domains[0].domain.name});
                    }
                    else {
                        const str = hoveredExon.domains.map(x => x.domain.name).join(', ');
                        content.push({key: 'Domains', value: str});
                    }
                }
                this.displayTooltip(position, content);
            }
            else {
                this.displayTooltip(position, null);
                let hoveredDomain = null;
                for (let i = 0; i < this._domainLegendPositions.length; i++) {
                    const testDomain = this._domainLegendPositions[i];
                    if (testDomain.boundaries.x1 <= position.x && testDomain.boundaries.x2 >= position.x && testDomain.boundaries.y1 <= position.y && testDomain.boundaries.y2 >= position.y) {
                        hoveredDomain = testDomain.domain;
                        break;
                    }
                }
                if (hoveredDomain !== this._hoveredDomain) {
                    this._hoveredDomain = hoveredDomain;
                    this._presentationChanged = true;
                    this._reRenderSceneFn();
                }
            }
        }
    }

    _registerExonPosition(exon, boundaries) {
        this._exonPositions.push({exon, boundaries});
    }

    _registerDomainLegendPosition(domain, boundaries) {
        this._domainLegendPositions.push({domain, boundaries});
    }

    _addExpandCollapseButton(position, container:PIXI.Container, expanded, onClick) {
        const graphics = new PIXI.Graphics();
        container.addChild(graphics);

        const render = (hovered) => {
            graphics.clear();
            graphics
                .beginFill(0xffffff, 1)
                .lineStyle(1, hovered ? 0x777777 : 0xcccccc, 1)
                .drawCircle(position.x, position.y, expandCollpaseButtonRadius)
                .endFill();
            if (!expanded) {
                graphics
                    .lineStyle(2, hovered ? 0x555555 : 0xaaaaaa, 1)
                    .moveTo(position.x - expandCollpaseButtonRadius / 2, position.y + expandCollpaseButtonRadius / 4)
                    .lineTo(position.x, position.y - expandCollpaseButtonRadius / 4)
                    .lineTo(position.x + expandCollpaseButtonRadius / 2, position.y + expandCollpaseButtonRadius / 4);
            }
            else {
                graphics
                    .lineStyle(2, hovered ? 0x555555 : 0xaaaaaa, 1)
                    .moveTo(position.x - expandCollpaseButtonRadius / 2, position.y - expandCollpaseButtonRadius / 4)
                    .lineTo(position.x, position.y + expandCollpaseButtonRadius / 4)
                    .lineTo(position.x + expandCollpaseButtonRadius / 2, position.y - expandCollpaseButtonRadius / 4);
            }
            this._updateSceneFn();
        };

        const shouldRerender = () => {
            this._dataChanged = true;
            this._reRenderSceneFn();
        };

        render(false);

        graphics.interactive = true;
        graphics.buttonMode = true;
        graphics
            .on('mouseout', () => {
                render(false);
            })
            .on('mouseover', () => {
                render(true);
            })
            .on('mousedown', () => {
                render(true);
                onClick();
                shouldRerender();
            });
    }

    _addRadioButton(position, container:PIXI.Container, selected, onClick) {
        const graphics = new PIXI.Graphics();
        container.addChild(graphics);

        const outerRadius = this.config.transcript.radio.radius;
        const innerRadius = this.config.transcript.radio.radius / 2.0;

        const color = selected ? 0x4285F4 : 0xcccccc;

        const render = (hovered) => {
            graphics.clear();
            graphics
                .beginFill(0xffffff, 1)
                .lineStyle(1, hovered ? ColorProcessor.darkenColor(color) : color, 1)
                .drawCircle(position.x, position.y, outerRadius)
                .endFill();
            if (selected) {
                graphics
                    .beginFill(hovered ? ColorProcessor.darkenColor(color) : color, 1)
                    .drawCircle(position.x, position.y, innerRadius)
                    .endFill();
            }
            this._updateSceneFn();
        };

        render(false);

        graphics.interactive = true;
        graphics.buttonMode = true;
        graphics
            .on('mouseout', () => {
                render(false);
            })
            .on('mouseover', () => {
                render(true);
            })
            .on('mousedown', () => {
                render(true);
                onClick();
            });
    }

    _renderLegends(config) {
        let totalDomainLegends = 0;
        for (const legend in this.variant.analysisResult.domainColors) {
            if (this.variant.analysisResult.domainColors.hasOwnProperty(legend)) {
                totalDomainLegends++;
            }
        }
        if (totalDomainLegends === 0)
            return;
        const localContainer = new PIXI.Container();
        this.container.addChild(localContainer);
        const graphics = new PIXI.Graphics();
        localContainer.addChild(graphics);
        const borderMargin = 5;
        const legendLabel = new PIXI.Text('DOMAINS', this.config.legend.mainLabel);
        legendLabel.resolution = drawingConfiguration.resolution;
        legendLabel.x = Math.round(this._mainOffset + borderMargin * 2);
        legendLabel.y = Math.round(this.variantZonesManager.getCenter('legendsLabel') - legendLabel.height / 2);
        localContainer.addChild(legendLabel);
        graphics.lineStyle(1, 0xcccccc, 1);
        graphics
            .moveTo(Math.round(legendLabel.x + legendLabel.width + borderMargin), Math.round(this.variantZonesManager.getCenter('legendsLabel')) - .5)
            .lineTo(Math.round(config.width - this._mainOffset), Math.round(this.variantZonesManager.getCenter('legendsLabel')) - .5)
            .moveTo(Math.round(this._mainOffset), Math.round(this.variantZonesManager.getCenter('legendsLabel')) - .5)
            .lineTo(Math.round(this._mainOffset + borderMargin), Math.round(this.variantZonesManager.getCenter('legendsLabel')) - .5);

        this._addExpandCollapseButton({
            x: config.width - this._mainOffset - expandCollpaseButtonRadius * 1.5,
            y: this.variantZonesManager.getCenter('legendsLabel')
        }, localContainer, this.variantZonesManager.isExpanded('legends'), () => {
            if (!this.variantZonesManager.isExpanded('legends')) {
                this.variantZonesManager.expand('legends');
                this._legendExpanded = true;
            }
            else {
                this.variantZonesManager.collapse('legends');
                this._legendExpanded = false;
            }
        });
        if (!this.variantZonesManager.isExpanded('legends')) {
            return;
        }
        graphics
            .moveTo(Math.round(this._mainOffset) - .5, Math.round(this.variantZonesManager.getCenter('legendsLabel')) - .5)
            .lineTo(Math.round(this._mainOffset) - .5, Math.round(this.variantZonesManager.getEndPosition('legends')) - .5)
            .lineTo(Math.round(config.width - this._mainOffset) - .5, Math.round(this.variantZonesManager.getEndPosition('legends')) - .5)
            .lineTo(Math.round(config.width - this._mainOffset) - .5, Math.round(this.variantZonesManager.getCenter('legendsLabel')) - .5);
        graphics.lineStyle(0, 0xffffff, 0);
        for (const legend in this.variant.analysisResult.domainColors) {
            if (this.variant.analysisResult.domainColors.hasOwnProperty(legend)) {
                const color = this.domainColorsManager.getDomainColor(legend);
                graphics.beginFill(color.fill, color.alpha);
                const y = this.variantZonesManager.getCenter('legends', legend);
                graphics.drawRect(
                    this._mainOffset + this.config.breakpoint.width / 2,
                    y - this.config.legend.bar.height / 2,
                    this.config.legend.bar.width,
                    this.config.legend.bar.height);
                graphics.endFill();
                const label = new PIXI.Text(legend, this.config.legend.label);
                label.resolution = drawingConfiguration.resolution;
                label.x = Math.round(this._mainOffset + this.config.breakpoint.width / 2 + this.config.legend.bar.width * 1.5);
                label.y = Math.round(y - label.height / 2);
                localContainer.addChild(label);
                this._registerDomainLegendPosition(legend, {
                    x1: this._mainOffset + this.config.breakpoint.width / 2,
                    y1: y - this.config.legend.bar.height / 2,
                    x2: label.x + label.width,
                    y2: y + this.config.legend.bar.height / 2
                });
            }
        }
    }

    _renderGeneStructure(config, gene, chromosome) {
        const viewports = this.viewports.get(`${chromosome.name.toUpperCase()}:${gene.name.toUpperCase()}`);
        if (viewports && viewports.length > 0) {
            for (let i = 0; i < viewports.length; i++) {
                if (i === 0 && !gene.empty) {
                    const label = new PIXI.Text(gene.name, this.config.gene.label);
                    label.resolution = drawingConfiguration.resolution;
                    label.x = Math.round(viewports[i].canvas.start);
                    label.y = Math.round(this.variantZonesManager.getEndPosition('reference', chromosome.name.toUpperCase(), 'gene') - label.height);
                    this.container.addChild(label);
                }
                this._renderExonsStructure(viewports[i], chromosome.name, gene);
            }
        }
        this._renderTranscriptsStructure(config, gene);
    }

    _renderAltStructure(altInfo, index, chromosomeName) {
        const viewports = this.viewports.get(`ALTERNATIVE:${chromosomeName.toUpperCase()}:${index}`);
        if (viewports && viewports.length > 0) {
            for (let i = 0; i < viewports.length; i++) {
                this._renderExonsStructure(viewports[i], chromosomeName, altInfo, 'alternative', true);
            }
        }
    }

    _renderEmptyGeneStructure(viewport:BaseViewport, chromosomeName, zone = 'reference') {
        chromosomeName = chromosomeName.toUpperCase();
        const localContainer = new PIXI.Container();
        this.container.addChild(localContainer);
        const graphics = new PIXI.Graphics();
        localContainer.addChild(graphics);
        const labelContainer = new PIXI.Container();
        this.container.addChild(labelContainer);

        const geneBorder = {
            leftBorder: viewport.chromosome.start === 0,
            rightBorder: viewport.chromosome.end === emptyGeneSize
        };
        const height = this.variantZonesManager.getHeight(zone, chromosomeName, 'chromosome') - 2 * this.config.chromosome.margin;
        const center = this.variantZonesManager.getStartPosition(zone, chromosomeName, 'chromosome') + this.variantZonesManager.getHeight(zone, chromosomeName, 'chromosome') / 2;
        const clippingRegion = {
            x: viewport.canvas.start - 1,
            y: this.variantZonesManager.getStartPosition(zone, chromosomeName, 'chromosome'),
            width: viewport.canvasSize + 2,
            height: this.variantZonesManager.getHeight(zone, chromosomeName, 'chromosome')
        };
        const clippingRules = {
            leftClip: false,
            rightClip: false
        };
        const chromosomeLineRect = {
            x1: viewport.project.brushBP2pixel(viewport.brush.start) - this.config.breakpoint.width / 2,
            y1: center - height / 4,
            x2: viewport.project.brushBP2pixel(viewport.brush.end) + this.config.breakpoint.width / 2,
            y2: center + height / 4
        };
        graphics.beginFill(0xaaaaaa, 1);
        graphics.drawRect(
            chromosomeLineRect.x1,
            chromosomeLineRect.y1,
            chromosomeLineRect.x2 - chromosomeLineRect.x1,
            chromosomeLineRect.y2 - chromosomeLineRect.y1
        );
        graphics.endFill();
        if (geneBorder.leftBorder && geneBorder.rightBorder) {
            clippingRules.leftClip = true;
            clippingRules.rightClip = true;
            clippingRegion.x = viewport.canvas.start - this.config.breakpoint.width / 3;
            clippingRegion.width = viewport.canvasSize + 2 * this.config.breakpoint.width / 3;
        }
        else if (geneBorder.leftBorder) {
            clippingRules.leftClip = true;
            clippingRegion.x = viewport.canvas.start - this.config.breakpoint.width / 3;
            clippingRegion.width = viewport.canvasSize + this.config.breakpoint.width / 3;
        }
        else if (geneBorder.rightBorder) {
            clippingRules.rightClip = true;
            clippingRegion.x = viewport.canvas.start;
            clippingRegion.width = viewport.canvasSize + this.config.breakpoint.width / 3;
        }
        this.featureCutOffRenderer
            .beginCuttingOff(localContainer)
            .cutOff(clippingRegion, clippingRules)
            .endCuttingOff();
    }

    _renderExonsStructure(viewport:BaseViewport, chromosomeName, gene, zone = 'reference', renderGeneLabels = false) {
        if (!viewport || !chromosomeName || !gene)
            return;
        chromosomeName = chromosomeName.toUpperCase();
        if (gene.empty) {
            this._renderEmptyGeneStructure(viewport, chromosomeName, zone);
            return;
        }
        const localContainer = new PIXI.Container();
        const notMaskedGraphics = new PIXI.Graphics();
        this.container.addChild(localContainer);
        this.container.addChild(notMaskedGraphics);
        const graphics = new PIXI.Graphics();
        localContainer.addChild(graphics);
        const labelContainer = new PIXI.Container();
        this.container.addChild(labelContainer);

        const geneBorder = {
            leftBorder: viewport.chromosome.start === 0,
            rightBorder: viewport.chromosome.end === gene.totalExonsLength
        };
        const height = this.variantZonesManager.getHeight(zone, chromosomeName, 'chromosome') - 2 * this.config.chromosome.margin;
        const center = this.variantZonesManager.getStartPosition(zone, chromosomeName, 'chromosome') + this.variantZonesManager.getHeight(zone, chromosomeName, 'chromosome') / 2;
        let chromosomeLineRect = null;
        const clippingRegion = {
            x: Math.round(viewport.canvas.start) - 1,
            y: Math.round(this.variantZonesManager.getStartPosition(zone, chromosomeName, 'chromosome')) - 1,
            width: Math.round(viewport.canvasSize) + 2,
            height: Math.round(this.variantZonesManager.getHeight(zone, chromosomeName, 'chromosome')) + 2
        };
        const clippingRules = {
            leftClip: false,
            rightClip: false
        };
        if (geneBorder.leftBorder && geneBorder.rightBorder) {
            clippingRules.leftClip = true;
            clippingRules.rightClip = true;
            clippingRegion.x = Math.round(viewport.canvas.start - this.config.breakpoint.width / 3) - 1;
            clippingRegion.width = Math.round(viewport.canvasSize + 2 * this.config.breakpoint.width / 3) + 2;
            chromosomeLineRect = {
                x1: viewport.canvas.start - this.config.breakpoint.width / 2,
                y1: center - height / 4,
                x2: viewport.canvas.start,
                y2: center + height / 4
            };
            graphics.beginFill(0xaaaaaa, 1);
            graphics.drawRect(
                Math.round(chromosomeLineRect.x1) - .5,
                Math.round(chromosomeLineRect.y1) - .5,
                Math.round(chromosomeLineRect.x2 - chromosomeLineRect.x1),
                Math.round(chromosomeLineRect.y2 - chromosomeLineRect.y1)
            );
            graphics.endFill();
            chromosomeLineRect = {
                x1: viewport.canvas.end,
                y1: center - height / 4,
                x2: viewport.canvas.end + this.config.breakpoint.width / 2,
                y2: center + height / 4
            };
            graphics.beginFill(0xaaaaaa, 1);
            graphics.drawRect(
                Math.round(chromosomeLineRect.x1) - .5,
                Math.round(chromosomeLineRect.y1) - .5,
                Math.round(chromosomeLineRect.x2 - chromosomeLineRect.x1),
                Math.round(chromosomeLineRect.y2 - chromosomeLineRect.y1)
            );
            graphics.endFill();
        }
        else if (geneBorder.leftBorder) {
            clippingRules.leftClip = true;
            clippingRegion.x = Math.round(viewport.canvas.start - this.config.breakpoint.width / 3) - 1;
            clippingRegion.width = Math.round(viewport.canvasSize + this.config.breakpoint.width / 3) + 2;
            chromosomeLineRect = {
                x1: viewport.canvas.start - this.config.breakpoint.width / 2,
                y1: center - height / 4,
                x2: viewport.canvas.start,
                y2: center + height / 4
            };
            graphics.beginFill(0xaaaaaa, 1);
            graphics.drawRect(
                Math.round(chromosomeLineRect.x1) - .5,
                Math.round(chromosomeLineRect.y1) - .5,
                Math.round(chromosomeLineRect.x2 - chromosomeLineRect.x1),
                Math.round(chromosomeLineRect.y2 - chromosomeLineRect.y1)
            );
            graphics.endFill();
        }
        else if (geneBorder.rightBorder) {
            clippingRules.rightClip = true;
            clippingRegion.x = Math.round(viewport.canvas.start) - 1;
            clippingRegion.width = Math.round(viewport.canvasSize + this.config.breakpoint.width / 3) + 2;
            chromosomeLineRect = {
                x1: viewport.canvas.end,
                y1: center - height / 4,
                x2: viewport.canvas.end + this.config.breakpoint.width / 2,
                y2: center + height / 4
            };
            graphics.beginFill(0xaaaaaa, 1);
            graphics.drawRect(
                Math.round(chromosomeLineRect.x1) - .5,
                Math.round(chromosomeLineRect.y1) - .5,
                Math.round(chromosomeLineRect.x2 - chromosomeLineRect.x1),
                Math.round(chromosomeLineRect.y2 - chromosomeLineRect.y1)
            );
            graphics.endFill();
        }
        let prevGeneName = null;
        for (let i = 0; i < gene.consensusExons.length; i++) {
            const exon = gene.consensusExons[i];
            if (exon.relativePosition.end < viewport.chromosome.start || exon.relativePosition.start > viewport.chromosome.end)
                continue;
            let alphaRatio = 1;
            if (this._hoveredDomain) {
                alphaRatio = .25;
                if (exon.domains && exon.domains.length > 0) {
                    for (let d = 0; d < exon.domains.length; d++) {
                        if (exon.domains[d].domain.name === this._hoveredDomain) {
                            alphaRatio = 1;
                            break;
                        }
                    }
                }
            }
            const rect = {
                x1: Math.max(viewport.project.brushBP2pixel(exon.relativePosition.start), viewport.canvas.start),
                y1: this.variantZonesManager.getStartPosition(zone, chromosomeName, 'chromosome') + this.config.chromosome.margin,
                x2: Math.min(viewport.project.brushBP2pixel(exon.relativePosition.end), viewport.canvas.end),
                y2: this.variantZonesManager.getEndPosition(zone, chromosomeName, 'chromosome') - this.config.chromosome.margin
            };
            if (exon.empty) {
                const emptyExonRect = {
                    x1: Math.max(viewport.project.brushBP2pixel(exon.relativePosition.start), viewport.canvas.start),
                    y1: center - height / 4,
                    x2: Math.min(viewport.project.brushBP2pixel(exon.relativePosition.end), viewport.canvas.end),
                    y2: center + height / 4
                };
                graphics.lineStyle(0, 0xaaaaaa, 0);
                graphics.beginFill(0xaaaaaa, 1);
                graphics.drawRect(
                    emptyExonRect.x1,
                    emptyExonRect.y1,
                    emptyExonRect.x2 - emptyExonRect.x1,
                    emptyExonRect.y2 - emptyExonRect.y1
                );
                graphics.endFill();
                continue;
            }
            if (renderGeneLabels && !exon.empty && (!prevGeneName || prevGeneName !== exon.geneName)) {
                const geneLabel = new PIXI.Text(exon.geneName, this.config.gene.label);
                geneLabel.resolution = drawingConfiguration.resolution;
                geneLabel.x = Math.round(Math.max(viewport.project.brushBP2pixel(exon.relativePosition.start), viewport.canvas.start));
                geneLabel.y = Math.round(this.variantZonesManager.getEndPosition(zone, chromosomeName, 'gene') - geneLabel.height);
                labelContainer.addChild(geneLabel);
                prevGeneName = exon.geneName;
            }
            this._registerExonPosition(exon, rect);
            const breakpointExonOffset = this.config.breakpoint.offset;
            const breakpointThickness = this.config.breakpoint.thickness;
            if (exon.domains && exon.domains.length > 0) {
                this.domainColorsManager.fillEmptyExon(exon.geneName, graphics, {
                    x: rect.x1,
                    y: rect.y1,
                    width: rect.x2 - rect.x1,
                    height: rect.y2 - rect.y1
                }, alphaRatio);
                for (let j = 0; j < exon.domains.length; j++) {
                    const exonDomain = exon.domains[j];
                    let domainAlphaRatio = 1;
                    if (this._hoveredDomain && exonDomain.domain.name !== this._hoveredDomain) {
                        domainAlphaRatio = .25;
                    }
                    const domainColor = this.domainColorsManager.getDomainColor(exonDomain.domain.name);
                    graphics.beginFill(domainColor.fill, domainColor.alpha * domainAlphaRatio);
                    graphics.lineStyle(0, 0x000000, 0);
                    const innerRect = {
                        x1: Math.max(viewport.project.brushBP2pixel(exonDomain.range.start), viewport.canvas.start),
                        y1: this.variantZonesManager.getStartPosition(zone, chromosomeName, 'chromosome') + this.config.chromosome.margin + .5,
                        x2: Math.min(viewport.project.brushBP2pixel(exonDomain.range.end), viewport.canvas.end),
                        y2: this.variantZonesManager.getEndPosition(zone, chromosomeName, 'chromosome') - this.config.chromosome.margin - .5
                    };
                    graphics.drawRect(innerRect.x1, innerRect.y1, innerRect.x2 - innerRect.x1, innerRect.y2 - innerRect.y1);
                    graphics.endFill();
                }
                graphics.beginFill(0xffffff, 0);
                graphics.lineStyle(1, 0x000000, alphaRatio);
                graphics.drawRect(Math.round(rect.x1) - .5,
                    Math.round(rect.y1) - .5,
                    Math.round(rect.x2 - rect.x1),
                    Math.round(rect.y2 - rect.y1));
                graphics.endFill();
                if (exon.isBreakpoint && this._options && this._options.highlightBreakpoints) {
                    switch (exon.breakpointPosition) {
                        case 'start':
                            notMaskedGraphics.lineStyle(breakpointThickness, this.config.breakpoint.color, 1);
                            notMaskedGraphics
                                .moveTo(Math.round(rect.x1 - breakpointExonOffset / 2) - breakpointThickness / 2, Math.round(rect.y1 - breakpointExonOffset) - breakpointThickness / 2)
                                .lineTo(Math.round(rect.x1) - breakpointThickness / 2, Math.round(rect.y1 - breakpointExonOffset) - breakpointThickness / 2)
                                .lineTo(Math.round(rect.x1) - breakpointThickness / 2, Math.round(rect.y2 + breakpointExonOffset) - breakpointThickness / 2)
                                .lineTo(Math.round(rect.x1 - breakpointExonOffset / 2) - breakpointThickness / 2, Math.round(rect.y2 + breakpointExonOffset) - breakpointThickness / 2);
                            break;
                        case 'end':
                            notMaskedGraphics.lineStyle(breakpointThickness, this.config.breakpoint.color, 1);
                            notMaskedGraphics
                                .moveTo(Math.round(rect.x2 + breakpointExonOffset / 2) - breakpointThickness / 2, Math.round(rect.y1 - breakpointExonOffset) - breakpointThickness / 2)
                                .lineTo(Math.round(rect.x2) - breakpointThickness / 2, Math.round(rect.y1 - breakpointExonOffset) - breakpointThickness / 2)
                                .lineTo(Math.round(rect.x2) - breakpointThickness / 2, Math.round(rect.y2 + breakpointExonOffset) - breakpointThickness / 2)
                                .lineTo(Math.round(rect.x2 + breakpointExonOffset / 2) - breakpointThickness / 2, Math.round(rect.y2 + breakpointExonOffset) - breakpointThickness / 2);
                            break;
                    }
                }
            }
            else {
                const rect = {
                    x1: Math.max(viewport.project.brushBP2pixel(exon.relativePosition.start), viewport.canvas.start),
                    y1: this.variantZonesManager.getStartPosition(zone, chromosomeName, 'chromosome') + this.config.chromosome.margin,
                    x2: Math.min(viewport.project.brushBP2pixel(exon.relativePosition.end), viewport.canvas.end),
                    y2: this.variantZonesManager.getEndPosition(zone, chromosomeName, 'chromosome') - this.config.chromosome.margin
                };
                this.domainColorsManager.fillEmptyExon(exon.geneName, graphics, {
                    x: rect.x1,
                    y: rect.y1,
                    width: rect.x2 - rect.x1,
                    height: rect.y2 - rect.y1
                }, alphaRatio);
                graphics.lineStyle(1, 0x000000, alphaRatio);
                graphics.drawRect(rect.x1 - .5, rect.y1 - .5, rect.x2 - rect.x1, rect.y2 - rect.y1);
                if (exon.isBreakpoint && this._options && this._options.highlightBreakpoints) {
                    switch (exon.breakpointPosition) {
                        case 'start':
                            notMaskedGraphics.lineStyle(breakpointThickness, this.config.breakpoint.color, 1);
                            notMaskedGraphics
                                .moveTo(Math.round(rect.x1 - breakpointExonOffset / 2) - breakpointThickness / 2, Math.round(rect.y1 - breakpointExonOffset) - breakpointThickness / 2)
                                .lineTo(Math.round(rect.x1) - breakpointThickness / 2, Math.round(rect.y1 - breakpointExonOffset) - breakpointThickness / 2)
                                .lineTo(Math.round(rect.x1) - breakpointThickness / 2, Math.round(rect.y2 + breakpointExonOffset) - breakpointThickness / 2)
                                .lineTo(Math.round(rect.x1 - breakpointExonOffset / 2) - breakpointThickness / 2, Math.round(rect.y2 + breakpointExonOffset) - breakpointThickness / 2);
                            break;
                        case 'end':
                            notMaskedGraphics.lineStyle(breakpointThickness, this.config.breakpoint.color, 1);
                            notMaskedGraphics
                                .moveTo(Math.round(rect.x2 + breakpointExonOffset / 2) - breakpointThickness / 2, Math.round(rect.y1 - breakpointExonOffset) - breakpointThickness / 2)
                                .lineTo(Math.round(rect.x2) - breakpointThickness / 2, Math.round(rect.y1 - breakpointExonOffset) - breakpointThickness / 2)
                                .lineTo(Math.round(rect.x2) - breakpointThickness / 2, Math.round(rect.y2 + breakpointExonOffset) - breakpointThickness / 2)
                                .lineTo(Math.round(rect.x2 + breakpointExonOffset / 2) - breakpointThickness / 2, Math.round(rect.y2 + breakpointExonOffset) - breakpointThickness / 2);
                            break;
                    }
                }
            }
            if (exon.strand !== undefined && exon.strand !== null) {
                this.strandDirectionRenderer.renderStrandDirectionForFeature(exon.strand ? 'positive' : 'negative', {
                    xStart: rect.x1,
                    xEnd: rect.x2,
                    yStart: rect.y2 - (rect.y2 - rect.y1) / 2,
                    yEnd: rect.y2
                }, localContainer, alphaRatio);
            }
            if (exon.index !== null && exon.index !== undefined) {
                const exonLabel = new PIXI.Text(exon.index + 1, this.config.exon.label);
                exonLabel.resolution = 2;
                exonLabel.alpha = alphaRatio;
                if (exonLabel.width < (rect.x2 - rect.x1)) {
                    exonLabel.x = Math.round((rect.x1 + rect.x2) / 2 - exonLabel.width / 2);
                    exonLabel.y = Math.round(rect.y1 + (rect.y2 - rect.y1) / 4 - exonLabel.height / 2);
                    localContainer.addChild(exonLabel);
                }
            }
        }
        this.featureCutOffRenderer
            .beginCuttingOff(localContainer)
            .cutOff(clippingRegion, clippingRules)
            .endCuttingOff();
    }

    _renderTranscriptsBlock(config, gene) {
        if (gene.selectedTranscript.empty) {
            return;
        }
        const localContainer = new PIXI.Container();
        const graphics = new PIXI.Graphics();
        localContainer.addChild(graphics);
        this.container.addChild(localContainer);

        const zoneNames = [`${gene.name} transcripts`];

        const borderMargin = 5;
        const transcriptLegendLabel = new PIXI.Text(`${gene.name.toUpperCase()} TRANSCRIPTS : ${gene.selectedTranscript.name.toUpperCase()} selected`, this.config.transcript.mainLabel);
        transcriptLegendLabel.resolution = drawingConfiguration.resolution;
        transcriptLegendLabel.x = Math.round(this._mainOffset + borderMargin * 2);
        transcriptLegendLabel.y = Math.round(this.variantZonesManager.getCenter(...zoneNames, 'transcriptLabel') - transcriptLegendLabel.height / 2);
        localContainer.addChild(transcriptLegendLabel);
        graphics.lineStyle(1, 0xcccccc, 1);
        graphics
            .moveTo(Math.round(transcriptLegendLabel.x + transcriptLegendLabel.width + borderMargin) - .5, Math.round(this.variantZonesManager.getCenter(...zoneNames, 'transcriptLabel')) - .5)
            .lineTo(Math.round(config.width - this._mainOffset) - .5, Math.round(this.variantZonesManager.getCenter(...zoneNames, 'transcriptLabel')) - .5)
            .moveTo(Math.round(this._mainOffset) - .5, Math.round(this.variantZonesManager.getCenter(...zoneNames, 'transcriptLabel')) - .5)
            .lineTo(Math.round(this._mainOffset + borderMargin) - .5, Math.round(this.variantZonesManager.getCenter(...zoneNames, 'transcriptLabel')) - .5);

        this._addExpandCollapseButton({
            x: config.width - this._mainOffset - expandCollpaseButtonRadius * 1.5,
            y: this.variantZonesManager.getCenter(...zoneNames, 'transcriptLabel')
        }, localContainer, this.variantZonesManager.isExpanded(...zoneNames, 'transcriptsZone'), () => {
            if (!this.variantZonesManager.isExpanded(...zoneNames, 'transcriptsZone')) {
                this.variantZonesManager.expand(...zoneNames, 'transcriptsZone');
                this._transcriptExpandedStatus[gene.name.toUpperCase()] = true; // todo
            }
            else {
                this.variantZonesManager.collapse(...zoneNames, 'transcriptsZone');
                this._transcriptExpandedStatus[gene.name.toUpperCase()] = false; // todo
            }
        });

        if (!this.variantZonesManager.isExpanded(...zoneNames, 'transcriptsZone')) {
            return;
        }

        graphics
            .moveTo(Math.round(this._mainOffset) - .5, Math.round(this.variantZonesManager.getCenter(...zoneNames, 'transcriptLabel')) - .5)
            .lineTo(Math.round(this._mainOffset) - .5, Math.round(this.variantZonesManager.getEndPosition(...zoneNames, 'transcriptsZone')) - .5)
            .lineTo(Math.round(config.width - this._mainOffset) - .5, Math.round(this.variantZonesManager.getEndPosition(...zoneNames, 'transcriptsZone')) - .5)
            .lineTo(Math.round(config.width - this._mainOffset) - .5, Math.round(this.variantZonesManager.getCenter(...zoneNames, 'transcriptLabel')) - .5);
        graphics.lineStyle(0, 0xffffff, 0);
    }

    _renderTranscriptsStructure(config, gene) {
        if (!gene) {
            return;
        }
        const localContainer = new PIXI.Container();
        const graphics = new PIXI.Graphics();
        localContainer.addChild(graphics);
        this.container.addChild(localContainer);

        this._renderTranscriptsBlock(config, gene);

        if (gene.empty || !gene.transcripts || !this.variantZonesManager.isExpanded(`${gene.name} transcripts`, 'transcriptsZone')) {
            return;
        }

        for (let t = 0; t < gene.transcripts.length; t++) {
            const transcript = gene.transcripts[t];
            const name = `${gene.name}_transcript_${t}`.toUpperCase();
            const viewport = this.viewports.get(name)[0];

            const radioPositionX = this._mainOffset + 5 + this.config.transcript.radio.margin;
            const radioPositionY = this.variantZonesManager.getCenter(`${gene.name} transcripts`, 'transcriptsZone',
                    `${gene.name}_transcript_${t}`, 'transcript');

            this._addRadioButton({
                x: radioPositionX, y: radioPositionY
            }, localContainer, transcript.name === gene.selectedTranscript.name, () => {
                if (this._affectedGeneTranscriptChanged) {
                    this._affectedGeneTranscriptChanged(transcript);
                }
            });

            for (let i = 0; i < transcript.canonicalCds.length; i++) {
                const exon = transcript.canonicalCds[i];
                if (exon.positionFromStart.end <= viewport.chromosome.start || exon.positionFromStart.start > viewport.chromosome.end)
                    continue;
                let alphaRatio = 1;
                if (this._hoveredDomain) {
                    alphaRatio = .25;
                    if (exon.domains && exon.domains.length > 0) {
                        for (let d = 0; d < exon.domains.length; d++) {
                            if (exon.domains[d].domain.name === this._hoveredDomain) {
                                alphaRatio = 1;
                                break;
                            }
                        }
                    }
                }
                let rect = {
                    x1: Math.max(viewport.project.brushBP2pixel(exon.positionFromStart.start), viewport.canvas.start),
                    y1: this.variantZonesManager.getStartPosition(`${gene.name} transcripts`, 'transcriptsZone',
                        `${gene.name}_transcript_${t}`, 'transcript') + this.config.transcript.margin,
                    x2: Math.min(viewport.project.brushBP2pixel(exon.positionFromStart.end), viewport.canvas.end),
                    y2: this.variantZonesManager.getEndPosition(`${gene.name} transcripts`, 'transcriptsZone',
                        `${gene.name}_transcript_${t}`, 'transcript') - this.config.transcript.margin
                };
                this._registerExonPosition(exon, rect);
                if (i === 0) {
                    const transcriptLabel = new PIXI.Text(transcript.name, this.config.transcriptName.label);
                    transcriptLabel.resolution = drawingConfiguration.resolution;
                    transcriptLabel.x = Math.round(rect.x1);
                    transcriptLabel.y = Math.round(this.variantZonesManager.getCenter(`${gene.name} transcripts`, 'transcriptsZone',
                            `${gene.name}_transcript_${t}`, 'transcriptName') - transcriptLabel.height / 2);
                    localContainer.addChild(transcriptLabel);
                }
                if (exon.domains && exon.domains.length > 0) {
                    this.domainColorsManager.fillEmptyExon(exon.geneName, graphics, {
                        x: rect.x1,
                        y: rect.y1,
                        width: rect.x2 - rect.x1,
                        height: rect.y2 - rect.y1
                    }, alphaRatio);
                    graphics.lineStyle(1, 0x000000, alphaRatio);
                    graphics.drawRect(Math.round(rect.x1) - .5,
                        Math.round(rect.y1) - .5,
                        Math.round(rect.x2 - rect.x1),
                        Math.round(rect.y2 - rect.y1));
                    for (let j = 0; j < exon.domains.length; j++) {
                        const exonDomain = exon.domains[j];
                        const x1 = Math.max(viewport.project.brushBP2pixel(exonDomain.rangeFromStart.start), viewport.canvas.start);
                        const x2 = Math.min(viewport.project.brushBP2pixel(exonDomain.rangeFromStart.end), viewport.canvas.end);
                        if (x1 >= x2) {
                            continue;
                        }
                        let domainAlphaRatio = 1;
                        if (this._hoveredDomain && exonDomain.domain.name !== this._hoveredDomain) {
                            domainAlphaRatio = .25;
                        }
                        const domainColor = this.domainColorsManager.getDomainColor(exonDomain.domain.name);
                        graphics.beginFill(domainColor.fill, domainColor.alpha * domainAlphaRatio);
                        graphics.lineStyle(0, 0x000000, 0);
                        const innerRect = {
                            x1: Math.max(viewport.project.brushBP2pixel(exonDomain.rangeFromStart.start), viewport.canvas.start),
                            y1: this.variantZonesManager.getStartPosition(`${gene.name} transcripts`, 'transcriptsZone',
                                `${gene.name}_transcript_${t}`, 'transcript') + this.config.transcript.margin + .5,
                            x2: Math.min(viewport.project.brushBP2pixel(exonDomain.rangeFromStart.end), viewport.canvas.end),
                            y2: this.variantZonesManager.getEndPosition(`${gene.name} transcripts`, 'transcriptsZone',
                                `${gene.name}_transcript_${t}`, 'transcript') - this.config.transcript.margin - .5
                        };
                        graphics.drawRect(innerRect.x1, innerRect.y1, innerRect.x2 - innerRect.x1, innerRect.y2 - innerRect.y1);
                        graphics.endFill();
                    }
                }
                else {
                    rect = {
                        x1: Math.max(viewport.project.brushBP2pixel(exon.positionFromStart.start), viewport.canvas.start),
                        y1: this.variantZonesManager.getStartPosition(`${gene.name} transcripts`, 'transcriptsZone',
                            `${gene.name}_transcript_${t}`, 'transcript') + this.config.transcript.margin,
                        x2: Math.min(viewport.project.brushBP2pixel(exon.positionFromStart.end), viewport.canvas.end),
                        y2: this.variantZonesManager.getEndPosition(`${gene.name} transcripts`, 'transcriptsZone',
                            `${gene.name}_transcript_${t}`, 'transcript') - this.config.transcript.margin
                    };
                    this.domainColorsManager.fillEmptyExon(exon.geneName, graphics, {
                        x: rect.x1,
                        y: rect.y1,
                        width: rect.x2 - rect.x1,
                        height: rect.y2 - rect.y1
                    }, alphaRatio);
                    graphics.lineStyle(1, 0x000000, alphaRatio);
                    graphics.drawRect(Math.round(rect.x1) - .5,
                        Math.round(rect.y1) - .5,
                        Math.round(rect.x2 - rect.x1),
                        Math.round(rect.y2 - rect.y1));
                }
                if (exon.index !== null && exon.index !== undefined) {
                    const exonLabel = new PIXI.Text(exon.index + 1, this.config.transcript.label);
                    exonLabel.resolution = 2;
                    exonLabel.alpha = alphaRatio;
                    if (exonLabel.width < (rect.x2 - rect.x1)) {
                        exonLabel.x = Math.round((rect.x1 + rect.x2) / 2 - exonLabel.width / 2);
                        exonLabel.y = Math.round((rect.y2 + rect.y1) / 2 - exonLabel.height / 2);
                        localContainer.addChild(exonLabel);
                    }
                }
            }
        }
    }

    _renderBreakpointConnections() {
        const localContainer = new PIXI.Container();
        this.container.addChild(localContainer);
        const graphics = new PIXI.Graphics();
        localContainer.addChild(graphics);
        for (let i = 0; i < this.variant.analysisResult.variantConnections.length; i++) {
            const variantConnection = this.variant.analysisResult.variantConnections[i];
            const startBreakpoint = this.variant.analysisResult.breakpoints[variantConnection.start.breakpointIndex];
            const endBreakpoint = this.variant.analysisResult.breakpoints[variantConnection.end.breakpointIndex];
            this._renderBreakpointConnection(startBreakpoint, variantConnection.start.attachedAtRight, endBreakpoint, variantConnection.end.attachedAtRight, i % 2, graphics);
        }
    }

    _renderBreakpointConnection(start, startAtRight, end, endAtRight, drawAtUpperLevel, graphics:PIXI.Graphics) {
        if (!start || !end)
            return;
        const startBreakpointXRange = this._breakpointPositioning.get(start);
        const endBreakpointXRange = this._breakpointPositioning.get(end);
        if (!startBreakpointXRange || !endBreakpointXRange)
            return;

        const startPosition = {
            x1: startBreakpointXRange.start + (startBreakpointXRange.end - startBreakpointXRange.start) * (startAtRight ? 0 : 1),
            x2: startBreakpointXRange.start + (startBreakpointXRange.end - startBreakpointXRange.start) * (startAtRight ? .33 : .66),
            y: this.variantZonesManager.getCenter('reference', start.chromosome.name.toUpperCase(), 'chromosome')
        };
        const endPosition = {
            x1: endBreakpointXRange.start + (endBreakpointXRange.end - endBreakpointXRange.start) * (endAtRight ? 0 : 1),
            x2: endBreakpointXRange.start + (endBreakpointXRange.end - endBreakpointXRange.start) * (endAtRight ? .33 : .66),
            y: this.variantZonesManager.getCenter('reference', end.chromosome.name.toUpperCase(), 'chromosome')
        };
        const controlPoints = [];
        controlPoints.push({x: startPosition.x1, y: startPosition.y});
        controlPoints.push({x: startPosition.x2, y: startPosition.y});
        if (start.chromosome.name === end.chromosome.name) {
            let y = this.variantZonesManager.getCenter('reference', start.chromosome.name.toUpperCase(), 'down', 'edges');
            if (drawAtUpperLevel) {
                y = this.variantZonesManager.getCenter('reference', start.chromosome.name.toUpperCase(), 'upper', 'edges');
            }
            controlPoints.push({x: startPosition.x2, y: y});
            controlPoints.push({x: endPosition.x2, y: y});
        }
        else {
            const middle = (startPosition.y + endPosition.y) / 2;
            controlPoints.push({x: startPosition.x2, y: middle});
            controlPoints.push({x: endPosition.x2, y: middle});
        }
        controlPoints.push({x: endPosition.x2, y: endPosition.y});
        controlPoints.push({x: endPosition.x1, y: endPosition.y});
        let thickness = 1;
        if (this._options && this._options.highlightBreakpoints) {
            thickness = this.config.breakpoint.thickness;
            graphics.lineStyle(this.config.breakpoint.thickness, this.config.breakpoint.color, 1);
        }
        else {
            graphics.lineStyle(1, 0x000000, 1);
        }
        for (let i = 0; i < controlPoints.length - 1; i++) {
            const p1 = controlPoints[i];
            const p2 = controlPoints[i + 1];
            graphics.moveTo(Math.round(p1.x) - thickness / 2, Math.round(p1.y) - thickness / 2);
            graphics.lineTo(Math.round(p2.x) - thickness / 2, Math.round(p2.y) - thickness / 2);
        }
    }

    _renderChromosomeNames(config) {
        for (let i = 0; i < this.variant.analysisResult.chromosomes.length; i++) {
            const name = this.variant.analysisResult.chromosomes[i].toUpperCase();
            const labelLeft = new PIXI.Text(this._getChromosomeDisplayName(name), this.config.chromosome.label);
            labelLeft.resolution = drawingConfiguration.resolution;
            labelLeft.x = Math.round(this._mainOffset - labelLeft.width);
            labelLeft.y = Math.round(this.variantZonesManager.getCenter('reference', name, 'chromosome') - labelLeft.height / 2);
            this.container.addChild(labelLeft);

            const labelRight = new PIXI.Text(this._getChromosomeDisplayName(name), this.config.chromosome.label);
            labelRight.resolution = drawingConfiguration.resolution;
            labelRight.x = Math.round(config.width - this._mainOffset);
            labelRight.y = Math.round(this.variantZonesManager.getCenter('reference', name, 'chromosome') - labelLeft.height / 2);
            this.container.addChild(labelRight);
        }
        for (let i = 0; i < this._altChromosomeNames.length; i++) {
            if (this._altChromosomeVisualInfo[this._altChromosomeNames[i]].isFake)
                continue;
            const name = this._altChromosomeNames[i];
            const labelLeft = new PIXI.Text(this._getChromosomeDisplayName(name), this.config.chromosome.label);
            labelLeft.resolution = drawingConfiguration.resolution;
            labelLeft.x = Math.round(this._mainOffset - labelLeft.width);
            labelLeft.y = Math.round(this.variantZonesManager.getCenter('alternative', name, 'chromosome') - labelLeft.height / 2);
            this.container.addChild(labelLeft);

            const labelRight = new PIXI.Text(this._getChromosomeDisplayName(name), this.config.chromosome.label);
            labelRight.resolution = drawingConfiguration.resolution;
            labelRight.x = Math.round(config.width - this._mainOffset);
            labelRight.y = Math.round(this.variantZonesManager.getCenter('alternative', name, 'chromosome') - labelLeft.height / 2);
            this.container.addChild(labelRight);
        }
    }

    _getChromosomeDisplayName(name) {
        if (!name.toLowerCase().startsWith('chr')) {
            return `CHR ${name.toUpperCase()}`;
        }
        return name.toUpperCase();
    }

}
