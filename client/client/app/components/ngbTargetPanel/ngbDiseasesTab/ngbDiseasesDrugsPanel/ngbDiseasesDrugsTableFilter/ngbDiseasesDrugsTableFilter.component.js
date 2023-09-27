import controller from './ngbDiseasesDrugsTableFilter.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbDiseasesDrugsTableFilter.tpl.html')
};
