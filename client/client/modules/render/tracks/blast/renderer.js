import {
    BTOPPartType,
    parseBtop
} from '../../../../app/shared/blastContext';
import {ColorProcessor} from '../../utilities';
import PIXI from 'pixi.js';
import {drawingConfiguration} from '../../core/configuration';

class BLASTAlignmentRenderer {
    constructor(viewport, config, pixiRenderer, options) {
        this.viewport = viewport;
        this.config = config;
        this.pixiRenderer = pixiRenderer;
        this.options = options;
        this.container = new PIXI.Container();
        this.height = config.height || 0;
    }

    initialize () {
        this.container.removeChildren();
        if (this.blastGraphicsContainer) {
            this.blastGraphicsContainer.removeChildren();
            this.blastGraphicsContainer = null;
        }
        if (this.centerLineGraphics) {
            this.centerLineGraphics = null;
        }
        this.centerLineGraphics = new PIXI.Graphics();
        this.blastGraphicsContainer = new PIXI.Container();
        this.container.addChild(this.blastGraphicsContainer);
        this.container.addChild(this.centerLineGraphics);
    }

    renderCenterLine(features) {
        if (!this.centerLineGraphics || !this.viewport || !features || !features.centerLine) {
            return;
        }
        this.centerLineGraphics.clear();
        const dashesCount = this.height / (2 * this.config.centerLine.dash.length);
        const length = this.config.centerLine.dash.length;
        const thickness = this.config.centerLine.dash.thickness;
        const color = this.config.centerLine.dash.fill;
        const drawVerticalDashLine = (x) => {
            this.centerLineGraphics.lineStyle(thickness, color, 1);
            for (let i = 0; i < dashesCount; i++) {
                this.centerLineGraphics
                    .moveTo(Math.floor(x) + thickness / 2.0, (2 * i) * length)
                    .lineTo(Math.floor(x) + thickness / 2.0, (2 * i + 1) * length);
            }
        };
        const center = Math.round(this.viewport.centerPosition);
        if (this.viewport.factor > 2) {
            drawVerticalDashLine(this.viewport.project.brushBP2pixel(center) - this.viewport.factor / 2);
            drawVerticalDashLine(this.viewport.project.brushBP2pixel(center) + this.viewport.factor / 2);
        }
        else {
            drawVerticalDashLine(this.viewport.project.brushBP2pixel(center));
        }
    }

    renderStrandMarker (graphics, x, direction, options) {
        const {
            height,
            minimum,
            maximum,
            baseColor
        } = options;
        if (x < minimum || x > maximum) {
            return;
        }
        x = Math.min(maximum, Math.max(minimum, x));
        graphics
            .lineStyle(0, baseColor, 0)
            .beginFill(baseColor, 1)
            .moveTo(
                x,
                -Math.round(height / 2.0),
            )
            .lineTo(
                x + direction * this.config.strandMarker.width,
                0
            )
            .lineTo(
                x,
                Math.round(height / 2.0)
            )
            .endFill();
    }

    renderMatch (graphics, x1, x2, options) {
        const {
            height,
            minimum,
            maximum,
            baseColor
        } = options;
        if (x2 < minimum || x1 > maximum) {
            return;
        }
        x1 = Math.min(maximum, Math.max(minimum, x1));
        x2 = Math.min(maximum, Math.max(minimum, x2));
        graphics
            .lineStyle(0, baseColor, 0)
            .beginFill(baseColor, 1)
            .drawRect(
                x1,
                -Math.round(height / 2.0),
                x2 - x1,
                height
            )
            .endFill();
    }

