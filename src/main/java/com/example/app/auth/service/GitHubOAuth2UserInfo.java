package com.example.app.auth.service;

import java.util.Map;

/**
 * OAuth2 user info implementation for GitHub
 */
public class GitHubOAuth2UserInfo extends OAuth2UserInfo {

    public GitHubOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return ((Integer) attributes.get("id")).toString();
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getImageUrl() {
        return (String) attributes.get("avatar_url");
    }

    @Override
    public String getFirstName() {
        String name = getName();
        if (name != null && name.contains(" ")) {
            return name.split(" ")[0];
        }
        return name;
    }

    @Override
    public String getLastName() {
        String name = getName();
        if (name != null && name.contains(" ")) {
            String[] nameParts = name.split(" ");
            if (nameParts.length > 1) {
                return nameParts[nameParts.length - 1];
            }
        }
        return "";
    }
}
