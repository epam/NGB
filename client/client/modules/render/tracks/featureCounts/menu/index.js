import {default as displayModeMenu} from './displayModeMenu';
import {default as generalMenu} from './generalMenu';
import {default as scaleModesMenu, scaleModesStateMutators} from './scaleModesMenu';
import {default as sourcesMenu} from './sourcesMenu';

export {scaleModesStateMutators};
export default [generalMenu, displayModeMenu, scaleModesMenu, sourcesMenu];
