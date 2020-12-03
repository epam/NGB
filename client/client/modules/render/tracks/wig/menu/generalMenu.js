import coverageColorPerform from './coverageColorPerform';
import coverageResizePerform from './coverageResizePerform';

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
        {
            perform: () => {},
            label: 'Font size',
            name: 'coverage>font',
            type: 'button'
        }
    ],
    label:'General',
    name:'coverage>general',
    type: 'submenu'
};