package org.sonatype.nexus.vui.core.browse;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.nexus.vui.AbstractFeature;
import org.sonatype.nexus.vui.BrowseFeature;
import org.sonatype.nexus.vui.Feature;
import org.sonatype.nexus.vui.app.APIResource;
import org.sonatype.nexus.vui.app.NamedExternalResource;

@Component( role = BrowseFeature.class, hint = StatusFeature.HINT )
public class StatusFeature
    extends AbstractFeature
{
    public static final String HINT = "status";
    
    @Override
    public String getDescription()
    {
        return "Inspect status and health of your Nexus.";
    }

    public int getOrderRank()
    {
        return 10;
    }

    @Override
    public String getName()
    {
        return "Status";
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
