package com.claudemani.domain.usecase

import com.claudemani.domain.repository.ReviewHistoryRepository
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * CalculateStreak (Algorithms v4.20 §11.3). Counts consecutive local
 * calendar days, going backward from today, on which at least one
 * ReviewHistory was recorded (all reviews on the same day count once).
 * A break of exactly one day (today has no review yet, but yesterday
 * did) does NOT reset the streak — the "قانون ملایم" (soft/lenient rule)
 * from the spec.
 *
 * [now] and [zoneId] are supplied by the caller rather than read
 * internally, so this stays deterministic/testable like the other
 * algorithms in this module; the caller passes the device's actual local
 * zone.
 */
class CalculateStreakUseCase @Inject constructor(
    private val reviewHistoryRepository: ReviewHistoryRepository
) {
    suspend operator fun invoke(now: Instant, zoneId: ZoneId = ZoneId.systemDefault()): Int {
        val reviewDays = reviewHistoryRepository.getAll()
            .map { it.reviewedAt.atZone(zoneId).toLocalDate() }
            .distinct()
            .sortedDescending()

        if (reviewDays.isEmpty()) return 0

        val today = now.atZone(zoneId).toLocalDate()
        val mostRecentDay = reviewDays.first()

        if (mostRecentDay != today && mostRecentDay != today.minusDays(1)) {
            return 0
        }

        var streak = 1
        for (i in 1 until reviewDays.size) {
            if (reviewDays[i] == reviewDays[i - 1].minusDays(1)) {
                streak += 1
            } else {
                break
            }
        }
        return streak
    }
}
