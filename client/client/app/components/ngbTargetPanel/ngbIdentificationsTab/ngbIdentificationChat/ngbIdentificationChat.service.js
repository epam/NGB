import {LLMName} from '../../../../shared/components/ngbLLM/models';

class NgbIdentificationChatService {
    static instance(
        dispatcher,
        targetLLMService,
        ngbTargetPanelService,
        targetDataService
    ) {
        return new NgbIdentificationChatService(
            dispatcher,
            targetLLMService,
            ngbTargetPanelService,
            targetDataService
        );
    }

    static get UID() {
        return 'ngbIdentificationChatService';
    }

    constructor(
        dispatcher,
        targetLLMService,
        ngbTargetPanelService,
        targetDataService
    ) {
        this.dispatcher = dispatcher;
        this.ngbTargetPanelService = ngbTargetPanelService;
        this.modelService = targetLLMService;
        this.targetDataService = targetDataService;
        this._abstractsRequest = undefined;
        this._session = undefined;
        this._dialog = [];
        this._context = [];
        this._pending = false;
        this._messagePending = false;
        this._error = undefined;
        this._requestToken = 0;
        this.dispatcher.on('target:identification:changed', this.targetChanged.bind(this, true));
        this.dispatcher.on('target:identification:publications:model:changed', this._onModelChanged.bind(this));
    }

    get initialized() {
        return !!this._session;
    }

    get pending() {
        return this._pending;
    }

    set pending(pending) {
        this._pending = pending;
    }

    get messagePending() {
        return this._messagePending;
    }

    set messagePending(messagePending) {
        this._messagePending = messagePending;
    }

    get messages() {
        return this._dialog;
    }

    get error() {
        return this._error;
    }

    _onModelChanged(model) {
        if (this._dialog.length > 0 && model) {
            this._dialog.push({
                content: `Model changed to <b>${LLMName[model.type] || model.type}</b>`,
                system: true
            });
        }
        if (model) {
            (this.finishSession)();
        }
    }

    _increaseRequestToken() {
        this._requestToken += 1;
        return this._requestToken;
    }

    _createRequestCommitPhase() {
        const token = this._increaseRequestToken();
        return (fn) => {
            if (token === this._requestToken && typeof fn === 'function') {
                fn();
            }
        };
    }

    targetChanged() {
        this._abstractsRequest = undefined;
        this.reset(true);
    }

    reset(clearDialog = true) {
        this._increaseRequestToken();
        this._session = undefined;
        this._pending = false;
        this._messagePending = false;
        this._context = [];
        if (clearDialog) {
            this._dialog = [];
        }
        this._error = undefined;
    }

    async buildAbstracts() {
        if (!this._abstractsRequest) {
            if (
                this.ngbTargetPanelService &&
                this.ngbTargetPanelService.genesIds &&
                this.ngbTargetPanelService.genesIds.length > 0
            ) {
                this._abstractsRequest = new Promise((resolve, reject) => {
                    const genes = this.ngbTargetPanelService.genesIds;
                    this.targetDataService.getAbstracts(
                        {geneIds: genes}
                    )
                        .then(resolve)
                        .catch(reject);
                });
            } else {
                throw new Error('Target genes not specified');
            }
        }
        return this._abstractsRequest;
    }

    async startSession(clearPreviousSession = true) {
        await this.finishSession();
        this.reset(clearPreviousSession);
        const commit = this._createRequestCommitPhase();
        this._pending = true;
        try {
            if (!this.modelService.model) {
                throw new Error('LLM model not specified')
            }
            const genes = this.ngbTargetPanelService.genesIds;
            if (genes.length === 0) {
                throw new Error('Target genes not specified');
            }
            const abstracts = await this.buildAbstracts();
            if (!abstracts || !abstracts.length) {
                throw new Error('Could not get abstracts for selected targets');
            }
            commit(() => {
                this._context.push({
                    role: 'USER',
                    content: abstracts
                });
                this._session = abstracts;
                this.dispatcher.emit('target:identification:publications:chat:initialized');
            });
        } catch (error) {
            console.warn(error.message);
            commit(() => {
                this._dialog.push({
                    content: error.message,
                    error: true
                });
                this._error = error.message;
            });
        } finally {
            commit(() => {
                this._pending = false;
            });
        }
    }

    async finishSession() {
        if (this._session) {
            this._createRequestCommitPhase();
            this._pending = false;
            this._session = undefined;
            this._messagePending = false;
        }
    }

    async askQuestion(question) {
        this._messagePending = true;
        this._dialog.push({
            content: question,
            my: true,
        });
        if (!this._session) {
            await this.startSession(false);
            if (!this._session) {
                this._messagePending = false;
                return;
            }
        }
        this._messagePending = true;
        const commit = this._createRequestCommitPhase();
        this._context.push({
            role: 'USER',
            content: question
        });
        try {
            const answer = await this.targetDataService.llmChat(
                this._context,
                this.modelService.model.type
            );
            commit(() => {
                this._context.push({
                    role: 'ASSISTANT',
                    content: answer
                })
                this._dialog.push({
                    content: answer
                });
                this.dispatcher.emit('target:identification:publications:chat:answer');
            });
        } catch (error) {
            console.warn(error.message);
            commit(() => {
                this._dialog.push({
                    content: error.message,
                    error: true
                });
                this._error = error.message;
            });
        } finally {
            commit(() => {
                this._messagePending = false;
            });
        }
    }
}

export default NgbIdentificationChatService;
