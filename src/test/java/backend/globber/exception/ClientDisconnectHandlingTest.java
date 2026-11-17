package backend.globber.exception;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.IOException;
import java.util.stream.Stream;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

class ClientDisconnectHandlingTest {

    private final MVCGlobalException handler = new MVCGlobalException();
    private final Logger logger = (Logger) LoggerFactory.getLogger(MVCGlobalException.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void attach() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detach() {
        logger.detachAppender(appender);
    }

    static Stream<Exception> clientDisconnects() {
        return Stream.of(
            new AsyncRequestNotUsableException("ServletOutputStream failed to write: java.io.IOException: Broken pipe",
                new IOException("Broken pipe")),
            new ClientAbortException(new IOException("Broken pipe")),
            new IOException("Broken pipe"),
            new IOException("Connection reset by peer"));
    }

    @ParameterizedTest
    @MethodSource("clientDisconnects")
    @DisplayName("클라이언트가 연결을 끊어 응답을 못 쓰는 경우는 ERROR·500으로 남기지 않고 응답을 쓰지 않는다")
    void clientDisconnect_notLoggedAsError(Exception e) {
        // when
        var response = handler.unexpectedExceptionHandler(e);

        // then
        assertThat(response).isNull();
        assertThat(appender.list).noneMatch(event -> event.getLevel().isGreaterOrEqual(Level.WARN));
    }

    @Test
    @DisplayName("연결 끊김이 아닌 IOException은 지금처럼 ERROR와 500이다")
    void otherIoException_stillError() {
        // when
        var response = handler.unexpectedExceptionHandler(new IOException("disk full"));

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(appender.list).anyMatch(event -> event.getLevel() == Level.ERROR);
    }
}
