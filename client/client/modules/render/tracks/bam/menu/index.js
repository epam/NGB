import {default as colorModeMenu} from './colorModeMenu';
import {default as coverageMenu, coverageStateMutators} from './coverageMenu';
import {default as generalMenu} from './generalMenu';
import {default as groupModeMenu} from './groupModeMenu';
import {default as readsViewMenu} from './readsViewMenu';
import {default as sortingModeMenu} from './sortingModeMenu';

const sashimiMenu = [];

export {sashimiMenu, coverageStateMutators};
export default [
    generalMenu,
    colorModeMenu,
    groupModeMenu,
    readsViewMenu,
    sortingModeMenu,
    coverageMenu
];
