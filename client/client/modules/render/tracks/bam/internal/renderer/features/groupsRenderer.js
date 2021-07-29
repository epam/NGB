import * as PIXI from 'pixi.js';
import {drawingConfiguration} from '../../../../../core';
import {groupModes} from '../../../modes';

const Math = window.Math;

function labelsIntersects(labelA, labelB){
    return ((labelA.x >= labelB.x && labelA.x <= labelB.x + labelB.width) ||
        (labelA.x + labelA.width >= labelB.x && labelA.x + labelA.width <= labelB.x + labelB.width)) &&
        ((labelA.y >= labelB.y && labelA.y <= labelB.y + labelB.height) ||
        (labelA.y + labelA.height >= labelB.y && labelA.y + labelA.height <= labelB.y + labelB.height));
}

export function renderGroups(features, groups, drawingConfig) {
    const {
        alignmentRowHeight,
        config,
        groupNamesContainer,
        groupsBackground,
        groupsSeparatorGraphics,
        height,
        scrollY,
        topMargin,
        viewport} = drawingConfig;
    groupsSeparatorGraphics.clear();
    if (features.groupMode === groupModes.defaultGroupingMode) {
        return;
    }

    const backgroundGraphics = new PIXI.Graphics();

    groupsBackground.addChild(backgroundGraphics);

    const graphics = new PIXI.Graphics();
    groupNamesContainer.addChild(graphics);

    const yElementOffset = alignmentRowHeight === 1 ? 0 : config.yElementOffset;
    const yScale = alignmentRowHeight - yElementOffset;

    const yOffset = scrollY * alignmentRowHeight - yElementOffset;

    const projectY = y => (Math.round(yScale / 2 - yOffset + y * alignmentRowHeight + topMargin));

    const labels = [];
    for (let i = 0; i < groups.length; i++) {
        const group = groups[i];
        const label = new PIXI.Text(group.displayName.toUpperCase(), config.groupNames.label);
        label.resolution = drawingConfiguration.resolution;
        labels.push(label);
    }
    const labelsPositions = [];
    const submitLabel = (label) => {
        let intersects = labelsPositions.length > 0;
        while (intersects) {
            intersects = false;
            for (let i = 0; i < labelsPositions.length; i++) {
                const candidateLabel = labelsPositions[i];
                if (labelsIntersects(candidateLabel, label)) {
                    intersects = true;
                    label.x = candidateLabel.x + candidateLabel.width + 1;
                    break;
                }
            }
        }
        labelsPositions.push(label);
        return label;
    };

    const BP_OFFSET = 0.5;

    for (let i = 0; i < groups.length; i++) {
        const group = groups[i];
        const label = labels[i];
        const y = features.viewAsPairs ? group.previousPairedLinesCount : group.previousLinesCount;
        const yEnd = y + (features.viewAsPairs ? group.pairedLines.length : group.lines.length);
        if (yEnd < scrollY)
            continue;
        const yPx = Math.max(topMargin, projectY(y - BP_OFFSET) - config.groupNames.margin.y);
        if (i % 2 === 1) {
            backgroundGraphics
                .lineStyle(0, 0x000000, 0)
                .beginFill(config.groupNames.oddFill, 1)
                .drawRect(
                    0,
                    projectY(y - BP_OFFSET),
                    viewport.canvasSize,
                    projectY(yEnd - BP_OFFSET) - projectY(y - BP_OFFSET))
                .endFill();
            if (yScale > config.groupNames.separator.hideThreshold) {
                groupsSeparatorGraphics
                    .lineStyle(config.groupNames.separator.strokeThickness,
                        config.groupNames.separator.stroke,
                        config.groupNames.separator.strokeAlpha)
                    .moveTo(0, projectY(y - BP_OFFSET) - config.groupNames.separator.strokeThickness / 2)
                    .lineTo(viewport.canvasSize,
                        projectY(y - BP_OFFSET) - config.groupNames.separator.strokeThickness / 2)
                    .moveTo(0,
                        projectY(yEnd - BP_OFFSET) - config.groupNames.separator.strokeThickness / 2)
                    .lineTo(viewport.canvasSize,
                        projectY(yEnd - BP_OFFSET) - config.groupNames.separator.strokeThickness / 2);
            }
        }

        const labelRect = submitLabel({
            height: label.height + 2 * config.groupNames.margin.y,
            width: label.width + 2 * config.groupNames.margin.x,
            x: Math.round(config.groupNames.offset) - config.groupNames.margin.x,
            y: yPx,
        });

        label.x = Math.round(labelRect.x + config.groupNames.margin.x);
        label.y = Math.round(labelRect.y + config.groupNames.margin.y);
        graphics
            .beginFill(config.groupNames.background.fill, config.groupNames.background.alpha)
            .lineStyle(config.groupNames.background.strokeThickness, config.groupNames.background.stroke, config.groupNames.background.alpha)
            .drawRoundedRect(
                labelRect.x + config.groupNames.background.strokeThickness / 2,
                labelRect.y + config.groupNames.background.strokeThickness / 2,
                labelRect.width,
                labelRect.height,
                labelRect.height / 2
            )
            .endFill();
        groupNamesContainer.addChild(label);
    }
    backgroundGraphics.endFill();
    const mask = new PIXI.Graphics();
    mask.beginFill(0x000000, 1)
        .drawRect(0, topMargin, viewport.canvasSize, height - topMargin)
        .endFill();
    groupNamesContainer.mask = mask;
    groupsBackground.mask = mask;
    groupsSeparatorGraphics.mask = mask;
}
