import angular from 'angular';

const MONOSPACE_RATIO = 1.75;
const DEFAULT_SYMBOL_WIDTH = 11;
const MIN_SEQ_PART_LENGTH = 10;
const SEQUENCE_RIGHT_MARGIN = 20;

export default class ngbBlastSearchAlignment {
    navigationAvailable = false;

    static get UID() {
        return 'ngbBlastSearchAlignment';
    }

    maxSeqLength = MIN_SEQ_PART_LENGTH;
    partedAlignment = [];
    windowElm = {};

    constructor($scope, $element, $window, $timeout, ngbBlastSearchAlignmentService, projectContext, dispatcher) {
        Object.assign(this, {
            $scope,
            $element,
            projectContext,
            dispatcher,
            ngbBlastSearchAlignmentService
        });
        this.navigationAvailable = false;
        this.initialize();
        this.windowElm = angular.element($window);
        const updateNavigationStateFn = this.updateNavigationState.bind(this);
        this.dispatcher.on('tracks:state:change', updateNavigationStateFn);
        this.$scope.$on('$destroy', () => {
            this.windowElm.off('resize', ::this.onResize);
            this.dispatcher.removeListener('tracks:state:change', updateNavigationStateFn);
        });
    }

    async updateNavigationState () {
        this.navigationAvailable = await this.ngbBlastSearchAlignmentService.navigationAvailable(this.alignment, this.search);
    }

    navigateToTracks () {
        this.ngbBlastSearchAlignmentService.navigateToTracks(this.alignment, this.search);
    }

    $onInit() {
        this.windowElm.on('resize', ::this.onResize);
    }

    initialize() {
        switch (this.alignment.sequenceStrand) {
            case 'plus': {
                this.alignment.sequenceStrandView = '+';
                break;
            }
            case 'minus': {
                this.alignment.sequenceStrandView = '-';
                break;
            }
        }
        this.alignment.diff = this.calculateDiff(this.alignment.btop);
        this.updateNavigationState();
    }

    calculateDiff(btop) {
        const splittedBtop = btop.match(/[\d]+|[^\d]+/g);
        let parsedItem, result = '';
        for (let i = 0; i < splittedBtop.length; i++) {
            parsedItem = parseInt(splittedBtop[i]);
            if (isNaN(parsedItem)) {
                result += new Array(Math.ceil(splittedBtop[i].length / 2) + 1).join(' ');
            } else {
                result += new Array(parsedItem + 1).join('|');
            }
        }
        return result;
    }

    splitAlignment(alignment) {
        const result = [];
        if (alignment.sequence.length > this.maxSeqLength) {
            const size = Math.ceil(alignment.sequence.length / this.maxSeqLength);
            const splitRegExp = new RegExp(`.{1,${this.maxSeqLength}}`, 'g');
            const splitQuerySequence = alignment.querySequence.match(splitRegExp);
            const splitDiff = alignment.diff.match(splitRegExp);
            const splitSequence = alignment.sequence.match(splitRegExp);
            const inv = alignment.sequenceStart > alignment.sequenceEnd;
            for (let i = 0; i < size; i++) {
                result.push({
                    querySequence: splitQuerySequence[i],
                    diff: splitDiff[i],
                    sequence: splitSequence[i],
                    queryStart: alignment.queryStart + i * this.maxSeqLength,
                    queryEnd: Math.min(alignment.queryEnd, alignment.queryStart + (i + 1) * this.maxSeqLength - 1),
                    sequenceStart: inv ? alignment.sequenceStart - i * this.maxSeqLength : alignment.sequenceStart + i * this.maxSeqLength,
                    sequenceEnd: inv
                        ? Math.max(alignment.sequenceEnd, alignment.sequenceStart - (i + 1) * this.maxSeqLength + 1)
                        : Math.min(alignment.sequenceEnd, alignment.sequenceStart + (i + 1) * this.maxSeqLength - 1)
                });
            }
        } else {
            result.push({
                querySequence: alignment.querySequence,
                diff: alignment.diff,
                sequence: alignment.sequence,
                queryStart: alignment.queryStart,
                queryEnd: alignment.queryEnd,
                sequenceStart: alignment.sequenceStart,
                sequenceEnd: alignment.sequenceEnd
            });
        }
        return result;
    }

    onResize() {
        const element = angular.element(this.$element);
        this.elements = {
            queryTitle: element.find('.alignment_query_title'),
            startPos: element.find('.alignment_start_pos'),
            sequence: element.find('.alignment_sequence'),
            endPos: element.find('.alignment_end_pos')
        };
        const newWidth = (element.width()
            - this.elements.queryTitle.width()
            - this.elements.startPos.width()
            - this.elements.endPos.width()
            - SEQUENCE_RIGHT_MARGIN
        ) || (element.width() - this.alignment.sequenceEnd.toString().length * 2 * DEFAULT_SYMBOL_WIDTH) / 2;
        if (newWidth < 0) {
            return;
        }
        const symbolWidth = Math.ceil(
            parseInt(this.elements.sequence.css('font-size'), 10) / MONOSPACE_RATIO
        ) || DEFAULT_SYMBOL_WIDTH;
        const newMaxSeqLength = Math.max(Math.ceil(newWidth / symbolWidth), MIN_SEQ_PART_LENGTH);
        if (this.maxSeqLength !== newMaxSeqLength) {
            this.maxSeqLength = newMaxSeqLength;
            this.partedAlignment = this.splitAlignment(this.alignment);
        }
    }
}
