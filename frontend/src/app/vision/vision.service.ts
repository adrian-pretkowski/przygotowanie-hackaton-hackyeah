import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CompareResult, LocateResult, VisionResponse } from './vision.models';

@Injectable({ providedIn: 'root' })
export class VisionService {
  private readonly http = inject(HttpClient);

  locate(image: Blob, question: string): Observable<VisionResponse<LocateResult>> {
    const form = new FormData();
    form.append('image', image, 'frame.jpg');
    form.append('question', question);
    return this.http.post<VisionResponse<LocateResult>>('/api/vision/locate', form);
  }

  compare(before: Blob, after: Blob, expectedStep: string): Observable<VisionResponse<CompareResult>> {
    const form = new FormData();
    form.append('before', before, 'before.jpg');
    form.append('after', after, 'after.jpg');
    form.append('expectedStep', expectedStep);
    return this.http.post<VisionResponse<CompareResult>>('/api/vision/compare', form);
  }
}
