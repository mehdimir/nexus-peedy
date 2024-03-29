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
package org.sonatype.nexus.proxy.maven;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.configuration.ConfigurationException;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.RemoteAccessException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.events.RepositoryConfigurationUpdatedEvent;
import org.sonatype.nexus.proxy.events.RepositoryEventEvictUnusedItems;
import org.sonatype.nexus.proxy.events.RepositoryEventRecreateMavenMetadata;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.maven.EvictUnusedMavenItemsWalkerProcessor.EvictUnusedMavenItemsWalkerFilter;
import org.sonatype.nexus.proxy.repository.AbstractProxyRepository;
import org.sonatype.nexus.proxy.repository.DefaultRepositoryKind;
import org.sonatype.nexus.proxy.repository.HostedRepository;
import org.sonatype.nexus.proxy.repository.MutableProxyRepositoryKind;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryKind;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.walker.DefaultWalkerContext;
import org.sonatype.nexus.proxy.walker.WalkerException;

/**
 * The abstract (layout unaware) Maven Repository.
 * 
 * @author cstamas
 */
public abstract class AbstractMavenRepository
    extends AbstractProxyRepository
    implements MavenRepository, MavenHostedRepository, MavenProxyRepository
{
    /**
     * Metadata manager.
     */
    @Requirement
    private MetadataManager metadataManager;

    /**
     * The artifact packaging mapper.
     */
    @Requirement
    private ArtifactPackagingMapper artifactPackagingMapper;

    private MutableProxyRepositoryKind repositoryKind;

    private ArtifactStoreHelper artifactStoreHelper;

    /** if download remote index flag changed, need special handling after save */
    private boolean downloadRemoteIndexEnabled = false;

    @Override
    protected AbstractMavenRepositoryConfiguration getExternalConfiguration( boolean forModification )
    {
        return (AbstractMavenRepositoryConfiguration) super.getExternalConfiguration( forModification );
    }

    @Override
    public boolean commitChanges()
        throws ConfigurationException
    {
        boolean result = super.commitChanges();

        if ( result )
        {
            this.downloadRemoteIndexEnabled = false;
        }

        return result;
    }

    @Override
    public boolean rollbackChanges()
    {
        this.downloadRemoteIndexEnabled = false;

        return super.rollbackChanges();
    }

    @Override
    protected RepositoryConfigurationUpdatedEvent getRepositoryConfigurationUpdatedEvent()
    {
        RepositoryConfigurationUpdatedEvent event = super.getRepositoryConfigurationUpdatedEvent();

        event.setDownloadRemoteIndexEnabled( this.downloadRemoteIndexEnabled );

        return event;
    }

    public ArtifactStoreHelper getArtifactStoreHelper()
    {
        if ( artifactStoreHelper == null )
        {
            artifactStoreHelper = new ArtifactStoreHelper( this );
        }

        return artifactStoreHelper;
    }

    public ArtifactPackagingMapper getArtifactPackagingMapper()
    {
        return artifactPackagingMapper;
    }

    /**
     * Override the "default" kind with Maven specifics.
     */
    public RepositoryKind getRepositoryKind()
    {
        if ( repositoryKind == null )
        {
            repositoryKind =
                new MutableProxyRepositoryKind( this, Arrays.asList( new Class<?>[] { MavenRepository.class } ),
                    new DefaultRepositoryKind( MavenHostedRepository.class, null ), new DefaultRepositoryKind(
                        MavenProxyRepository.class, null ) );
        }

        return repositoryKind;
    }

    @Override
    public Collection<String> evictUnusedItems( ResourceStoreRequest request, final long timestamp )
    {
        if ( getRepositoryKind().isFacetAvailable( ProxyRepository.class ) )
        {
            Collection<String> result =
                doEvictUnusedItems( request, timestamp, new EvictUnusedMavenItemsWalkerProcessor( timestamp ),
                    new EvictUnusedMavenItemsWalkerFilter() );

            getApplicationEventMulticaster().notifyEventListeners( new RepositoryEventEvictUnusedItems( this ) );

            return result;
        }
        else
        {
            return super.evictUnusedItems( request, timestamp );
        }
    }

    public boolean recreateMavenMetadata( ResourceStoreRequest request )
    {
        if ( !getRepositoryKind().isFacetAvailable( HostedRepository.class ) )
        {
            return false;
        }

        if ( StringUtils.isEmpty( request.getRequestPath() ) )
        {
            request.setRequestPath( RepositoryItemUid.PATH_ROOT );
        }

        try
        {
            if ( !this.getLocalStorage().containsItem( this, request ) )
            {
                getLogger().info(
                    "Skip rebuilding Maven2 Metadata in repository ID='" + getId()
                        + "' because it does not contain path='" + request.getRequestPath() + "'." );

                return false;
            }
        }
        catch ( StorageException e )
        {
            getLogger().warn( "Skip rebuilding Maven2 Metadata in repository ID='" + getId() + "'.", e );

            return false;
        }

        getLogger().info(
            "Recreating Maven2 metadata in repository ID='" + getId() + "' from path='" + request.getRequestPath()
                + "'" );

        return doRecreateMavenMetadata( request );
    }

    protected boolean doRecreateMavenMetadata( ResourceStoreRequest request )
    {
        RecreateMavenMetadataWalkerProcessor wp = new RecreateMavenMetadataWalkerProcessor( this.getLogger() );

        DefaultWalkerContext ctx = new DefaultWalkerContext( this, request );

        ctx.getProcessors().add( wp );

        try
        {
            getWalker().walk( ctx );
        }
        catch ( WalkerException e )
        {
            if ( !( e.getWalkerContext().getStopCause() instanceof ItemNotFoundException ) )
            {
                // everything that is not ItemNotFound should be reported,
                // otherwise just neglect it
                throw e;
            }
        }

        getApplicationEventMulticaster().notifyEventListeners( new RepositoryEventRecreateMavenMetadata( this ) );

        return !ctx.isStopped();
    }

    public boolean isDownloadRemoteIndexes()
    {
        return getExternalConfiguration( false ).isDownloadRemoteIndex();
    }

    public void setDownloadRemoteIndexes( boolean downloadRemoteIndexes )
    {
        boolean oldValue = isDownloadRemoteIndexes();
        boolean newValue = downloadRemoteIndexes;

        getExternalConfiguration( true ).setDownloadRemoteIndex( downloadRemoteIndexes );

        if ( oldValue == false && newValue == true )
        {
            this.downloadRemoteIndexEnabled = true;
        }
    }

    public RepositoryPolicy getRepositoryPolicy()
    {
        return getExternalConfiguration( false ).getRepositoryPolicy();
    }

    public void setRepositoryPolicy( RepositoryPolicy repositoryPolicy )
    {
        getExternalConfiguration( true ).setRepositoryPolicy( repositoryPolicy );
    }

    public boolean isCleanseRepositoryMetadata()
    {
        return getExternalConfiguration( false ).isCleanseRepositoryMetadata();
    }

    public void setCleanseRepositoryMetadata( boolean cleanseRepositoryMetadata )
    {
        getExternalConfiguration( true ).setCleanseRepositoryMetadata( cleanseRepositoryMetadata );
    }

    public ChecksumPolicy getChecksumPolicy()
    {
        return getExternalConfiguration( false ).getChecksumPolicy();
    }

    public void setChecksumPolicy( ChecksumPolicy checksumPolicy )
    {
        getExternalConfiguration( true ).setChecksumPolicy( checksumPolicy );
    }

    public int getArtifactMaxAge()
    {
        return getExternalConfiguration( false ).getArtifactMaxAge();
    }

    public void setArtifactMaxAge( int maxAge )
    {
        getExternalConfiguration( true ).setArtifactMaxAge( maxAge );
    }

    public int getMetadataMaxAge()
    {
        return getExternalConfiguration( false ).getMetadataMaxAge();
    }

    public void setMetadataMaxAge( int metadataMaxAge )
    {
        getExternalConfiguration( true ).setMetadataMaxAge( metadataMaxAge );
    }

    public abstract boolean shouldServeByPolicies( ResourceStoreRequest request );

    public void storeItemWithChecksums( ResourceStoreRequest request, InputStream is, Map<String, String> userAttributes )
        throws UnsupportedStorageOperationException, IllegalOperationException, StorageException, AccessDeniedException
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "storeItemWithChecksums() :: " + request.getRequestPath() );
        }

        getArtifactStoreHelper().storeItemWithChecksums( request, is, userAttributes );
    }

    public void deleteItemWithChecksums( ResourceStoreRequest request )
        throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException,
        StorageException, AccessDeniedException
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "deleteItemWithChecksums() :: " + request.getRequestPath() );
        }

        getArtifactStoreHelper().deleteItemWithChecksums( request );
    }

    public void storeItemWithChecksums( boolean fromTask, AbstractStorageItem item )
        throws UnsupportedStorageOperationException, IllegalOperationException, StorageException
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "storeItemWithChecksums() :: " + item.getRepositoryItemUid().toString() );
        }

        getArtifactStoreHelper().storeItemWithChecksums( fromTask, item );
    }

    public void deleteItemWithChecksums( boolean fromTask, ResourceStoreRequest request )
        throws UnsupportedStorageOperationException, IllegalOperationException, ItemNotFoundException, StorageException
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "deleteItemWithChecksums() :: " + request.toString() );
        }

        getArtifactStoreHelper().deleteItemWithChecksums( fromTask, request );
    }

    public MetadataManager getMetadataManager()
    {
        return metadataManager;
    }

    // =================================================================================
    // DefaultRepository customizations

    @Override
    protected StorageItem doRetrieveItem( ResourceStoreRequest request )
        throws IllegalOperationException, ItemNotFoundException, StorageException
    {
        if ( !shouldServeByPolicies( request ) )
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug(
                    "The serving of item " + request.toString() + " is forbidden by Maven repository policy." );
            }

            throw new ItemNotFoundException( request, this );
        }

        return super.doRetrieveItem( request );
    }

    @Override
    public void storeItem( boolean fromTask, StorageItem item )
        throws UnsupportedStorageOperationException, IllegalOperationException, StorageException
    {
        if ( shouldServeByPolicies( new ResourceStoreRequest( item ) ) )
        {
            super.storeItem( fromTask, item );
        }
        else
        {
            String msg =
                "Storing of item " + item.getRepositoryItemUid().toString()
                    + " is forbidden by Maven Repository policy. Because " + getId() + " is a "
                    + getRepositoryPolicy().name() + " repository";

            getLogger().info( msg );

            throw new UnsupportedStorageOperationException( msg );
        }
    }

    @Override
    public boolean isCompatible( Repository repository )
    {
        if ( super.isCompatible( repository ) && MavenRepository.class.isAssignableFrom( repository.getClass() )
            && getRepositoryPolicy().equals( ( (MavenRepository) repository ).getRepositoryPolicy() ) )
        {
            return true;
        }

        return false;
    }

    // =================================================================================
    // DefaultRepository customizations

    @Override
    protected AbstractStorageItem doRetrieveRemoteItem( ResourceStoreRequest request )
        throws ItemNotFoundException, RemoteAccessException, StorageException
    {
        String path = request.getRequestPath();

        if ( !path.endsWith( ".sha1" ) && !path.endsWith( ".md5" ) )
        {
            // we are about to download an artifact from remote repository
            // lets clean any existing (stale) checksum files
            removeLocalChecksum( request );
        }

        return super.doRetrieveRemoteItem( request );
    }

    private void removeLocalChecksum( ResourceStoreRequest request )
        throws StorageException
    {
        try
        {
            request.pushRequestPath( request.getRequestPath() + ".sha1" );

            try
            {
                getLocalStorage().deleteItem( this, request );
            }
            catch ( ItemNotFoundException e )
            {
                // this is exactly what we're trying to achieve
            }

            request.popRequestPath();

            request.pushRequestPath( request.getRequestPath() + ".md5" );

            try
            {
                getLocalStorage().deleteItem( this, request );
            }
            catch ( ItemNotFoundException e )
            {
                // this is exactly what we're trying to achieve
            }

            request.popRequestPath();
        }
        catch ( UnsupportedStorageOperationException e )
        {
            // huh?
        }

    }

    @Override
    protected void markItemRemotelyChecked( ResourceStoreRequest request )
        throws StorageException, ItemNotFoundException
    {
        super.markItemRemotelyChecked( request );

        request.pushRequestPath( request.getRequestPath() + ".sha1" );

        if ( getLocalStorage().containsItem( this, request ) )
        {
            super.markItemRemotelyChecked( request );
        }

        request.popRequestPath();

        request.pushRequestPath( request.getRequestPath() + ".md5" );

        if ( getLocalStorage().containsItem( this, request ) )
        {
            super.markItemRemotelyChecked( request );
        }

        request.popRequestPath();
    }
}
