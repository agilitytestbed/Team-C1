package nl.utwente.ing.authorization;

import nl.utwente.ing.session.SessionService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.servlet.http.HttpUtils.parseQueryString;

@Component
public class SessionIdFilter extends GenericFilterBean {
    private static final String AUTHENTICATION_HEADER = "X-Session-ID";
    private static final String SESSIONS_PATH = "/sessions";
    private static final String METHOD_POST = "POST";

    private final SessionService sessionService;

    public SessionIdFilter(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        if (httpServletRequest.getRequestURI().equals(httpServletRequest.getContextPath() + SESSIONS_PATH) && ((HttpServletRequest) servletRequest).getMethod().equals(METHOD_POST)) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            String id = httpServletRequest.getHeader(AUTHENTICATION_HEADER);
            String[] queryId = parseQueryString(httpServletRequest.getQueryString()).getOrDefault("session_id", null);
            if (id != null && this.sessionService.verifyById(id)) {
                SecurityContextHolder.getContext().setAuthentication(new SessionIdAuthentication(id, true));
                filterChain.doFilter(servletRequest, servletResponse);
            } else if (queryId != null && this.sessionService.verifyById(queryId[0])) {
                SecurityContextHolder.getContext().setAuthentication(new SessionIdAuthentication(queryId[0], true));
                filterChain.doFilter(servletRequest, servletResponse);
            } else {
                httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session ID missing or invalid.");
            }
        }
    }
}

