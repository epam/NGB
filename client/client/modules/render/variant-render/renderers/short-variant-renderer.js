import VariantBaseRenderer from './base-renderer';
import {BaseViewport, drawingConfiguration} from '../../core';
import {ShortVariantConfig, CommonConfig} from '../configs';
import {PixiTextSize} from '../../utilities';
import {
    FeatureCutOffRenderer,
    DashLines,
    SequenceRenderer,
    AminoacidsRenderer,
    ExonsCoverageRenderer,
    StrandDirectionRenderer
} from './drawing';
import {ShortVariantTransformer} from '../transformers';
import {VariantZonesManager} from './zones';
import {DragManager} from './interaction';
import PIXI from 'pixi.js';

const Math = window.Math;

export default class ShortVariantRenderer extends VariantBaseRenderer {

    _config = Object.assign(CommonConfig, ShortVariantConfig);
    _featureCutOffRenderer: FeatureCutOffRenderer = new FeatureCutOffRenderer(this._config);
    _sequenceRenderer: SequenceRenderer = new SequenceRenderer(this._config);
    _aminoacidRenderer: AminoacidsRenderer = new AminoacidsRenderer(this._config);
    _exonsCoverageRenderer: ExonsCoverageRenderer = new ExonsCoverageRenderer(this._config);
    _strandDirectionRenderer: StrandDirectionRenderer = new StrandDirectionRenderer(this._config);
    _variantZonesManager: VariantZonesManager = new VariantZonesManager(this._config);
    _dragManager: DragManager = null;
    _detailedMode = false;

    get config() {
        return this._config;
    }

    get featureCutOffRenderer(): FeatureCutOffRenderer {
        return this._featureCutOffRenderer;
    }

    get sequenceRenderer(): SequenceRenderer {
        return this._sequenceRenderer;
    }

    get aminoacidRenderer(): AminoacidsRenderer {
        return this._aminoacidRenderer;
    }

    get exonsCoverageRenderer(): ExonsCoverageRenderer {
        return this._exonsCoverageRenderer;
    }

    get strandDirectionRenderer(): StrandDirectionRenderer {
        return this._strandDirectionRenderer;
    }

    get variantZonesManager(): VariantZonesManager {
        return this._variantZonesManager;
    }

    get dragManager(): DragManager {
        return this._dragManager;
    }

    get height() {
        return this.variantZonesManager.getTotalHeight();
    }

    constructor(variant, heightChanged, showTooltip, updateSceneFn, reRenderScene) {
        super(variant, heightChanged, showTooltip, updateSceneFn, reRenderScene);
        this._dragManager = new DragManager(this.config, this.container);
        this._config.nucleotide.size.width.minimum = Math.min(this._config.nucleotide.size.width.minimum, PixiTextSize.getTextSize('W', this._config.nucleotide.label).width + 2 * this._config.nucleotide.margin.x);
    }

    renderObjects(flags, config) {
        if (super.renderObjects(flags, config)) {
            if (this.variant !== null && this.variant !== undefined) {
                this._renderAlternativeAllele(this.variant.selectedAltField);
                if (this._height !== this.height) {
                    this._height = this.height;
                    this._heightChanged();
                }
            }
        }
    }

