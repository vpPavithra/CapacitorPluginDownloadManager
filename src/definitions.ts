export interface DownloadManagerPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
  enqueue(options: { req: object | string }): Promise<{ req: object | string }>;
  query(options: {filter: any}): Promise<any>;
  remove(options: {ids: Array<any>}): Promise<any>;
  addCompletedDownload(options: {req: any}): Promise<any>;
  fetchSpeedLog(): Promise<any>;
}
