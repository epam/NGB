import GeneFeatureAnalyzer from '../../../../../../dataServices/gene/gene-feature-analyzer';
import {GeneTransformer} from '../../../gene/internal';
import {Sorting} from '../../../../utilities';

export class FeatureCountsTransformer extends GeneTransformer {
    transformData(data) {
        let genes = [];
        const unmappedFeatures = [];
        for (let i = 0; i < data.length; i++) {
            if (data[i].mapped !== undefined && data[i].mapped !== null && !data[i].mapped) {
                unmappedFeatures.push(data[i]);
            } else if (data[i].feature !== null && data[i].feature !== undefined && data[i].feature.toLowerCase() === 'gene') {
                data[i].name = data[i].name || GeneFeatureAnalyzer.getFeatureName(
                    data[i],
                    'gene_name',
                    'gene_symbol',
                    'gene_id'
                );
                genes.push(data[i]);
            }
        }
        for (let i = 0; i < unmappedFeatures.length; i++) {
            const item = GeneFeatureAnalyzer.updateFeatureName(unmappedFeatures[i]);
            if (item !== null) {
                genes.push(item);
            }
        }
        genes = Sorting.quickSort(genes, false, item => item.endIndex - item.startIndex);
        return genes;
    }
}
