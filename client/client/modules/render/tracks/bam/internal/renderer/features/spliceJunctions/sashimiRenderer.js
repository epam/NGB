import PIXI from 'pixi.js';
import renderArea from './renderArea';
import {NumberFormatter} from '../../../../../../utilities';
import {drawingConfiguration} from '../../../../../../core/configuration';
const Math = window.Math;

const margin = 10;

function intersects(candidate, test) {
    const {start, end} = test;
    const {start: candidateStart, end: candidateEnd} = candidate;
    const s = Math.min(start, candidateStart) - margin;
    const e = Math.max(end, candidateEnd) + margin;
    return e - s < (end - start + candidateEnd - candidateStart + 4 * margin);
}

export function renderSashimiPlot(spliceJunctions, viewport, drawingConfig) {
    const {config, graphics, labelsContainer, shouldRender, hovered} = drawingConfig;
    graphics.clear();
    const {centerLine, height} = renderArea(viewport, drawingConfig);
    if (shouldRender) {
        graphics.lineStyle(config.border.thickness, config.border.stroke, 1);
        const sorted = spliceJunctions
            .filter(item => item.end > viewport.brush.start && item.start < viewport.brush.end)
            .map(spliceJunction => ({
                start: viewport.project.brushBP2pixel(spliceJunction.start),
                end: viewport.project.brushBP2pixel(spliceJunction.end),
                count: spliceJunction.count,
                length: spliceJunction.end - spliceJunction.start,
                hovered: hovered && hovered.start === spliceJunction.start && hovered.end === spliceJunction.end &&
                    hovered.count === spliceJunction.count && hovered.strand === spliceJunction.strand
            }));
        sorted.sort(({length: a}, {length: b}) => a - b);
        let maxLevels = 1;
        for (let i = 0; i < sorted.length; i++) {
            const spliceJunction = sorted[i];
            let up = 0;
            let down = 0;
            for (let j = 0; j < i; j++) {
                if (intersects(spliceJunction, sorted[j])) {
                    const {sashimi} = sorted[j];
                    if (sashimi < 0 && sashimi < down) {
                        down = sashimi;
                    } else if (sashimi > 0 && sashimi > up) {
                        up = sashimi;
                    }
                }
            }
            if (-down < up) {
                spliceJunction.sashimi = down - 1;
            } else {
                spliceJunction.sashimi = up + 1;
            }
            maxLevels = Math.max(Math.abs(spliceJunction.sashimi), maxLevels);
        }
        const levelHeight = Math.max(
            config.levelHeight,
            Math.min(
                config.maxLevelHeight,
                (height / 2.0 - 15) / maxLevels
            )
        );
        const renderArc = (startPx, endPx, level) => {
            const radius = Math.abs(level) * levelHeight;
            const xRadius = (endPx - startPx) / 2;
            const centerPoint = {
                x: (endPx + startPx) / 2,
                y: centerLine
            };
            let steps = (endPx - startPx) / 2;
            if (steps > viewport.canvasSize) {
                steps = viewport.canvasSize;
            } else if (steps < 100) {
                steps = 100;
            }
            const step = Math.PI / steps;
            const color = config.color;
            graphics.lineStyle(2, color, 1);
            const multiplier = level < 0 ? 1 : -1;
            for (let s = 0; s <= steps; s++) {
                if (s === 0) {
                    graphics.moveTo(centerPoint.x + xRadius * Math.cos(s * step), centerPoint.y + multiplier * radius * Math.sin(s * step));
                }
                graphics.lineTo(centerPoint.x + xRadius * Math.cos(s * step), centerPoint.y + multiplier * radius * Math.sin(s * step));
            }
        };
        const renderLabel = (startPx, endPx, level, count) => {
            const radius = Math.abs(level) * levelHeight;
            const multiplier = level < 0 ? 1 : -1;
            const centerPoint = {
                x: (endPx + startPx) / 2,
                y: centerLine
            };
            const label = new PIXI.Text(
                NumberFormatter.textWithPrefix(+count, false),
                config.label
            );
            label.resolution = drawingConfiguration.resolution;
            graphics
                .lineStyle(0, 0x000000, 0)
                .beginFill(0xFFFFFF, 0.5)
                .drawEllipse(
                    centerPoint.x,
                    centerPoint.y + multiplier * (radius + label.height / 2.0),
                    label.width / 2.0,
                    label.height / 2.0)
                .endFill();
            label.x = Math.round(centerPoint.x -label.width / 2);
            label.y = Math.round(centerPoint.y + multiplier * radius - (multiplier < 0 ? 1 : 0) * label.height);
            labelsContainer.addChild(label);
        };
        for (let i = 0; i < sorted.length; i++) {
            const spliceJunctionItem = sorted[i];
            renderArc(
                spliceJunctionItem.start,
                spliceJunctionItem.end,
                spliceJunctionItem.sashimi
            );
        }
        for (let i = 0; i < sorted.length; i++) {
            const spliceJunctionItem = sorted[i];
            renderLabel(
                spliceJunctionItem.start,
                spliceJunctionItem.end,
                spliceJunctionItem.sashimi,
                spliceJunctionItem.count
            );
        }
    }
}
