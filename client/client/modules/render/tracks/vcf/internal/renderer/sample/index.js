import * as PIXI from 'pixi.js-legacy';
import {CachedTrackRenderer} from '../../../../../core';
import {ColorProcessor, NumberFormatter} from '../../../../../utilities';
import findPositionForLabel from './utilities/find-position-for-label';
import splitChromosomeByDeletions from './utilities/split-chromosome-by-deletions';

function generateCorrectXFn (viewport) {
    return x => Math.max(
        -viewport.canvasSize,
        Math.min(
            2 * viewport.canvasSize,
            x
        )
    );
}

class VCFSampleRenderer extends CachedTrackRenderer {
    get height() {
        return this._height;
    }

    set height(value) {
        this._height = value;
    }

    constructor(config, track) {
        super(track);
        this._config = config;
        this._colors = track && track.config && track.config.colors
            ? track.config.colors
            : {};
        this.chromosomeGraphics = new PIXI.Graphics();
        this.graphics = new PIXI.Graphics();
        this.lettersContainer = new PIXI.Container();
        this.lettersBackground = new PIXI.Graphics();
        this.hoveredGraphics = new PIXI.Graphics();
        this.dataContainer.addChild(this.chromosomeGraphics);
        this.dataContainer.addChild(this.graphics);
        this.dataContainer.addChild(this.hoveredGraphics);
        this.dataContainer.addChild(this.lettersBackground);
        this.dataContainer.addChild(this.lettersContainer);
        this.freeLabelsZones = [];
        this.visualInfo = [];
        this.hoveredItem = undefined;
        this.initializeCentralLine();
    }

    renderDeletion (deletion, options = {}) {
        const {
            viewport = this._track ? this._track.viewport : undefined,
            y1,
            y2,
            graphics = this.graphics,
            hovered = false
        } = options;
        if (!viewport) {
            return;
        }
        const {
            startIndex,
            endIndex,
            highlightColor
        } = deletion;
        const color = this._colors.del || 0x000000;
        const correctX = generateCorrectXFn(viewport);
        const x1 = Math.floor(correctX(viewport.project.brushBP2pixel(startIndex - 0.5)));
        const x2 = Math.ceil(correctX(viewport.project.brushBP2pixel(endIndex + 0.5)));
        if (highlightColor) {
            const height = Math.floor((y2 - y1) * 0.5);
            graphics
                .beginFill(highlightColor, 1)
                .lineStyle(0, 0, 0)
                .drawRect(
                    Math.min(x1, x2),
                    Math.floor((y1 + y2) / 2.0 - height / 2.0),
                    Math.abs(x1 - x2),
                    height
                )
                .endFill();
        }
        graphics
            .lineStyle(hovered ? 2 : 1, color, 1)
            .moveTo(x1, y1)
            .lineTo(x1, y2)
            .moveTo(x1, (y1 + y2) / 2.0)
            .lineTo(x2, (y1 + y2) / 2.0)
            .moveTo(x2, y1)
            .lineTo(x2, y2);
        return {
            variant: deletion,
            position: {
                x1, x2
            }
        };
    }

    renderInsertion (insertion, options) {
        const {
            viewport = this._track ? this._track.viewport : undefined,
            y1,
            y2,
            graphics = this.graphics,
            hovered = false
        } = options;
        if (!viewport) {
            return;
        }
        const {
            startIndex,
            highlightColor
        } = insertion;
        const color = this._colors.ins || 0x000000;
        const correctX = generateCorrectXFn(viewport);
        const x = Math.floor(correctX(viewport.project.brushBP2pixel(startIndex - 0.5)));
        const offset = Math.max(viewport.factor / 5.0, 3);
        const x1 = Math.floor(x - offset);
        const x2 = Math.ceil(x + offset);
        if (highlightColor) {
            const width = offset;
            graphics
                .beginFill(highlightColor, 1)
                .lineStyle(0, 0, 0)
                .drawRect(
                    Math.floor(x - width / 2.0),
                    Math.floor(y1 + 2),
                    width,
                    (y2 - y1 - 4)
                )
                .endFill();
        }
        graphics
            .lineStyle(hovered ? 2 : 1, color, 1)
            .moveTo(x, Math.round(y1 + 1))
            .lineTo(x, Math.round(y2 - 1))
            .moveTo(x1, Math.round(y1 + 1))
            .lineTo(x2, Math.round(y1 + 1))
            .moveTo(x1, Math.round(y2 - 1))
            .lineTo(x2, Math.round(y2 - 1));
        return {
            variant: insertion,
            position: {
                x1,
                x2
            }
        };
    }

