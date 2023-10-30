import type {
  Gene,
  KnownDrugsDGIdbItem,
  KnownDrugsOpenTargetsItem,
  KnownDrugsPharmGKBItem,
  DiseasesOpenTargetsItem,
  DiseasesPharmGKBItem,
  SequencesItem,
  SequencesData,
} from "../model/types";
import {KnownDrugsSource, DiseasesSource} from "../model/types";
import type {
  KnownDrugsCount,
  SequencesCount,
  DiseasesCount,
  StructuresCount,
  PublicationsCount
} from "../model/types/total-counts";
import {Publication} from "../model/types/bibliography";

const interest: Gene[] = [];
const translational: Gene[] = [];
for (let i = 0; i < 5; i += 1) {
  interest.push({
    id: `interest0000${i + 1}`,
    name: `INT${i + 1}`,
    species: 'Homo sapiens',
    description: i % 2 === 0 ? `Involved in double-strand break repair and/or homologous recombination. Binds RAD51 and potentiates recombinational DNA repair by promoting assembly of RAD51 onto single-stranded DNA (ssDNA). Acts by targeting RAD51 to ssDNA over double-stranded DNA, enabling RAD51 to displace replication protein-A (RPA) from ssDNA and stabilizing RAD51- ssDNA filaments by blocking ATP hydrolysis. Part of a PALB2-scaffolded HR complex containing RAD51C and which is thought to play a role in DNA repair by HR. May participate in S phase checkpoint activation. Binds selectively to ssDNA, and to ssDNA in tailed duplexes and replication fork structures. May play a role in the extension step after strand invasion at replication-dependent DNA double-strand breaks; together with PALB2 is involved in both POLH localization at collapsed replication forks and DNA polymerization activity. In concert with NPM1, regulates centrosome duplication. Interacts with the TREX-2 complex (transcription and export complex 2) subunits PCID2 and SEM1, and is required to prevent R-loop-associated DNA damage and thus transcription-associated genomic instability. Silencing of BRCA2 promotes R-loop accumulation at actively transcribed genes in replicating and non-replicating cells, suggesting that BRCA2 mediates the control of R-loop associated genomic instability, independently of its known role in homologous recombination (PubMed:<a href='https://europepmc.org/article/med/24896180'>24896180</a>). {ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/15115758'>15115758</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/15199141'>15199141</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/15671039'>15671039</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/18317453'>18317453</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/20729832'>20729832</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/20729858'>20729858</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/20729859'>20729859</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/21084279'>21084279</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/21719596'>21719596</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/24485656'>24485656</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/24896180'>24896180</a>}.Inherited mutations in BRCA1 and this gene, BRCA2, confer increased lifetime risk of developing breast or ovarian cancer. Both BRCA1 and BRCA2 are involved in maintenance of genome stability, specifically the homologous recombination pathway for double-strand DNA repair. The largest exon in both genes is exon 11, which harbors the most important and frequent mutations in breast cancer patients. The BRCA2 gene was found on chromosome 13q12.3 in human. The BRCA2 protein contains several copies of a 70 aa motif called the BRC motif, and these motifs mediate binding to the RAD51 recombinase which functions in DNA repair. BRCA2 is considered a tumor suppressor gene, as tumors with BRCA2 mutations generally exhibit loss of heterozygosity (LOH) of the wild-type allele. [provided by RefSeq, May 2020]` : undefined
  });
  translational.push({
    id: `translational0000${i + 1}`,
    name: `TRA${i + 1}`,
    species: 'Canis lupus familiaris',
    description: i % 2 === 0 ? `Involved in double-strand break repair and/or homologous recombination. Binds RAD51 and potentiates recombinational DNA repair by promoting assembly of RAD51 onto single-stranded DNA (ssDNA). Acts by targeting RAD51 to ssDNA over double-stranded DNA, enabling RAD51 to displace replication protein-A (RPA) from ssDNA and stabilizing RAD51- ssDNA filaments by blocking ATP hydrolysis. Part of a PALB2-scaffolded HR complex containing RAD51C and which is thought to play a role in DNA repair by HR. May participate in S phase checkpoint activation. Binds selectively to ssDNA, and to ssDNA in tailed duplexes and replication fork structures. May play a role in the extension step after strand invasion at replication-dependent DNA double-strand breaks; together with PALB2 is involved in both POLH localization at collapsed replication forks and DNA polymerization activity. In concert with NPM1, regulates centrosome duplication. Interacts with the TREX-2 complex (transcription and export complex 2) subunits PCID2 and SEM1, and is required to prevent R-loop-associated DNA damage and thus transcription-associated genomic instability. Silencing of BRCA2 promotes R-loop accumulation at actively transcribed genes in replicating and non-replicating cells, suggesting that BRCA2 mediates the control of R-loop associated genomic instability, independently of its known role in homologous recombination (PubMed:<a href='https://europepmc.org/article/med/24896180'>24896180</a>). {ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/15115758'>15115758</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/15199141'>15199141</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/15671039'>15671039</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/18317453'>18317453</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/20729832'>20729832</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/20729858'>20729858</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/20729859'>20729859</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/21084279'>21084279</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/21719596'>21719596</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/24485656'>24485656</a>, ECO:0000269|PubMed:<a href='https://europepmc.org/article/med/24896180'>24896180</a>}.Inherited mutations in BRCA1 and this gene, BRCA2, confer increased lifetime risk of developing breast or ovarian cancer. Both BRCA1 and BRCA2 are involved in maintenance of genome stability, specifically the homologous recombination pathway for double-strand DNA repair. The largest exon in both genes is exon 11, which harbors the most important and frequent mutations in breast cancer patients. The BRCA2 gene was found on chromosome 13q12.3 in human. The BRCA2 protein contains several copies of a 70 aa motif called the BRC motif, and these motifs mediate binding to the RAD51 recombinase which functions in DNA repair. BRCA2 is considered a tumor suppressor gene, as tumors with BRCA2 mutations generally exhibit loss of heterozygosity (LOH) of the wild-type allele. [provided by RefSeq, May 2020]` : undefined
  });
}
const knownDrugsOpenTargets: KnownDrugsOpenTargetsItem[] = [];
const knownDrugsDGIdb: KnownDrugsDGIdbItem[] = [];
const knownDrugsPharmGKB: KnownDrugsPharmGKBItem[] = [];
const generateData = (count, name) => (new Array(count)).fill(name).map((o, i) => `${o} ${i + 1}`);
const types = ['Small molecule', 'Large molecule'];
const mechanisms = generateData(10, 'Mechanism');
const actions = generateData(10, 'Action');
const phases = generateData(7, 'Phase');
const statuses = generateData(4, 'Status');
const sources = generateData(13, 'Source');
const interactionSources = generateData(13, 'Interaction source');
const interactionTypes = generateData(13, 'Interaction type');

