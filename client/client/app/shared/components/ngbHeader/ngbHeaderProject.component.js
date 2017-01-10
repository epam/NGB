export default  {
    controller: class ngbHeaderProjectController {
        constructor(projectContext) {
            this.toolbarVisibility = projectContext.toolbarVisibility;
        }
    },
    template: require('./ngbHeaderProject.tpl.html')
};