    renderSNV (snv, options) {
        const {
            viewport = this._track ? this._track.viewport : undefined,
            y1,
            y2,
            graphics = this.graphics,
            hovered = false,
            renderLabels = true
        } = options;
        if (!viewport) {
            return;
        }
        const {
            alternativeAlleles = [],
            startIndex,
            highlightColor
        } = snv;
        if (alternativeAlleles.length > 0) {
            const renderNucleotides = renderLabels &&
                viewport.factor > this._config.collapsed.nucleotide.threshold;
            const correctX = generateCorrectXFn(viewport);
            const [allele] = alternativeAlleles;
            const letters = allele.split();
            for (let l = 0; l < letters.length; l += 1) {
                const letter = letters[l];
                const color = this._colors[letter];
                if (color) {
                    const position = startIndex + l;
                    const x1 = Math.floor(correctX(viewport.project.brushBP2pixel(position - 0.5)));
                    const x2 = Math.ceil(correctX(viewport.project.brushBP2pixel(position + 0.5)));
                    if (highlightColor) {
                        graphics.lineStyle(1, highlightColor, 1);
                    } else {
                        graphics.lineStyle(0, 0, 0);
                    }
                    graphics
                        .beginFill(
                            hovered ? ColorProcessor.darkenColor(color) : color,
                            1
                        )
                        .drawRect(
                            x1,
                            y1,
                            x2 - x1,
                            y2 - y1
                        )
                        .endFill();
                    if (renderNucleotides && this.labelsManager) {
                        const nucleotide = this.labelsManager.getLabel(
                            letter,
                            this._config.collapsed.nucleotide.font
                        );
                        if (nucleotide) {
                            nucleotide.x = (x1 + x2) / 2.0 - nucleotide.width / 2.0;
                            nucleotide.y = (y1 + y2) / 2.0 - nucleotide.height / 2.0;
                            this.lettersContainer.addChild(nucleotide);
                        }
                    }
                }
            }
            return {
                variant: snv,
                position: {
                    x1: Math.floor(
                        correctX(
                            viewport.project.brushBP2pixel(
                                startIndex - 0.5
                            )
                        )
                    ),
                    x2: Math.floor(
                        correctX(
                            viewport.project.brushBP2pixel(startIndex + letters.length - 0.5)
                        )
                    )
                }
            };
        }
        return undefined;
    }

    renderInversion (inversion, options = {}) {
        const {
            viewport = this._track ? this._track.viewport : undefined,
            y1,
            y2,
            graphics = this.graphics,
            hovered = false
        } = options;
        if (!viewport) {
            return;
        }
        const {
            startIndex,
            endIndex,
            interChromosome,
            highlightColor
        } = inversion;
        if (!interChromosome) {
            const color = this._colors.ins || 0x000000;
            const correctX = generateCorrectXFn(viewport);
            const x1 = Math.floor(correctX(viewport.project.brushBP2pixel(startIndex - 0.5)));
            const x2 = Math.ceil(correctX(viewport.project.brushBP2pixel(endIndex + 0.5)));
            const x1ArrowDirection = Math.sign(x2 - x1);
            const x2ArrowDirection = -x1ArrowDirection;
            const yCenter = Math.round((y1 + y2) / 2.0);
            const arrowSize = Math.max(
                2,
                Math.abs(y2 - y1) / 2.0 - 1
            );
            if (highlightColor) {
                const height = Math.floor((y2 - y1) * 0.5);
                graphics
                    .beginFill(highlightColor, 1)
                    .lineStyle(0, 0, 0)
                    .drawRect(
                        Math.min(x1, x2),
                        Math.floor((y1 + y2) / 2.0 - height / 2.0),
                        Math.abs(x1 - x2),
                        height
                    )
                    .endFill();
            }
            graphics
                .lineStyle(hovered ? 2 : 1, color, 1)
                .moveTo(x1, yCenter)
                .lineTo(
                    Math.round(x1 + x1ArrowDirection * arrowSize),
                    Math.round(yCenter - arrowSize)
                )
                .moveTo(x1, yCenter)
                .lineTo(
                    Math.round(x1 + x1ArrowDirection * arrowSize),
                    Math.round(yCenter + arrowSize)
                )
                .moveTo(x1, yCenter)
                .lineTo(x2, yCenter)
                .moveTo(x2, yCenter)
                .lineTo(
                    Math.round(x2 + x2ArrowDirection * arrowSize),
                    Math.round(yCenter - arrowSize)
                )
                .moveTo(x2, yCenter)
                .lineTo(
                    Math.round(x2 + x2ArrowDirection * arrowSize),
                    Math.round(yCenter + arrowSize)
                );
            return {
                variant: inversion,
                position: {
                    x1,
                    x2
                }
            };
        }
        return this.renderStructural(inversion, options);
    }

