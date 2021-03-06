/* 
 * Copyright 2015 (C) Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package simMPLS.hardware.dmgp;

import java.util.LinkedList;

/**
 * This class implements an entry that will store data related to a
 * retransmission requested by a node.
 *
 * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
 * @version 1.1
 */
public class TGPSRPRequestEntry implements Comparable {

    /**
     * This is the class constructor. Implements a new instance of
     * TGPSRPRequestsEntry.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @param incomingOrder Incoming order. This is a number that must be used to make
     * this entry shorted in a collection in a coherent way.
     * @since 1.0
     */
    public TGPSRPRequestEntry(int incomingOrder) {
        this.timeout = TGPSRPRequestsMatrix.TIMEOUT;
        this.attempts = TGPSRPRequestsMatrix.ATTEMPTS;
        this.flowID = -1;
        this.packetID = -1;
        this.outgoingPort = -1;
        this.crossedNodes = new LinkedList();
        this.order = incomingOrder;
    }

    /**
     * This method obtains the incoming order to the entry in order to be
     * shorted in a collection.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @return Incoming order to the entry.
     * @since 1.0
     */
    public int getOrder() {
        return order;
    }

    /**
     * This method establishes the flow the entry belongs to.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @param flowID The flow the entry belongs to.
     * @since 1.0
     */
    public void setFlowID(int flowID) {
        this.flowID = flowID;
    }

    /**
     * This method obtains the flow the entry belongs to.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @return The flow the entry belongs to.
     * @since 1.0
     */
    public int getFlowID() {
        return this.flowID;
    }

    /**
     * This method establishes the identifier of the packet this entry refers
     * to.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @param packetID The packet identifier.
     * @since 1.0
     */
    public void setPacketID(int packetID) {
        this.packetID = packetID;
    }

    /**
     * This method obtains the identifier of the packet this entry refers to.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @return The packet identifier.
     * @since 1.0
     */
    public int getPacketID() {
        return this.packetID;
    }

    /**
     * This method establishes the outgoing port by where the retransmission
     * request has been sent.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @param outgoingPort Outgoing port.
     * @since 1.0
     */
    public void setOutgoingPort(int outgoingPort) {
        this.outgoingPort = outgoingPort;
    }

    /**
     * This method obtains the outgoing port by where the retransmission request
     * has been sent.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @return Outgoing port.
     * @since 1.0
     */
    public int getOutgoingPort() {
        return this.outgoingPort;
    }

    /**
     * This method establishes the IP address of an active node that will be
     * requested for a packet retransmission.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @param crossedNodeIP IP address of the node to be requested for a packet
     * retransmission.
     * @since 1.0
     */
    public void setCrossedNodeIP(String crossedNodeIP) {
        this.crossedNodes.addFirst(crossedNodeIP);
    }

    /**
     * This method obtains the IP address of the next active node that will be
     * requested for a packet retransmission.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @return IP address of the next active node to be requested for a packet
     * retransmission. If there is not a node to be requested, this method
     * return NULL.
     * @since 1.0
     */
    public String getCrossedNodeIPv4() {
        if (this.crossedNodes.size() > 0) {
            return ((String) this.crossedNodes.removeFirst());
        }
        return null;
    }

    /**
     * This method decreases the retransmission TimeOut.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @param nanoseconds Number of nanoseconds to decrease from the timeout.
     * @since 1.0
     */
    public void decreaseTimeout(int nanoseconds) {
        this.timeout -= nanoseconds;
        if (this.timeout < 0) {
            this.timeout = 0;
        }
    }

    /**
     * This method restores the retransmission TimeOut to its original value.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @since 1.0
     */
    public void resetTimeout() {
        if (this.timeout == 0) {
            if (this.attempts > 0) {
                this.timeout = TGPSRPRequestsMatrix.TIMEOUT;
                this.attempts--;
            }
        }
    }

    /**
     * This method forces the TimeOut restoration to its original value and also
     * increases the number of expired retransmission attempts.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @since 1.0
     */
    public void forceTimeoutReset() {
        this.timeout = TGPSRPRequestsMatrix.TIMEOUT;
        this.attempts--;
        if (this.attempts < 0) {
            attempts = 0;
            timeout = 0;
        }
    }

    /**
     * This method ckeck whether the retransmission request must be retried
     * again or not.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @return TRUE, if the retransmission must be retried. Otherwise, FALSE.
     * @since 1.0
     */
    public boolean isRetryable() {
        if (this.attempts > 0) {
            if (this.timeout == 0) {
                if (this.crossedNodes.size() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This method check whether the entry must be removed from the table
     * (because retransmission is not going to be retried) or not.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @return TRUE, if the entry must be removed. Otherwise, FALSE.
     * @since 1.0
     */
    public boolean isPurgeable() {
        if (this.crossedNodes.size() == 0) {
            return true;
        }
        if (this.attempts == 0) {
            if (this.timeout == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method compares the current instance with another of the same type
     * passed as an argument to know the order to be inserted in a collection.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @param o Instancia con la que se va a comparar la actual.
     * @return -1, 0, 1, depending on wheter the curren instance is lower,
     * equal, or greater than the one passed as an argument. In terms of
     * shorting.
     * @since 1.0
     */
    @Override
    public int compareTo(Object o) {
        TGPSRPRequestEntry e = (TGPSRPRequestEntry) o;
        if (this.order < e.getOrder()) {
            return TGPSRPRequestEntry.THIS_LOWER;
        }
        if (this.order > e.getOrder()) {
            return TGPSRPRequestEntry.THIS_GREATER;
        }
        return TGPSRPRequestEntry.THIS_EQUAL;
    }

    private static final int THIS_LOWER = -1;
    private static final int THIS_EQUAL = 0;
    private static final int THIS_GREATER = 1;

    private int timeout;
    private int flowID;
    private int packetID;
    private int outgoingPort;
    private LinkedList crossedNodes;
    private int order;
    private int attempts;
}
