export interface KMRootNode {
  root: KMSubNode;
  template: string;
  theme: string;
  version: string;
}

export interface KMSubNode {
  data: {
    id: string;
    text: string;
    created: number;
  };
  children: KMSubNode[];
}
