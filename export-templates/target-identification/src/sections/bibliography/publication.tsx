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
  const {title, authors, date} = publication;
  const authortsString = `${authors.join(', ')}`;
  return (
    <div
      className={className}
      style={style}
    >
      <div className="my-1"><a href={title.link}>{title.name}</a></div>
      {
        authors && authors.length
          ? <div className="my-1">{authortsString}</div>
          : null
      }
      {
        date
          ? <div className="my-1">{date}</div>
          : null
      }
    </div>
  );
}
