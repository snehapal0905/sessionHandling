import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/ReadCookieServlet")
public class ReadCookieServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();

        Cookie[] cookies = request.getCookies();

        String username = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {

                if ("username".equals(cookie.getName())) {
                    username = cookie.getValue();
                    break;
                }
            }
        }

        out.println("<h2>Cookie Session Handling</h2>");

        if (username != null) {
            out.println("Welcome, " + username);
        } else {
            out.println("Cookie not found.");
        }
    }
}