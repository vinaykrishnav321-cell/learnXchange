package com.learnxchange.model;

import java.util.List;

/** One ranked suggestion. All component scores are 0..1; percentage is 0..100. */
public record MatchResult(User user, double percentage,
                          double skillScore, double reverseScore, double proficiencyScore,
                          double availabilityScore, double ratingScore,
                          List<String> theyTeachMe, List<String> iTeachThem) { }