const transcript = generateData(8, 'Transcript');
const protein = generateData(20, 'Protein');
const proteinName = generateData(20, 'Protein name');

const knownDrugsCount: KnownDrugsCount = {
  drugs: 17,
  records: 122
};
const diseasesCount: DiseasesCount = 1111;
const sequencesCount: SequencesCount = {
  dnas: 4,
  mrnas: 5,
  proteins: 9,
};
const structuresCount: StructuresCount = 0;
const publicationsCount: PublicationsCount = 0;

const diseasesOpenTargets: DiseasesOpenTargetsItem[] = [];
const diseasesPharmGKB: DiseasesPharmGKBItem[] = [];

const sequences1: SequencesItem[] = [];
const sequences2: SequencesItem[] = [];
const sequences3: SequencesItem[] = [];
const sequencesData: SequencesData[] = [];

const getElement = (i, array) => array[i % array.length];
const getRandomElement = (array) => getElement(Math.floor(Math.random() * array.length), array);
const getNumber = (max) => Math.random() * (max - 1) + 1;

for (let i = 0; i < 10000; i += 1) {
  knownDrugsOpenTargets.push({
    target: getElement(i, interest).id,
    drug: i % 5 === 0 ? `DRUG${i + 1}` : {value: `DRUG${i + 1}`, link: 'https://platform.opentargets.org/drug/CHEMBL4594350'},
    type: getElement(i, types),
    mechanism: getElement(i, mechanisms),
    action: getElement(i, actions),
    disease: {value: `Disease${i + 1}`, link: 'https://platform.opentargets.org/disease/EFO_0003060'},
    phase: getElement(i, phases),
    status: getElement(i, statuses),
    source: {value: `Source${i + 1}`, link: 'https://clinicaltrials.gov/study/NCT03202940'},
  });
  knownDrugsDGIdb.push({
    target: getElement(i, interest).id,
    drug: i % 5 === 0 ? `DRUG${i + 1}` : {value: `DRUG${i + 1}`, link: 'https://platform.opentargets.org/drug/CHEMBL4594350'},
    interactionSource: getElement(i, interactionSources),
    interactionType: getElement(i, interactionTypes),
  });
  knownDrugsPharmGKB.push({
    target: getElement(i, interest).id,
    drug: i % 5 === 0 ? `DRUG${i + 1}` : {value: `DRUG${i + 1}`, link: 'https://platform.opentargets.org/drug/CHEMBL4594350'},
    source: getElement(i, sources),
  });
}

