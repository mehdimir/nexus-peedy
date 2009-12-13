package org.sonatype.nexus.vui.core.browse;

import org.sonatype.nexus.Nexus;

import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings( "serial" )
public class StatusUI
    extends VerticalLayout
{
    public StatusUI( Nexus nexus )
    {
        setSpacing( true );

        addComponent( new Label( "<h3>Nexus state and version</h3>", Label.CONTENT_XHTML ) );
        addComponent( new Label(
            "Nexus exposes it's <b>state</b> and <b>exact version and edition</b> over UI and also over REST API." ) );

        addComponent( new Label( "<h2>Nexus complete name</h2>", Label.CONTENT_XHTML ) );
        addComponent( new Label( nexus.getSystemStatus().getFormattedAppName() ) );

        addComponent( new Label( "<h2>Nexus version</h2>", Label.CONTENT_XHTML ) );
        addComponent( new Label( nexus.getSystemStatus().getVersion() ) );

        addComponent( new Label( "<h2>Nexus operation mode</h2>", Label.CONTENT_XHTML ) );
        addComponent( new Label( nexus.getSystemStatus().getOperationMode().toString() ) );
    }
}
