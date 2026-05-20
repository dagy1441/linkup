/**
 * Cross-module audit support: {@link Auditable @MappedSuperclass} inherited by every
 * aggregate root and the {@code AuditorAware} implementation reading the JWT subject.
 */
@org.springframework.modulith.NamedInterface("audit")
package com.siide.linkup.core.audit;
