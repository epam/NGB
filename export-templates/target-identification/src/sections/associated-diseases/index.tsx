import Section from "../../common/section";

export default function AssociatedDiseases() {
  return (
    <Section
      title="Associated diseases"
      details={(
        <div className="whitespace-pre">
          <Section.Details count={1234} name="disease" />
        </div>
      )}
    >
      Associated diseases
    </Section>
  );
}
