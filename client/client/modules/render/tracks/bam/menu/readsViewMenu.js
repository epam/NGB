import {getDivider} from './divider';
import {readsViewTypes} from '../modes';

export default {
    displayAdditionalName: state => {
        if (state.viewAsPairs) {
            return 'View as pairs';
        }
    },
    displayName: state => {
        switch (state.readsViewMode) {
            case readsViewTypes.readsViewCollapsed:
                return 'Collapsed';
            case readsViewTypes.readsViewExpanded:
                return 'Expanded';
            case readsViewTypes.readsViewAutomatic:
                return 'Automatic';
        }
    },
    fields: [
        {
            disable: () => {},
            enable: state => state.readsViewMode = readsViewTypes.readsViewCollapsed,
            isEnabled: state => state.readsViewMode === readsViewTypes.readsViewCollapsed,
            label: 'Collapsed',
            name: 'bam>readsView>collapsed',
            type: 'checkbox'
        },
        {
            disable: () => {},
            enable: state => state.readsViewMode = readsViewTypes.readsViewExpanded,
            isEnabled: state => state.readsViewMode === readsViewTypes.readsViewExpanded,
            label: 'Expanded',
            name: 'bam>readsView>expanded',
            type: 'checkbox'
        },
        {
            disable: () => {},
            enable: state => state.readsViewMode = readsViewTypes.readsViewAutomatic,
            isEnabled: state => state.readsViewMode === readsViewTypes.readsViewAutomatic,
            label: 'Automatic',
            name: 'bam>readsView>automatic',
            type: 'checkbox'
        },
        getDivider(),
        {
            disable: state => state.viewAsPairs = false,
            enable: state => state.viewAsPairs = true,
            isEnabled: state => state.viewAsPairs,
            label: 'View as pairs',
            name: 'bam>readsView>pairs',
            type: 'checkbox'
        }
    ],
    label: 'Reads view',
    name: 'bam>readsView',
    type: 'submenu'
};