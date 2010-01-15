package org.sonatype.nexus.repositories.metadata;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.codehaus.plexus.util.IOUtil;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.item.ByteArrayContentLocator;
import org.sonatype.nexus.proxy.item.DefaultStorageFileItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.repository.metadata.RawTransport;

public class NexusRawTransport
    implements RawTransport
{
    private final Repository repository;

    private final boolean localOnly;

    private final boolean remoteOnly;

    private StorageFileItem lastReadFile;

    private StorageFileItem lastWriteFile;

    public NexusRawTransport( Repository repository, boolean localOnly, boolean remoteOnly )
    {
        this.repository = repository;

        this.localOnly = localOnly;

        this.remoteOnly = remoteOnly;
    }

    public byte[] readRawData( String path )
        throws Exception
    {
        InputStream is = null;

        ByteArrayOutputStream os = null;

        try
        {
            ResourceStoreRequest request = new ResourceStoreRequest( path, localOnly, remoteOnly );

            StorageItem item = repository.retrieveItem( false, request );

            if ( item instanceof StorageFileItem )
            {
                StorageFileItem file = (StorageFileItem) item;

                is = file.getInputStream();

                os = new ByteArrayOutputStream();

                IOUtil.copy( is, os );

                lastReadFile = file;

                return os.toByteArray();
            }
            else
            {
                return null;
            }

        }
        catch ( ItemNotFoundException e )
        {
            // not found should return null
            return null;
        }
        finally
        {
            IOUtil.close( is );

            IOUtil.close( os );
        }
    }

    public void writeRawData( String path, byte[] data )
        throws Exception
    {
        DefaultStorageFileItem file = new DefaultStorageFileItem(
            repository,
            new ResourceStoreRequest( path ),
            new ByteArrayContentLocator( data, "text/xml" ) );

        repository.storeItem( false, file );

        lastWriteFile = file;
    }

    // ==

    public StorageFileItem getLastReadFile()
    {
        return lastReadFile;
    }

    public StorageFileItem getLastWriteFile()
    {
        return lastWriteFile;
    }
}
