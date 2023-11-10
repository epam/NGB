import type {GlobalData} from '../types';

declare global {
  interface Window {
    injected_data?: GlobalData;
  }
}


export default function useInjectedData(): GlobalData {
  return typeof window.injected_data === 'object' ? window.injected_data : {};
}
