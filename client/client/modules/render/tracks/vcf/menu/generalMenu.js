import {menu} from '../../../utilities';

export default {
    fields:[
        menu.getActionButton(
          'coverage>font',
          'Font size',
          (config, dispatcher, state) => {
              dispatcher.emitSimpleEvent('tracks:header:style:configure', {
                  config: {
                      defaults: config.header,
                      settings: {
                          ...config.header,
                          ...state.header
                      },
                  },
                  source: config.name,
              });
          }
        ),
    ],
    label:'General',
    name:'vcf>general',
    type: 'submenu'
};
