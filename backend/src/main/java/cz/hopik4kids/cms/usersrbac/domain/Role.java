package cz.hopik4kids.cms.usersrbac.domain;

/** Fixed team roles (prd §7.2). Start set: OWNER, ADMIN, TRAINER; others later. */
public enum Role {
    OWNER,
    ADMIN,
    TRAINER,
    ACCOUNTANT,
    /** PR / social media manager: schedule, filming signups, locations, media consents only. */
    PR,
    VIEWER
}
