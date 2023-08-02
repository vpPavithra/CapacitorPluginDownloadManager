export interface DownloadManagerPluginPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}
