import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/HiddenFieldServlet")
public class HiddenFieldServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();

        String username = request.getParameter("username");

        out.println("<h2>Hidden Form Field</h2>");

        out.println("<form action='HiddenFieldServlet' method='post'>");

        out.println("<input type='hidden' name='username' value='"
                + username + "'>");

        out.println("<input type='submit' value='Continue'>");

        out.println("</form>");
    }

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html");

        PrintWriter out = response.getWriter();

        String username = request.getParameter("username");

        out.println("<h2>Hidden Form Field</h2>");
        out.println("Welcome, " + username);
    }
}