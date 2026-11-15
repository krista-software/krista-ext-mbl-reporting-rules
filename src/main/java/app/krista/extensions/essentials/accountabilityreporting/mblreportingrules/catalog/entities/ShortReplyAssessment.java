package app.krista.extensions.essentials.accountabilityreporting.mblreportingrules.catalog.entities;

import app.krista.extension.impl.anno.Attribute;
import app.krista.extension.impl.anno.Domain;
import app.krista.extension.impl.anno.Entity;
import app.krista.extension.impl.anno.Field;
import app.krista.extension.impl.anno.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Whether a message is a throwaway acknowledgement and whether it replies to mail we sent. Either
 * signal means the message is set aside rather than filed.
 */
@Domain(id = "catEntryDomain_56218221-d59c-4082-b138-6a13f3c2c707",
        name = "Accountability Reporting",
        ecosystemId = "catEntryEcosystem_84b53163-327b-4b1b-8c96-9334d292f9f5",
        ecosystemName = "Essentials",
        ecosystemVersion = "af527a74-6686-4c9a-8ac5-487795bd427f")
@Entity(name = "Short Reply Assessment", id = "localDomainEntity_7c3e9d62-4ba5-5e13-9f36-801d4c915e33",
        primaryKey = "Subject", supportStore = false,
        description = "Whether a message is a short reply and whether it replies to our own mail. "
                + "Word count is measured after quoted text and signatures are removed.")
public class ShortReplyAssessment {

    @ToString
    @Field.Text(name = "Subject", required = false,
            description = "The subject of the message that was assessed.",
            attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public String subject;

    @Field.Boolean(name = "Is Short Reply", required = false,
            description = "True when the sender's own words are an acknowledgement or below the word threshold.",
            attributes = {}, options = {})
    public Boolean isShortReply;

    @Field.Boolean(name = "Is Reply To Our Mail", required = false,
            description = "True when In Reply To or References names one of our own message ids.",
            attributes = {}, options = {})
    public Boolean isReplyToOurMail;

    @Field(name = "Own Word Count", type = "Number", required = false,
            description = "Words left after quoted text, signature and disclaimer are stripped.",
            attributes = {@Attribute(name = "visualWidth", value = "S")}, options = {})
    public Double ownWordCount;

    @Field(name = "Reason", type = "Paragraph", required = false,
            description = "Which rule fired, in plain language.",
            attributes = {@Attribute(name = "visualWidth", value = "L")}, options = {})
    public String reason;

    /** Insertion-ordered field map keyed by the @Field display names. */
    public Map<String, Object> toFields() {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("Subject", subject);
        f.put("Is Short Reply", isShortReply);
        f.put("Is Reply To Our Mail", isReplyToOurMail);
        f.put("Own Word Count", ownWordCount);
        f.put("Reason", reason);
        return f;
    }
}
