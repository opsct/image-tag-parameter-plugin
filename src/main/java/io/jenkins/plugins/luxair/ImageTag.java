package io.jenkins.plugins.luxair;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import kong.unirest.GetRequest;
import kong.unirest.HttpResponse;
import kong.unirest.Interceptor;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONObject;

public class ImageTag {

    private static final Logger logger = Logger.getLogger(ImageTag.class.getName());
    private static final Interceptor errorInterceptor = new ErrorInterceptor();

    private ImageTag() {
        throw new IllegalStateException("Utility class");
    }

    public static List<String> getTags(String image, String registry, String filter, String user, String password, boolean isGooglePrivateRegistry) {
        String[] authService = getAuthService(registry);
        String token = getAuthToken(authService, image, user, password);
        List<String> tags;
        if (registry.contains("gcr.io") || isGooglePrivateRegistry) {
            tags = getImageTagsFromGooglePrivateRegistry(image, registry, authService[0], token);
        } else {
            tags = getImageTagsFromRegistry(image, registry, authService[0], token);
        }
        return tags.stream().filter(tag -> tag.matches(filter))
            .sorted(Collections.reverseOrder())
            .collect(Collectors.toList());
    }

    private static String[] getAuthService(String registry) {

        String[] rtn = new String[3];
        rtn[0] = ""; // type
        rtn[1] = ""; // realm
        rtn[2] = ""; // service
        String url = registry + "/v2/";

        Unirest.config().reset();
        Unirest.config().enableCookieManagement(false).interceptor(errorInterceptor);
        String headerValue = Unirest.get(url).asEmpty()
            .getHeaders().getFirst("Www-Authenticate");
        Unirest.shutDown();

        String type = "";

        String typePattern = "^(\\S+)";
        Matcher typeMatcher = Pattern.compile(typePattern).matcher(headerValue);
        if (typeMatcher.find()) {
            type = typeMatcher.group(1);
        }

        if (type.equals("Basic")) {
            rtn[0] = "Basic";
            logger.info("AuthService: type=Basic");

            return rtn;
        }

        if (type.equals("Bearer")) {
            String pattern = "Bearer realm=\"(\\S+)\",service=\"(\\S+)\"";
            Matcher m = Pattern.compile(pattern).matcher(headerValue);
            if (m.find()) {
                rtn[0] = "Bearer";
                rtn[1] = m.group(1);
                rtn[2] = m.group(2);
                logger.info("AuthService: type=Bearer, realm=" + rtn[0] + ", service=" + rtn[1]);
            } else {
                logger.warning("No AuthService available from " + url);
            }

            return rtn;
        }

        // Ops!
        logger.warning("Unknown authorization type " + type);

        return rtn;
    }

    private static String getAuthToken(String[] authService, String image, String user, String password) {

        String type = authService[0];
        String token = "";

        if (type.equals("Basic")) {
            try {
                token = Base64.getEncoder().encodeToString((user + ":" + password).getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                logger.warning("UnsupportedEncodingException when creating basic token");
            }

            return token;
        }

        String realm = authService[1];
        String service = authService[2];

        Unirest.config().reset();
        Unirest.config().enableCookieManagement(false).interceptor(errorInterceptor);
        GetRequest request = Unirest.get(realm);
        if (!user.isEmpty() && !password.isEmpty()) {
            logger.info("Basic authentication");
            request = request.basicAuth(user, password);
        } else {
            logger.info("No basic authentication");
        }
        HttpResponse<JsonNode> response = request
            .queryString("service", service)
            .queryString("scope", "repository:" + image + ":pull")
            .asJson();
        if (response.isSuccess()) {
            JSONObject jsonObject = response.getBody().getObject();
            if (jsonObject.has("token")) {
                token = jsonObject.getString("token");
            } else if (jsonObject.has("access_token")) {
                token = jsonObject.getString("access_token");
            } else {
                logger.warning("Token not received");
            }
            logger.info("Token received");
        } else {
            logger.warning("Token not received");
        }
        Unirest.shutDown();

        return token;
    }

    private static List<String> getImageTagsFromGooglePrivateRegistry(String image, String registry, String authType, String token) {
        List<String> tags = new ArrayList<>();
        String getTokenUrl = registry + "/v2/token?service=gcr.io&scope=repository:{image}:pull";
        String getTagsUrl = registry + "/v2/" + image + "/tags/list";

        Unirest.config().reset();
        Unirest.config().enableCookieManagement(false).interceptor(errorInterceptor);
        HttpResponse<JsonNode> response = Unirest.get(getTokenUrl)
            .header("Authorization", authType + " " + token)
            .routeParam("image", image)
            .asJson();
        if (response.isSuccess() && response.getBody().getObject().has("token")) {
            logger.info("HTTP status: " + response.getStatusText());
            HttpResponse<JsonNode> response2 = Unirest.get(getTagsUrl)
            .header("Authorization", "Bearer " + response.getBody().getObject().getString("token"))
            .asJson();
            if (response2.isSuccess()) {
                logger.info("HTTP status: " + response2.getStatusText());
                response2.getBody().getObject()
                    .getJSONArray("tags")
                    .forEach(item -> tags.add(item.toString()));
            } else {
                logger.warning("HTTP status: " + response2.getStatusText());
                tags.add(" " + response2.getStatusText() + " !");
            }
        } else {
            logger.warning("HTTP status: " + response.getStatusText());
            tags.add(" " + response.getStatusText() + " !");
        }
        Unirest.shutDown();

        return tags;
    }

    private static List<String> getImageTagsFromRegistry(String image, String registry, String authType, String token) {
        List<String> tags = new ArrayList<>();
        String url = registry + "/v2/{image}/tags/list";

        Unirest.config().reset();
        Unirest.config().enableCookieManagement(false).interceptor(errorInterceptor);
        HttpResponse<JsonNode> response = Unirest.get(url)
            .header("Authorization", authType + " " + token)
            .routeParam("image", image)
            .asJson();
        if (response.isSuccess()) {
            logger.info("HTTP status: " + response.getStatusText());
            response.getBody().getObject()
                .getJSONArray("tags")
                .forEach(item -> tags.add(item.toString()));
        } else {
            logger.warning("HTTP status: " + response.getStatusText());
            tags.add(" " + response.getStatusText() + " !");
        }
        Unirest.shutDown();

        return tags;
    }
}
