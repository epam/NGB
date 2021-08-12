const NUCLEOTIDES = 'ACGT';
const IUPAC_CHARACTERS = 'ACGTUMRWSYKVHDBN'; //https://droog.gs.washington.edu/mdecode/images/iupac.html

const ALL_CHARACTERS = [...(new Set(NUCLEOTIDES.concat(IUPAC_CHARACTERS).split('')))];

const SEQUENCE_REGEXP = new RegExp(`^[${ALL_CHARACTERS.join('')}]+$`, 'i');

function isValidSequenceRegExpString (regExpString) {
    if (!regExpString) {
        return false;
    }
    const letters = regExpString
        .split('')
        .filter(character => /^[a-zA-Z0-9]$/.test(character))
        .join('');
    if (SEQUENCE_REGEXP.test(letters)) {
        // all letters are valid; we should test if this is valid RegExp string
        try {
            new RegExp(regExpString);
            return true;
        } catch (e) {
            return false;
        }
    }
    return false;
}

export default function () {
    return {
        restrict: 'A',
        require: 'ngModel',
        link(scope, elm, attrs, ngModel) {
            ngModel.$validators.motifSequence = function(modelValue) {
                if (!modelValue) {
                    return false;
                }
                return SEQUENCE_REGEXP.test(modelValue) || isValidSequenceRegExpString(modelValue);
            };
        }
    };
}
