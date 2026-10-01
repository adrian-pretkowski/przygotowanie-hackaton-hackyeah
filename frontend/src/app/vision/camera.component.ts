import { Component, ElementRef, OnDestroy, signal, viewChild } from '@angular/core';
import { toJpeg } from './image-utils';

/** Camera preview (rear camera on phones) and frame capture as JPEG. */
@Component({
  selector: 'app-camera',
  template: `
    <div class="frame">
      <video #video autoplay playsinline muted [class.hidden]="!active()"></video>
      @if (!active()) {
        <div class="placeholder">
          @if (error()) {
            <p class="error">{{ error() }}</p>
          }
          <button type="button" (click)="start()">Włącz kamerę</button>
        </div>
      }
    </div>
  `,
  styles: `
    .frame { position: relative; background: #111; border-radius: 12px; overflow: hidden; aspect-ratio: 4 / 3; }
    video { width: 100%; height: 100%; object-fit: contain; display: block; }
    .hidden { display: none; }
    .placeholder { position: absolute; inset: 0; display: grid; place-content: center; gap: 12px; color: #eee; text-align: center; padding: 16px; }
    .error { color: #ff8a80; }
  `,
})
export class CameraComponent implements OnDestroy {
  private readonly video = viewChild.required<ElementRef<HTMLVideoElement>>('video');
  private stream: MediaStream | null = null;

  readonly active = signal(false);
  readonly error = signal<string | null>(null);

  async start(): Promise<void> {
    this.error.set(null);
    if (!navigator.mediaDevices?.getUserMedia) {
      this.error.set('Brak dostępu do kamery. Poza localhostem strona musi być otwarta przez HTTPS.');
      return;
    }
    try {
      this.stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: 'environment' }, width: { ideal: 1920 }, height: { ideal: 1080 } },
        audio: false,
      });
      this.video().nativeElement.srcObject = this.stream;
      this.active.set(true);
    } catch (e) {
      this.error.set(`Nie udało się włączyć kamery: ${(e as Error).message}`);
    }
  }

  stop(): void {
    this.stream?.getTracks().forEach(t => t.stop());
    this.stream = null;
    this.active.set(false);
  }

  /** Returns the current frame as JPEG (scaled down to MAX_IMAGE_SIDE). */
  capture(): Promise<Blob> {
    const v = this.video().nativeElement;
    if (!this.active() || !v.videoWidth) {
      return Promise.reject(new Error('Kamera nie jest włączona'));
    }
    return toJpeg(v, v.videoWidth, v.videoHeight);
  }

  ngOnDestroy(): void {
    this.stop();
  }
}