    manageViewports(config) {
        super.manageViewports(config);
        this._detailedMode = false;
        const maximumBasePairsRange = (config.width - 2 * this.config.reference.width) / this._config.nucleotide.size.width.minimum;
        const minimumBasePairsRange = (config.width - 2 * this.config.reference.width) / this._config.nucleotide.size.width.maximum;
        const aminoacidLengthInBasePairs = 3;
        const extraBasePairsPerSide = aminoacidLengthInBasePairs * 2;
        const alternativeAlleleInfo = this.variant.selectedAltField;
        this._detailedMode = (alternativeAlleleInfo.info.length || 0) + 2 * extraBasePairsPerSide > maximumBasePairsRange;
        let subZones = [{name: 'aminoacid'}, {name: 'sequence'}];
        let displayStrand = true;
        if (alternativeAlleleInfo.affectedFeaturesStructure.affectedGenesData === null || alternativeAlleleInfo.affectedFeaturesStructure.affectedGenesData === undefined || alternativeAlleleInfo.affectedFeaturesStructure.affectedGenesData.length === 0) {
            displayStrand = false;
        }
        if (alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts === null || alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts === undefined || alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts.length === 0) {
            subZones = [{name: 'sequence'}];
        }
        if (!this._detailedMode) {
            let zonesConfiguration = [
                {name: 'reference', zones: subZones},
                {name: 'alternative', zones: subZones}
            ];
            if (displayStrand) {
                zonesConfiguration = [{name: 'strand'}, ...zonesConfiguration];
            }
            this.variantZonesManager.configureZones(zonesConfiguration);
            const pointOfInterest = this.variant.variantInfo.startIndexCorrected + (alternativeAlleleInfo.info.length || 0) / 2 - 0.5;
            const rangeOfInterest = Math.min(maximumBasePairsRange, Math.max(minimumBasePairsRange, (alternativeAlleleInfo.info.length || 0) + 2 * extraBasePairsPerSide));
            let start = pointOfInterest - rangeOfInterest / 2;
            const end = Math.min(this.variant.chromosome.size, start + rangeOfInterest);
            start = Math.max(1, end - rangeOfInterest);
            const viewportArgs = {
                chromosome: {start: 1, end: this.variant.chromosome.size},
                brush: {start: start, end: end},
                canvas: {start: this.config.reference.width, end: config.width - this.config.reference.width}
            };
            const viewport = new BaseViewport(viewportArgs);
            this.viewports.set(alternativeAlleleInfo, viewport);
        }
        else {
            let zonesConfiguration = [
                {name: 'reference', zones: subZones},
                {name: 'alternative', zones: subZones},
                {name: 'modified', zones: subZones}
            ];
            if (this.variant.variantInfo.type.toLowerCase() === 'del') {
                zonesConfiguration = [
                    {name: 'modified', zones: subZones},
                    {name: 'reference', zones: subZones},
                    {name: 'alternative', zones: subZones}
                ];
            }
            if (displayStrand) {
                zonesConfiguration = [{name: 'strand'}, ...zonesConfiguration];
            }
            this.variantZonesManager.configureZones(zonesConfiguration);
            const factor = this._config.nucleotide.size.width.maximum;
            const modifiedSequenceLengthInPixels = (alternativeAlleleInfo.info.length || 0) * factor;
            const mainCanvasSize = Math.min(config.width - this.config.reference.width - 2 * this._config.sequences.margin.horizontal, modifiedSequenceLengthInPixels);
            const canvases = {
                main: {
                    start: config.width / 2 - mainCanvasSize / 2,
                    end: config.width / 2 + mainCanvasSize / 2
                },
                subLeft: {
                    start: this.config.reference.width,
                    end: config.width / 2 - this._config.sequences.modifiedRange.width / 2
                },
                subRight: {
                    start: config.width / 2 + this._config.sequences.modifiedRange.width / 2,
                    end: config.width - this.config.reference.width - this._config.sequences.margin.horizontal
                }
            };

            const modifiedSequenceRange = {
                start: this.variant.variantInfo.startIndexCorrected - 0.5,
                end: this.variant.variantInfo.startIndexCorrected + (alternativeAlleleInfo.info.length || 0) - 0.5
            };
            const mainViewport:BaseViewport = new BaseViewport(
                {
                    chromosome: modifiedSequenceRange,
                    brush: {
                        start: this.variant.variantInfo.startIndexCorrected - 0.5
                    },
                    canvas: canvases.main,
                    factor: factor
                });
            const subLeftViewport:BaseViewport = new BaseViewport(
                {
                    chromosome: {
                        start: 1,
                        end: this.variant.chromosome.size
                    },
                    brush: {
                        end: this.variant.variantInfo.startIndexCorrected - 0.5
                    },
                    canvas: canvases.subLeft,
                    factor: factor
                });
            const subRightViewport:BaseViewport = new BaseViewport(
                {
                    chromosome: {
                        start: 1,
                        end: this.variant.chromosome.size
                    },
                    brush: {
                        start: this.variant.variantInfo.startIndexCorrected + ((alternativeAlleleInfo.info.length || 0)) - 0.5
                    },
                    canvas: canvases.subRight,
                    factor: factor
                });
            this.viewports.set('main', mainViewport);
            this.viewports.set('sub-left', subLeftViewport);
            this.viewports.set('sub-right', subRightViewport);
        }
    }

    _renderAlternativeAllele(alternativeAlleleInfo) {
        if (this._detailedMode) {
            this._renderAlternativeAlleleUsingDetailedMode(alternativeAlleleInfo);
        }
        else {
            this._renderAlternativeAlleleUsingDefaultMode(alternativeAlleleInfo);
        }
    }

