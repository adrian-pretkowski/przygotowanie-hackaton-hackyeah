import { Component, ElementRef, OnDestroy, effect, input, viewChild } from '@angular/core';
import { BBox, LocateResult } from './vision.models';

/**
 * Shows a frozen frame with an overlay highlighting the element.
 *
 * The canvas has the image resolution (1:1) and CSS scales it proportionally (width: 100%, height: auto).
 * So the bbox (0-1000) maps directly to image pixels, without the letterbox math
 * needed when drawing over a <video> element with object-fit.
 */
@Component({
  selector: 'app-annotated-frame',
  template: `<canvas #canvas></canvas>`,
  styles: `canvas { width: 100%; height: auto; display: block; border-radius: 12px; background: #111; }`,
})
export class AnnotatedFrameComponent implements OnDestroy {
  readonly image = input.required<Blob>();
  readonly result = input<LocateResult | null>(null);

  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private bitmap: ImageBitmap | null = null;
  private animationId = 0;

  constructor() {
    effect(onCleanup => {
      const image = this.image();
      const result = this.result();
      let cancelled = false;
      onCleanup(() => {
        cancelled = true;
        cancelAnimationFrame(this.animationId);
      });
      createImageBitmap(image).then(bitmap => {
        if (cancelled) {
          bitmap.close();
          return;
        }
        this.bitmap?.close();
        this.bitmap = bitmap;
        this.animate(result);
      });
    });
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.animationId);
    this.bitmap?.close();
  }

  private animate(result: LocateResult | null): void {
    const canvas = this.canvas().nativeElement;
    const bitmap = this.bitmap!;
    canvas.width = bitmap.width;
    canvas.height = bitmap.height;
    const ctx = canvas.getContext('2d')!;

    const draw = (time: number) => {
      ctx.drawImage(bitmap, 0, 0);
      const target = result?.target;
      if (target?.visible) {
        const rect = toPixels(target.bbox, canvas.width, canvas.height);
        const pulse = 0.5 + 0.5 * Math.sin(time / 250);
        drawHighlight(ctx, rect, target.label, pulse, canvas.width);
        if (result!.action === 'ROTATE' || result!.action === 'TURN') {
          drawRotateArrow(ctx, rect, canvas.width);
        }
        this.animationId = requestAnimationFrame(draw);
      }
    };
    this.animationId = requestAnimationFrame(draw);
  }
}

interface Rect {
  x: number;
  y: number;
  w: number;
  h: number;
}

function toPixels(b: BBox, width: number, height: number): Rect {
  return { x: (b.x / 1000) * width, y: (b.y / 1000) * height, w: (b.w / 1000) * width, h: (b.h / 1000) * height };
}

function drawHighlight(ctx: CanvasRenderingContext2D, r: Rect, label: string, pulse: number, canvasWidth: number): void {
  const lw = Math.max(3, canvasWidth / 250);
  // dim everything outside the highlighted element
  ctx.save();
  ctx.fillStyle = 'rgba(0, 0, 0, 0.35)';
  ctx.beginPath();
  ctx.rect(0, 0, ctx.canvas.width, ctx.canvas.height);
  ctx.rect(r.x, r.y, r.w, r.h);
  ctx.fill('evenodd');
  ctx.restore();

  ctx.save();
  ctx.strokeStyle = `rgba(255, 214, 0, ${0.6 + 0.4 * pulse})`;
  ctx.lineWidth = lw * (1 + 0.5 * pulse);
  ctx.strokeRect(r.x, r.y, r.w, r.h);

  if (label) {
    const fontSize = Math.max(16, canvasWidth / 40);
    ctx.font = `bold ${fontSize}px system-ui, sans-serif`;
    const textW = ctx.measureText(label).width + fontSize;
    const ty = r.y > fontSize * 2 ? r.y - fontSize * 1.6 : r.y + r.h + 4;
    ctx.fillStyle = 'rgba(255, 214, 0, 0.95)';
    ctx.fillRect(r.x, ty, textW, fontSize * 1.5);
    ctx.fillStyle = '#111';
    ctx.fillText(label, r.x + fontSize / 2, ty + fontSize * 1.1);
  }
  ctx.restore();
}

/** Curved arrow next to the element - signals "rotate / turn". */
function drawRotateArrow(ctx: CanvasRenderingContext2D, r: Rect, canvasWidth: number): void {
  const cx = r.x + r.w / 2;
  const cy = r.y + r.h / 2;
  const radius = Math.max(r.w, r.h) / 2 + canvasWidth / 40;
  const start = -Math.PI * 0.8;
  const end = -Math.PI * 0.2;
  ctx.save();
  ctx.strokeStyle = '#00e5ff';
  ctx.fillStyle = '#00e5ff';
  ctx.lineWidth = Math.max(4, canvasWidth / 200);
  ctx.beginPath();
  ctx.arc(cx, cy, radius, start, end);
  ctx.stroke();
  const ex = cx + radius * Math.cos(end);
  const ey = cy + radius * Math.sin(end);
  const head = ctx.lineWidth * 3;
  const angle = end + Math.PI / 2;
  ctx.beginPath();
  ctx.moveTo(ex + head * Math.cos(angle), ey + head * Math.sin(angle));
  ctx.lineTo(ex + head * Math.cos(angle + 2.5), ey + head * Math.sin(angle + 2.5));
  ctx.lineTo(ex + head * Math.cos(angle - 2.5), ey + head * Math.sin(angle - 2.5));
  ctx.closePath();
  ctx.fill();
  ctx.restore();
}
