
import fs from 'fs';

import { extractHARData } from './lib.js';

const harFile = fs.readdirSync('./').find((name) => {
    return name.endsWith('.har');
});

if (!harFile) {
    throw new Error(`no *.har file found in directory`);
}

const input = fs.readFileSync(harFile, 'utf8');
const datasets = extractHARData(input);

fs.writeFileSync('output.json', JSON.stringify(datasets), 'utf8');
