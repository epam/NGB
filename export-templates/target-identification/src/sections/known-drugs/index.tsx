import {useState} from "react";
import Section from '../../common/section';
import Select from '../../common/select';
import {
  KnownDrugsSource,
  KnownDrugsSourceNames
} from '../../model/types';
import {useKnownDrugs} from '../../model/main';
import Table from '../../common/table';
import {useColumnsForSource} from './columns';

export default function KnownDrugs() {
  const [source, setSource] = useState(KnownDrugsSource.openTargets);
  const data = useKnownDrugs(source);
  const columns = useColumnsForSource(source);
  return (
    <Section
      title="Known drugs"
      details={(
        <div className="whitespace-pre">
          <Section.Details count={12} name="drug" />
          <Section.Details count={188} name="records" />
        </div>
      )}
    >
      <div class="flex justify-end items-center">
        <span className="mr-1">Source:</span>
        <Select onChange={setSource} value={source} bordered>
          {
            Object.values(KnownDrugsSource).map((value) => (
              <Select.Option key={value} value={value}>
                {KnownDrugsSourceNames[value]}
              </Select.Option>
            ))
          }
        </Select>
      </div>
      <Table data={data} columns={columns} className="mt-1" />
    </Section>
  );
}
