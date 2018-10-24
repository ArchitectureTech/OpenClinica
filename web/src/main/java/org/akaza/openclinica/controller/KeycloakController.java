package org.akaza.openclinica.controller;

import net.sf.json.util.JSONUtils;
import org.akaza.openclinica.bean.login.UserAccountBean;
import org.akaza.openclinica.service.KeycloakUser;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.representations.AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.Map;

@Component
public class KeycloakController {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getName());

    public String buildAuthorizeUrl(HttpServletRequest request) {
        AuthzClient authzClient = AuthzClient.create();
        String coreAuthUrl = authzClient.getConfiguration().getAuthServerUrl();
        int port = request.getServerPort();
        String portStr ="";
        if (port != 80 && port != 443) {
            portStr = ":" + port;
        }
        String redirectUri = request.getScheme() + "://" + request.getServerName() + portStr + request.getContextPath() + "/pages/login";
        String authUrl = coreAuthUrl + "/realms/" + authzClient.getConfiguration().getRealm()
                + "/protocol/openid-connect/auth?scope=openid&client_id=" + authzClient.getConfiguration().getResource() +
                "&response_type=code&login=true&redirect_uri=" + redirectUri;
        JSONObject json = null;
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            switch ((entry.getKey())) {

                case "forceRenewAuth":
                    if (json == null)
                        json = new JSONObject();
                    json.put(entry.getKey(), entry.getValue()[0]);
                    UserAccountBean ub = (UserAccountBean) request.getSession().getAttribute("userBean");
                    if (ub != null && StringUtils.isNotEmpty(ub.getName()))
                        json.put("loggedUser", ub.getName());
                    break;
                case "state":
                    if (JSONUtils.mayBeJSON(entry.getValue()[0])) {
                        json = new JSONObject(entry.getValue()[0]);
                        try {
                            json.remove("forceRenewAuth");
                        } catch (Exception e) {
                            logger.error("State parameter:", e);
                        }
                    }
                    break;
                default:
                    if (json == null)
                        json = new JSONObject();
                    json.put(entry.getKey(), entry.getValue()[0]);
                    break;
            }
        };
        if (json != null)
        {
            try {
                authUrl += "&state=" + URLEncoder.encode(json.toString(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.error("Url encoding:", e);
            }
        }
        return authUrl;
    }
    public String getReturnTo(HttpServletRequest req) {
        return "/OpenClinica/MainMenu";
    }

    public String getOcUserUuid(HttpServletRequest req) {
        String ocUserUuid = null;
        final Principal userPrincipal = req.getUserPrincipal();
        if (userPrincipal == null)
            return ocUserUuid;
        KeycloakPrincipal<KeycloakSecurityContext> kp = (KeycloakPrincipal<KeycloakSecurityContext>) ((KeycloakAuthenticationToken) userPrincipal).getPrincipal();
        AccessToken token = kp.getKeycloakSecurityContext().getToken();
        req.getSession().setAttribute("accessToken", kp.getKeycloakSecurityContext().getTokenString());
        KeycloakUser user = new KeycloakUser(token);

        ocUserUuid = (String) user.getUserContext().get("userUuid");
        return ocUserUuid;

    }

}
