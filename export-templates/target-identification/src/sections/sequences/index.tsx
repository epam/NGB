import Section from "../../common/section";

export default function Sequences() {
  return (
    <Section
      title="Sequences"
      details={(
        <div className="whitespace-pre">
          <Section.Details count={6} name="DNA" strict />
          <Section.Details count={287} name="RNA" strict />
        </div>
      )}
    >
      Sequences
    </Section>
  );
}
