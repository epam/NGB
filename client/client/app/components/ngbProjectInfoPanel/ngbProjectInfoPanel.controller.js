export default class ngbProjectInfoPanelController {
    /**
     * @returns {string}
     */
    static get UID() {
        return 'ngbProjectInfoPanelController';
    }

    projectContext;
    isDescriptionFile = true;
    blob_iframe = {};

    /**
     * @constructor
     */
    /** @ngInject */
    constructor(projectContext, $element, $timeout) {
        this.projectContext = projectContext;
        this.$element = $element;
        this.$timeout = $timeout;
    }

    $onInit() {
        if (this.isDescriptionFile) {
            const blob = new Blob([], {type: 'text/html'});
            this.$timeout(() => {
                this.blob_iframe = this.$element.find('#description_file');
                this.blob_iframe[0].src = URL.createObjectURL(blob);
            });
        }
    }

    get containsVcfFiles() {
        return this.projectContext.containsVcfFiles && !this.projectContext.variantsGroupError;
    }
}
