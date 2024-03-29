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
package org.sonatype.nexus.rest.artifact;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Random;

import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * This component simply preserves "state" and gets the needed GAVP values from the POM that it stores temporarily.
 * 
 * @author cstamas
 */
public class PomArtifactManager
{
    private File tmpStorage = null;

    private File tmpPomFile = null;

    private int state = 0;

    private ArtifactCoordinate artifactCoordinate;

    private static final int STATE_NONE = 0;

    private static final int STATE_FILE_STORED = 1;

    private static final int STATE_GAV_GENERATED = 2;

    private static final Random identifierGenerator = new Random();

    public PomArtifactManager( File tmpStorage )
    {
        super();

        this.tmpStorage = tmpStorage;
    }

    public void storeTempPomFile( InputStream is )
        throws IOException
    {
        if ( STATE_NONE != state )
        {
            throw new IllegalStateException( "There is already a temporary pom file managed by this PomArtifactManager" );
        }

        tmpPomFile = new File( tmpStorage, getNextIdentifier() + ".xml" );

        tmpPomFile.deleteOnExit();

        FileOutputStream os = null;

        try
        {
            os = new FileOutputStream( tmpPomFile );

            IOUtil.copy( is, os );

            state = STATE_FILE_STORED;
        }
        finally
        {
            IOUtil.close( is );

            IOUtil.close( os );
        }
    }

    public InputStream getTempPomFileInputStream()
        throws IOException
    {
        if ( STATE_FILE_STORED > state )
        {
            throw new IllegalStateException( "The temporary pom file has not yet been stored" );
        }

        return new FileInputStream( tmpPomFile );
    }

    public ArtifactCoordinate getArtifactCoordinateFromTempPomFile()
        throws IOException,
            XmlPullParserException
    {
        if ( STATE_FILE_STORED > state )
        {
            throw new IllegalStateException( "The temporary POM file has not yet been stored" );
        }

        if ( STATE_GAV_GENERATED == state )
        {
            return artifactCoordinate;
        }

        Reader reader = null;

        try
        {
            reader = ReaderFactory.newXmlReader( tmpPomFile );

            artifactCoordinate = parsePom( reader );

            state = STATE_GAV_GENERATED;
        }
        finally
        {
            IOUtil.close( reader );
        }

        return artifactCoordinate;
    }

    /**
     * Clean up.
     */
    public void removeTempPomFile()
    {
        if ( STATE_FILE_STORED > state )
        {
            throw new IllegalStateException( "The temporary pom file has not yet been stored" );
        }

        tmpPomFile.delete();

        artifactCoordinate = null;

        state = STATE_NONE;
    }

    // ==
    // Private

    private long getNextIdentifier()
    {
        synchronized ( identifierGenerator )
        {
            return identifierGenerator.nextLong();
        }
    }

    /**
     * Pulls out the GAVP by parsing the temporarily stored POM.
     * 
     * @param reader
     * @return
     * @throws IOException
     * @throws XmlPullParserException
     */
    private ArtifactCoordinate parsePom( Reader reader )
        throws IOException,
            XmlPullParserException
    {
        String groupId = null;

        String artifactId = null;

        String version = null;

        String packaging = "jar";

        XmlPullParser parser = new MXParser();

        parser.setInput( reader );

        boolean foundRoot = false;

        boolean inParent = false;

        int eventType = parser.getEventType();

        // TODO: we should detect when we got all we need and simply stop parsing further
        // since we are neglecting other contents anyway
        while ( eventType != XmlPullParser.END_DOCUMENT )
        {
            if ( eventType == XmlPullParser.START_TAG )
            {
                if ( parser.getName().equals( "project" ) )
                {
                    foundRoot = true;
                }
                else if ( parser.getName().equals( "parent" ) )
                {
                    inParent = true;
                }
                else if ( parser.getName().equals( "groupId" ) )
                {
                    // 1st: if found project/groupId -> overwrite
                    // 2nd: if in parent, and groupId is still null, overwrite
                    if ( parser.getDepth() == 2 || ( inParent && groupId == null ) )
                    {
                        groupId = StringUtils.trim( parser.nextText() );
                    }
                }
                else if ( parser.getName().equals( "artifactId" ) )
                {
                    // 1st: if found project/artifactId -> overwrite
                    // 2nd: if in parent, and artifactId is still null, overwrite
                    if ( parser.getDepth() == 2 || ( inParent && artifactId == null ) )
                    {
                        artifactId = StringUtils.trim( parser.nextText() );
                    }
                }
                else if ( parser.getName().equals( "version" ) )
                {
                    // 1st: if found project/version -> overwrite
                    // 2nd: if in parent, and version is still null, overwrite
                    if ( parser.getDepth() == 2 || ( inParent && version == null ) )
                    {
                        version = StringUtils.trim( parser.nextText() );
                    }
                }
                else if ( parser.getName().equals( "packaging" ) )
                {
                    // 1st: if found project/packaging -> overwrite
                    if ( parser.getDepth() == 2 )
                    {
                        packaging = StringUtils.trim( parser.nextText() );
                    }
                }
                else if ( !foundRoot )
                {
                    throw new XmlPullParserException( "Unrecognised tag: '" + parser.getName() + "'", parser, null );
                }
            }
            else if ( eventType == XmlPullParser.END_TAG )
            {
                if ( parser.getName().equals( "parent" ) )
                {
                    inParent = false;
                }
            }

            eventType = parser.next();
        }

        ArtifactCoordinate artifactCoordinates = new ArtifactCoordinate();

        artifactCoordinates.setGroupId( groupId );

        artifactCoordinates.setArtifactId( artifactId );

        artifactCoordinates.setVersion( version );

        artifactCoordinates.setPackaging( packaging );

        return artifactCoordinates;
    }
}