    _renderAlternativeAlleleUsingDefaultMode(alternativeAlleleInfo) {
        const viewport:BaseViewport = this.viewports.get(alternativeAlleleInfo);
        if (viewport !== null && viewport !== undefined) {
            const container = new PIXI.Container();
            if (alternativeAlleleInfo.affectedFeaturesStructure.affectedGenesData !== null && alternativeAlleleInfo.affectedFeaturesStructure.affectedGenesData !== undefined && alternativeAlleleInfo.affectedFeaturesStructure.affectedGenesData.length > 0) {
                this.strandDirectionRenderer.renderStrandDirection(alternativeAlleleInfo.affectedFeaturesStructure.affectedGenesData[0].strand,
                    viewport,
                    container,
                    this.variantZonesManager.getStartPosition('strand'),
                    this.variantZonesManager.getEndPosition('strand'));
            }

            const sequences = ShortVariantTransformer.transformSequences(this.variant, alternativeAlleleInfo);
            this._renderAlternativeAlleleSequences(sequences, alternativeAlleleInfo, viewport, {
                leftClip: true,
                rightClip: true
            }, container);

            this._renderReferenceLabel(container, viewport);
            this._renderAltLabel(container, viewport);

            const bpLength = viewport.factor;
            const startPx = viewport.project.brushBP2pixel(this.variant.variantInfo.startIndexCorrected) - bpLength / 2;
            const endPx = viewport.project.brushBP2pixel(this.variant.variantInfo.startIndexCorrected + alternativeAlleleInfo.info.length) - bpLength / 2;
            container.addChild(DashLines.drawDashLine([{
                x: startPx,
                y: this.variantZonesManager.getStartPosition('reference')
            }, {x: startPx, y: this.variantZonesManager.getEndPosition('alternative')}], this._config.callout));
            container.addChild(DashLines.drawDashLine([{
                x: endPx,
                y: this.variantZonesManager.getStartPosition('reference')
            }, {x: endPx, y: this.variantZonesManager.getEndPosition('alternative')}], this._config.callout));
            this.container.addChild(container);
        }
    }

    _renderAlternativeAlleleUsingDetailedMode(alternativeAlleleInfo) {
        const mainViewport:BaseViewport = this.viewports.get('main');
        const leftViewport:BaseViewport = this.viewports.get('sub-left');
        const rightViewport:BaseViewport = this.viewports.get('sub-right');
        if (mainViewport !== null && mainViewport !== undefined && leftViewport !== null && leftViewport !== undefined && rightViewport !== null && rightViewport !== undefined) {
            const container = new PIXI.Container();
            if (alternativeAlleleInfo.affectedFeaturesStructure.affectedGenesData !== null && alternativeAlleleInfo.affectedFeaturesStructure.affectedGenesData !== undefined && alternativeAlleleInfo.affectedFeaturesStructure.affectedGenesData.length > 0) {
                this.strandDirectionRenderer.renderStrandDirection(alternativeAlleleInfo.affectedFeaturesStructure.affectedGenesData[0].strand,
                    mainViewport,
                    container,
                    this.variantZonesManager.getStartPosition('strand'),
                    this.variantZonesManager.getEndPosition('strand'));
            }
            const sequences = ShortVariantTransformer.transformSequences(this.variant, alternativeAlleleInfo);
            this._renderAlternativeAlleleSequences(sequences, alternativeAlleleInfo, leftViewport, {
                leftClip: true,
                rightClip: false
            }, container);
            this._renderAlternativeAlleleSequences(sequences, alternativeAlleleInfo, rightViewport, {
                leftClip: false,
                rightClip: true
            }, container);

            this._renderReferenceLabel(container, mainViewport);
            this._renderAltLabel(container, mainViewport);

            const bpLength = leftViewport.factor;
            const startPx = leftViewport.project.brushBP2pixel(this.variant.variantInfo.startIndexCorrected) - bpLength / 2;
            const endPx = rightViewport.project.brushBP2pixel(this.variant.variantInfo.startIndexCorrected + alternativeAlleleInfo.info.length) - bpLength / 2;
            const graphics = DashLines.drawDashLine([{
                x: startPx,
                y: this.variantZonesManager.getStartPosition('reference')
            }, {
                x: startPx,
                y: this.variantZonesManager.getEndPosition('alternative')
            }], this._config.callout, graphics);
            DashLines.drawDashLine([{x: endPx, y: this.variantZonesManager.getStartPosition('reference')}, {
                x: endPx,
                y: this.variantZonesManager.getEndPosition('alternative')
            }], this._config.callout, graphics);
            container.addChild(graphics);

            this.container.addChild(container);

            const modifiedSequenceContainer = new PIXI.Container();
            this.container.addChild(modifiedSequenceContainer);
            let modifiedSequence = sequences.alternative;
            if (this.variant.variantInfo.type.toLowerCase() === 'del') {
                modifiedSequence = sequences.reference;
            }
            this._renderModifiedSequence(modifiedSequenceContainer, modifiedSequence, sequences.alternativeAminoacidsData[0], alternativeAlleleInfo);
        }
    }

