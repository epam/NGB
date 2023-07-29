import processLinks from '../../../components/ngbTargetPanel/utilities/process-links';

class NgbChatController {
    static get UID() {
        return 'ngbChatController';
    }

    constructor($scope, $element, $sce) {
        this.$sce = $sce;
        this.currentMessage = '';
        this.messagesContainer = $element.find('.ngb-chat-messages')[0];
    }

    get myMessages() {
        return (this.messages || []).filter((msg) => msg.my);
    }

    $onInit() {
        this.startScrollCheck();
    }

    $onDestroy() {
        this.stopScrollCheck();
    }

    stopScrollCheck() {
        cancelAnimationFrame(this.raf);
    }

    getMessageHtml (message) {
        return this.$sce.trustAsHtml(processLinks(message));
    }

    startScrollCheck() {
        this.stopScrollCheck();
        let previousMessagesCount = 0;
        const check = () => {
            const current = (this.messages || []).length;
            if (
                current !== previousMessagesCount &&
                this.messagesContainer &&
                this.messagesContainer.clientHeight > 0
            ) {
                previousMessagesCount = current;
                const {
                    clientHeight,
                    scrollHeight
                } = this.messagesContainer;
                this.messagesContainer.scrollTo({
                    top: Math.max(0, scrollHeight - clientHeight),
                    behavior: 'smooth'
                });
            } else if (
                !this.messagesContainer ||
                !this.messagesContainer.clientHeight
            ) {
                previousMessagesCount = undefined;
            }
            this.raf = requestAnimationFrame(check);
        }
        check();
    }

    onCancel () {
        this._messageIdx = undefined;
        this.currentMessage = undefined;
    }

    onEnter () {
        this._messageIdx = undefined;
        if (this.currentMessage && typeof this.onSendMessage === 'function') {
            this.onSendMessage(this.currentMessage);
            this.currentMessage = undefined;
        }
    }

    increaseArrowIndex() {
        if (this._messageIdx === undefined) {
            this._messageIdx = 0;
        } else {
            this._messageIdx += 1;
        }
        if (this._messageIdx >= this.myMessages.length) {
            this._messageIdx = undefined;
        }
        this.substituteMessage();
    }

    decreaseArrowIndex() {
        const myMessages = this.myMessages;
        if (this._messageIdx > 0) {
            this._messageIdx -= 1;
        } else if (this._messageIdx === 0) {
            this._messageIdx = undefined;
        } else if (myMessages.length > 0) {
            this._messageIdx = myMessages.length - 1;
        }
        this.substituteMessage();
    }

    substituteMessage() {
        if (this._messageIdx === undefined) {
            this.currentMessage = undefined;
        } else {
            const msg = this.myMessages.reverse()[this._messageIdx];
            this.currentMessage = msg ? msg.content : undefined;
        }
    }

    onKeyPress (event) {
        switch ((event.code || '').toLowerCase()) {
            case 'enter':
                this.onEnter();
                break;
            case 'escape':
            case 'esc':
                this.onCancel();
                break;
            case 'arrowup':
                this.increaseArrowIndex();
                break;
            case 'arrowdown':
                this.decreaseArrowIndex();
                break;
            default:
                break;
        }
    }
}

export default NgbChatController;
