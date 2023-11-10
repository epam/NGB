import type {ReactNode} from 'react';
import {Disclosure} from "@headlessui/react";
import { ChevronUpIcon } from '@heroicons/react/20/solid';
import type {CommonParentProps, CommonProps} from '../types';
import classNames from "classnames";

export type SectionProps = CommonParentProps & {
  title: ReactNode;
  details?: ReactNode;
}

export type SectionDetailsProps = CommonProps & {
  count: number;
  name: string;
  strict?: boolean;
}

function SectionDetails(props: SectionDetailsProps) {
  const {
    count,
    name,
    strict,
    className,
    style,
  } = props;
  if (count === 0) {
    return null;
  }
  const str = count === 1 || strict ? name : name.concat('s');
  return (
    <span
      className={classNames(className, 'section-details', 'whitespace-pre')}
      style={style}
    >
      <b>{count}</b>
      <span>{` ${str}`}</span>
    </span>
  );
}

function Section(props: SectionProps) {
  const {
    children,
    title,
    details,
  } = props;
  return (
    <Disclosure>
      {({ open }) => (
        <>
          <Disclosure.Button
            className={classNames(
              'bg-slate-200',
              'px-2',
              'py-1',
              'w-full',
              'text-left',
              'flex',
              'items-center',
              'hover:underline',
              'mt-1',
              {
                'rounded-md': !open,
                'rounded-t-md': open
              }
            )}
          >
            <ChevronUpIcon
              className={classNames('h-5', 'w-5', 'mr-1', 'transition-transform', {
                'rotate-180': open,
                transform: open,
              })}
            />
            <span className="flex-1 text-base">{title}</span>
            {
              details && (
                <span className="text-sm">
                  {details}
                </span>
              )
            }
          </Disclosure.Button>
          <Disclosure.Panel className="px-2 py-2 border border-slate-200 rounded-b-md shadow-sm text-sm">
            {children}
          </Disclosure.Panel>
        </>
      )}
    </Disclosure>
  );
}

Section.Details = SectionDetails;

export default Section;
