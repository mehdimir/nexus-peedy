package org.sonatype.nexus.remote.impl;

import javax.ws.rs.Path;

import org.sonatype.nexus.remote.api.StatusService;
import org.sonatype.nexus.remote.domain.Status;

@Path( "/status" )
public class DefaultStatusService
    implements StatusService
{
    public Status getStatus()
    {
        Status result = new Status();

        result.setVersion( "1.0.0" );

        return result;
    }
}
