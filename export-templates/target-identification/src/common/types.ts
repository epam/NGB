import type {ReactNode, CSSProperties} from 'react';

export type CommonProps = {
  className?: string;
  style?: CSSProperties;
}

export type CommonParentProps = CommonProps & {
  children?: ReactNode;
}

export enum VerticalAlign {
  left = 'left',
  right = 'right',
  center = 'center',
}
