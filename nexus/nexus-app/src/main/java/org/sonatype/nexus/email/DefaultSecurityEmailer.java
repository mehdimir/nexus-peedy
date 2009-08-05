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
package org.sonatype.nexus.email;

import java.util.List;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.micromailer.Address;
import org.sonatype.micromailer.MailRequest;
import org.sonatype.micromailer.imp.DefaultMailType;
import org.sonatype.security.email.SecurityEmailer;

/**
 * The default emailer.
 *
 * @author cstamas
 * @author Alin Dreghiciu
 */
@Component( role = SecurityEmailer.class )
public class DefaultSecurityEmailer
    implements SecurityEmailer
{

    @Requirement
    private NexusEmailer emailer;

    public void sendNewUserCreated( String email, String userid, String password )
    {
        MailRequest request = new MailRequest( NexusEmailer.NEXUS_MAIL_ID, DefaultMailType.DEFAULT_TYPE_ID );
        request.setFrom( new Address( emailer.getSystemEmailAddress(), "Nexus Repository Manager" ) );
        request.getToAddresses().add( new Address( email ) );
        request.getBodyContext().put( DefaultMailType.SUBJECT_KEY, "Nexus: New user account created." );

        StringBuilder body = new StringBuilder();
        body.append( "User Account " );
        body.append( userid );
        body.append( " has been created.  Another email will be sent shortly containing your password." );

        request.getBodyContext().put( DefaultMailType.BODY_KEY, body.toString() );

        emailer.sendMail( request );

        request = new MailRequest( NexusEmailer.NEXUS_MAIL_ID, DefaultMailType.DEFAULT_TYPE_ID );
        request.setFrom( new Address( emailer.getSystemEmailAddress(), "Nexus Repository Manager" ) );
        request.getToAddresses().add( new Address( email ) );
        request.getBodyContext().put( DefaultMailType.SUBJECT_KEY, "Nexus: New user account created." );

        body = new StringBuilder();
        body.append( "Your new password is " );
        body.append( password );

        request.getBodyContext().put( DefaultMailType.BODY_KEY, body.toString() );

        emailer.sendMail( request );
    }

    public void sendForgotUsername( String email, List<String> userIds )
    {
        MailRequest request = new MailRequest( NexusEmailer.NEXUS_MAIL_ID, DefaultMailType.DEFAULT_TYPE_ID );
        request.setFrom( new Address( emailer.getSystemEmailAddress(), "Nexus Repository Manager" ) );
        request.getToAddresses().add( new Address( email ) );
        request.getBodyContext().put( DefaultMailType.SUBJECT_KEY, "Nexus: User account notification." );

        StringBuilder body = new StringBuilder();

        body.append( "Your email is associated with the following Nexus User Id(s):\n " );
        for( String userId : userIds )
        {
            body.append( "\n - \"" );
            body.append( userId );
            body.append( "\"" );
        }

        request.getBodyContext().put( DefaultMailType.BODY_KEY, body.toString() );

        emailer.sendMail( request );
    }

    public void sendResetPassword( String email, String password )
    {
        MailRequest request = new MailRequest( NexusEmailer.NEXUS_MAIL_ID, DefaultMailType.DEFAULT_TYPE_ID );
        request.setFrom( new Address( emailer.getSystemEmailAddress(), "Nexus Repository Manager" ) );
        request.getToAddresses().add( new Address( email ) );
        request.getBodyContext().put( DefaultMailType.SUBJECT_KEY, "Nexus: User account notification." );

        StringBuilder body = new StringBuilder();
        body.append( "Your password has been reset.  Your new password is: " );
        body.append( password );

        request.getBodyContext().put( DefaultMailType.BODY_KEY, body.toString() );

        emailer.sendMail( request );
    }

}