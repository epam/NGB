import {useState} from "react";
import Section from "../../common/section";
import Select from '../../common/select';
import Table from '../../common/table';
import {
  useGenesOfInterest,
  useTotalCountSequences,
  useSequencesGenes,
  useSequencesData,
  useGeneAndSpecies,
} from '../../model/main';
import {useSequencesColumns} from './columns';

export default function Sequences() {
  const interestGene = useGenesOfInterest()[0];
  const [geneId, setGeneId] = useState(useGeneAndSpecies(interestGene).key);
  const genes = useSequencesGenes();
  const genesOptions = genes.map(useGeneAndSpecies);
  const {dnas, mrnas, proteins} = useTotalCountSequences();
  const data = useSequencesData(geneId);
  const columns = useSequencesColumns();
  return (
    <Section
      title="Sequences"
      details={(
        <div className="whitespace-pre">
          <Section.Details count={dnas} name="DNA" strict />
          <Section.Details count={mrnas} name="RNA" strict />
          <Section.Details count={proteins} name="protein" />
        </div>
      )}
    >
      <div class="flex justify-end items-center">
        <span className="mr-1">Gene:</span>
        <Select onChange={setGeneId} value={geneId} bordered>
          {
            Object.values(genesOptions).map((o) => (
              <Select.Option key={o.key} value={o.key}>
                {o.value}
              </Select.Option>
            ))
          }
        </Select>
      </div>
      <Table data={data} columns={columns} className="mt-1" />
    </Section>
  );
}
