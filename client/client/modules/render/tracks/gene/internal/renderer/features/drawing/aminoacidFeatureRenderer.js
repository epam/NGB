import FeatureBaseRenderer from './featureBaseRenderer';
import PIXI from 'pixi.js';
import {ColorProcessor, PixiTextSize} from '../../../../../../utilities';
import {drawingConfiguration} from '../../../../../../core';

const AMINOACID_LENGTH_IN_BASE_PAIRS = 3;
const FEATURE_INDEX_AMINOACID = 2;
const Math = window.Math;

const AMINOACID_DESCRIPTION = {
    'A': {name: 'Alanine', symbol: 'Ala'},
    'C': {name: 'Cysteine', symbol: 'Cys'},
    'D': {name: 'Aspartic acid', symbol: 'Asp'},
    'E': {name: 'Glutamic acid', symbol: 'Glu'},
    'F': {name: 'Phenylalanine', symbol: 'Phe'},
    'G': {name: 'Glycine', symbol: 'Gly'},
    'H': {name: 'Histidine', symbol: 'His'},
    'I': {name: 'Isoleucine', symbol: 'Ile'},
    'K': {name: 'Lysine', symbol: 'Lys'},
    'L': {name: 'Leucine', symbol: 'Leu'},
    'M': {name: 'Methionine', symbol: 'Met'},
    'N': {name: 'Asparagine', symbol: 'Asn'},
    'O': {name: 'Pyrrolysine', symbol: 'Pyl'},
    'P': {name: 'Proline', symbol: 'Pro'},
    'Q': {name: 'Glutamine', symbol: 'Gln'},
    'R': {name: 'Arginine', symbol: 'Arg'},
    'S': {name: 'Serine', symbol: 'Ser'},
    'START': {name: 'Start codon', symbol: 'START'},
    'STOP': {name: 'Stop codon', symbol: 'STOP'},
    'T': {name: 'Threonine', symbol: 'Thr'},
    'U': {name: 'Selenocysteine', symbol: 'Sec'},
    'V': {name: 'Valine', symbol: 'Val'},
    'W': {name: 'Tryptophan', symbol: 'Trp'},
    'Y': {name: 'Tyrosine', symbol: 'Tyr'}
};

export default class AminoacidFeatureRenderer extends FeatureBaseRenderer {
    gffShowNumbersAminoacid;

    constructor(config, registerLabel, registerDockableElement, registerFeaturePosition) {
        super(config, registerLabel, registerDockableElement, registerFeaturePosition);
        this._aminoacidLabelWidth = PixiTextSize.getTextSize('W', this.config.aminoacid.label.defaultStyle).width +
            this.config.aminoacid.label.margin * 2;
        this._startLabelWidth = PixiTextSize.getTextSize('START', this.config.aminoacid.label.defaultStyle).width +
            this.config.aminoacid.label.margin * 2;
        this._stopLabelWidth = PixiTextSize.getTextSize('STOP', this.config.aminoacid.label.defaultStyle).width +
            this.config.aminoacid.label.margin * 2;
        this._maxAminoacidNumberWidth = PixiTextSize.getTextSize('99999', this.config.aminoacid.number).width;
    }

    shouldDrawAminoacids(viewport) {
        return viewport.convert.brushBP2pixel(AMINOACID_LENGTH_IN_BASE_PAIRS) >= this._aminoacidLabelWidth;
    }

    shouldNumberAminoacids(viewport) {
        return viewport.convert.brushBP2pixel(AMINOACID_LENGTH_IN_BASE_PAIRS) >= this._maxAminoacidNumberWidth;
    }

    _getLabelStyleConfig(acid, shouldDisplayStopLabel, shouldDisplayStartLabel) {
        let fill = this.config.aminoacid.even.fill;
        let labelStyle = Object.assign({}, this.config.aminoacid.label.defaultStyle, this.config.aminoacid.even.label);
        let shouldDisplayLabel = true;
        if (acid.text.toLowerCase() === 'stop') {
            fill = this.config.aminoacid.stop.fill;
            labelStyle = Object.assign({}, this.config.aminoacid.label.defaultStyle, this.config.aminoacid.stop.label);
            shouldDisplayLabel = shouldDisplayStopLabel;
        }
        else if (acid.text.toLowerCase() === 'start') {
            fill = this.config.aminoacid.start.fill;
            labelStyle = Object.assign({}, this.config.aminoacid.label.defaultStyle, this.config.aminoacid.start.label);
            shouldDisplayLabel = shouldDisplayStartLabel;
        } else if (acid.index % 2 === 1) {
            fill = this.config.aminoacid.odd.fill;
            labelStyle = Object.assign({}, this.config.aminoacid.label.defaultStyle, this.config.aminoacid.odd.label);
        }
        return {
            fill,
            labelStyle,
            shouldDisplayLabel
        };
    }

    static getStrandDirectionEnumValue(feature) {
        let strandDirection = 0;
        if (feature.hasOwnProperty('strand')) {
            strandDirection = (feature.strand.toLowerCase() === 'positive') ? 1 : -1;
        }
        return strandDirection;
    }

