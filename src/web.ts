import { WebPlugin } from '@capacitor/core';

import type { DownloadManagerPluginPlugin } from './definitions';

export class DownloadManagerPluginWeb extends WebPlugin implements DownloadManagerPluginPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
  async enqueue(options: { req: string | object; }): Promise<{ req: string | object; }> {
    console.log('options enqueue', options.req)
    return options;
  }
  async query(options: { filter: object; }): Promise<any> {
    console.log('options query', options)
    return options;
  }
  async remove(options: { ids: any[]; }): Promise<any> {
    console.log('options remove ids', options)
    return options;
  }
  async addCompletedDownload(options: { req: any; }): Promise<any> {
    console.log('options addCompletedDownload ', options.req)
    return options;
  }
  async fetchSpeedLog(): Promise<any> {
    console.log('fetch speed log ')
    return true;
  }
}
