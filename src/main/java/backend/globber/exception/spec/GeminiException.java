package backend.globber.exception.spec;

public class GeminiException extends ClientException {

    public GeminiException() {
        super("Gemini API 호출 중 오류가 발생했습니다.");
    }
}
