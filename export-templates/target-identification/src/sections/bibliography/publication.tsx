import type {CommonProps} from "../../common/types";
import type {Publication} from "../../model/types/bibliography";

export type PublicationProps = CommonProps & {
  publication: Publication;
}

export default function PublicationCard(props: PublicationProps) {
  const {
    className,
    style,
    publication,
  } = props;
  return (
    <div
      className={className}
      style={style}
    >
      <div>{publication.title}</div>
    </div>
  );
}
