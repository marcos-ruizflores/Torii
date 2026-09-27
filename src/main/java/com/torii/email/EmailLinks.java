package com.torii.email;

/** Frontend routes the emails link to. Tokens are base64url, safe in a query string. */
final class EmailLinks {

    private EmailLinks() {}

    static String verifyEmail(String appUrl, String token) {
        return appUrl + "/verificar-email?token=" + token;
    }

    static String resetPassword(String appUrl, String token) {
        return appUrl + "/restablecer-contrasena?token=" + token;
    }
}
