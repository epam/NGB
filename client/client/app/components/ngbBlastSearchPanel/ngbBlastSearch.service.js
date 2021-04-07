export default class ngbBlastSearchService {
    static instance(projectContext){
        return new ngbBlastSearchService(projectContext);
    }
    constructor(projectContext){
        Object.assign(this, { projectContext });
    }

    generateBlastSearchResults() {
        const results = [];
        this.projectContext.chromosomes.slice(0, 8).forEach(chr => {
            for (let i = 0; i < 1000; i++) {
                const start = 1 + Math.floor(Math.random() * (chr.size - 1));
                const singleSized = Math.random() >= 0.5;
                const end = Math.min(
                    start + (singleSized ? 1 : Math.floor(Math.random() * chr.size / 100)),
                    chr.size,
                );
                results.push({startIndex: start, endIndex: end, chromosome: chr.name});
            }
        });
        return results;
  }
}
