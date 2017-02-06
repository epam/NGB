export default  {
    controller: class ngbHeaderProjectController {
        constructor(projectContext) {
            this.toolbarVisibility = projectContext.toolbarVisibility;
        }
    },
    template: require('./ngbMainToolbar.tpl.html')
};
