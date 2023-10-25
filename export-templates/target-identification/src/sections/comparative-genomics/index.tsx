import Section from "../../common/section";

export default function ComparativeGenomics() {
  return (
    <Section
      title="Comparative genomics"
      details={(
        <div className="whitespace-pre">
          <Section.Details count={588} name="homolog" />
        </div>
      )}
    >
      Comparative genomics
    </Section>
  );
}
