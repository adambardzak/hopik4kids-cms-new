package cz.hopik4kids.cms.core.domain;

/** Program visibility (prd §3B.1, §4.7). Programs have no draft/publish workflow - only status. */
public enum ProgramStatus {
    ACTIVE,
    HIDDEN,
    ARCHIVED
}
