import {useCallback, useState} from "react";
import {CommonParentProps, CommonProps} from "../../common/types";
import './expandable-text.css';
import classNames from "classnames";

export type ExpandableTextProps = CommonParentProps & {
  expanded: boolean;
}

export type ExpandedChangeFn = (expanded: boolean) => void;
export type ExpandedChange = (expanded: boolean | ExpandedChangeFn) => void;

export type ExpanderProps = CommonProps & {
  expanded: boolean;
  onExpandedChanged: ExpandedChange;
}

export function useExpander(): {
  expanded: boolean;
  onExpandedChanged: ExpandedChange;
} {
  const [expanded, onExpandedChanged] = useState(false);
  return {
    expanded,
    onExpandedChanged,
  };
}

export function Expander(props: ExpanderProps) {
  const {
    className,
    style,
    expanded,
    onExpandedChanged,
  } = props;
  const toggle = useCallback(() => {
    onExpandedChanged((curr) => !curr);
  }, [onExpandedChanged]);
  return (
    <a onClick={toggle} className={className} style={style}>
      {expanded ? 'Show less' : 'Show more'}
    </a>
  );
}

export function ExpandableText(props: ExpandableTextProps) {
  const {
    className,
    style,
    children,
    expanded
  } = props;
  const expandableTextClassName = classNames(
    className,
    'expandable-text',
    {
      expanded,
    }
  )
  if (typeof children === 'string') {
    return (
      <div
        className={expandableTextClassName}
        style={style}
        dangerouslySetInnerHTML={{__html: children}}
      />
    );
  }
  return (
    <div
      className={expandableTextClassName}
      style={style}
    >
      {children}
    </div>
  );
}
