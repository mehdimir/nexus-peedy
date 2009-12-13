package org.sonatype.nexus.vui.core.browse;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.vui.AbstractFeature;
import org.sonatype.nexus.vui.BrowseFeature;
import org.sonatype.nexus.vui.Feature;
import org.sonatype.nexus.vui.app.APIResource;
import org.sonatype.nexus.vui.app.NamedExternalResource;

@Component( role = BrowseFeature.class, hint = WelcomeFeature.HINT )
public class WelcomeFeature
    extends AbstractFeature
{
    public static final String HINT = "welcome";

    @Override
    public String getDescription()
    {
        return "Welcome to the greatest of all the Maven Repository Managers! Seriously.";
    }

    public int getOrderRank()
    {
        return 5;
    }

    @Override
    public String getName()
    {
        return "Welcome";
    }

    @Override
    public APIResource[] getRelatedAPI()
    {
        return null;
    }

    @Override
    public Class<? extends Feature>[] getRelatedFeatures()
    {
        return null;
    }

    @Override
    public NamedExternalResource[] getRelatedResources()
    {
        return null;
    }

    @Override
    public com.vaadin.ui.Component getUI()
    {
        return null;
    }

}
