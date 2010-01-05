package org.sonatype.nexus.proxy.item;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;

import org.apache.velocity.VelocityContext;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.velocity.VelocityComponent;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.repository.Repository;

@Component( role = ContentGenerator.class, hint = VelocityContentGenerator.ID )
public class VelocityContentGenerator
    implements ContentGenerator
{
    public static final String ID = "velocity";

    @Requirement
    private VelocityComponent velocityComponent;

    public ContentLocator generateContent( Repository repository, String path, StorageFileItem item )
        throws StorageException
    {
        InputStreamReader isr = null;

        try
        {
            StringWriter sw = new StringWriter();

            VelocityContext vctx = new VelocityContext( item.getItemContext() );

            isr = new InputStreamReader( item.getInputStream(), "UTF-8" );

            velocityComponent.getEngine().evaluate( vctx, sw, item.getRepositoryItemUid().toString(), isr );

            return new StringContentLocator( sw.toString() );
        }
        catch ( IOException e )
        {
             // XXX: nonsense!
            throw new StorageException( "Unable to generate content!", e );
        }
        finally
        {
            IOUtil.close( isr );
        }
    }
}
