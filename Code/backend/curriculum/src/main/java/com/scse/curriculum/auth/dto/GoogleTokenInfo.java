package com.scse.curriculum.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleTokenInfo {

    private String iss;
    private String aud;
    private String sub;
    private String email;

    @JsonProperty("email_verified")
    private String emailVerified;

    private String hd;
    private String name;
    private String picture;
}
