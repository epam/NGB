import Section from "../../common/section";
import {usePublications} from "../../model/main";
import PagedList from "../../common/paged-list";
import {useCallback} from "react";
import {Publication} from "../../model/types/bibliography";
import PublicationCard from "./publication";

export default function Bibliography() {
  const publications = usePublications();
  const publicationRenderer = useCallback((publication: Publication) => (
    <PublicationCard publication={publication} />
  ), []);
  return (
    <Section
      title="Bibliography"
      details={(
        <div className="whitespace-pre">
          <Section.Details count={publications.length} name="publication" />
        </div>
      )}
    >
      <div>
        <b>Publications:</b>
      </div>
      <PagedList data={publications} itemRenderer={publicationRenderer} />
    </Section>
  );
}
