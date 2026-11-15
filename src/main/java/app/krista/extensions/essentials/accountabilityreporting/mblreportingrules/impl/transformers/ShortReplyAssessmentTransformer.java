package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.transformers;

import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities.ShortReplyAssessment;
import app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.impl.rules.ShortReplyDetector;

/** Stateless {@link ShortReplyDetector.ShortReplyResult} → {@link ShortReplyAssessment} transformer. */
public final class ShortReplyAssessmentTransformer {

    private ShortReplyAssessmentTransformer() {
    }

    public static ShortReplyAssessment transform(ShortReplyDetector.ShortReplyResult result) {
        if (result == null) {
            return null;
        }
        ShortReplyAssessment e = new ShortReplyAssessment();
        e.subject = result.subject();
        e.isShortReply = result.isShortReply();
        e.isReplyToOurMail = result.isReplyToOurMail();
        e.ownWordCount = (double) result.ownWordCount();
        e.reason = result.reason();
        return e;
    }
}
