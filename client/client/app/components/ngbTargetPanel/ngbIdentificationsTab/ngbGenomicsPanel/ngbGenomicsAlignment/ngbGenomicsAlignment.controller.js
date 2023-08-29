import angular from 'angular';

const MONOSPACE_RATIO = 1.75;
const MIN_SEQ_PART_LENGTH = 10;
const SEQUENCE_RIGHT_MARGIN = 20;
const DEFAULT_SYMBOL_WIDTH = 7;
const CONTAINER_MARGIN_AND_PADDING = (20 + 10) * 2;
const ADDITIONAL_TITLE_SIGNS = 4;

const SELECT_MESSAGE = 'Select gene and protein for target and query.';
const RESIZE_MESSAGE = 'Resize window to see the results';

export default class ngbGenomicsAlignmentController {

    maxSeqLength = MIN_SEQ_PART_LENGTH;
    partedAlignment = [];
    windowElm = {};
    warningMessage = SELECT_MESSAGE;

    static get UID() {
        return 'ngbGenomicsAlignmentController';
    }

    constructor($scope, $element, $window, $timeout, dispatcher, ngbGenomicsPanelService) {
        Object.assign(this, {$scope, $element, $timeout, dispatcher, ngbGenomicsPanelService});
        this.initialize();
        this.windowElm = angular.element($window);

        this.$scope.$on('$destroy', () => {
            this.windowElm.off('resize', this.onResize.bind(this));
        });
        this.dispatcher.on('target:identification:alignment:updated', this.initialize.bind(this));
    }

    get alignment() {
        return this.ngbGenomicsPanelService.alignment;
    }

    $onInit() {
        this.windowElm.on('resize', this.onResize.bind(this));
    }

    initialize(isUpdated) {
        if (this.alignment) {
            this.alignment.length = this.calculateLength(this.alignment);
            this.alignment.diff = this.calculateDiff(this.alignment);
            this.alignment.numIdentity = this.calculateNumIdentity(this.alignment);
            this.alignment.gaps = this.calculateGap(this.alignment);
        }
        this.onResize(isUpdated);
        this.$timeout(() => this.$scope.$apply());
    }

    splitAlignment(alignment) {
        const result = [];
        const {targetSequence, querySequence} = alignment;
        const maxLength = Math.max(targetSequence.length, querySequence.length);
        if (maxLength > this.maxSeqLength) {
            const size = Math.ceil(maxLength / this.maxSeqLength);
            const splitRegExp = new RegExp(`.{1,${this.maxSeqLength}}`, 'g');
            const splitQuerySequence = alignment.querySequence.match(splitRegExp);
            const splitDiff = alignment.diff.match(splitRegExp);
            const splitTarget = alignment.targetSequence.match(splitRegExp);
            const inv = alignment.targetStart > alignment.targetEnd;
            for (let i = 0; i < size; i++) {
                result.push({
                    targetName: alignment.targetName,
                    targetTooltip: alignment.targetTooltip,
                    queryName: alignment.queryName,
                    queryTooltip: alignment.queryTooltip,
                    querySequence: splitQuerySequence[i],
                    diff: splitDiff[i],
                    targetSequence: splitTarget[i],
                    queryStart: alignment.queryStart + i * this.maxSeqLength,
                    queryEnd: Math.min(alignment.queryEnd, alignment.queryStart + (i + 1) * this.maxSeqLength - 1),
                    targetStart: inv ? alignment.targetStart - i * this.maxSeqLength : alignment.targetStart + i * this.maxSeqLength,
                    targetEnd: inv
                        ? Math.max(alignment.targetEnd, alignment.targetStart - (i + 1) * this.maxSeqLength + 1)
                        : Math.min(alignment.targetEnd, alignment.targetStart + (i + 1) * this.maxSeqLength - 1)
                });
            }
        } else {
            result.push(alignment);
        }
        return result;
    }

    calculateDiff(alignment) {
        const {targetSequence, querySequence} = alignment;
        let result = '';
        for (let i = 0; i < alignment.length; i++) {
            const target = targetSequence[i] || '-';
            const query = querySequence[i] || '-';
            if (target === '-' || query === '-') {
                result += ' ';
            } else if (target !== query) {
                result += '*';
            } else {
                result += '|'
            }
        }
        return result;
    }

    calculateLength(alignment) {
        return Math.max(alignment.targetSequence.length, alignment.querySequence.length);
    }

    calculateGap(alignment) {
        return (alignment.diff.match(/ /g) || []).length;
    }

    calculateNumIdentity(alignment) {
        return (alignment.diff.match(/[^\* ]/g) || []).length;
    }

    onResize(isUpdated) {
        const element = angular.element(this.$element);
        if (!element.width() || !this.alignment) return;
        this.sequence = element.find('.genomics_alignment_sequence');
        const titleCollection = document.getElementsByClassName('genomics_alignment_query_title');
        const containerWidth = element.width() - CONTAINER_MARGIN_AND_PADDING - SEQUENCE_RIGHT_MARGIN;
        let newWidth;
        if (titleCollection && titleCollection.length) {
            newWidth = containerWidth
                - titleCollection[titleCollection.length - 1].offsetWidth
        } else {
            const longestTitle = Math.max(this.alignment.targetName.length, this.alignment.queryName.length);
            newWidth = (containerWidth
                - (this.alignment.targetEnd.toString().length * 2
                    + ADDITIONAL_TITLE_SIGNS
                    + longestTitle
                ) * DEFAULT_SYMBOL_WIDTH
            );
        }
        if (newWidth < 30) {
            this.partedAlignment = [];
            this.warningMessage = RESIZE_MESSAGE;
            return;
        }
        const symbolWidth = Math.ceil(
            parseInt(this.sequence.css('font-size'), 10) / MONOSPACE_RATIO
            ) || DEFAULT_SYMBOL_WIDTH;
        const newMaxSeqLength = Math.max(Math.ceil(newWidth / symbolWidth), MIN_SEQ_PART_LENGTH);
        if (this.maxSeqLength !== newMaxSeqLength || isUpdated) {
            this.maxSeqLength = newMaxSeqLength;
            this.partedAlignment = this.splitAlignment(this.alignment);
        }
    }
}
