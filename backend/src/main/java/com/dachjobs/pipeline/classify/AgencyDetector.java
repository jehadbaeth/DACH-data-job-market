package com.dachjobs.pipeline.classify;

import java.util.regex.Pattern;

/** Ports AGENCY_RX from notebooks/03_silver_clean.py: recruiter/staffing firms. */
public final class AgencyDetector {

    private static final Pattern AGENCY = Pattern.compile(
            "(consulting|personal|recruit|staffing|hays|randstad|michael page|robert half|"
                    + "experis|gulp|solcom|amoria|darwin|huzzle|talent)");

    private AgencyDetector() {
    }

    /** companyNorm must already be folded, e.g. via {@link TitleNormalizer#normalize}. */
    public static boolean isAgency(String companyNorm) {
        return companyNorm != null && AGENCY.matcher(companyNorm).find();
    }
}