    renderMisMatch (graphics, x1, x2, alt, options) {
        const {
            height,
            minimum,
            maximum,
            baseColor
        } = options;
        if (x2 < minimum || x1 > maximum) {
            return;
        }
        x1 = Math.min(maximum, Math.max(minimum, x1));
        x2 = Math.min(maximum, Math.max(minimum, x2));
        const colorCode = (alt || '').toUpperCase();
        const color = this.config.colors && this.config.colors.hasOwnProperty(colorCode)
            ? this.config.colors[colorCode]
            : ColorProcessor.darkenColor(baseColor);
        graphics
            .lineStyle(0, 0x0, 0)
            .beginFill(color, 1)
            .drawRect(
                x1,
                -Math.round(height / 2.0),
                x2 - x1,
                height
            )
            .endFill();
        if (alt) {
            const label = new PIXI.Text(
                alt,
                this.config.sequence.mismatch.label,
                drawingConfiguration.resolution
            );
            if (label.width < x2 - x1) {
                label.x = Math.round((x1 + x2) / 2.0 - label.width / 2.0);
                label.y = Math.round(
                    -label.height / 2.0
                );
                this.blastGraphicsContainer.addChild(label);
            }
        }
    }

    renderInsertion (graphics, x1, alt, options) {
        const {
            height,
            minimum,
            maximum,
            bpWidth,
            baseColor
        } = options;
        if (x1 < minimum || x1 > maximum) {
            return;
        }
        x1 = Math.min(maximum, Math.max(minimum, x1));
        const colorCode = (alt || '').toUpperCase();
        const color = this.config.colors && this.config.colors.hasOwnProperty(colorCode)
            ? this.config.colors[colorCode]
            : ColorProcessor.darkenColor(baseColor);
        graphics
            .lineStyle(1, color, 1)
            .moveTo(
                x1,
                -Math.round(height / 2.0)
            )
            .lineTo(
                x1,
                Math.round(height / 2.0)
            )
            .moveTo(
                x1 - bpWidth / 3.0,
                -Math.round(height / 2.0)
            )
            .lineTo(
                x1 + bpWidth / 3.0,
                -Math.round(height / 2.0)
            )
            .moveTo(
                x1 - bpWidth / 3.0,
                Math.round(height / 2.0)
            )
            .lineTo(
                x1 + bpWidth / 3.0,
                Math.round(height / 2.0)
            );
    }

    renderDeletion (graphics, x1, x2, options) {
        const {
            height,
            minimum,
            maximum,
            baseColor
        } = options;
        if (x2 < minimum || x1 > maximum) {
            return;
        }
        x1 = Math.min(maximum, Math.max(minimum, x1));
        x2 = Math.min(maximum, Math.max(minimum, x2));
        const color = this.config.colors && this.config.colors.del
            ? this.config.colors.del
            : ColorProcessor.darkenColor(baseColor);
        graphics
            .lineStyle(1, color, 0)
            .moveTo(
                x1,
                -Math.round(height / 2.0)
            )
            .lineTo(
                x1,
                Math.round(height / 2.0)
            )
            .moveTo(
                x1,
                0
            )
            .lineTo(
                x2,
                0
            )
            .moveTo(
                x2,
                -Math.round(height / 2.0)
            )
            .lineTo(
                x2,
                Math.round(height / 2.0)
            );
    }

    renderNotAlignedMarker (graphics, size, x, direction, options) {
        if (size > 0) {
            const {
                height,
                minimum,
                maximum
            } = options;
            if (x < minimum || x > maximum) {
                return;
            }
            x = Math.min(maximum, Math.max(minimum, x));
            const notAlignedMarkerWidth = this.config.sequence.notAligned.width;
            const margin = this.config.sequence.notAligned.margin;
            graphics
                .lineStyle(1, this.config.sequence.notAligned.color, 1)
                .drawRect(
                    direction < 0 ? (x - notAlignedMarkerWidth) : x,
                    -Math.round(height / 2.0),
                    notAlignedMarkerWidth,
                    height
                );
            const label = new PIXI.Text(
                `${size}`,
                this.config.sequence.notAligned.label,
                drawingConfiguration.resolution
            );
            label.x = Math.round(
                direction < 0
                    ? (x - notAlignedMarkerWidth - label.width - margin)
                    : (x + notAlignedMarkerWidth + margin)
            );
            label.y = Math.round(
                -label.height / 2.0
            );
            this.blastGraphicsContainer.addChild(label);
        }
    }

