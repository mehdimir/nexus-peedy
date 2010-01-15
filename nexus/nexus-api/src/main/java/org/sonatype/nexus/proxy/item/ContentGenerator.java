package org.sonatype.nexus.proxy.item;

import javax.inject.Singleton;

import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.plugin.ExtensionPoint;

/**
 * A content generator is a special component, that is able to generate content on-the-fly, that will be substituted
 * with the content coming from the Local Storage.
 * 
 * @author cstamas
 */
@ExtensionPoint
@Singleton
public interface ContentGenerator
{
    public static final String CONTENT_GENERATOR_ID = "contentGenerator";

    ContentLocator generateContent( Repository repository, String path, StorageFileItem item )
        throws StorageException;
}
