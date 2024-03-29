/**
 * Sonatype Nexus (TM) Open Source Version.
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://nexus.sonatype.org/dev/attributions.html
 * This program is licensed to you under Version 3 only of the GNU General Public License as published by the Free Software Foundation.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License Version 3 for more details.
 * You should have received a copy of the GNU General Public License Version 3 along with this program.
 * If not, see http://www.gnu.org/licenses/.
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc.
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */
package org.sonatype.nexus.rest.repositories;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Collection;

import org.codehaus.plexus.component.annotations.Requirement;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.configuration.application.AuthenticationInfoConverter;
import org.sonatype.nexus.configuration.application.GlobalHttpProxySettings;
import org.sonatype.nexus.configuration.application.GlobalRemoteConnectionSettings;
import org.sonatype.nexus.configuration.model.CRemoteAuthentication;
import org.sonatype.nexus.configuration.model.CRemoteConnectionSettings;
import org.sonatype.nexus.configuration.model.CRemoteHttpProxySettings;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.maven.ChecksumPolicy;
import org.sonatype.nexus.proxy.maven.MavenProxyRepository;
import org.sonatype.nexus.proxy.maven.MavenRepository;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.registry.ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.RemoteStatus;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.ShadowRepository;
import org.sonatype.nexus.rest.AbstractNexusPlexusResource;
import org.sonatype.nexus.rest.NexusCompat;
import org.sonatype.nexus.rest.NoSuchRepositoryAccessException;
import org.sonatype.nexus.rest.global.AbstractGlobalConfigurationPlexusResource;
import org.sonatype.nexus.rest.model.AuthenticationSettings;
import org.sonatype.nexus.rest.model.RemoteConnectionSettings;
import org.sonatype.nexus.rest.model.RemoteHttpProxySettings;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryListResource;
import org.sonatype.nexus.rest.model.RepositoryListResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;
import org.sonatype.nexus.rest.model.RepositoryResource;
import org.sonatype.nexus.rest.model.RepositoryResourceRemoteStorage;
import org.sonatype.nexus.rest.model.RepositoryResourceResponse;
import org.sonatype.nexus.rest.model.RepositoryShadowResource;

