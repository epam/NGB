
import fs from 'fs';

const input = fs.readFileSync('input.txt', 'utf8');

const urls = input.split(/\r?\n/).map(url => {
    let raw = url.trim();
    let correctStart = raw.slice(raw.indexOf('http'), raw.length);
    return correctStart.split(' ')[0];
});

const results = [];

urls.forEach((url) => {
    let urlObject = new URL(url);
    let [ head, tail ] = urlObject.hash.split('?');
    let headKeys = ['referenceId', 'chromosome', 'start', 'end'];
    let result = { };

    head.split('/').forEach((el) => {
        if (el.length && el !== '#') {
            let key = headKeys.shift();
            result[key] = el;
        }
    });

    tail.split('&').forEach((queryPair) => {
        const [ queryParamName, queryParamValue ] = queryPair.split('=');
        result[queryParamName] = queryParamValue;
    });

    if (result['tracks']) {
        result['tracks'] = JSON.parse(decodeURIComponent(result['tracks']));
    }

    results.push(result);
});


fs.writeFileSync('output.json', JSON.stringify(results, null, 4), 'utf8');