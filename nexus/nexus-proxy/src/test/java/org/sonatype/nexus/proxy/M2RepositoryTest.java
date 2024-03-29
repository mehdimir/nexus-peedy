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
package org.sonatype.nexus.proxy;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.codehaus.plexus.util.FileUtils;
import org.sonatype.nexus.artifact.VersionUtils;
import org.sonatype.nexus.configuration.model.CRepositoryCoreConfiguration;
import org.sonatype.nexus.proxy.access.AccessManager;
import org.sonatype.nexus.proxy.events.RepositoryItemEventCache;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StringContentLocator;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.M2Repository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.repository.RepositoryWritePolicy;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.plexus.appevents.Event;
import org.sonatype.plexus.appevents.EventListener;

public class M2RepositoryTest
    extends M2ResourceStoreTest
{
    private static final long A_DAY = 24L * 60L * 60L * 1000L;

    protected static final String SPOOF_RELEASE = "/spoof/spoof/1.0/spoof-1.0.txt";

    protected static final String SPOOF_SNAPSHOT = "/spoof/spoof/1.0-SNAPSHOT/spoof-1.0-SNAPSHOT.txt";

    @Override
    protected String getItemPath()
    {
        return "/activemq/activemq-core/1.2/activemq-core-1.2.jar";
    }

    @Override
    protected ResourceStore getResourceStore()
        throws NoSuchRepositoryException, IOException
    {
        Repository repo1 = getRepositoryRegistry().getRepository( "repo1" );

        repo1.setWritePolicy( RepositoryWritePolicy.ALLOW_WRITE );

        getApplicationConfiguration().saveConfiguration();

        return repo1;
    }

    public void testPoliciesWithRetrieve()
        throws Exception
    {
        M2Repository repository = (M2Repository) getResourceStore();

        // a "release"
        repository.setRepositoryPolicy( RepositoryPolicy.RELEASE );
        repository.getCurrentCoreConfiguration().commitChanges();

        StorageItem item = getResourceStore().retrieveItem( new ResourceStoreRequest( SPOOF_RELEASE, false ) );
        checkForFileAndMatchContents( item );

        try
        {
            item = getResourceStore().retrieveItem( new ResourceStoreRequest( SPOOF_SNAPSHOT, false ) );

            fail( "Should not be able to get snapshot from release repo" );
        }
        catch ( ItemNotFoundException e )
        {
            // good
        }

        // reset NFC
        repository.expireCaches( new ResourceStoreRequest( RepositoryItemUid.PATH_ROOT, true ) );

        // a "snapshot"
        repository.setRepositoryPolicy( RepositoryPolicy.SNAPSHOT );
        repository.getCurrentCoreConfiguration().commitChanges();

        item = getResourceStore().retrieveItem( new ResourceStoreRequest( SPOOF_SNAPSHOT, false ) );
        checkForFileAndMatchContents( item );

        try
        {
            item = getResourceStore().retrieveItem( new ResourceStoreRequest( SPOOF_RELEASE, false ) );

            fail( "Should not be able to get release from snapshot repo" );
        }
        catch ( ItemNotFoundException e )
        {
            // good
        }
    }

    public void testPoliciesWithStore()
        throws Exception
    {
        M2Repository repository = (M2Repository) getResourceStore();

        // a "release"
        repository.setRepositoryPolicy( RepositoryPolicy.RELEASE );
        repository.getCurrentCoreConfiguration().commitChanges();

        DefaultStorageFileItem item =
            new DefaultStorageFileItem( repository, new ResourceStoreRequest( SPOOF_RELEASE ),
                new StringContentLocator( SPOOF_RELEASE ) );

        repository.storeItem( false, item );

        try
        {
            item =
                new DefaultStorageFileItem( repository, new ResourceStoreRequest( SPOOF_SNAPSHOT ),
                    new StringContentLocator( SPOOF_SNAPSHOT ) );

            repository.storeItem( false, item );

            fail( "Should not be able to store snapshot to release repo" );
        }
        catch ( UnsupportedStorageOperationException e )
        {
            // good
        }

        // reset NFC
        repository.expireCaches( new ResourceStoreRequest( RepositoryItemUid.PATH_ROOT, true ) );

        // a "snapshot"
        repository.setRepositoryPolicy( RepositoryPolicy.SNAPSHOT );
        repository.getCurrentCoreConfiguration().commitChanges();

        item =
            new DefaultStorageFileItem( repository, new ResourceStoreRequest( SPOOF_SNAPSHOT ),
                new StringContentLocator( SPOOF_SNAPSHOT ) );

        repository.storeItem( false, item );

        try
        {
            item =
                new DefaultStorageFileItem( repository, new ResourceStoreRequest( SPOOF_RELEASE ),
                    new StringContentLocator( SPOOF_RELEASE ) );

            repository.storeItem( false, item );

            fail( "Should not be able to store release to snapshot repo" );
        }
        catch ( UnsupportedStorageOperationException e )
        {
            // good
        }
    }

    public void testShouldServeByPolicies()
        throws Exception
    {
        M2Repository repository = (M2Repository) getResourceStore();

        String releasePom =
            "/org/codehaus/plexus/plexus-container-default/1.0-alpha-40/plexus-container-default-1.0-alpha-40.pom";
        String releaseArtifact =
            "/org/codehaus/plexus/plexus-container-default/1.0-alpha-40/plexus-container-default-1.0-alpha-40.jar";
        String snapshotPom =
            "/org/codehaus/plexus/plexus-container-default/1.0-alpha-41-SNAPSHOT/plexus-container-default-1.0-alpha-41-20071205.190351-1.pom";
        String snapshotArtifact =
            "/org/codehaus/plexus/plexus-container-default/1.0-alpha-41-SNAPSHOT/plexus-container-default-1.0-alpha-41-20071205.190351-1.jar";
        String metadata1 = "/org/codehaus/plexus/plexus-container-default/maven-metadata.xml";
        String metadataR = "/org/codehaus/plexus/plexus-container-default/1.0-alpha-40/maven-metadata.xml";
        String metadataS = "/org/codehaus/plexus/plexus-container-default/1.0-alpha-41-SNAPSHOT/maven-metadata.xml";
        String someDirectory = "/classworlds/";
        String anyNonArtifactFile = "/any/file.txt";

        ResourceStoreRequest request = new ResourceStoreRequest( "" );

        // it is equiv of repo type: RELEASE
        repository.setRepositoryPolicy( RepositoryPolicy.RELEASE );
        repository.getCurrentCoreConfiguration().commitChanges();

        request.setRequestPath( releasePom );
        assertEquals( true, repository.shouldServeByPolicies( request ) );
        request.setRequestPath( releaseArtifact );
        assertEquals( true, repository.shouldServeByPolicies( request ) );
        request.setRequestPath( snapshotPom );
        assertEquals( false, repository.shouldServeByPolicies( request ) );
        request.setRequestPath( snapshotArtifact );
        assertEquals( false, repository.shouldServeByPolicies( request ) );
        request.setRequestPath( metadata1 );
        assertEquals( true, repository.shouldServeByPolicies( request ) );
        request.setRequestPath( metadataR );
        assertEquals( true, repository.shouldServeByPolicies( request ) );
        request.setRequestPath( metadataS );
        assertEquals( false, repository.shouldServeByPolicies( request ) );
        request.setRequestPath( someDirectory );
        assertEquals( true, repository.shouldServeByPolicies( request ) );
        request.setRequestPath( anyNonArtifactFile );
        assertEquals( true, repository.shouldServeByPolicies( request ) );

        // it is equiv of repo type: SNAPSHOT
        repository.setRepositoryPolicy( RepositoryPolicy.SNAPSHOT );
        repository.getCurrentCoreConfiguration().commitChanges();

        request.setRequestPath( releasePom );
        assertEquals( false, repository.shouldServeByPolicies( request ) );
        request.setRequestPath( releaseArtifact );
        assertEquals( false, repository.shouldServeByPolicies( request ) );
        request.setRequestPath( snapshotPom );
        assertEquals( true, repository.shouldServeByPolicies( request ) );
        request.setRequestPath( snapshotArtifact );
        assertEquals( true, repository.shouldServeByPolicies( request ) );
        request.setRequestPath( metadata1 );
        assertEquals( true, repository.shouldServeByPolicies( request ) );
        request.setRequestPath( metadataR );
        assertEquals( true, repository.shouldServeByPolicies( request ) );
        request.setRequestPath( metadataS );
        assertEquals( true, repository.shouldServeByPolicies( request ) );
        request.setRequestPath( someDirectory );
        assertEquals( true, repository.shouldServeByPolicies( request ) );
        request.setRequestPath( anyNonArtifactFile );
        assertEquals( true, repository.shouldServeByPolicies( request ) );
    }

    public void testGetLatestVersionSimple()
        throws Exception
    {
        M2Repository repository = (M2Repository) getResourceStore();

        List<String> versions = new ArrayList<String>();
        versions.add( "1.0.0" );
        versions.add( "1.0.1" );
        versions.add( "1.0.2" );
        versions.add( "1.1.2" );
        assertEquals( "1.1.2", repository.getLatestVersion( versions ) );
    }

    public void testGetLatestVersionClassifiers()
        throws Exception
    {
        M2Repository repository = (M2Repository) getResourceStore();

        List<String> versions = new ArrayList<String>();
        versions.add( "1.0-alpha-19" );
        versions.add( "1.0-alpha-9-stable-1" );
        versions.add( "1.0-alpha-20" );
        versions.add( "1.0-alpha-21" );
        versions.add( "1.0-alpha-22" );
        versions.add( "1.0-alpha-40" );
        assertEquals( "1.0-alpha-40", repository.getLatestVersion( versions ) );
    }

    public void testIsSnapshot()
        throws Exception
    {
        // M2Repository repository = (M2Repository) getResourceStore();

        assertEquals( false, VersionUtils.isSnapshot( "1.0.0" ) );
        assertEquals( true, VersionUtils.isSnapshot( "1.0.0-SNAPSHOT" ) );
        assertEquals( false, VersionUtils.isSnapshot( "1.0-alpha-25" ) );
        assertEquals( true, VersionUtils.isSnapshot( "1.0-alpha-25-20070518.002146-2" ) );
    }

    public void testExpiration_NEXUS1675()
        throws Exception
    {
        doTestExpiration_NEXUS1675( "/spoof/maven-metadata.xml" );
    }

    public void testExpiration_NEXUS3065()
        throws Exception
    {
        doTestExpiration_NEXUS1675( "/spoof/spoof/1.0/spoof-1.0.txt" );
    }

    public void doTestExpiration_NEXUS1675( String path )
        throws Exception
    {
        CounterListener ch = new CounterListener();

        M2Repository repository = (M2Repository) getResourceStore();

        getApplicationEventMulticaster().addEventListener( ch );

        File mdFile = new File( new File( getBasedir() ), "target/test-classes/repo1" + path );

        assertTrue( mdFile.exists() );

        // ==

        try
        {
            repository.deleteItem( new ResourceStoreRequest( "/spoof", true ) );
        }
        catch ( ItemNotFoundException e )
        {
            // ignore
        }

        repository.setMetadataMaxAge( 0 );
        repository.setArtifactMaxAge( 0 );
        repository.getCurrentCoreConfiguration().commitChanges();

        mdFile.setLastModified( System.currentTimeMillis() - ( 3L * 24L * 60L * 60L * 1000L ) );

        Thread.sleep( 200 ); // wait for FS

        repository.retrieveItem( new ResourceStoreRequest( path, false ) );

        mdFile.setLastModified( System.currentTimeMillis() - ( 2L * 24L * 60L * 60L * 1000L ) );

        Thread.sleep( 200 ); // wait for FS

        repository.retrieveItem( new ResourceStoreRequest( path, false ) );

        mdFile.setLastModified( System.currentTimeMillis() - ( 1L * 24L * 60L * 60L * 1000L ) );

        Thread.sleep( 200 ); // wait for FS

        repository.retrieveItem( new ResourceStoreRequest( path, false ) );

        assertEquals( "Every request should end up in server.", 3, ch.getRequestCount() );

        // ==

        ch.reset();

        try
        {
            repository.deleteItem( new ResourceStoreRequest( "/spoof", true ) );
        }
        catch ( ItemNotFoundException e )
        {
            // ignore
        }

        repository.setMetadataMaxAge( 5 );
        repository.setArtifactMaxAge( -1 );
        repository.getCurrentCoreConfiguration().commitChanges();

        mdFile.setLastModified( System.currentTimeMillis() );

        Thread.sleep( 200 ); // wait for FS

        repository.retrieveItem( new ResourceStoreRequest( path, false ) );

        mdFile.setLastModified( System.currentTimeMillis() );

        Thread.sleep( 200 ); // wait for FS

        repository.retrieveItem( new ResourceStoreRequest( path, false ) );

        mdFile.setLastModified( System.currentTimeMillis() );

        Thread.sleep( 200 ); // wait for FS

        repository.retrieveItem( new ResourceStoreRequest( path, false ) );

        assertEquals( "Only one (1st) of the request should end up in server.", 1, ch.getRequestCount() );
    }

    public void testLocalStorageChanges()
        throws Exception
    {
        M2Repository repository = (M2Repository) getResourceStore();

        String changedUrl = repository.getLocalUrl() + "foo";

        repository.setLocalUrl( changedUrl );

        assertFalse( "Should not be the same!", changedUrl.equals( repository.getLocalUrl() ) );

        repository.getCurrentCoreConfiguration().commitChanges();

        assertTrue( "Should be the same!", changedUrl.equals( repository.getLocalUrl() ) );
    }

    public void testRemoteStorageChanges()
        throws Exception
    {
        M2Repository repository = (M2Repository) getResourceStore();

        String changedUrl = repository.getRemoteUrl() + "/foo";

        repository.setRemoteUrl( changedUrl );

        assertFalse( "Should not be the same!", changedUrl.equals( repository.getRemoteUrl() ) );

        repository.getCurrentCoreConfiguration().commitChanges();

        assertTrue( "Should be the same!", changedUrl.equals( repository.getRemoteUrl() ) );
    }

    public void testProxyLastRequestedAttribute()
        throws Exception
    {
        M2Repository repository = (M2Repository) this.getRepositoryRegistry().getRepository( "repo1" );

        String item = "/org/slf4j/slf4j-api/1.4.3/slf4j-api-1.4.3.pom";
        ResourceStoreRequest request = new ResourceStoreRequest( item );
        request.getRequestContext().put( AccessManager.REQUEST_REMOTE_ADDRESS, "127.0.0.1" );
        StorageItem storageItem = repository.retrieveItem( request );
        long lastRequest = System.currentTimeMillis() - 10 * A_DAY;
        storageItem.setLastRequested( lastRequest );
        repository.storeItem( false, storageItem );

        // now request the object, the lastRequested timestamp should be updated
        StorageItem resultItem = repository.retrieveItem( request );
        Assert.assertTrue( resultItem.getLastRequested() > lastRequest );

        // check the shadow attributes
        AbstractStorageItem shadowStorageItem =
            repository.getLocalStorage().getAttributesHandler().getAttributeStorage().getAttributes(
                repository.createUid( request.getRequestPath() ) );
        Assert.assertEquals( resultItem.getLastRequested(), shadowStorageItem.getLastRequested() );
    }

    public void testHostedLastRequestedAttribute()
        throws Exception
    {
        String itemPath = "/org/test/foo.junk";

        M2Repository repository = (M2Repository) this.getRepositoryRegistry().getRepository( "inhouse" );
        File inhouseLocalStorageDir =
            new File( new URL( ( (CRepositoryCoreConfiguration) repository.getCurrentCoreConfiguration() )
                .getConfiguration( false ).getLocalStorage().getUrl() ).getFile() );

        File artifactFile = new File( inhouseLocalStorageDir, itemPath );
        artifactFile.getParentFile().mkdirs();

        FileUtils.fileWrite( artifactFile.getAbsolutePath(), "Some Text so the file is not empty" );

        ResourceStoreRequest request = new ResourceStoreRequest( itemPath );
        request.getRequestContext().put( AccessManager.REQUEST_REMOTE_ADDRESS, "127.0.0.1" );
        StorageItem storageItem = repository.retrieveItem( request );
        long lastRequest = System.currentTimeMillis() - 10 * A_DAY;
        storageItem.setLastRequested( lastRequest );
        repository.storeItem( false, storageItem );

        // now request the object, the lastRequested timestamp should be updated
        StorageItem resultItem = repository.retrieveItem( request );
        Assert.assertTrue( resultItem.getLastRequested() > lastRequest );

        // check the shadow attributes
        AbstractStorageItem shadowStorageItem =
            repository.getLocalStorage().getAttributesHandler().getAttributeStorage().getAttributes(
                repository.createUid( request.getRequestPath() ) );
        Assert.assertEquals( resultItem.getLastRequested(), shadowStorageItem.getLastRequested() );
    }

    // ==

    protected class CounterListener
        implements EventListener
    {
        private int requestCount = 0;

        public int getRequestCount()
        {
            return this.requestCount;
        }

        public void reset()
        {
            this.requestCount = 0;
        }

        public void onEvent( Event<?> evt )
        {
            if ( evt instanceof RepositoryItemEventCache
                && ( ( (RepositoryItemEventCache) evt ).getItem().getPath().endsWith( "maven-metadata.xml" ) || ( (RepositoryItemEventCache) evt )
                    .getItem().getPath().endsWith( "spoof-1.0.txt" ) ) )
            {
                requestCount = requestCount + 1;
            }
        }
    }
}
