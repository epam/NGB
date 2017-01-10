import {PixiTextSize} from '../../utilities';
const Math = window.Math;

export default function getRulerHeight(_rulerConfig) {
    let tickSize = _rulerConfig.tick.height + 2 * _rulerConfig.tick.margin +
        PixiTextSize.getTextSize('test', _rulerConfig.tick.label).height;
    if (_rulerConfig.centerTick) {
        tickSize = Math.max(tickSize, _rulerConfig.centerTick.height + 2 * _rulerConfig.centerTick.margin +
            PixiTextSize.getTextSize('test', _rulerConfig.centerTick.label).height);
    }
    return _rulerConfig.body.height + tickSize;
}
