import type {Gene} from "../../model/types";
import {useGenesOfInterest, useIdentificationName, useTranslationalGenes} from "../../model/main";
import {CommonProps} from "../../common/types";
import classNames from "classnames";
import Section from "../../common/section";
import {ExpandableText, Expander, useExpander} from "./expandable-text";
import {useMemo} from "react";

type GeneCardProps = CommonProps & {
  gene: Gene;
}

function GeneCard(props: GeneCardProps) {
  const {
    className,
    style,
    gene,
  } = props;
  return (
    <div
      className={classNames('inline-block bg-slate-100 rounded leading-none', className)}
      style={style}
    >
      <span>{gene.name}</span>
      <span>{` (${gene.species})`}</span>
    </div>
  );
}

export default function MainSection() {
  const name = useIdentificationName();
  const interest = useGenesOfInterest();
  const translational = useTranslationalGenes();
  const {expanded, onExpandedChanged} = useExpander();
  const genesWithDescription: Gene[] = useMemo(
    () => [...interest, ...translational]
      .filter((gene) => gene.description),
    [interest, translational],
  );
  return (
    <>
      <table>
        <tbody>
        <tr>
          <th colSpan={2} className="text-left text-lg">
            {name}
          </th>
        </tr>
        {
          interest.length
            ? (
              <tr className="text-sm">
                <th className="text-left align-top">
                  <span className="py-1 leading-none">Interest:</span>
                </th>
                <td className="align-top">
                  {interest.map((gene) => (
                    <GeneCard
                      key={gene.id}
                      className="mx-1 px-1 py-1"
                      gene={gene}
                    />
                  ))}
                </td>
              </tr>
            ) : null
        }
        {
          translational.length
            ? (
            <tr className="text-sm">
              <th className="text-left align-top">
                <span className="py-1 leading-none">Translational:</span>
              </th>
              <td className="align-top">
                {translational.map((gene) => (
                  <GeneCard
                    key={gene.id}
                    className="mx-1 px-1 py-1"
                    gene={gene}
                  />
                ))}
              </td>
            </tr>
          ) : null
        }
        </tbody>
      </table>
      <Section title="Description">
        {genesWithDescription
          .map((gene) => (
            <div
              key={gene.id}
              className="block mb-2"
            >
              <span className="font-bold float-left mr-1">
                {gene.name} ({gene.species}):
              </span>
              <ExpandableText expanded={expanded} className="text-justify">
                {gene.description}
              </ExpandableText>
            </div>
          ))}
        {
          genesWithDescription.length > 0 && (
            <div>
              <Expander expanded={expanded} onExpandedChanged={onExpandedChanged} />
            </div>
          )
        }
      </Section>
    </>
  );
}
