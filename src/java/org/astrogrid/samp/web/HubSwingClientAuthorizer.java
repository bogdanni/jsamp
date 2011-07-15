package org.astrogrid.samp.web;

import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.text.MessageFormat;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import org.astrogrid.samp.httpd.HttpServer;

/**
 * ClientAuthorizer implementation that queries the user for permission
 * via a popup dialogue.
 *
 * @author   Mark Taylor
 * @since    2 Feb 2011
 */
public class HubSwingClientAuthorizer implements ClientAuthorizer {

    private final Component parent_;
    private static final Logger logger_ =
        Logger.getLogger( HubSwingClientAuthorizer.class.getName() );

    /**
     * Constructor.
     *
     * @param  parent  parent component
     */
    public HubSwingClientAuthorizer( Component parent ) {
        parent_ = parent;
        if ( GraphicsEnvironment.isHeadless() ) {
            throw new HeadlessException( "Client authorization dialogues "
                                       + "impossible - no graphics" );
        }
    }

    public boolean authorize( HttpServer.Request request, String appName ) {

        // Prepare an internationalised query dialogue.
        AuthResourceBundle.Content authContent =
            AuthResourceBundle
           .getAuthContent( ResourceBundle
                           .getBundle( AuthResourceBundle.class.getName() ) );
        String[] qmsg = getMessageLines( request, appName, authContent );
        String noOpt = authContent.noWord();
        String yesOpt = authContent.yesWord();

        // Just calling showOptionDialog can end up with the popup being
        // obscured by other windows on the desktop, at least for win XP.
        JOptionPane jop =
            new JOptionPane( qmsg, JOptionPane.WARNING_MESSAGE,
                             JOptionPane.YES_NO_OPTION, null,
                             new String[] { noOpt, yesOpt }, noOpt );
        JDialog dialog = jop.createDialog( parent_, "SAMP Hub Security" );
        dialog.setAlwaysOnTop( true );
        dialog.setModal( true );
        dialog.setDefaultCloseOperation( JDialog.DISPOSE_ON_CLOSE );

        // It seems to be OK to call Dialog.setVisible on a modal dialogue
        // from threads other than the AWT Event Dispatch Thread.
        // I admit though that I haven't seen document which assures that
        // this is true however.
        dialog.setVisible( true );
        dialog.dispose();
        return jop.getValue() == yesOpt;
    }

    /**
     * Returns some lines of text describing the applying client.
     * Suitable for directing to the user.
     * 
     * @param  request  HTTP request bearing the application
     * @param   appName  application name claimed by the applicant
     * @param   authContent  content of AuthResourceBundle bundle
     * @return   array of lines of plain text suitable for describing the
     *           applicant to the user; not too long; each element
     *           represents a screen line
     */
    private String[] getMessageLines( HttpServer.Request request,
                                      String appName,
                                      AuthResourceBundle.Content authContent ) {
        // Application origin (see http://www.w3.org/TR/cors/,
        // http://tools.ietf.org/html/draft-abarth-origin).
        String origin =
            HttpServer.getHeader( request.getHeaderMap(), "Origin" );
        if ( origin == null ) {
            origin = authContent.undeclaredWord();
        }

        List lineList = new ArrayList();
        lineList.addAll( Arrays.asList( authContent.appIntroductionLines() ) );
        lineList.add( "\n" );
        lineList.add( "    " + authContent.nameWord() + ": " + appName );
        lineList.add( "    " + authContent.originWord() + ": " + origin );
        lineList.add( "\n" );
        String[] pfmtLines = authContent.privilegeWarningFormatLines();
        String user = System.getProperty( "user.name" );
        for ( int il = 0; il < pfmtLines.length; il++ ) {
            lineList.add( MessageFormat.format( pfmtLines[ il ],
                                                new Object[] { user } ) );
        }
        lineList.add( "\n" );
        lineList.addAll( Arrays.asList( authContent.adviceLines() ) );
        lineList.add( "\n" );
        lineList.add( authContent.questionLine() );
        return (String[]) lineList.toArray( new String[ 0 ] );
    }
}
