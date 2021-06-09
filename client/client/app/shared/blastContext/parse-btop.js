const match = /[\d]+/;
const mismatch = /[a-z][a-z]/i;
const deletion = /\-[a-z]/i;
const insertion = /[a-z]\-/i;

const parts = [
    match,
    mismatch,
    deletion,
    insertion
]
    .map(regExp => regExp.source)
    .join('|');

const BTOP = new RegExp(`(${parts})`, 'ig');

const BTOPPartType = {
    match: 'match',
    mismatch: 'mismatch',
    deletion: 'deletion',
    insertion: 'insertion'
};

export {BTOPPartType};

export default function parseBtop (btop) {
    if (!btop || typeof btop !== 'string') {
        return [];
    }
    const result = [];
    let exec = BTOP.exec(btop);
    while (exec) {
        const part = exec[1];
        if (match.test(part)) {
            result.push({
                type: BTOPPartType.match,
                length: +part
            });
        } else if (mismatch.test(part)) {
            result.push({
                type: BTOPPartType.mismatch,
                ref: part[1],
                alt: part[0],
                length: 1
            });
        } else if (deletion.test(part)) {
            result.push({
                type: BTOPPartType.deletion,
                length: 1
            });
        } else if (insertion.test(part)) {
            result.push({
                type: BTOPPartType.insertion,
                length: 0,
                alt: part[0]
            });
        }
        exec = BTOP.exec(btop);
    }
    return result;
}
