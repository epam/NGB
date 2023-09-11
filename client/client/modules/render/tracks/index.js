import {BAMTrack as BAM} from './bam';
import {BEDTrack as BED} from './bed';
import {BLASTTrack as BLAST} from './blast';
import {
    FeatureCountsTrack as FEATURE_COUNTS
} from './featureCounts';
import {GENETrack as GENE} from './gene';
import {default as HEATMAP} from './heatmap';
import {MAFTrack as MAF} from './maf';
import {MultiSampleVCFTrack as MultiSampleVCF} from './vcf/multi-sample-vcf';
import {REFERENCETrack as REFERENCE} from './reference';
import {RulerTrack as Ruler} from './ruler';
import {SEGTrack as SEG} from './seg';
import {VCFTrack as VCF} from './vcf';
import {WIGTrack as WIG} from './wig';
import {MOTIFSTrack as MOTIFS} from './motifs';
import {ComparisonTrack as COMPARISON} from './comparison';

export default {
    BAM,
    BED,
    BLAST,
    GENE,
    FEATURE_COUNTS,
    MAF,
    REFERENCE,
    Ruler,
    SEG,
    VCF,
    MultiSampleVCF,
    WIG,
    HEATMAP,
    MOTIFS,
    COMPARISON
};