public abstract class AbstractRepositoryPlexusResource
    extends AbstractNexusPlexusResource
{
    /** Key to store Repo with which we work against. */
    public static final String REPOSITORY_ID_KEY = "repositoryId";

    /** Repo type hosted. */
    public static final String REPO_TYPE_HOSTED = "hosted";

    /** Repo type proxied. */
    public static final String REPO_TYPE_PROXIED = "proxy";

    /** Repo type virtual (shadow in nexus). */
    public static final String REPO_TYPE_VIRTUAL = "virtual";

    /** Repo type group. */
    public static final String REPO_TYPE_GROUP = "group";

    @Requirement
    private RepositoryTypeRegistry repositoryTypeRegistry;

    @Requirement
    private AuthenticationInfoConverter authenticationInfoConverter;

    @Requirement
    private GlobalRemoteConnectionSettings globalRemoteConnectionSettings;

    @Requirement
    private GlobalHttpProxySettings globalHttpProxySettings;

    @Requirement
    private ApplicationConfiguration applicationConfiguration;

    protected AuthenticationInfoConverter getAuthenticationInfoConverter()
    {
        return authenticationInfoConverter;
    }

    protected GlobalRemoteConnectionSettings getGlobalRemoteConnectionSettings()
    {
        return globalRemoteConnectionSettings;
    }

    protected GlobalHttpProxySettings getGlobalHttpProxySettings()
    {
        return globalHttpProxySettings;
    }

    protected ApplicationConfiguration getApplicationConfiguration()
    {
        return applicationConfiguration;
    }

    /**
     * Pull the repository Id out of the Request.
     *
     * @param request
     * @return
     */
    protected String getRepositoryId( Request request )
    {
        return request.getAttributes().get( REPOSITORY_ID_KEY ).toString();
    }

    // CLEAN
    public String getRestRepoRemoteStatus( ProxyRepository repository, Request request, Response response )
        throws ResourceException
    {
        Form form = request.getResourceRef().getQueryAsForm();

        boolean forceCheck = form.getFirst( "forceCheck" ) != null;

        RemoteStatus rs =
            repository.getRemoteStatus( new ResourceStoreRequest( RepositoryItemUid.PATH_ROOT ), forceCheck );

        if ( RemoteStatus.UNKNOWN.equals( rs ) )
        {
            // set status to ACCEPTED, since we have incomplete info
            response.setStatus( Status.SUCCESS_ACCEPTED );
        }

        return rs == null ? null : rs.toString();
    }

    // CLEAN
    protected String _getRepoFormat( String role, String hint )
    {
        ContentClass cc = repositoryTypeRegistry.getRepositoryContentClass( role, hint );

        if ( cc != null )
        {
            return cc.getId();
        }
        else
        {
            return null;
        }
    }

    // clean
    public String getRestRepoType( Repository repository )
    {
        if ( repository.getRepositoryKind().isFacetAvailable( ProxyRepository.class ) )
        {
            return REPO_TYPE_PROXIED;
        }
        else if ( repository.getRepositoryKind().isFacetAvailable( HostedRepository.class ) )
        {
            return REPO_TYPE_HOSTED;
        }
        else if ( repository.getRepositoryKind().isFacetAvailable( ShadowRepository.class ) )
        {
            return REPO_TYPE_VIRTUAL;
        }
        else if ( repository.getRepositoryKind().isFacetAvailable( GroupRepository.class ) )
        {
            return REPO_TYPE_GROUP;
        }
        else
        {
            throw new IllegalArgumentException( "The passed model with class" + repository.getClass().getName()
                + " is not recognized!" );
        }
    }

    protected RepositoryListResourceResponse listRepositories( Request request, boolean allReposes )
        throws ResourceException
    {
        return listRepositories( request, allReposes, true );
    }

    // clean
    protected RepositoryListResourceResponse listRepositories( Request request, boolean allReposes,
                                                               boolean includeGroups )
        throws ResourceException
    {
        RepositoryListResourceResponse result = new RepositoryListResourceResponse();

        RepositoryListResource repoRes;

        Collection<Repository> repositories = getRepositoryRegistry().getRepositories();

        for ( Repository repository : repositories )
        {
            // To save UI changes at the moment, not including groups in repo call
            if ( ( allReposes || repository.isUserManaged() )
                && ( includeGroups || !repository.getRepositoryKind().isFacetAvailable( GroupRepository.class ) ) )
            {
                repoRes = new RepositoryListResource();

                repoRes.setResourceURI( createRepositoryReference( request, repository.getId() ).toString() );
                
                repoRes.setContentResourceURI( createRepositoryContentReference( request, repository.getId() ).toString() );
                
                repoRes.setRepoType( getRestRepoType( repository ) );
                
                repoRes.setProvider( NexusCompat.getRepositoryProviderHint( repository ) );
                
                repoRes.setProviderRole( NexusCompat.getRepositoryProviderRole( repository ) );

                repoRes.setFormat( repository.getRepositoryContentClass().getId() );

                repoRes.setId( repository.getId() );

                repoRes.setName( repository.getName() );

                repoRes.setUserManaged( repository.isUserManaged() );

                repoRes.setExposed( repository.isExposed() );

                repoRes.setEffectiveLocalStorageUrl( repository.getLocalUrl() );

                if ( repository.getRepositoryKind().isFacetAvailable( MavenRepository.class ) )
                {
                    repoRes.setRepoPolicy( repository.adaptToFacet( MavenRepository.class ).getRepositoryPolicy().toString() );
                }

                if ( repository.getRepositoryKind().isFacetAvailable( ProxyRepository.class ) )
                {
                    repoRes.setRemoteUri( repository.adaptToFacet( ProxyRepository.class ).getRemoteUrl() );
                }

                result.addData( repoRes );
            }
        }

        return result;
    }

    // clean
    protected RepositoryResourceResponse getRepositoryResourceResponse( Request request, String repoId )
        throws ResourceException
    {
        RepositoryResourceResponse result = new RepositoryResourceResponse();

        try
        {
            RepositoryBaseResource resource = null;

            Repository repository = getRepositoryRegistry().getRepository( repoId );

            if ( repository.getRepositoryKind().isFacetAvailable( GroupRepository.class ) )
            {
                // it is a group, not a repo
                throw new ResourceException( Status.CLIENT_ERROR_NOT_FOUND, "Repository Not Found" );
            }

            resource = getRepositoryRestModel( request, repository );

            result.setData( resource );
        }
        catch ( NoSuchRepositoryAccessException e )
        {
            getLogger().warn( "Repository access denied, id=" + repoId );

            throw new ResourceException( Status.CLIENT_ERROR_FORBIDDEN, "Access Denied to Repository" );
        }
        catch ( NoSuchRepositoryException e )
        {
            getLogger().warn( "Repository not found, id=" + repoId );

            throw new ResourceException( Status.CLIENT_ERROR_NOT_FOUND, "Repository Not Found" );
        }
        return result;
    }

    /**
     * Converting App model to REST DTO.
     */
    public RepositoryBaseResource getRepositoryRestModel( Request request, Repository repository )
    {
        RepositoryResource resource = null;

        if ( repository.getRepositoryKind().isFacetAvailable( ProxyRepository.class ) )
        {
            resource = getRepositoryProxyRestModel( repository.adaptToFacet( ProxyRepository.class ) );
        }
        else if ( repository.getRepositoryKind().isFacetAvailable( ShadowRepository.class ) )
        {
            return getRepositoryShadowRestModel( request, repository.adaptToFacet( ShadowRepository.class ) );
        }
        else
        {
            resource = new RepositoryResource();
        }
        
        resource.setContentResourceURI( createRepositoryContentReference( request, repository.getId() ).toString() );

        resource.setProvider( NexusCompat.getRepositoryProviderHint( repository ) );
        
        resource.setProviderRole( NexusCompat.getRepositoryProviderRole( repository ) );

        resource.setFormat( repository.getRepositoryContentClass().getId() );

        resource.setRepoType( getRestRepoType( repository ) );

        resource.setId( repository.getId() );

        resource.setName( repository.getName() );

        resource.setWritePolicy( repository.getWritePolicy().name() );

        resource.setBrowseable( repository.isBrowseable() );

        resource.setIndexable( repository.isSearchable() );

        resource.setExposed( repository.isExposed() );

        resource.setNotFoundCacheTTL( repository.getNotFoundCacheTimeToLive() );

        // TODO: remove the default local storage, this is a work around for NEXUS-1994
        // the new 1.4 API doesn't store the default URL, well, it is part of the CRepo, but it is not exposed.
        // so we can figure it out again, I think the default local Storage should be removed from the REST message
        // which is part of the reason for not exposing it. The other part is it is not used anywhere except to set
        // the localUrl if not already set.

        File defaultStorageFile =
            new File( new File( this.applicationConfiguration.getWorkingDirectory(), "storage" ), repository.getId() );
        // make sure both URLs either end with '/' or do not end with '/'
        String defaultLocalStorage = "";
        try
        {
            defaultLocalStorage = defaultStorageFile.toURL().toString();
            defaultLocalStorage =
                ( defaultLocalStorage.endsWith( "/" ) ) ? defaultLocalStorage : defaultLocalStorage + "/";
        }
        catch ( MalformedURLException e )
        {
            this.getLogger().warn( "Could not figure out the default storage URL for repository: " + repository.getId() );
        }

        String currentLocalStorage = repository.getLocalUrl();
        currentLocalStorage = ( currentLocalStorage.endsWith( "/" ) ) ? currentLocalStorage : currentLocalStorage + "/";

        resource.setDefaultLocalStorageUrl( defaultLocalStorage );

        // now if the currentLocalStorage is different from the override local storage set it.
        if ( !defaultLocalStorage.equals( currentLocalStorage ) )
        {
            resource.setOverrideLocalStorageUrl( currentLocalStorage );
        }

        if ( repository.getRepositoryKind().isFacetAvailable( MavenRepository.class ) )
        {
            resource.setRepoPolicy( repository.adaptToFacet( MavenRepository.class ).getRepositoryPolicy().toString() );

            if ( repository.getRepositoryKind().isFacetAvailable( MavenProxyRepository.class ) )
            {
                resource.setChecksumPolicy( repository.adaptToFacet( MavenProxyRepository.class ).getChecksumPolicy().toString() );

                resource.setDownloadRemoteIndexes( repository.adaptToFacet( MavenProxyRepository.class ).isDownloadRemoteIndexes() );
            }
        }
        //as this is a required field on ui, we need this to be set for non-maven type repos
        else
        {
            resource.setRepoPolicy( RepositoryPolicy.MIXED.name() );
            resource.setChecksumPolicy( ChecksumPolicy.IGNORE.name() );
            resource.setDownloadRemoteIndexes( false );
        }

        return resource;

    }

    /**
     * Converting App model to REST DTO.
     */
    public RepositoryProxyResource getRepositoryProxyRestModel( ProxyRepository repository )
    {
        RepositoryProxyResource resource = new RepositoryProxyResource();

        resource.setRemoteStorage( new RepositoryResourceRemoteStorage() );

        resource.getRemoteStorage().setRemoteStorageUrl( repository.getRemoteUrl() );

        resource.getRemoteStorage().setAuthentication(
                                                       AbstractGlobalConfigurationPlexusResource.convert( NexusCompat.getRepositoryRawConfiguration(
                                                                                                                                                     repository ).getRemoteStorage().getAuthentication() ) );

        resource.getRemoteStorage().setConnectionSettings(
                                                           AbstractGlobalConfigurationPlexusResource.convert( NexusCompat.getRepositoryRawConfiguration(
                                                                                                                                                         repository ).getRemoteStorage().getConnectionSettings() ) );

        resource.getRemoteStorage().setHttpProxySettings(
                                                          AbstractGlobalConfigurationPlexusResource.convert( NexusCompat.getRepositoryRawConfiguration(
                                                                                                                                                        repository ).getRemoteStorage().getHttpProxySettings() ) );

        if ( repository.getRepositoryKind().isFacetAvailable( MavenProxyRepository.class ) )
        {
            resource.setArtifactMaxAge( repository.adaptToFacet( MavenProxyRepository.class ).getArtifactMaxAge() );

            resource.setMetadataMaxAge( repository.adaptToFacet( MavenProxyRepository.class ).getMetadataMaxAge() );
        }

        return resource;
    }

    public RepositoryShadowResource getRepositoryShadowRestModel( Request request, ShadowRepository shadow )
    {
        RepositoryShadowResource resource = new RepositoryShadowResource();

        resource.setId( shadow.getId() );

        resource.setName( shadow.getName() );

        resource.setContentResourceURI( createRepositoryContentReference( request, shadow.getId() ).toString() );

        resource.setProvider( NexusCompat.getRepositoryProviderHint( shadow ) );

        resource.setRepoType( REPO_TYPE_VIRTUAL );

        resource.setFormat( shadow.getRepositoryContentClass().getId() );

        resource.setShadowOf( shadow.getMasterRepositoryId() );

        resource.setSyncAtStartup( shadow.isSynchronizeAtStartup() );

        resource.setExposed( shadow.isExposed() );

        return resource;
    }

    protected CRemoteAuthentication convertAuthentication( AuthenticationSettings authentication, String oldPassword )
    {
        if ( authentication == null )
        {
            return null;
        }

        CRemoteAuthentication appModelSettings = new CRemoteAuthentication();
        appModelSettings.setUsername( authentication.getUsername() );
        appModelSettings.setPassword( this.getActualPassword( authentication.getPassword(), oldPassword ) );
        appModelSettings.setNtlmDomain( authentication.getNtlmDomain() );
        appModelSettings.setNtlmHost( authentication.getNtlmHost() );

        return appModelSettings;
    }

    protected CRemoteHttpProxySettings convertHttpProxySettings( RemoteHttpProxySettings remoteHttpProxySettings,
                                                                 String oldPassword )
    {
        if ( remoteHttpProxySettings == null )
        {
            return null;
        }

        CRemoteHttpProxySettings httpProxySettings = new CRemoteHttpProxySettings();

        httpProxySettings.setProxyHostname( remoteHttpProxySettings.getProxyHostname() );

        httpProxySettings.setProxyPort( remoteHttpProxySettings.getProxyPort() );

        httpProxySettings.setAuthentication( convertAuthentication( remoteHttpProxySettings.getAuthentication(),
                                                                    oldPassword ) );

        return httpProxySettings;
    }

    protected CRemoteConnectionSettings convertRemoteConnectionSettings(
                                                                         RemoteConnectionSettings remoteConnectionSettings )
    {
        if ( remoteConnectionSettings == null )
        {
            return null;
        }

        CRemoteConnectionSettings cRemoteConnectionSettings = new CRemoteConnectionSettings();

        cRemoteConnectionSettings.setConnectionTimeout( remoteConnectionSettings.getConnectionTimeout() * 1000 );

        cRemoteConnectionSettings.setQueryString( remoteConnectionSettings.getQueryString() );

        cRemoteConnectionSettings.setRetrievalRetryCount( remoteConnectionSettings.getRetrievalRetryCount() );

        cRemoteConnectionSettings.setUserAgentCustomizationString( remoteConnectionSettings.getUserAgentString() );

        return cRemoteConnectionSettings;
    }
}