export const BLAST_TOOLS = [
    'blastn',
    'blastp',
    'blastx',
    'tblastn',
    'tblastx',
];
export const ALGORITHM_NAME = {
    'megablast': 'megablast',
    'dc-megablast': 'discontiguous megablast',
    'blastn': 'blastn',
    'blastn-short': 'blastn-short',
    'blastp': 'blastp',
    'blastp-short': 'blastp-short',
    'blastp-fast': 'blastp-fast',
    'blastx': 'blastx',
    'blastx-fast': 'blastx-fast',
    'tblastn': 'tblastn',
    'tblastn-fast': 'tblastn-fast'
};
export const ALGORITHMS = {
    blastn: ['megablast', 'dc-megablast', 'blastn', 'blastn-short'],
    blastp: ['blastp', 'blastp-short', 'blastp-fast'],
    blastx: ['blastx', 'blastx-fast'],
    tblastn: ['tblastn', 'tblastn-fast']
};
export const BLAST_TOOL_DB = {
    blastn: 'NUCLEOTIDE',
    blastp: 'PROTEIN',
    blastx: 'PROTEIN',
    tblastn: 'NUCLEOTIDE',
    tblastx: 'NUCLEOTIDE'
};
