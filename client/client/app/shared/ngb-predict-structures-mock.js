import ngbConstants from "../../constants";

const urls = [{
    name: 'rank_001_ptm_model_2',
    url: 'https://cloud-pipeline-oss-builds.s3.amazonaws.com/tools/demo/ngb/alphafold/rank_001_ptm_model_2.pdb',
}, {
    name: 'rank_002_ptm_model_1',
    url: 'https://cloud-pipeline-oss-builds.s3.amazonaws.com/tools/demo/ngb/alphafold/rank_002_ptm_model_1.pdb',
}, {
    name: 'rank_003_ptm_model_5',
    url: 'https://cloud-pipeline-oss-builds.s3.amazonaws.com/tools/demo/ngb/alphafold/rank_003_ptm_model_5.pdb',
}, {
    name: 'rank_004_ptm_model_4',
    url: 'https://cloud-pipeline-oss-builds.s3.amazonaws.com/tools/demo/ngb/alphafold/rank_004_ptm_model_4.pdb',
}, {
    name: 'rank_005_ptm_model_3',
    url: 'https://cloud-pipeline-oss-builds.s3.amazonaws.com/tools/demo/ngb/alphafold/rank_005_ptm_model_3.pdb',
}];

export default function ngbPredictStructuresMock() {
    let base = ngbConstants.urlPrefix || '';
    if (base && base.length) {
        if (!base.endsWith('/')) {
            base = base.concat('/');
        }
    }
    return urls.map((url) => ({
        name: url.name,
        url: `${base}miew/index.html?load=${url.url || url.name}`,
    }));
}