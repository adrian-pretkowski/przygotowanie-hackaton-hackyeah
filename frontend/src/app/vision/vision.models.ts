// Types matching the backend records (pl.hackyeah.pokazzapytaj.vision.model).

export type Action = 'CLICK' | 'PRESS' | 'TURN' | 'ROTATE' | 'READ' | 'TYPE' | 'NONE';

/** Rectangle on a 0-1000 scale relative to image width / height. */
export interface BBox {
  x: number;
  y: number;
  w: number;
  h: number;
}

export interface Target {
  visible: boolean;
  label: string;
  bbox: BBox;
}

export interface ExtractedText {
  url: string;
  ssid: string;
  password: string;
  other: string;
}

export interface LocateResult {
  deviceType: string;
  answer: string;
  action: Action;
  target: Target;
  extractedText: ExtractedText;
  confidence: number;
  needsBetterView: boolean;
  safetyWarning: string;
}

export interface CompareResult {
  stepCompleted: boolean;
  observedChange: string;
  confidence: number;
}

export interface VisionResponse<T> {
  result: T;
  model: string;
  modelLatencyMs: number;
  totalLatencyMs: number;
  inputTokens: number;
  outputTokens: number;
  imageWidth: number;
  imageHeight: number;
}
