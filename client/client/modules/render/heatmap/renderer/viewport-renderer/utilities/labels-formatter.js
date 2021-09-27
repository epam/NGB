import config from '../config';

function splitWord (word, offset = 1) {
    const threshold = config.label.wrap.lineCharsCount;
    if (word.length > threshold + offset) {
        return [word.slice(0, threshold), ...splitWord(word.slice(threshold), offset)];
    }
    return [word];
}

export default function splitLabel(label) {
    if (!label) {
        return [''];
    }
    const threshold = config.label.wrap.lineCharsCount;
    const words = label.split(' ').filter(o => o.length > 0);
    if (words.length > 1) {
        const lines = [];
        const lineSize = (...line) => line.reduce((r, c) => r + c.length, 0) + line.length - 1;
        for (let w = 0; w < words.length; w++) {
            const word = words[w];
            if (lines.length === 0) {
                lines.push([]);
            }
            const splittedWord = splitWord(word, threshold / 2.0);
            for (let s = 0; s < splittedWord.length; s++) {
                if (lineSize(...lines[lines.length - 1], splittedWord[s]) > threshold) {
                    lines.push([]);
                }
                lines[lines.length - 1].push(splittedWord[s]);
            }
        }
        return lines.map(line => line.join(' '));
    } else if (label.length > threshold + 1) {
        return splitWord(label);
    }
    return [label];
}
