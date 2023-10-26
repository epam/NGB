import {useState} from "react";
import Section from "../../common/section";
import Select from '../../common/select';
import Table from '../../common/table';
import {
  DiseasesSource,
  DiseasesSourceNames,
} from '../../model/types';
import {useTotalCountDiseases, useAssociatedDiseases} from '../../model/main';
import {useColumnsForSource} from './columns';

export default function AssociatedDiseases() {
  const [source, setSource] = useState(DiseasesSource.openTargets);
  const diseases = useTotalCountDiseases();
  const data = useAssociatedDiseases(source);
  const columns = useColumnsForSource(source);
  return (
    <Section
      title="Associated diseases"
      details={(
        <div className="whitespace-pre">
          <Section.Details count={diseases} name="disease" />
        </div>
      )}
    >
      <div class="flex justify-end items-center">
        <span className="mr-1">Source:</span>
        <Select onChange={setSource} value={source} bordered>
          {
            Object.values(DiseasesSource).map((value) => (
              <Select.Option key={value} value={value}>
                {DiseasesSourceNames[value]}
              </Select.Option>
            ))
          }
        </Select>
      </div>
      <Table data={data} columns={columns} className="mt-1" />
    </Section>
  );
}