    static getAminoacidInfo(acid) {
        const feature = {};
        const aminoacidDescription = AMINOACID_DESCRIPTION[acid.text.toUpperCase()];
        if (aminoacidDescription) {
            feature.name = aminoacidDescription.name;
        } else {
            feature.name = acid.text.toUpperCase();
        }
        feature.index = acid.index + 1;
        feature.feature = 'aminoacid';
        return feature;
    }

    aminoacidsFitsViewport(transcript, viewport) {
        let fits = false;
        const minStartIndex = viewport.project.pixel2brushBP(- viewport.factor / 2);
        const maxEndIndex = viewport.project.pixel2brushBP(viewport.canvasSize + viewport.factor / 2);
        if (transcript.structure) {
            for (let i = 0; i < transcript.structure.length; i++) {
                const block = transcript.structure[i];
                if (!block.isEmpty) {
                    for (let j = 0; j < block.items.length; j++) {
                        const sequence = block.items[j].aminoacidSequence || [];
                        for (let a = 0; a < sequence.length; a++) {
                            const acid = sequence[a];
                            if (acid.endIndex < minStartIndex || acid.startIndex > maxEndIndex) {
                                continue;
                            }
                            fits = true;
                            break;
                        }
                    }
                }
            }
        }
        return fits;
    }

    analyzeBoundaries(feature, viewport) {
        this._aminoacidNumberHeight = this.gffShowNumbersAminoacid && this.shouldNumberAminoacids(viewport) ? PixiTextSize.getTextSize('1', this.config.aminoacid.number).height : 0;
        const boundaries = super.analyzeBoundaries(feature, viewport);
        const rectBoundaries = boundaries.rect;
        if (this.aminoacidsFitsViewport(feature, viewport)) {
            rectBoundaries.y2 += this._aminoacidNumberHeight;
        }
        return boundaries;
    }