for (let i = 0; i < 1000; i += 1) {
  diseasesOpenTargets.push({
    target: getElement(i, interest).id,
    disease: {value: `Disease${i + 1}`, link: 'https://platform.opentargets.org/disease/EFO_0003060'},
    overallScore: Math.random(),
    geneticAssociation: Math.random(),
    somaticMutations: Math.random(),
    drugs: Math.random(),
    pathwaysSystems: Math.random(),
    textMining: Math.random(),
    animalModels: Math.random(),
    RNAExpression: Math.random(),

  });
  diseasesPharmGKB.push({
    target: getElement(i, interest).id,
    disease: {value: `Disease${i + 1}`, link: 'https://platform.opentargets.org/disease/EFO_0003060'},
  });
}

for (let i = 0; i < 1000; i += 1) {
  sequences1.push({
    target: getElement(i, interest).id,
    transcript: getElement(i, transcript),
    mrnaLength: getNumber(3000),
    protein: getElement(i, protein),
    proteinLength: getNumber(3000),
    proteinName: getElement(i, proteinName),
  });
}

for (let i = 0; i < 200; i += 1) {
  sequences2.push({
    target: getElement(i, interest).id,
    transcript: getElement(i, transcript),
    mrnaLength: getNumber(3000),
    protein: getElement(i, protein),
    proteinLength: getNumber(3000),
    proteinName: getElement(i, proteinName),
  });
}

for (let i = 0; i < 400; i += 1) {
  sequences3.push({
    target: getElement(i, interest).id,
    transcript: getElement(i, transcript),
    mrnaLength: getNumber(3000),
    protein: getElement(i, protein),
    proteinLength: getNumber(3000),
    proteinName: getElement(i, proteinName),
  });
}

const genes = [...interest, ...translational];

sequencesData.push({
  gene: genes[0],
  data: sequences1,
});

sequencesData.push({
  gene: genes[1],
  data: sequences2,
});

sequencesData.push({
  gene: genes[2],
  data: sequences3,
});

const publications: Publication[] = [];

const authors = generateData(100, 'Author');

for (let i = 0; i < 678; i += 1) {
  const auth: string[] = [];
  for (let a = 0; a < 10; a += 1) {
    auth.push(getRandomElement(authors));
  }
  publications.push({
    title: `Publication title ${i + 1}`,
    authors: auth,
    date: 'October, 2023'
  });
}

window.injected_data = typeof (window as any).injected_data === 'object' ? (window as any).injected_data : {
  name: 'BRCA Complex',
  interest,
  translational,
  totalCounts: {
    knownDrugs: knownDrugsCount,
    diseases: diseasesCount,
    sequences: sequencesCount,
    structures: structuresCount,
    publications: publicationsCount
  },
  knownDrugs: [
    {
      source: KnownDrugsSource.openTargets,
      data: knownDrugsOpenTargets,
    },
    {
      source: KnownDrugsSource.dgIdb,
      data: knownDrugsDGIdb,
    },
    {
      source: KnownDrugsSource.pharmGKB,
      data: knownDrugsPharmGKB,
    },
  ],
  associatedDiseases: [
    {
      source: DiseasesSource.openTargets,
      data: diseasesOpenTargets,
    },
    {
      source: DiseasesSource.pharmGKB,
      data: diseasesPharmGKB,
    },
  ],
  sequences: sequencesData,
  publications,
};