    _renderReferenceLabel(container, viewport) {
        let style = this.config.reference.label;
        const label = new PIXI.Text('REF', style);
        label.resolution = drawingConfiguration.resolution;
        if (label.width + 5 > this.variantZonesManager.getHeight('reference')) {
            style = this.config.reference.smallLabel;
            label.style = style;
        }
        label.x = 0;
        label.y = Math.round(this.variantZonesManager.getStartPosition('reference') + this.variantZonesManager.getHeight('reference') / 2 + label.width / 2);
        label.rotation = -Math.PI / 2;
        container.addChild(label);

        const labelRight = new PIXI.Text('REF', style);
        labelRight.resolution = drawingConfiguration.resolution;
        labelRight.x = Math.round(viewport.canvas.end);
        labelRight.y = Math.round(this.variantZonesManager.getStartPosition('reference') + this.variantZonesManager.getHeight('reference') / 2 + labelRight.width / 2);
        labelRight.rotation = -Math.PI / 2;
        container.addChild(labelRight);
    }

    _renderAltLabel(container, viewport) {
        let style = this.config.reference.label;
        const label = new PIXI.Text('ALT', style);
        label.resolution = drawingConfiguration.resolution;
        if (label.width + 5 > this.variantZonesManager.getHeight('alternative')) {
            style = this.config.reference.smallLabel;
            label.style = style;
        }
        label.x = 0;
        label.y = Math.round(this.variantZonesManager.getStartPosition('alternative') + this.variantZonesManager.getHeight('alternative') / 2 + label.width / 2);
        label.rotation = -Math.PI / 2;
        container.addChild(label);

        const labelRight = new PIXI.Text('ALT', style);
        labelRight.resolution = drawingConfiguration.resolution;
        labelRight.x = Math.round(viewport.canvas.end);
        labelRight.y = Math.round(this.variantZonesManager.getStartPosition('alternative') + this.variantZonesManager.getHeight('alternative') / 2 + labelRight.width / 2);
        labelRight.rotation = -Math.PI / 2;
        container.addChild(labelRight);
    }

