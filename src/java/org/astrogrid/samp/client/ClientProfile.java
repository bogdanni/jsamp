package org.astrogrid.samp.client;

import org.astrogrid.samp.SampException;

/**
 * Defines Profile-specific aspects of the SAMP client implementation.
 *
 * @author   Mark Taylor
 * @since    15 Jul 2008
 */
public interface ClientProfile {

    /**
     * Attempts to register with a SAMP hub and return a corresponding
     * connection object.  Some profile-specific hub discovery mechanism
     * is used to locate the hub.
     * If no hub is running, null will normally be returned.
     *
     * @return   hub connection representing a new registration, or null
     * @throws   SampException  in case of some unexpected error
     */
    HubConnection register() throws SampException;
}
