package org.sonatype.nexus.rest.mirrors;

import java.io.IOException;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.resource.Variant;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.repositories.metadata.NexusRepositoryMetadataHandler;
import org.sonatype.nexus.repository.metadata.MetadataHandlerException;
import org.sonatype.nexus.repository.metadata.model.RepositoryMetadata;
import org.sonatype.nexus.repository.metadata.model.RepositoryMirrorMetadata;
import org.sonatype.nexus.rest.model.MirrorResource;
import org.sonatype.nexus.rest.model.MirrorResourceListResponse;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;
import org.sonatype.plexus.rest.resource.PlexusResource;

@Component( role = PlexusResource.class, hint = "RepositoryPredefinedMirrorListPlexusResource" )
public class RepositoryPredefinedMirrorListPlexusResource
    extends AbstractRepositoryMirrorPlexusResource
{
    @Requirement
    private NexusRepositoryMetadataHandler repoMetadata;
    
    @Requirement
    private RepositoryRegistry repoRegistry;

    public RepositoryPredefinedMirrorListPlexusResource()
    {
        setModifiable( false );
    }

    @Override
    public Object getPayloadInstance()
    {
        return null;
    }

    @Override
    public PathProtectionDescriptor getResourceProtection()
    {
        return new PathProtectionDescriptor( "/repository_predefined_mirrors/*", "authcBasic,perms[nexus:repositorypredefinedmirrors]" );
    }

    @Override
    public String getResourceUri()
    {
        return "/repository_predefined_mirrors/{" + REPOSITORY_ID_KEY + "}";
    }

    @Override
    public Object get( Context context, Request request, Response response, Variant variant )
        throws ResourceException
    {
        MirrorResourceListResponse dto = new MirrorResourceListResponse();

        String repositoryId = this.getRepositoryId( request );

        // get remote metadata
        RepositoryMetadata metadata = this.getMetadata( repositoryId, true );
        // if its missing get local metadata
        if ( metadata == null )
        {
            metadata = this.getMetadata( repositoryId, false );
        }

        for ( RepositoryMirrorMetadata mirror : (List<RepositoryMirrorMetadata>) metadata.getMirrors() )
        {
            MirrorResource resource = new MirrorResource();
            resource.setId( mirror.getId() );
            resource.setUrl( mirror.getUrl() );
            dto.addData( resource );
        }

        return dto;
    }

    private RepositoryMetadata getMetadata( String repositoryId, boolean fromRemote )
        throws ResourceException
    {
        RepositoryMetadata metadata = null;
        try
        {
            if ( fromRemote )
            {
                // we need to figure out the remote URL for this,
                // TODO: this needs to be refactored out of this
                // i should be able to pass in just the repo Id for this
                try
                {
                    ProxyRepository repo = this.repoRegistry.getRepositoryWithFacet( repositoryId, ProxyRepository.class );
                    metadata = repoMetadata.readRemoteRepositoryMetadata( repo.getRemoteUrl() );
                }
                catch ( NoSuchRepositoryException e )
                {
                    getLogger().debug( "ProxyRepository with id: "+ repositoryId +" does not exists, will check local repositories.", e );
                }
            }
            else
            {
                metadata = repoMetadata.readRepositoryMetadata( repositoryId );
            }

        }
        catch ( NoSuchRepositoryException e )
        {
            getLogger().error( "Unable to retrieve metadata", e );
            throw new ResourceException( Status.CLIENT_ERROR_BAD_REQUEST, "Invalid repository ID", e );
        }
        catch ( MetadataHandlerException e )
        {
            getLogger().error( "Unable to retrieve metadata, returning no items", e );
        }
        catch ( IOException e )
        {
            getLogger().error( "Unable to retrieve metadata", e );
            throw new ResourceException( Status.SERVER_ERROR_INTERNAL, "Metadata handling error", e );
        }

        return metadata;
    }

}
