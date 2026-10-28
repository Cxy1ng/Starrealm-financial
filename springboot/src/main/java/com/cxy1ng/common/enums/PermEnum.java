package com.cxy1ng.common.enums;

public enum PermEnum {
    USER(1, 9, "USER"),
    ADMIN(10, 14, "ADMIN"),
    SUPER_ADMIN(15, 15, "SUPER_ADMIN");

    private final int minPerm; // 权限最小值
    private final int maxPerm; // 权限最大值
    private final String role; // 角色

    PermEnum(int minPerm, int maxPerm, String role) {
        this.minPerm = minPerm;
        this.maxPerm = maxPerm;
        this.role = role;
    }

    public int getMinPerm() {
        return minPerm;
    }

    public int getMaxPerm() {
        return maxPerm;
    }

    public String getRole() {
        return role;
    }

    public static PermEnum getRoleByPerm(int perm) {
        for (PermEnum role : PermEnum.values()) {
            if (perm >= role.minPerm && perm <= role.maxPerm) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知的权限值：" + perm);
    }
}