    _renderModifiedSequence(container: PIXI.Container, sequence, aminoacids, alternativeAlleleInfo) {
        const mainViewport:BaseViewport = this.viewports.get('main');
        const leftViewport:BaseViewport = this.viewports.get('sub-left');
        const rightViewport:BaseViewport = this.viewports.get('sub-right');
        if (container !== null && container !== undefined && mainViewport !== null && mainViewport !== undefined && leftViewport !== null && leftViewport !== undefined && rightViewport !== null && rightViewport !== undefined) {
            container.removeChildren();

            const graphics = new PIXI.Graphics();
            const sequenceContainer = new PIXI.Container();
            container.addChild(sequenceContainer);
            container.addChild(graphics);

            const localCanvas = {
                start: leftViewport.canvas.end + this._config.nucleotide.margin.x * 2,
                end: rightViewport.canvas.start - this._config.nucleotide.margin.x * 2 - 1
            };
            const localCanvasSize = localCanvas.end - localCanvas.start;
            const mainCanvas = mainViewport.canvas;
            const mainCanvasSize = mainViewport.canvasSize;

            const convertation = (value) => value / mainCanvasSize * localCanvasSize;
            const projection = (x) => localCanvas.start + convertation(x - mainCanvas.start);

            const gapZoneName = (this.variant.variantInfo.type.toLowerCase() === 'del') ? 'reference' : 'alternative';

            const zonesDirection = this.variantZonesManager.getStartPosition(gapZoneName) > this.variantZonesManager.getStartPosition('modified') ? -1 : 1;
            const calloutPosition = zonesDirection > 0 ? this.variantZonesManager.getStartPosition('modified') : this.variantZonesManager.getEndPosition('modified');
            const calloutGapPosition = this.variantZonesManager.getStartPosition(gapZoneName, 'sequence') +
                this.variantZonesManager.getHeight(gapZoneName, 'sequence') * (zonesDirection < 0 ? 1 : 0) +
                this.config.nucleotide.margin.y * zonesDirection;

            const modifiedSequenceGapBoundaries = {
                x: projection(mainViewport.project.chromoBP2pixel(mainViewport.chromosome.start)),
                y: this.variantZonesManager.getStartPosition(gapZoneName, 'sequence') + this.config.nucleotide.margin.y,
                width: convertation(mainViewport.convert.chromoBP2pixel(mainViewport.chromosomeSize)),
                height: this.config.nucleotide.size.height
            };

            const modifiedSequenceBoundaries = {
                x: mainViewport.canvas.start,
                y: this.variantZonesManager.getStartPosition('modified'),
                width: mainViewport.canvasSize,
                height: this.variantZonesManager.getHeight('modified')
            };

            let gene = null;
            let exonArray = null;
            if (alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts.length > 0) {
                //todo: consensus exon structure
                for (let i = 0; i < alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts.length; i++) {
                    exonArray = alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts[i].transcript.value.exon;
                    if (exonArray !== null && exonArray !== undefined && exonArray.length > 0) {
                        gene = alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts[i].gene.value;
                        break;
                    }
                    exonArray = null;
                }
            }


            const render = () => {
                graphics.clear();
                graphics
                    .beginFill(0xEEEEEE, 1)
                    .lineStyle(1, 0x000000, 1)
                    .drawRect(modifiedSequenceGapBoundaries.x, modifiedSequenceGapBoundaries.y, modifiedSequenceGapBoundaries.width, modifiedSequenceGapBoundaries.height)
                    .endFill()
                    .beginFill(this._config.sequences.modifiedRange.ruler, 0.25)
                    .lineStyle(0, 0x000000, 0)
                    .drawRect(
                        projection(mainViewport.project.chromoBP2pixel(mainViewport.brush.start)),
                        modifiedSequenceGapBoundaries.y,
                        convertation(mainViewport.convert.chromoBP2pixel(mainViewport.brushSize + 1)),
                        modifiedSequenceGapBoundaries.height)
                    .endFill()
                    .lineStyle(1, this._config.sequences.modifiedRange.ruler, 1)
                    .moveTo(mainViewport.canvas.start, calloutPosition + 4 * zonesDirection)
                    .lineTo(mainViewport.canvas.start, calloutPosition)
                    .lineTo(projection(mainViewport.project.chromoBP2pixel(mainViewport.brush.start)), calloutPosition)
                    .lineTo(projection(mainViewport.project.chromoBP2pixel(mainViewport.brush.start)), calloutGapPosition)
                    .moveTo(mainViewport.canvas.end, calloutPosition + 4 * zonesDirection)
                    .lineTo(mainViewport.canvas.end, calloutPosition)
                    .lineTo(projection(mainViewport.project.chromoBP2pixel(mainViewport.brush.end + 1)), calloutPosition)
                    .lineTo(projection(mainViewport.project.chromoBP2pixel(mainViewport.brush.end + 1)), calloutGapPosition);

                sequenceContainer.removeChildren();
                if (exonArray && gene) {
                    this.exonsCoverageRenderer.renderExonsConverage(gene, exonArray, mainViewport, sequenceContainer, this.variantZonesManager.getStartPosition('modified'), this.variantZonesManager.getEndPosition('modified'));
                }
                this.sequenceRenderer.renderSequence(sequence, mainViewport, sequenceContainer, this.variantZonesManager.getStartPosition('modified', 'sequence'));
                this.aminoacidRenderer.renderAminoacids(aminoacids, mainViewport, sequenceContainer, this.variantZonesManager.getStartPosition('modified', 'aminoacid'));

                const clippingRules = {
                    rightClip: mainViewport.brush.end < mainViewport.chromosome.end,
                    leftClip: mainViewport.brush.start > mainViewport.chromosome.start
                };
                this.featureCutOffRenderer
                    .beginCuttingOff(sequenceContainer)
                    .cutOff(modifiedSequenceBoundaries, clippingRules)
                    .endCuttingOff();
            };

            render();

            const boundaries = {
                x: mainViewport.canvas.start,
                y: this.variantZonesManager.getStartPosition('modified'),
                width: mainViewport.canvasSize,
                height: this.variantZonesManager.getHeight('modified')
            };
            const dragCallback = (dx) => {
                let newStart = mainViewport.brush.start - dx / mainViewport.factor;
                let newEnd = Math.min(mainViewport.chromosome.end, newStart + mainViewport.brushSize);
                newStart = Math.max(mainViewport.chromosome.start, newEnd - mainViewport.brushSize);
                newEnd = Math.min(mainViewport.chromosome.end, newStart + mainViewport.brushSize);
                mainViewport.brush = {
                    start: newStart,
                    end: newEnd
                };
                render();
                this._updateSceneFn();
            };

            this.dragManager.addDraggableZone(boundaries, dragCallback);
        }
    }