    renderStructural (variation, options) {
        const {
            viewport = this._track ? this._track.viewport : undefined,
            y1,
            y2,
            graphics = this.graphics,
            hovered = false,
            renderLabels = true
        } = options;
        if (!viewport) {
            return;
        }
        const config = this._config;
        const {
            startIndex,
            interChromosome,
            alternativeAllelesInfo = [],
            highlightColor
        } = variation;
        const {
            mate
        } = alternativeAllelesInfo[0] || {};
        let direction = 1;
        let mateInfo;
        let endPosition;
        let visualInfo;
        if (interChromosome && mate) {
            const {
                attachedAt,
                chromosome,
                position
            } = mate;
            direction = /^left$/i.test(attachedAt) ? -1 : 1;
            if (chromosome && position) {
                mateInfo = `${chromosome} ${NumberFormatter.formattedText(position)}`;
                if (direction > 0) {
                    mateInfo = '\u2192'.concat(' ', mateInfo);
                } else {
                    mateInfo = mateInfo.concat(' ', '\u2190');
                }
            }
        } else if (!interChromosome && mate) {
            const {
                attachedAt,
                position
            } = mate;
            direction = /^left$/i.test(attachedAt) ? -1 : 1;
            if (position && position !== startIndex) {
                direction = Math.sign(position - startIndex);
                endPosition = position;
            }
        }
        const color = 0x000000;
        const correctX = generateCorrectXFn(viewport);
        const x = Math.floor(correctX(viewport.project.brushBP2pixel(startIndex - direction * 0.5)));
        graphics
            .lineStyle(hovered ? 2 : 1, color, 1)
            .moveTo(x, y1)
            .lineTo(x, y2);
        if (renderLabels) {
            let labelText = variation.type;
            if (mateInfo) {
                if (direction > 0) {
                    labelText = labelText.concat(' ', mateInfo);
                } else {
                    labelText = mateInfo.concat(' ', labelText);
                }
            }
            const label = this.labelsManager.getLabel(
                labelText,
                config.collapsed.variation.label.font
            );
            const initialPosition = x +
                (
                    label.width / 2.0 + config.collapsed.variation.label.margin + 1
                ) * direction -
                label.width / 2.0;
            const bestPosition = findPositionForLabel(
                this.freeLabelsZones,
                {
                    x: initialPosition,
                    width: label.width
                }
            );
            if (bestPosition) {
                label.y = Math.floor((y1 + y2) / 2.0 - label.height / 2.0);
                label.x = Math.round(bestPosition);
                this.lettersContainer.addChild(label);
                this.lettersBackground
                    .lineStyle(0, 0xffffff, 0)
                    .beginFill(
                        highlightColor || (
                            this._colors.base !== undefined ? this._colors.base : 0xffffff
                        ),
                        highlightColor ? 1 : 0.8
                    )
                    .drawRect(
                        label.x - config.collapsed.variation.label.margin,
                        label.y,
                        label.width + 2.0 * config.collapsed.variation.label.margin,
                        label.height
                    )
                    .endFill();
                visualInfo = {
                    x1: label.x,
                    x2: label.x + label.width
                };
                if (bestPosition !== initialPosition) {
                    const {
                        radius = 1,
                        color: stroke = color
                    } = config.collapsed.variation.cutout || {};
                    const xx = Math.round(
                        bestPosition -
                        (label.width / 2.0 + radius + 1) * direction +
                        label.width / 2.0
                    );
                    this.lettersBackground
                        .lineStyle(1, stroke, 1)
                        .beginFill(stroke, 1)
                        .drawCircle(
                            xx,
                            Math.round((y1 + y2) / 2.0),
                            radius
                        )
                        .endFill()
                        .moveTo(x, y2 - radius)
                        .lineTo(xx, y2 - radius)
                        .lineTo(xx, Math.round((y1 + y2) / 2.0));
                }
            }
        }
        if (!mateInfo && endPosition) {
            const fromX = x;
            const toX = Math.round(correctX(viewport.project.brushBP2pixel(endPosition)));
            if (highlightColor) {
                graphics
                    .beginFill(highlightColor, 1)
                    .lineStyle(0, 0, 0)
                    .drawRect(
                        Math.min(fromX, toX),
                        Math.floor((y1 + y2) / 2.0 - config.collapsed.variation.highlight.lineHeight / 2.0),
                        Math.abs(fromX - toX),
                        config.collapsed.variation.highlight.lineHeight
                    )
                    .endFill();
            }
            graphics
                .lineStyle(hovered ? 2 : 1, color, 1)
                .moveTo(toX, y1)
                .lineTo(toX, y2)
                .lineStyle(hovered ? 2 : 1, config.collapsed.variation.line.stroke, 1)
                .moveTo(toX, Math.round((y1 + y2) / 2.0) - 0.5)
                .lineTo(fromX, Math.round((y1 + y2) / 2.0) - 0.5);
            visualInfo = {
                x1: Math.min(fromX, toX),
                x2: Math.max(fromX, toX)
            };
        }
        if (visualInfo) {
            return {
                variant: variation,
                position: visualInfo
            };
        }
        return undefined;
    }

