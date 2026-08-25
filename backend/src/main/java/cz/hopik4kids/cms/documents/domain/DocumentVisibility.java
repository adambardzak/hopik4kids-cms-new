package cz.hopik4kids.cms.documents.domain;

/** Who can see a document (prd §6A.8 B). */
public enum DocumentVisibility {
    /** Visible to trainers (and admins/owners). */
    TRAINERS,
    /** Admin/owner only. */
    ADMIN
}
