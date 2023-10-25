import type {CommonProps} from '../types';
import type {ItemSimpleValue, ItemValue} from "../../model/types";
import {isUndefined} from "../../model/utilities";

export type DisplayValueProps = CommonProps & {
  value: ItemValue;
};

type LinkProps = CommonProps & {
  value: ItemSimpleValue;
  link: string;
}

function Link(props: LinkProps) {
  const {
    className,
    style,
    value,
    link,
  } = props;
  return (
    <a
      className={className}
      style={style}
      href={link}
      target="_blank"
    >
      {value}
    </a>
  );
}

export default function DisplayValue(props: DisplayValueProps) {
  const {
    value,
    className,
    style,
  } = props;
  if (isUndefined(value)) {
    return null;
  }
  if (typeof value === 'string' && /^https?:\/\//i.test(value)) {
    return (
      <Link
        className={className}
        style={style}
        link={value}
        value={value}
      />
    );
  }
  if (typeof value === 'string' || typeof value === 'number') {
    return (
      <span className={className} style={style}>
        {value}
      </span>
    );
  }
  if (typeof value === 'boolean') {
    return (
      <span className={className} style={style}>
        {`${value}`}
      </span>
    );
  }
  const {
    value: realValue,
    link,
  } = value;
  if (link) {
    return (
      <Link
        className={className}
        style={style}
        link={link}
        value={realValue}
      />
    );
  }
  return (<DisplayValue value={realValue} className={className} style={style} />);
}
