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
package org.sonatype.nexus.proxy.attributes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.IOUtil;
import org.sonatype.nexus.configuration.ConfigurationChangeEvent;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.access.Action;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.DefaultStorageCollectionItem;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.DefaultStorageLinkItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.plexus.appevents.ApplicationEventMulticaster;
import org.sonatype.plexus.appevents.Event;
import org.sonatype.plexus.appevents.EventListener;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;

/**
 * AttributeStorage implementation driven by XStream.
 * 
 * @author cstamas
 */
@Component( role = AttributeStorage.class )
public class DefaultAttributeStorage
    extends AbstractLogEnabled
    implements AttributeStorage, EventListener, Initializable
{
    @Requirement
    private ApplicationEventMulticaster applicationEventMulticaster;

    @Requirement
    private ApplicationConfiguration applicationConfiguration;

    /**
     * The base dir.
     */
    private File workingDirectory;

    /** The xstream. */
    private XStream xstream;

    /**
     * Instantiates a new FSX stream attribute storage.
     */
    public DefaultAttributeStorage()
    {
        super();
        this.xstream = new XStream();
        this.xstream.alias( "file", DefaultStorageFileItem.class );
        this.xstream.alias( "collection", DefaultStorageCollectionItem.class );
        this.xstream.alias( "link", DefaultStorageLinkItem.class );
    }

    public void initialize()
    {
        applicationEventMulticaster.addEventListener( this );
    }

    public void onEvent( Event<?> evt )
    {
        if ( ConfigurationChangeEvent.class.isAssignableFrom( evt.getClass() ) )
        {
            this.workingDirectory = null;
        }
    }

    /**
     * Gets the base dir.
     * 
     * @return the base dir
     */
    public File getWorkingDirectory()
        throws IOException
    {
        if ( workingDirectory == null )
        {
            workingDirectory = applicationConfiguration.getWorkingDirectory( "proxy/attributes" );

            if ( workingDirectory.exists() )
            {
                if ( workingDirectory.isFile() )
                {
                    throw new IllegalArgumentException( "The attribute storage exists and is not a directory: "
                        + workingDirectory.getAbsolutePath() );
                }
            }
            else
            {
                getLogger().info( "Attribute storage directory does not exists, creating it here: " + workingDirectory );

                if ( !workingDirectory.mkdirs() )
                {
                    throw new IllegalArgumentException( "Could not create the attribute storage directory on path "
                        + workingDirectory.getAbsolutePath() );
                }
            }
        }

        return workingDirectory;
    }

    public void setWorkingDirectory( File baseDir )
    {
        this.workingDirectory = baseDir;
    }

    public boolean deleteAttributes( RepositoryItemUid uid )
    {
        uid.lockAttributes( Action.delete );

        try
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "Deleting attributes on UID=" + uid.toString() );
            }

            boolean result = false;

            try
            {
                File ftarget = getFileFromBase( uid );

                result = ftarget.exists() && ftarget.isFile() && ftarget.delete();
            }
            catch ( IOException e )
            {
                getLogger().warn( "Got IOException during delete of UID=" + uid.toString(), e );
            }

            return result;
        }
        finally
        {
            uid.unlockAttributes();
        }
    }

    public AbstractStorageItem getAttributes( RepositoryItemUid uid )
    {
        uid.lockAttributes( Action.read );

        try
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "Loading attributes on UID=" + uid.toString() );
            }
            try
            {
                AbstractStorageItem result = null;

                result = doGetAttributes( uid );

                return result;
            }
            catch ( IOException ex )
            {
                getLogger().error( "Got IOException during reading of UID=" + uid.toString(), ex );

                return null;
            }
        }
        finally
        {
            uid.unlockAttributes();
        }
    }

    public void putAttribute( StorageItem item )
    {
        RepositoryItemUid origUid = item.getRepositoryItemUid();

        origUid.lockAttributes( Action.create );

        try
        {
            if ( getLogger().isDebugEnabled() )
            {
                getLogger().debug( "Storing attributes on UID=" + item.getRepositoryItemUid() );
            }

            if ( StorageCollectionItem.class.isAssignableFrom( item.getClass() ) )
            {
                // not saving attributes for directories anymore
                return;
            }

            try
            {
                AbstractStorageItem onDisk = doGetAttributes( item.getRepositoryItemUid() );

                if ( onDisk != null && ( onDisk.getGeneration() > item.getGeneration() ) )
                {
                    // change detected, overlay the to be saved onto the newer one and swap
                    onDisk.setResourceStoreRequest( item.getResourceStoreRequest() );

                    onDisk.overlay( item );

                    // and overlay other things too
                    onDisk.setRepositoryItemUid( item.getRepositoryItemUid() );

                    item = onDisk;
                }

                File target = getFileFromBase( item.getRepositoryItemUid() );

                target.getParentFile().mkdirs();

                if ( target.getParentFile().exists() && target.getParentFile().isDirectory() )
                {
                    FileOutputStream fos = null;

                    try
                    {
                        fos = new FileOutputStream( target );

                        item.incrementGeneration();

                        xstream.toXML( item, fos );

                        fos.flush();
                    }
                    finally
                    {
                        IOUtil.close( fos );
                    }
                }
                else
                {
                    getLogger().error(
                        "Could not store attributes on UID=" + item.getRepositoryItemUid()
                            + ", parent exists but is not a directory!" );
                }
            }
            catch ( IOException ex )
            {
                getLogger().error( "Got IOException during store of UID=" + item.getRepositoryItemUid(), ex );
            }
        }
        finally
        {
            origUid.unlockAttributes();
        }
    }

    // ==

    /**
     * Gets the attributes.
     * 
     * @param uid the uid
     * @param isCollection the is collection
     * @return the attributes
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected AbstractStorageItem doGetAttributes( RepositoryItemUid uid )
        throws IOException
    {
        File target = getFileFromBase( uid );

        AbstractStorageItem result = null;

        if ( target.exists() && target.isFile() )
        {
            FileInputStream fis = null;

            try
            {
                fis = new FileInputStream( target );

                result = (AbstractStorageItem) xstream.fromXML( fis );

                result.setRepositoryItemUid( uid );

                // fixing remoteChecked
                if ( result.getRemoteChecked() == 0 || result.getRemoteChecked() == 1 )
                {
                    result.setRemoteChecked( System.currentTimeMillis() );

                    result.setExpired( true );
                }

                // fixing lastRequested
                if ( result.getLastRequested() == 0 )
                {
                    result.setLastRequested( System.currentTimeMillis() );
                }
            }
            catch ( XStreamException e )
            {
                // it is corrupt
                if ( getLogger().isDebugEnabled() )
                {
                    // we log the stacktrace
                    getLogger().info( "Attributes of " + uid + " are corrupt, deleting it.", e );
                }
                else
                {
                    // just remark about this
                    getLogger().info( "Attributes of " + uid + " are corrupt, deleting it." );
                }

                deleteAttributes( uid );
            }
            finally
            {
                IOUtil.close( fis );
            }
        }

        return result;
    }

    /**
     * Gets the file from base.
     * 
     * @param uid the uid
     * @param isCollection the is collection
     * @return the file from base
     */
    protected File getFileFromBase( RepositoryItemUid uid )
        throws IOException
    {
        File repoBase = new File( getWorkingDirectory(), uid.getRepository().getId() );

        File result = null;

        String path = FilenameUtils.getPath( uid.getPath() );

        String name = FilenameUtils.getName( uid.getPath() );

        result = new File( repoBase, path + "/" + name );

        // to be foolproof
        // 2007.11.09. - Believe or not, Nexus deleted my whole USB rack! (cstamas)
        // ok, now you may laugh :)
        if ( !result.getAbsolutePath().startsWith( getWorkingDirectory().getAbsolutePath() ) )
        {
            throw new IOException( "FileFromBase evaluated directory wrongly! baseDir="
                + getWorkingDirectory().getAbsolutePath() + ", target=" + result.getAbsolutePath() );
        }
        else
        {
            return result;
        }
    }
}
