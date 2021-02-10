export function extractHARData(str) {
    const input = JSON.parse(str);
    const jsons = input.log.entries.filter((el) => {
        return el.response.content.mimeType === 'application/json' && el.request.url.includes('restapi');
    });
    
    const datasets = {};

    jsons.forEach((r) => {
        const key = r.request.url.split('restapi')[1];
        datasets[`restapi${key}`] = r.response.content.text;
    })
    
    return datasets;
}
