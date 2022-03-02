import {ColorFormats, HeatmapColorSchemes, HeatmapDataType} from '../../../../modules/render/heatmap';

const colorPickerOptions = {
    format: 'hex',
    pos: 'bottom right',
    swatch: true,
    swatchOnly: true,
};

export default {
    schemes: Object.values(HeatmapColorSchemes),
    dataTypes: HeatmapDataType,
    colorFormats: ColorFormats,
    HeatmapColorSchemes,
    schemeName (scheme) {
        switch (scheme) {
            case HeatmapColorSchemes.discrete:
                return 'Discrete';
            case HeatmapColorSchemes.continuous:
            default:
                return 'Continuous';
        }
    },
    colorPickerOptions,
    colorPickerOptionsLeft: {
        ...colorPickerOptions,
        pos: 'bottom right'
    },
    colorPickerOptionsRight: {
        ...colorPickerOptions,
        pos: 'bottom left'
    }
};
