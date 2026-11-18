import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ScoreMeter } from '../../../shared/components/score-meter/score-meter';
import { SkillChips } from '../../../shared/components/skill-chips/skill-chips';
import { Recommendation } from '../../../shared/models/recommendation.model';
import { isSafeHttpUrl } from '../../../shared/utils/file-validation';

@Component({
  selector: 'app-recommendation-card',
  imports: [RouterLink, ScoreMeter, SkillChips],
  templateUrl: './recommendation-card.html',
  styleUrl: './recommendation-card.scss',
})
export class RecommendationCard {
  readonly rec = input.required<Recommendation>();
  readonly rank = input.required<number>();

  readonly score = computed(() => Math.round(this.rec().matchScore));
  // Only plain http(s) links are allowed for the external apply button.
  readonly applyUrl = computed(() => {
    const url = this.rec().jobUrl;
    return isSafeHttpUrl(url) ? url : null;
  });

  round(n: number): number {
    return Math.round(n);
  }
}