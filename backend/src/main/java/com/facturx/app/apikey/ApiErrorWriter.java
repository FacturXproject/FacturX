package com.facturx.app.apikey;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Corps d'erreur JSON {"error","message"} ecrit directement par la securite de l'API publique (F17). */
public final class ApiErrorWriter {

    private ApiErrorWriter() {
    }

    public static void write(HttpServletResponse response, int status, String error, String message)
            throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }
}
