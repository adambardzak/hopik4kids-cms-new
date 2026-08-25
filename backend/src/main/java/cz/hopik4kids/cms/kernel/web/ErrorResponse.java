package cz.hopik4kids.cms.kernel.web;

/** Error envelope per prd §5.1: {@code { error: { code, message } }}. */
public record ErrorResponse(ErrorBody error) {

    public record ErrorBody(String code, String message) {
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorBody(code, message));
    }
}