    renderAlignment (alignment) {
        this.blastGraphicsContainer.removeChildren();
        if (!alignment) {
            return false;
        }
        const {
            btop,
            sequenceStart,
            sequenceEnd,
            queryLength,
            sequenceStrandView
        } = alignment;
        let {
            queryStart,
            queryEnd,
        } = alignment;
        if (!sequenceStart || !sequenceEnd || !this.blastGraphicsContainer) {
            return false;
        }
        const positiveStrand = sequenceStrandView === '+';
        if (!positiveStrand) {
            const temp = queryStart;
            queryStart = queryLength - queryEnd + 1;
            queryEnd = queryLength - temp + 1;
        }
        const start = Math.min(sequenceStart, sequenceEnd);
        const end = Math.max(sequenceStart, sequenceEnd);
        const graphics = new PIXI.Graphics();
        this.blastGraphicsContainer.addChild(graphics);
        const height = this.config.sequence.height;
        const startOffset = this.viewport.project.brushBP2pixel(start);
        const width = this.viewport.project.brushBP2pixel(end) -
            startOffset;
        const bpWidth = this.viewport.factor;
        const baseColor = this.config.colors && this.config.colors.base
            ? this.config.colors.base
            : this.config.sequence.color;
        const minimum = -this.viewport.canvasSize;
        const maximum = 2 * this.viewport.canvasSize;
        const options = {
            height,
            minimum,
            maximum,
            bpWidth,
            baseColor
        };
        this.renderStrandMarker(
            graphics,
            positiveStrand
                ? (width + bpWidth / 2.0)
                : (-bpWidth / 2.0),
            positiveStrand ? 1 : -1,
            options
        );
        const btopParsed = parseBtop(btop);
        let relativePosition = start;
        for (let p = 0; p < btopParsed.length; p++) {
            const part = positiveStrand ? btopParsed[p] : btopParsed[btopParsed.length - 1 - p];
            const x = this.viewport.project.brushBP2pixel(
                relativePosition
            ) - bpWidth / 2.0 - startOffset;
            const x2 = this.viewport.project.brushBP2pixel(
                relativePosition + (part.length || 0)
            ) - bpWidth / 2.0 - startOffset;
            switch (part.type) {
                case BTOPPartType.match:
                    this.renderMatch(graphics, x, x2, options);
                    break;
                case BTOPPartType.mismatch:
                    this.renderMisMatch(graphics, x, x2, part.alt, options);
                    break;
                case BTOPPartType.insertion:
                    this.renderInsertion(graphics, x, part.alt, options);
                    break;
                case BTOPPartType.deletion:
                    this.renderDeletion(graphics, x, x2, options);
                    break;
            }
            relativePosition += (part.length || 0);
        }
        this.renderNotAlignedMarker(
            graphics,
            queryStart - 1,
            -bpWidth / 2.0,
            -1,
            options
        );
        this.renderNotAlignedMarker(
            graphics,
            queryLength - queryEnd,
            width + bpWidth / 2.0,
            1,
            options
        );
        return true;
    }

    moveAlignment (alignment) {
        if (!alignment) {
            return;
        }
        const {
            sequenceStart,
            sequenceEnd
        } = alignment;
        if (!sequenceStart || !sequenceEnd || !this.blastGraphicsContainer) {
            return;
        }
        const start = Math.min(sequenceStart, sequenceEnd);
        this.blastGraphicsContainer.x = this.viewport.project.brushBP2pixel(start);
        this.blastGraphicsContainer.y = Math.round(this.height / 2.0);
    }

    render (alignment, searchResults, flags, features) {
        this._state = features;
        if (flags.renderReset) {
            this.initialize();
        }
        const didSomethingChange = flags.renderReset
            || flags.widthChanged
            || flags.heightChanged
            || flags.brushChanged
            || flags.dataChanged
            || flags.renderFeaturesChanged
            || flags.dragFinished;
        if (
            flags.renderReset ||
            flags.dataChanged ||
            flags.dragFinished ||
            flags.renderFeaturesChanged
        ) {
            this.renderAlignment(alignment, searchResults);
        }
        if (didSomethingChange) {
            this.moveAlignment(alignment);
            this.renderCenterLine(features);
        }
        return didSomethingChange || flags.hoverChanged;
    }
}

export default BLASTAlignmentRenderer;
