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
package org.sonatype.nexus.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Date;

import org.codehaus.plexus.util.IOUtil;
import org.restlet.data.MediaType;
import org.restlet.data.Tag;
import org.restlet.resource.OutputRepresentation;
import org.sonatype.nexus.proxy.attributes.inspectors.DigestCalculatingInspector;
import org.sonatype.nexus.proxy.item.StorageFileItem;

public class StorageFileItemRepresentation
    extends OutputRepresentation
{
    private final StorageFileItem file;

    public StorageFileItemRepresentation( StorageFileItem file )
    {
        super( MediaType.valueOf( file.getMimeType() ) );

        this.file = file;

        setSize( file.getLength() );

        setModificationDate( new Date( file.getModified() ) );

        setAvailable( true );

        if ( file.getAttributes().containsKey( DigestCalculatingInspector.DIGEST_SHA1_KEY ) )
        {
            setTag( new Tag( file.getAttributes().get( DigestCalculatingInspector.DIGEST_SHA1_KEY ), false ) );
        }

    }

    protected StorageFileItem getStorageFileItem()
    {
        return file;
    }

    public boolean isTransient()
    {
        return !getStorageFileItem().isReusableStream();
    }

    @Override
    public void write( OutputStream outputStream )
        throws IOException
    {
        InputStream is = null;

        try
        {
            is = getStorageFileItem().getInputStream();

            IOUtil.copy( is, outputStream );
        }
        catch ( SocketException e )
        {
            // https://issues.sonatype.org/browse/NEXUS-217
        }
        catch ( IOException e )
        {
            if ( e.getClass().getName().equals( "org.mortbay.jetty.EofException" ) )
            {
                // https://issues.sonatype.org/browse/NEXUS-217
            }
            else
            {
                // rethrow it
                throw e;
            }
        }
        finally
        {
            IOUtil.close( is );
        }
    }

}
