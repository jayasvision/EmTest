package io.github.emtest.application.config.oauth2;

import com.mongodb.DBObject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;

import java.io.Serializable;
import java.util.*;

/**
 * Converter to deserialize back into an OAuth2Authentication Object made necessary because
 * Spring Mongo can't map clientAuthentication to authorizationRequest.
 */
@ReadingConverter
public class OAuth2AuthenticationReadConverter implements Converter<DBObject, OAuth2Authentication> {

    @Override
    public OAuth2Authentication convert(DBObject source) {
        DBObject storedRequest = (DBObject)source.get("storedRequest");
        List<String> rsIds = (List<String>)storedRequest.get("resourceIds");
        OAuth2Request oAuth2Request = new OAuth2Request(
            (Map<String, String>)storedRequest.get("requestParameters"),
            (String)storedRequest.get("clientId"),
            getAuthorities((List<Map<String, String>>) storedRequest.get("authorities")),
            (boolean) storedRequest.get("approved"),
            new HashSet<>((List<String>)storedRequest.get("scope")),
            new HashSet<>((List<String>)storedRequest.get("resourceIds")),
            (String)storedRequest.get("redirectUri"),
            new HashSet<>((List<String>)storedRequest.get("responseTypes")),
            (Map<String, Serializable>)storedRequest.get("extensionProperties"));

        DBObject userAuthorization = (DBObject)source.get("userAuthentication");
        Object principal = getPrincipalObject(userAuthorization.get("principal"));
        Authentication userAuthentication = new UsernamePasswordAuthenticationToken(principal,
                userAuthorization.get("credentials"), getAuthorities((List<Map<String, String>>) userAuthorization.get("authorities")));

        return new OAuth2Authentication(oAuth2Request,  userAuthentication );
    }

    private Object getPrincipalObject(Object principal) {
        if(principal instanceof DBObject) {
            DBObject principalDBObject = (DBObject)principal;

            String userName = (String) principalDBObject.get("username");
            String password = "";
            boolean enabled = (boolean) principalDBObject.get("enabled");
            boolean accountNonExpired = (boolean) principalDBObject.get("accountNonExpired");
            boolean credentialsNonExpired = (boolean) principalDBObject.get("credentialsNonExpired");
            boolean accountNonLocked = (boolean) principalDBObject.get("accountNonLocked");

            return new org.springframework.security.core.userdetails.User(userName, password, enabled,
                    accountNonExpired, credentialsNonExpired, accountNonLocked, getAuthorities((List<Map<String, String>>) principalDBObject.get("authorities")));
        } else {
            return principal;
        }
    }

    private Collection<GrantedAuthority> getAuthorities(List<Map<String, String>> authorities) {
        Set<GrantedAuthority> grantedAuthorities = new HashSet<>(authorities.size());
        for(Map<String, String> authority : authorities) {
            grantedAuthorities.add(new SimpleGrantedAuthority(authority.get("role")));
        }
        return grantedAuthorities;
    }
}
