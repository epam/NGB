import commonMenu from '../../common/menu';
import coverageColorPerform from './coverageColorPerform';
import {getDivider} from '../../../utilities/menu';

export default {
    fields:[
        {
            label: 'Color',
            name: 'coverage>color',
            perform: coverageColorPerform,
            type: 'button'
        },
        getDivider(),
        ...commonMenu
    ],
    label:'General',
    name:'coverage>general',
    type: 'submenu'
};