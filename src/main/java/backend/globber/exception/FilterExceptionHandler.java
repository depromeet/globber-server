package backend.globber.exception;

import backend.globber.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Slf4j
public class FilterExceptionHandler extends OncePerRequestFilter {

    private final ObjectMapper ObjectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (CustomException e) {
            log.error("FilterExceptionHandler: " + e.getResponse());
            setErrorResponse(response, e.getHttpStatus().value(), e.getResponse());
        } catch (Exception e) {
            log.error("FilterExceptionHandler: " + e.getMessage(), e);
            log.error("FilterExceptionHandler: " + "필터 예외가 발생했습니다.");
            ApiResponse<?> err = ApiResponse.fail("필터 내부의 예외가 발생했습니다.");
            setErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, err);
        }
    }

    private void setErrorResponse(HttpServletResponse response, int httpStatus, ApiResponse<?> err)
        throws IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(ObjectMapper.writeValueAsString(err));
    }
}