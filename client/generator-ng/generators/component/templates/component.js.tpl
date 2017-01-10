import controller from './<%= name %>.controller';

export default  {
    bindings: {},
    controller: controller.UID,
    template: require('./<%= name %>.tpl.html')
};

