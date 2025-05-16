package org.example.paymentservice.dtos;

import java.util.List;

public class TokenIntrospectionResponseDTO {
    private boolean active;
    private String sub;
    private String email;
    private List<String> roles;
    private List<String> scopes;
    private Long exp;

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getSub() { return sub; }
    public void setSub(String sub) { this.sub = sub; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }

    public List<String> getScopes() { return scopes; }
    public void setScopes(List<String> scopes) { this.scopes = scopes; }

    public Long getExp() { return exp; }
    public void setExp(Long exp) { this.exp = exp; }
}