    renderVariation (variation, options) {
        if (
            /^del$/i.test(variation.type) &&
            (!variation.structural || !variation.interChromosome)
        ) {
            return this.renderDeletion(variation, options);
        }
        if (/^ins$/i.test(variation.type)) {
            return this.renderInsertion(variation, options);
        }
        if (/^(snv|snp)$/i.test(variation.type)) {
            return this.renderSNV(variation, options);
        }
        if (/^inv$/i.test(variation.type)) {
            return this.renderInversion(variation, options);
        }
        return this.renderStructural(variation, options);
    }

    rebuildContainer(viewport, cache) {
        super.rebuildContainer(viewport, cache);
        this.clear();
        const {data = []} = cache || {};
        const config = this._config;
        const chromosomeParts = splitChromosomeByDeletions(
            data.filter(variation => /^del$/i.test(variation.type))
        );
        if (this._colors.base) {
            this.chromosomeGraphics
                .lineStyle(0, 0xcccccc, 0)
                .beginFill(this._colors.base, 1);
        } else {
            this.chromosomeGraphics
                .lineStyle(1, 0xcccccc, 1)
                .beginFill(0xffffff, 1);
        }
        const correctX = generateCorrectXFn(viewport);
        const y1 = Math.floor(
            config.collapsedSampleHeight / 2.0 - config.collapsed.bar.height / 2.0
        );
        const y2 = Math.floor(
            config.collapsedSampleHeight / 2.0 + config.collapsed.bar.height / 2.0
        );
        for (const chromosomePart of chromosomeParts) {
            const {
                startIndex,
                endIndex
            } = chromosomePart;
            const x1 = Math.floor(
                correctX(
                    viewport.project.brushBP2pixel(startIndex - 0.5)
                )
            );
            const x2 = Math.ceil(
                correctX(
                    viewport.project.brushBP2pixel(endIndex - 0.5)
                )
            );
            this.chromosomeGraphics
                .drawRect(
                    x1,
                    y1,
                    x2 - x1,
                    y2 - y1
                );
        }
        this.chromosomeGraphics.endFill();
        const sorted = data.slice();
        const variantLength = v => Math.abs(v.startIndex - v.endIndex);
        const variantTypeWeight = v => /^(del|inv)$/i.test(v.type) ? 1 : 0;
        sorted
            .sort((a, b) => variantTypeWeight(a) - variantTypeWeight(b))
            .sort((a, b) => variantLength(b) - variantLength(a));
        sorted.forEach(variation => {
            const variantVisualInfo = this.renderVariation(
                variation,
                {viewport, y1, y2}
            );
            if (variantVisualInfo) {
                this.visualInfo.push(variantVisualInfo);
            }
        });
        const getVisualSizePx = o => Math.abs(o.position.x2 - o.position.x1);
        this.visualInfo.sort((a, b) => getVisualSizePx(a) - getVisualSizePx(b));
    }

    clear() {
        this.hoveredItem = undefined;
        this.chromosomeGraphics.clear();
        this.graphics.clear();
        this.hoveredGraphics.clear();
        this.lettersBackground.clear();
        this.lettersContainer.removeChildren();
        this.freeLabelsZones = [{start: -Infinity, end: Infinity}];
        this.visualInfo = [];
    }

    getElementUnderPosition (position) {
        if (!position) {
            return undefined;
        }
        const {x} = position;
        const [element] = (this.visualInfo || [])
            .filter(o => o.position && (o.position.x1 <= x && o.position.x2 >= x));
        return element;
    }

    onClick(position) {
        const element = this.getElementUnderPosition(position);
        return element ? element.variant : undefined;
    }

    onMove(position) {
        const element = this.getElementUnderPosition(position);
        if (element !== this.hoveredItem) {
            this.hoveredGraphics.clear();
            this.graphics.alpha = 1.0;
            this.hoveredItem = element;
            if (element) {
                const config = this._config;
                this.graphics.alpha = config.collapsed.hoveredAlpha || 1;
                const y1 = Math.floor(
                    config.collapsedSampleHeight / 2.0 - config.collapsed.bar.height / 2.0
                );
                const y2 = Math.floor(
                    config.collapsedSampleHeight / 2.0 + config.collapsed.bar.height / 2.0
                );
                this.renderVariation(
                    element.variant,
                    {
                        y1,
                        y2,
                        hovered: true,
                        graphics: this.hoveredGraphics,
                        renderLabels: false
                    }
                );
            }
        }
        return element;
    }

    animate() {
        return false;
    }
}

export {VCFSampleCoverageRenderer} from './coverage';
export {VCFSamplesScroll} from './scroll';
export {VCFSampleRenderer};
