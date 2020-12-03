import {default as colorModeMenu} from './colorModeMenu';
import {default as displayModesMenu} from '../../wig/menu/displayModesMenu';
import {default as generalMenu} from './generalMenu';
import {default as groupModeMenu} from './groupModeMenu';
import {default as readsViewMenu} from './readsViewMenu';
import {default as scaleModesMenu} from '../../wig/menu/scaleModesMenu';
import {default as sortingModeMenu} from './sortingModeMenu';

const sashimiFieldsAllowed = [
    'coverage>font',
];

const sashimiMenu = [
    Object.assign({}, generalMenu, {
        fields: generalMenu.fields
          .filter(field => sashimiFieldsAllowed.includes(field.name))
    }),
];

const coverageDisplayModesMenu = Object.assign({}, displayModesMenu, {label: 'Coverage Display'});

export {sashimiMenu};
export default [
    colorModeMenu,
    groupModeMenu,
    readsViewMenu,
    sortingModeMenu,
    generalMenu,
    coverageDisplayModesMenu,
    scaleModesMenu
];
