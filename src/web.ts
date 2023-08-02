import { WebPlugin } from '@capacitor/core';

import type { DownloadManagerPluginPlugin } from './definitions';

export class DownloadManagerPluginWeb
  extends WebPlugin
  implements DownloadManagerPluginPlugin
{
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
