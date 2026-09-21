import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/CookieServlet")
public class CookieServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();

        String username = request.getParameter("username");

        Cookie cookie = new Cookie("username", username);

        response.addCookie(cookie);

        out.println("<h2>Cookie Session Handling</h2>");
        out.println("Cookie created successfully.<br>");

        out.println("<a href='ReadCookieServlet'>Read Cookie</a>");
    }
}