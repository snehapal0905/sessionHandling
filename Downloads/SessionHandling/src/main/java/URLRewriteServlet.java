
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@WebServlet("/URLRewriteServlet")
public class URLRewriteServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();

        String username = request.getParameter("username");

        if (username == null || username.isEmpty()) {
            username = "Guest";
        }

        out.println("<html>");
        out.println("<head><title>URL Rewriting</title></head>");
        out.println("<body>");

        out.println("<h2>URL Rewriting</h2>");
        out.println("<p>Welcome, " + username + "</p>");

        String encodedUsername =
                URLEncoder.encode(username, StandardCharsets.UTF_8);

        String url = response.encodeURL(
                "URLRewriteServlet?username=" + encodedUsername
        );

        out.println("<p>Username is passed through the URL.</p>");
        out.println("<a href='" + url + "'>Continue</a>");

        out.println("</body>");
        out.println("</html>");
    }
}