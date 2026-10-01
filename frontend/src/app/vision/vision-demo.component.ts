import { DecimalPipe, JsonPipe, NgTemplateOutlet, PercentPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { Observable } from 'rxjs';
import { AnnotatedFrameComponent } from './annotated-frame.component';
import { CameraComponent } from './camera.component';
import { fileToJpeg } from './image-utils';
import { CompareResult, LocateResult, VisionResponse } from './vision.models';
import { VisionService } from './vision.service';

type Mode = 'locate' | 'compare';
type Slot = 'before' | 'after';

/** Demo page: camera → question → model answer with overlay + metrics (latency, tokens). */
@Component({
  selector: 'app-vision-demo',
  imports: [CameraComponent, AnnotatedFrameComponent, JsonPipe, DecimalPipe, PercentPipe, NgTemplateOutlet],
  templateUrl: './vision-demo.component.html',
  styleUrl: './vision-demo.component.scss',
})
export class VisionDemoComponent {
  private readonly vision = inject(VisionService);
  private readonly camera = viewChild.required(CameraComponent);

  readonly mode = signal<Mode>('locate');
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  // "where to click" mode
  readonly question = signal('Jak zmienić hasło do Wi-Fi?');
  readonly frame = signal<Blob | null>(null);
  readonly locateResponse = signal<VisionResponse<LocateResult> | null>(null);

  // "was the step completed" mode
  readonly expectedStep = signal('Otworzyć ustawienia sieci bezprzewodowej');
  readonly before = signal<Blob | null>(null);
  readonly after = signal<Blob | null>(null);
  readonly compareResponse = signal<VisionResponse<CompareResult> | null>(null);

  // H4: response time history for this session
  readonly latencies = signal<number[]>([]);
  readonly latencyStats = computed(() => {
    const sorted = [...this.latencies()].sort((a, b) => a - b);
    if (!sorted.length) return null;
    const pct = (p: number) => sorted[Math.min(sorted.length - 1, Math.ceil(p * sorted.length) - 1)];
    return { count: sorted.length, p50: pct(0.5), p90: pct(0.9) };
  });

  private readonly beforeUrl = computed(() => this.objectUrl('before', this.before()));
  private readonly afterUrl = computed(() => this.objectUrl('after', this.after()));
  private readonly urls: Partial<Record<Slot, string>> = {};

  previewUrl(slot: Slot): string | null {
    return slot === 'before' ? this.beforeUrl() : this.afterUrl();
  }

  setMode(mode: Mode): void {
    this.mode.set(mode);
    this.error.set(null);
  }

  async captureAndAsk(): Promise<void> {
    await this.run(async () => {
      const frame = await this.camera().capture();
      this.ask(frame);
    });
  }

  async uploadAndAsk(event: Event): Promise<void> {
    const file = takeFile(event);
    if (!file) return;
    await this.run(async () => this.ask(await fileToJpeg(file)));
  }

  async captureSlot(slot: Slot): Promise<void> {
    await this.run(async () => this.slot(slot).set(await this.camera().capture()), false);
  }

  async uploadSlot(slot: Slot, event: Event): Promise<void> {
    const file = takeFile(event);
    if (!file) return;
    await this.run(async () => this.slot(slot).set(await fileToJpeg(file)), false);
  }

  checkStep(): void {
    const before = this.before();
    const after = this.after();
    if (!before || !after) {
      this.error.set('Najpierw zrób zdjęcie PRZED i PO.');
      return;
    }
    this.compareResponse.set(null);
    this.call(this.vision.compare(before, after, this.expectedStep()), r => this.compareResponse.set(r));
  }

  private ask(frame: Blob): void {
    this.frame.set(frame);
    this.locateResponse.set(null);
    this.call(this.vision.locate(frame, this.question()), r => this.locateResponse.set(r));
  }

  private call<T>(request: Observable<VisionResponse<T>>, onSuccess: (r: VisionResponse<T>) => void): void {
    this.loading.set(true);
    this.error.set(null);
    request.subscribe({
      next: r => {
        this.latencies.update(l => [...l, r.totalLatencyMs]);
        onSuccess(r);
        this.loading.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.error.set(e.error?.detail ?? e.message);
        this.loading.set(false);
      },
    });
  }

  /** Error handling for local steps (camera, file decoding). */
  private async run(action: () => Promise<void>, clearError = true): Promise<void> {
    if (clearError) this.error.set(null);
    try {
      await action();
    } catch (e) {
      this.error.set((e as Error).message);
    }
  }

  private slot(slot: Slot) {
    return slot === 'before' ? this.before : this.after;
  }

  private objectUrl(slot: Slot, blob: Blob | null): string | null {
    if (this.urls[slot]) URL.revokeObjectURL(this.urls[slot]!);
    this.urls[slot] = blob ? URL.createObjectURL(blob) : undefined;
    return this.urls[slot] ?? null;
  }
}

function takeFile(event: Event): File | null {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0] ?? null;
  input.value = ''; // allows uploading the same file again
  return file;
}
