import {useCallback} from "react";
import Section from "../../common/section";
import PagedList from "../../common/paged-list";
import {Publication} from "../../model/types/bibliography";
import PublicationCard from "./publication";
import {useTotalCountPublications, usePublications} from '../../model/main';

export default function Bibliography() {
  const publicationsCount = useTotalCountPublications();
  const publications = usePublications();
  const cardClassName = "my-2.5 border-b";
  const publicationRenderer = useCallback((publication: Publication) => (
    <PublicationCard publication={publication} className={cardClassName} />
  ), []);
  return (
    <Section
      title="Bibliography"
      details={(
        <div className="whitespace-pre">
          <Section.Details count={publicationsCount} name="publication" />
        </div>
      )}
    >
      <div>
        <b className="uppercase">Publications</b>
      </div>
      <PagedList data={publications} itemRenderer={publicationRenderer} />
    </Section>
  );
}
