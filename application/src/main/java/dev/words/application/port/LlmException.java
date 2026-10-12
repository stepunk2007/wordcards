package dev.words.application.port;

/**
 * The LLM could not produce a word card. The {@link Reason} tells the caller what to say to the user
 * and whether trying again later makes sense.
 */
public class LlmException extends RuntimeException {

    public enum Reason {
        /** No answer in time; retrying later may help. */
        TIMEOUT(true),
        /** The provider is down, overloaded or rate limited; retrying later may help. */
        UNAVAILABLE(true),
        /** Missing or invalid API key, wrong model name: needs the operator, not the user. */
        MISCONFIGURED(false),
        /** The answer did not match the expected format. */
        INVALID_RESPONSE(true),
        /** The model declined to answer, e.g. for offensive input. */
        REFUSED(false);

        private final boolean retryable;

        Reason(boolean retryable) {
            this.retryable = retryable;
        }

        public boolean retryable() {
            return retryable;
        }
    }

    private final Reason reason;

    public LlmException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public LlmException(Reason reason, String message) {
        this(reason, message, null);
    }

    public Reason reason() {
        return reason;
    }
}
