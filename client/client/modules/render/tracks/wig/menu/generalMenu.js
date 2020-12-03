import coverageColorPerform from './coverageColorPerform';
import coverageResizePerform from './coverageResizePerform';
import header from '../../common/menu/header';

export default {
    fields:[
        {
            perform: coverageResizePerform,
            label: 'Resize',
            name: 'coverage>resize',
            type: 'button'
        },
        {
            perform: coverageColorPerform,
            label: 'Color',
            name: 'coverage>color',
            type: 'button'
        },
        header
    ],
    label:'General',
    name:'coverage>general',
    type: 'submenu'
};