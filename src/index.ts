import { registerPlugin } from '@capacitor/core';

import type { DownloadManagerPluginPlugin } from './definitions';

const DownloadManagerPlugin = registerPlugin<DownloadManagerPluginPlugin>(
  'DownloadManagerPlugin',
  {
    web: () => import('./web').then(m => new m.DownloadManagerPluginWeb()),
  },
);

export * from './definitions';
export { DownloadManagerPlugin };
