package com.pharmaCx.dms.api.dto;

import com.pharmaCx.dms.domain.enums.UserRole;

public class AuthResponse {

    private String token;
    private String userId;
    private String username;
    private String fullName;
    private UserRole role;
    private String unitId;
    private String unitCode;
    private String unitDisplayName;

    public AuthResponse(String token, String userId, String username, String fullName,
                        UserRole role, String unitId, String unitCode, String unitDisplayName) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.unitId = unitId;
        this.unitCode = unitCode;
        this.unitDisplayName = unitDisplayName;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }

    public String getUnitCode() { return unitCode; }
    public void setUnitCode(String unitCode) { this.unitCode = unitCode; }

    public String getUnitDisplayName() { return unitDisplayName; }
    public void setUnitDisplayName(String unitDisplayName) { this.unitDisplayName = unitDisplayName; }
}
