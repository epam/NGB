export default class ngbBlastSearchService {
  static instance(projectContext){
    return new ngbBlastSearchService(projectContext);
  }
  constructor(projectContext){
    Object.assign(this, {
      projectContext
    })
  }

  generateBlastSearchResults(){
    let results = [];
    this.projectContext.chromosomes.slice(0,8).forEach(chr => {
      const pos1 = Math.floor(Math.random() * chr.size);
      const pos2 = Math.floor(Math.random() * chr.size);
      results.push({ startIndex: Math.min(pos1, pos2), endIndex:Math.max(pos1, pos2), chromosome:chr.name });
    })
    return results;
  }
}