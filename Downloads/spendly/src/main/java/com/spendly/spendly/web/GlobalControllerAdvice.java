package com.spendly.spendly.web;

import com.spendly.spendly.domain.AppUser;
import com.spendly.spendly.service.BusinessException;
import com.spendly.spendly.service.CurrentUser;
import com.spendly.spendly.service.InsightService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.RequestContextUtils;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final CurrentUser currentUser;
    private final InsightService insightService;

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    /** Current query string without the page parameter, ending in '&' when non-empty. */
    @ModelAttribute("pageQuery")
    public String pageQuery(HttpServletRequest request) {
        String qs = request.getQueryString();
        if (qs == null || qs.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : qs.split("&")) {
            if (!part.startsWith("page=") && !part.isBlank()) {
                sb.append(part).append('&');
            }
        }
        return sb.toString();
    }

    @ModelAttribute("currentUser")
    public AppUser currentUser() {
        return currentUser.find();
    }

    @ModelAttribute("currency")
    public String currency() {
        AppUser user = currentUser.find();
        return user == null ? "₹" : user.getCurrencySymbol();
    }

    /** Risks shown as a count next to Insights in the sidebar. */
    @ModelAttribute("alertCount")
    public long alertCount(HttpServletRequest request) {
        AppUser user = currentUser.find();
        if (user == null || !"GET".equals(request.getMethod())) {
            return 0;
        }
        return insightService.alertCount(user, Periods.today());
    }

    /** Business rule violations: go back to the previous page with the message as a flash error. */
    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request).put("error", ex.getMessage());
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/dashboard");
    }
}