    render(feature, viewport, graphicsObj, labelContainer, dockableElementsContainer, attachedElementsContainer, position) {
        super.render(feature, viewport, graphicsObj, labelContainer, dockableElementsContainer, attachedElementsContainer, position);
        const {
            graphics,
            hoveredGraphics
        } = graphicsObj || {};
        feature.aminoacidSequence = feature.aminoacidSequence || [];

        const minStartIndex = viewport.project.pixel2brushBP(-viewport.canvasSize);
        const maxEndIndex = viewport.project.pixel2brushBP(2 * viewport.canvasSize);
        const height = this.config.transcript.height;
        const pixelsInBp = viewport.factor;
        const shouldDisplayAminoacidLabels = viewport.convert.brushBP2pixel(AMINOACID_LENGTH_IN_BASE_PAIRS) >= this._aminoacidLabelWidth;
        const shouldDisplayStartLabel = viewport.convert.brushBP2pixel(AMINOACID_LENGTH_IN_BASE_PAIRS) >= this._startLabelWidth;
        const shouldDisplayStopLabel = viewport.convert.brushBP2pixel(AMINOACID_LENGTH_IN_BASE_PAIRS) >= this._stopLabelWidth;
        const strandDirection = AminoacidFeatureRenderer.getStrandDirectionEnumValue(feature);
        for (let i = 0; i < feature.aminoacidSequence.length; i++) {
            const acid = feature.aminoacidSequence[i];
            if (acid.startIndex < minStartIndex || acid.startIndex > maxEndIndex) {
                continue;
            }

            if (this.gffShowNumbersAminoacid && this.shouldNumberAminoacids(viewport)) {
                const indexAcid = acid.index + 1;
                const aminoacidNumber = new PIXI.Text(indexAcid, this.config.aminoacid.number);
                aminoacidNumber.resolution = drawingConfiguration.resolution;

                const aminoacidNumberPosition = {
                    x: viewport.project.brushBP2pixel(acid.startIndex) +
                    viewport.convert.brushBP2pixel(acid.endIndex - acid.startIndex + 1) / 2 - aminoacidNumber.width / 2,
                    y: Math.round(position.y - height / 2 - aminoacidNumber.height)
                };
                aminoacidNumber.x = Math.round(aminoacidNumberPosition.x);
                aminoacidNumber.y = Math.round(aminoacidNumberPosition.y);
                labelContainer.addChild(aminoacidNumber);
                this.registerLabel(aminoacidNumber, aminoacidNumberPosition, {
                    end: acid.endIndex,
                    start: acid.startIndex,
                }, false, true);
            }

            let startStrandFactor = 0;
            let endStrandFactor = 0;
            let startDiff = 0;
            let endDiff = 0;
            const startEndDiffHeightFactor = 4;
            if (!((strandDirection > 0 && i === 0) || (strandDirection < 0 && i === feature.aminoacidSequence.length - 1))) {
                startDiff = -strandDirection * this.config.transcript.features.strand.arrow.height / startEndDiffHeightFactor;
                startStrandFactor = strandDirection;
            }
            if (!((strandDirection < 0 && i === 0) || (strandDirection > 0 && i === feature.aminoacidSequence.length - 1))) {
                endDiff = -strandDirection * this.config.transcript.features.strand.arrow.height / startEndDiffHeightFactor;
                endStrandFactor = strandDirection;
            }
            const {fill, labelStyle, shouldDisplayLabel} = this._getLabelStyleConfig(acid, shouldDisplayStopLabel, shouldDisplayStartLabel);
            graphics
                .beginFill(fill, 1)
                .lineStyle(0, fill, 0)
                .moveTo(
                    viewport.project.brushBP2pixel(acid.startIndex) - pixelsInBp / 2 + startDiff,
                    position.y - height / 2
                )
                .lineTo(
                    viewport.project.brushBP2pixel(acid.startIndex) - pixelsInBp / 2 + startDiff +
                    startStrandFactor * this.config.transcript.features.strand.arrow.height / 2,
                    position.y
                )
                .lineTo(
                    viewport.project.brushBP2pixel(acid.startIndex) - pixelsInBp / 2 + startDiff,
                    position.y + height / 2
                )
                .lineTo(
                    viewport.project.brushBP2pixel(acid.endIndex) + pixelsInBp / 2 + endDiff,
                    position.y + height / 2
                )
                .lineTo(
                    viewport.project.brushBP2pixel(acid.endIndex) + pixelsInBp / 2 + endDiff +
                    endStrandFactor * this.config.transcript.features.strand.arrow.height / 2,
                    position.y
                )
                .lineTo(
                    viewport.project.brushBP2pixel(acid.endIndex) + pixelsInBp / 2 + endDiff,
                    position.y - height / 2
                )
                .lineTo(
                    viewport.project.brushBP2pixel(acid.startIndex) - pixelsInBp / 2 + startDiff,
                    position.y - height / 2
                )
                .endFill();

            hoveredGraphics
                .beginFill(ColorProcessor.darkenColor(fill, 0.2), 1)
                .lineStyle(0, ColorProcessor.darkenColor(fill, 0.2), 0)
                .moveTo(
                    viewport.project.brushBP2pixel(acid.startIndex) - pixelsInBp / 2 + startDiff,
                    position.y - height / 2
                )
                .lineTo(
                    viewport.project.brushBP2pixel(acid.startIndex) - pixelsInBp / 2 + startDiff +
                    startStrandFactor * this.config.transcript.features.strand.arrow.height / 2,
                    position.y
                )
                .lineTo(
                    viewport.project.brushBP2pixel(acid.startIndex) - pixelsInBp / 2 + startDiff,
                    position.y + height / 2
                )
                .lineTo(
                    viewport.project.brushBP2pixel(acid.endIndex) + pixelsInBp / 2 + endDiff,
                    position.y + height / 2
                )
                .lineTo(
                    viewport.project.brushBP2pixel(acid.endIndex) + pixelsInBp / 2 + endDiff +
                    endStrandFactor * this.config.transcript.features.strand.arrow.height / 2,
                    position.y
                )
                .lineTo(
                    viewport.project.brushBP2pixel(acid.endIndex) + pixelsInBp / 2 + endDiff,
                    position.y - height / 2
                )
                .lineTo(
                    viewport.project.brushBP2pixel(acid.startIndex) - pixelsInBp / 2 + startDiff,
                    position.y - height / 2
                )
                .endFill();

            this.updateTextureCoordinates(
                {
                    x: viewport.project.brushBP2pixel(acid.startIndex) - pixelsInBp / 2 + startDiff,
                    y: position.y - height / 2
                });

            if (shouldDisplayLabel && shouldDisplayAminoacidLabels && acid.endIndex - acid.startIndex >= 1) {
                const label = new PIXI.Text(acid.text.toUpperCase(), labelStyle);
                label.resolution = drawingConfiguration.resolution;
                const yOffset = 0.5;
                const labelPosition = {
                    x: viewport.project.brushBP2pixel(acid.startIndex) +
                    viewport.convert.brushBP2pixel(acid.endIndex - acid.startIndex + 1) / 2 - label.width / 2,
                    y: Math.round(position.y - label.height / 2 - yOffset)
                };
                label.x = Math.round(labelPosition.x);
                label.y = Math.round(labelPosition.y);
                labelContainer.addChild(label);
                this.registerLabel(
                    label,
                    labelPosition,
                    {
                        end: acid.endIndex,
                        start: acid.startIndex,
                    },
                    false,
                    true);
                this.registerFeaturePosition(AminoacidFeatureRenderer.getAminoacidInfo(acid), {
                    x1: viewport.project.brushBP2pixel(acid.startIndex) - pixelsInBp / 2,
                    x2: viewport.project.brushBP2pixel(acid.endIndex) + pixelsInBp / 2,
                    y1: position.y - height / 2,
                    y2: position.y + height / 2
                }, null, FEATURE_INDEX_AMINOACID);
            }
        }
    }
}
