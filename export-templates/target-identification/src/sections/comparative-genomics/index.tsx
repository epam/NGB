import Section from "../../common/section";
import Table from '../../common/table';
import {useTotalCountGenomics, useComparativeGenomics} from '../../model/main';
import {useGenomicsColumns} from './columns';

export default function ComparativeGenomics() {
  const genomics = useTotalCountGenomics();
  const data = useComparativeGenomics();
  const columns = useGenomicsColumns();
  return (
    <Section
      title="Comparative genomics"
      details={(
        <div className="whitespace-pre">
          <Section.Details count={genomics} name="homolog" />
        </div>
      )}
    >
      <Table data={data} columns={columns} className="mt-1" />
    </Section>
  );
}
