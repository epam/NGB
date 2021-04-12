type ColorModel =
  | "noColor"
  | "pairOrientation"
  | "insertSize"
  | "insertSizeAndPairOrientation"
  | "readStrand"
  | "firstInPairStrand";

type TrackFormat =
  | "BAM"
  | "BED"
  | "GENE"
  | "MAF"
  | "REFERENCE"
  | "Ruler"
  | "SEG"
  | "VCF"
  | "WIG";

type DisplayMode = "Bar Graph" | "Heat Map";

type PropType<TObj, TProp extends keyof TObj> = TObj[TProp];
type VariantsView = "Collapsed" | "Expanded";
type GroupMode = "default" | "firstInPair" | "pairOrientation" | "chromosomeOfMate" | "readStrand";
enum ReadsViewMode {
  Collapsed = 0,
  Expanded = 1,
  Automatic = 2
}

type ITrackStateDefinition = {
  index: number | string; // figure out
  alignments: boolean;
  arrows: boolean;
  colorMode: ColorModel;
  coverage: boolean;
  diffBase: boolean;
  geneTranscript: "collapsed" | "expanded";
  groupAutoScale: "default" | "manual" | "group";
  groupMode: GroupMode;
  ins_del: boolean;
  mismatches: boolean;
  readsViewMode: ReadsViewMode;
  shadeByQuality: boolean;
  softClip: boolean;
  spliceJunctions: boolean;
  variantsView: VariantsView;
  viewAsPairs: boolean;
  coverageDisplayMode: DisplayMode;
  coverageScaleMode: 'default' | 'manual' | 'group';
  coverageLogScale: boolean;
  coverageScaleFrom: number;
  coverageScaleTo: number;
  referenceShowTranslation: boolean;
  referenceShowForwardStrand: boolean;
  referenceShowReverseStrand: boolean;
  header: {
    fontSize: string;
  },
  color: unknown; // @todo - figure out
}

type ITrackDefinition = {
  bioDataItemId: string;
  name: string;
  projectId: string;
  height: number;
  format: TrackFormat;
  isLocal: boolean;
  state: ITrackStateDefinition;
};

export type ITrack = {
  projectIdNumber?: number;
  b: PropType<ITrackDefinition, "bioDataItemId">;
  h?: PropType<ITrackDefinition, "height">;
  p: PropType<ITrackDefinition, "projectId">;
  n?: PropType<ITrackDefinition, "name">;
  l?: PropType<ITrackDefinition, "isLocal">;
  f?: PropType<ITrackDefinition, "format">;
  s?: {
    rsfs?: PropType<ITrackStateDefinition, "referenceShowForwardStrand">;
    rsrs?: PropType<ITrackStateDefinition, "referenceShowReverseStrand">;
    rt?: PropType<ITrackStateDefinition, "referenceShowTranslation">;
    a?: PropType<ITrackStateDefinition, "arrows">;
    d?: PropType<ITrackStateDefinition, "diffBase">;
    aa?: PropType<ITrackStateDefinition, "alignments">;
    m?: PropType<ITrackStateDefinition, "mismatches">;
    c?: PropType<ITrackStateDefinition, "colorMode">;
    co?: PropType<ITrackStateDefinition, "color">;
    i?: PropType<ITrackStateDefinition, "ins_del">;
    i1?: PropType<ITrackStateDefinition, "index">;
    g?: PropType<ITrackStateDefinition, "geneTranscript">;
    v?: PropType<ITrackStateDefinition, "variantsView">;
    c1?: PropType<ITrackStateDefinition, "coverage">;
    g1?: PropType<ITrackStateDefinition, "groupMode">;
    gas?: PropType<ITrackStateDefinition, "groupAutoScale">;
    r?: PropType<ITrackStateDefinition, "readsViewMode">;
    s1?: PropType<ITrackStateDefinition, "shadeByQuality">;
    s2?: PropType<ITrackStateDefinition, "softClip">;
    s3?: PropType<ITrackStateDefinition, "spliceJunctions">;
    v1?: PropType<ITrackStateDefinition, "viewAsPairs">;
    cdm?: PropType<ITrackStateDefinition, "coverageDisplayMode">;
    cls?: PropType<ITrackStateDefinition, "coverageLogScale">;
    csm?: PropType<ITrackStateDefinition, "coverageScaleMode">;
    csf?: PropType<ITrackStateDefinition, "coverageScaleFrom">;
    cst?: PropType<ITrackStateDefinition, "coverageScaleTo">;
    he?: PropType<ITrackStateDefinition, "header">;
    wigColors?: {
      negative: number;
      positive: number;
    };
  };
};
