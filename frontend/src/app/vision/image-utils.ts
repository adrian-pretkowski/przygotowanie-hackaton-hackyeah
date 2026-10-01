/** Longer side of the frame sent to the backend. Larger images are downscaled by the model anyway - not worth the upload time. */
export const MAX_IMAGE_SIDE = 1568;

/** Draws the source (video or bitmap) on a size-limited canvas and returns a JPEG. */
export function toJpeg(source: CanvasImageSource, width: number, height: number, quality = 0.85): Promise<Blob> {
  const scale = Math.min(1, MAX_IMAGE_SIDE / Math.max(width, height));
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(width * scale);
  canvas.height = Math.round(height * scale);
  canvas.getContext('2d')!.drawImage(source, 0, 0, canvas.width, canvas.height);
  return new Promise((resolve, reject) =>
    canvas.toBlob(b => (b ? resolve(b) : reject(new Error('Nie udało się zakodować JPEG'))), 'image/jpeg', quality),
  );
}

/** Loads a user file (respecting EXIF orientation) and normalizes it to JPEG. */
export async function fileToJpeg(file: Blob): Promise<Blob> {
  const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
  try {
    return await toJpeg(bitmap, bitmap.width, bitmap.height);
  } finally {
    bitmap.close();
  }
}