    _renderAlternativeAlleleSequences(sequences, alternativeAlleleInfo, viewport, clipping, container) {
        if (viewport !== null && viewport !== undefined) {

            const localContainer = new PIXI.Container();
            container.addChild(localContainer);

            const gap = {
                start: this.variant.variantInfo.startIndexCorrected,
                end: this.variant.variantInfo.startIndexCorrected + (alternativeAlleleInfo.info.length || 0)
            };

            const refGap = this.variant.variantInfo.type.toLowerCase() === 'ins' ? gap : null;
            const altGap = this.variant.variantInfo.type.toLowerCase() === 'del' ? gap : null;

            let gene = null;
            let refExonArray = null;
            let altExonArray = null;
            if (alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts.length > 0) {
                //todo: consensus exon structure
                for (let i = 0; i < alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts.length; i++) {
                    let exonArray = alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts[i].transcript.value.exon;
                    if (exonArray !== null && exonArray !== undefined && exonArray.length > 0) {
                        gene = alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts[i].gene.value;
                        switch (this.variant.variantInfo.type.toLowerCase()) {
                            case 'ins': {
                                refExonArray = alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts[i].transcript.value.modifiedExons;
                                altExonArray = exonArray;
                            }
                                break;
                            case 'del': {
                                altExonArray = alternativeAlleleInfo.affectedFeaturesStructure.affectedTranscripts[i].transcript.value.modifiedExons;
                                refExonArray = exonArray;
                            }
                                break;
                            default: {
                                refExonArray = exonArray;
                                altExonArray = exonArray;
                            }
                                break;
                        }
                        break;
                    }
                    exonArray = null;
                }
            }
            if (refExonArray && altExonArray && gene) {
                this.exonsCoverageRenderer.renderExonsConverage(gene, refExonArray, viewport, localContainer, this.variantZonesManager.getStartPosition('reference'), this.variantZonesManager.getEndPosition('reference'), refGap);
                this.exonsCoverageRenderer.renderExonsConverage(gene, altExonArray, viewport, localContainer, this.variantZonesManager.getStartPosition('alternative'), this.variantZonesManager.getEndPosition('alternative'), altGap);
            }
            this.sequenceRenderer.renderSequence(sequences.reference, viewport, localContainer, this.variantZonesManager.getStartPosition('reference', 'sequence'));
            this.sequenceRenderer.renderSequence(sequences.alternative, viewport, localContainer, this.variantZonesManager.getStartPosition('alternative', 'sequence'));
            this.aminoacidRenderer.renderAminoacids(sequences.referenceAminoacidsData[0], viewport, localContainer, this.variantZonesManager.getStartPosition('reference', 'aminoacid'), refGap);
            this.aminoacidRenderer.renderAminoacids(sequences.alternativeAminoacidsData[0], viewport, localContainer, this.variantZonesManager.getStartPosition('alternative', 'aminoacid'), altGap);

            const referenceSequenceBoundaries = {
                x: viewport.canvas.start,
                y: this.variantZonesManager.getStartPosition('reference'),
                width: viewport.canvasSize,
                height: this.variantZonesManager.getHeight('reference')
            };
            const alternativeSequenceBoundaries = {
                x: viewport.canvas.start,
                y: this.variantZonesManager.getStartPosition('alternative'),
                width: viewport.canvasSize,
                height: this.variantZonesManager.getHeight('alternative')
            };

            this.featureCutOffRenderer
                .beginCuttingOff(localContainer)
                .cutOff(referenceSequenceBoundaries, clipping)
                .cutOff(alternativeSequenceBoundaries, clipping)
                .endCuttingOff();
        }
    }

}
