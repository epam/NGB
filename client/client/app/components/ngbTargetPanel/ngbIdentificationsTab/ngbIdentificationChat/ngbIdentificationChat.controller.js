class NgbIdentificationChatController {
    static get UID() {
        return 'ngbIdentificationChatController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbTargetPanelService,
        ngbIdentificationChatService,
    ) {
        this.$scope = $scope;
        this.$timeout = $timeout;
        this.ngbTargetPanelService = ngbTargetPanelService;
        this.service = ngbIdentificationChatService;
        this.onSendMessageCallback = this.onSendMessage.bind(this);
        const apply = () => $scope.$apply();
        dispatcher.on('target:identification:publications:chat:initialized', apply);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:identification:publications:chat:initialized', apply);
        });
    }

    get chatName() {
        if (
            this.ngbTargetPanelService &&
            this.ngbTargetPanelService.identificationTarget &&
            this.ngbTargetPanelService.identificationTarget.target &&
            this.ngbTargetPanelService.identificationTarget.target.name
        ) {
            return `${this.ngbTargetPanelService.identificationTarget.target.name} - Generative AI Chat`;
        }
        return 'Generative AI Chat';
    }

    get loading() {
        return this.service ? this.service.pending : false;
    }

    get messagePending() {
        return this.service ? this.service.messagePending : false;
    }

    get error() {
        return this.service ? this.service.error : undefined;
    }

    get messages() {
        return this.service ? this.service.messages : [];
    }

    $onDestroy() {
        this.service.finishSession();
    }

    onSendMessage (message) {
        if (message && this.service) {
            this.service.askQuestion(message)
                .then(() => {
                    this.$scope.$apply();
                });
            this.$timeout(() => this.$scope.$apply());
        }
    }
}

export default NgbIdentificationChatController;
