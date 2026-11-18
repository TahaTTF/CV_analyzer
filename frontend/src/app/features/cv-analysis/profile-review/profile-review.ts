import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { SkillChips } from '../../../shared/components/skill-chips/skill-chips';
import { warningLabel } from '../../../shared/utils/warning-labels';
import { CvAnalysisStore } from '../cv-analysis.store';

@Component({
  selector: 'app-profile-review',
  imports: [RouterLink, SkillChips],
  templateUrl: './profile-review.html',
  styleUrl: './profile-review.scss',
})
export class ProfileReview {
  private readonly store = inject(CvAnalysisStore);
  private readonly router = inject(Router);

  readonly result = this.store.parseResponse;
  readonly fileName = computed(() => this.store.file()?.name ?? '');

  readonly warnings = computed(() =>
    (this.result()?.warnings ?? []).map((code) => warningLabel(code)),
  );

  chooseAnother(): void {
    this.store.reset();
    this.router.navigate(['/cv']);
  }
}