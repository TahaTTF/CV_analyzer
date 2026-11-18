import { Component, ElementRef, OnDestroy, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { CvApiService } from '../../../core/services/cv-api.service';
import { ErrorPanel } from '../../../shared/components/error-panel/error-panel';
import { ApiError } from '../../../shared/models/api-error.model';
import { validateCvFile } from '../../../shared/utils/file-validation';
import { CvAnalysisStore } from '../cv-analysis.store';

@Component({
  selector: 'app-cv-upload',
  imports: [ErrorPanel],
  templateUrl: './cv-upload.html',
  styleUrl: './cv-upload.scss',
})
export class CvUpload implements OnDestroy {
  private readonly api = inject(CvApiService);
  private readonly store = inject(CvAnalysisStore);
  private readonly router = inject(Router);
  private sub?: Subscription;

  readonly fileInput = viewChild.required<ElementRef<HTMLInputElement>>('fileInput');

  readonly file = signal<File | null>(null);
  readonly localError = signal<string | null>(null);
  readonly apiError = signal<ApiError | null>(null);
  readonly uploading = signal(false);
  readonly dragOver = signal(false);
  readonly sessionExpired =
    inject(ActivatedRoute).snapshot.queryParamMap.get('session') === 'expired';
  openPicker(): void {
    if (!this.uploading()) this.fileInput().nativeElement.click();
  }

  onPick(event: Event): void {
    const input = event.target as HTMLInputElement;
    const picked = input.files?.[0];
    if (picked) this.select(picked);
    input.value = ''; // allows choosing the same file again
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    if (!this.uploading()) this.dragOver.set(true);
  }

  onDragLeave(): void {
    this.dragOver.set(false);
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver.set(false);
    if (this.uploading()) return;
    const files = event.dataTransfer?.files;
    if (!files || files.length === 0) return;
    if (files.length > 1) {
      this.localError.set('Please drop a single file.');
      return;
    }
    this.select(files[0]);
  }

  private select(file: File): void {
    this.apiError.set(null);
    const problem = validateCvFile(file);
    if (problem) {
      this.file.set(null);
      this.localError.set(problem);
      return;
    }
    this.localError.set(null);
    this.file.set(file);
  }

  remove(): void {
    this.file.set(null);
    this.localError.set(null);
    this.apiError.set(null);
  }

  submit(): void {
    const file = this.file();
    if (!file || this.uploading()) return; // blocks duplicate submissions
    this.uploading.set(true);
    this.apiError.set(null);

    this.sub = this.api.parse(file).subscribe({
      next: (response) => {
        this.store.setParsed(file, response);
        this.uploading.set(false);
        this.router.navigate(['/cv/review']);
      },
      error: (err: ApiError) => {
        this.apiError.set(err);
        this.uploading.set(false);
      },
    });
  }

  cancel(): void {
    this.sub?.unsubscribe(); // aborts the HTTP request
    this.uploading.set(false);
  }

  formatSize(bytes: number): string {
    return bytes < 1024 * 1024
      ? `${(bytes / 1024).toFixed(0)} KB`
      : `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  }

  fileKind(file: File): string {
    return file.name.toLowerCase().endsWith('.pdf') ? 'PDF' : 'DOCX';
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }
}