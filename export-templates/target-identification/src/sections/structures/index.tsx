import {useState} from "react";
import Section from "../../common/section";
import Select from '../../common/select';
import Table from '../../common/table';
import {
  StructuresSource,
  StructuresSourceNames,
} from '../../model/types';
import {
  useTotalCountStructures,
  useStructuresData,
} from '../../model/main';
import {useColumnsForSource} from './columns';

export default function Sequences() {
  const [source, setSource] = useState(StructuresSource.proteinDataBank);
  const structures = useTotalCountStructures();
  const data = useStructuresData(source);
  const columns = useColumnsForSource(source);
  return (
    <Section
      title="Structures"
      details={(
        <div className="whitespace-pre">
          <Section.Details count={structures} name="model" />
        </div>
      )}
    >
      <div class="flex justify-end items-center">
        <span className="mr-1">Source:</span>
        <Select onChange={setSource} value={source} bordered>
          {
            Object.values(StructuresSource).map((value) => (
              <Select.Option key={value} value={value}>
                {StructuresSourceNames[value]}
              </Select.Option>
            ))
          }
        </Select>
      </div>
      <Table data={data} columns={columns} className="mt-1" />
    </Section>
  );
}
