package org.sonatype.nexus.remote.api;

import javax.ws.rs.GET;

import org.sonatype.nexus.remote.domain.Status;

public interface StatusService
{
    @GET
    Status getStatus();
}
