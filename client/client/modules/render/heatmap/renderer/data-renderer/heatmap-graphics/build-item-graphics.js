import {ColorFormats} from '../../../color-scheme';

/**
 *
 * @param {HeatmapDataItemWithSize} item
 * @param {PIXI.Graphics} graphics
 * @param {ColorScheme} colorScheme
 */
export default function buildItemGraphics(item, graphics, colorScheme) {
    if (!item || !graphics || !colorScheme || !colorScheme.initialized) {
        return;
    }
    const colorFormat = colorScheme.colorFormat;
    colorScheme.colorFormat = ColorFormats.number;
    const color = colorScheme.getColorForValue(item.value, true);
    if (color === undefined) {
        return;
    }
    colorScheme.colorFormat = colorFormat;
    graphics.cacheAsBitmap = false;
    graphics
        .beginFill(color, 1)
        .drawRect(
            item.column,
            item.row,
            item.width || 1,
            item.height || 1
        )
        .endFill();
}
