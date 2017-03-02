import {NumberFormatter} from '../../../../../../modules/render/utilities';

export default function() {
    return function (value) {
        if (value !== undefined) {
            return NumberFormatter.textWithPrefix(value, true, {
                'units': ' bytes',
                'K': ' KB',
                'M': ' MB',
                'G': ' GB',
                'T': ' TB',
                'P': ' PB'
            });
        }
        return value;
    };
}