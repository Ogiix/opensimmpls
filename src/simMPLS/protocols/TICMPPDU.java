/* 
 * Copyright 2016 (C) Gaetan Bulpa.
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
package simMPLS.protocols;

/**
 * This class implements an ICMP packet.
 *
 * @author Gaetan Bulpa - gaetan.bulpa@gmail.com
 * @version 2.0
 */
public class TICMPPDU extends TAbstractPDU {

    /**
     * This method is the constructor of the class. It is create a new instance
     * of TICMPPDU.
     *
     * @author Gaetan Bulpa - gaetan.bulpa@gmail.com
     * @param id Packet identifier.
     * @param originIP IP addres of this packet's sender.
     * @param targetIP IP addres of this packet's receiver.
     * @since 2.0
     */
    public TICMPPDU(long id, String originIP, String targetIP) {
        super(id, originIP, targetIP);
        this.typeICMP = 8;
        this.codeICMP = 0;
        this.payloadICMP = null;
        this.subType = TAbstractPDU.ICMP;
    }
    
    /**
     * This method is the constructor of the class. It is create a new instance
     * of TICMPPDU.
     *
     * @author Gaetan Bulpa - gaetan.bulpa@gmail.com
     * @param id Packet identifier.
     * @param originIP IP addres of this packet's sender.
     * @param targetIP IP addres of this packet's receiver.
     * @param typeICMP Type of ICMP message.
     * @param codeICMP Code of ICMP message.
     * @since 2.0
     */
    public TICMPPDU(long id, String originIP, String targetIP, int typeICMP, int codeICMP) {
        super(id, originIP, targetIP);
        this.typeICMP = typeICMP;
        this.codeICMP = codeICMP;
        this.payloadICMP = null;
        this.subType = TAbstractPDU.ICMP;
    }
    
    /**
     * This method is the constructor of the class. It is create a new instance
     * of TICMPPDU.
     *
     * @author Gaetan Bulpa - gaetan.bulpa@gmail.com
     * @param id Packet identifier.
     * @param originIP IP addres of this packet's sender.
     * @param targetIP IP addres of this packet's receiver.
     * @param typeICMP Type of ICMP message.
     * @param codeICMP Code of ICMP message.
     * @param payloadICMP ICMP payload for extended informations.
     * @since 2.0
     */
    public TICMPPDU(long id, String originIP, String targetIP, int typeICMP, int codeICMP, TMPLSLabelStack payloadICMP) {
        super(id, originIP, targetIP);
        this.typeICMP = typeICMP;
        this.codeICMP = codeICMP;
        this.payloadICMP = payloadICMP;
        this.subType = TAbstractPDU.ICMP;
    }

    /**
     * This method returns the size of the packet in bytes (octects).
     *
     * @author Gaetan Bulpa - gaetan.bulpa@gmail.com
     * @return Size of this packet in bytes (octects).
     * @since 1.0
     */
    @Override
    public int getSize() {
        int size;
        size = super.getIPv4Header().getSize() + 8;
        if(payloadICMP != null)
            size += (4 * this.payloadICMP.getSize());
        return size;
    }

    /**
     * This method returns the type of the packet, as defined by constants in
     * TAbstractPDU class.
     *
     * @author Gaetan Bulpa - gaetan.bulpa@gmail.com
     * @return The type of this packet.
     * @since 1.0
     */
    @Override
    public int getType() {
        return TAbstractPDU.ICMP;
    }

    /**
     * This method return the payload of this packet.
     *
     * @author Gaetan Bulpa - gaetan.bulpa@gmail.com
     * @return ICMP payload of this packet.
     * @since 2.0
     */
    public TMPLSLabelStack getPayloadICMP() {
        return this.payloadICMP;
    }

    /**
     * This method set the TCP payload of this packet.
     *
     * @author Gaetan Bulpa - gaetan.bulpa@gmail.com
     * @param payloadICMP The TCP payload for this packet.
     * @since 2.0
     */
    public void setPayloadICMP(TMPLSLabelStack payloadICMP) {
        this.payloadICMP = payloadICMP;
    }

    /**
     * This method gets the IPv4 header of this packet.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @return The IPv4 header of this packet.
     * @since 1.0
     */
    @Override
    public TIPv4Header getIPv4Header() {
        return super.getIPv4Header();
    }

    /**
     * This method returns the subtype of the packet.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @return The subtype of this packet. For instances of this class, it
     * returns IPV4, as defined in TAbstractPDU.
     * @since 1.0
     */
    @Override
    public int getSubtype() {
        return this.subType;
    }

    /**
     * This method has to be implemented by any subclasses. It has to allow
     * setting the subtype of the packet.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @param subtype The subtype of the packet.
     * @since 1.0
     */
    @Override
    public void setSubtype(int subtype) {
        this.subType = subtype;
    }
    
    /**
     * @author Gaetan Bulpa - gaetan.bulpa@gmail.com
     * @since 2.0
     * @return the typeICMP
     */
    public int getTypeICMP() {
        return typeICMP;
    }

    /**
     * @author Gaetan Bulpa - gaetan.bulpa@gmail.com
     * @since 2.0
     * @param typeICMP the typeICMP to set
     */
    public void setTypeICMP(int typeICMP) {
        this.typeICMP = typeICMP;
    }

    /**
     * @author Gaetan Bulpa - gaetan.bulpa@gmail.com
     * @since 2.0
     * @return the codeICMP
     */
    public int getCodeICMP() {
        return codeICMP;
    }

    /**
     * @author Gaetan Bulpa - gaetan.bulpa@gmail.com
     * @since 2.0
     * @param codeICMP the codeICMP to set
     */
    public void setCodeICMP(int codeICMP) {
        this.codeICMP = codeICMP;
    }

    private int subType;
    private int typeICMP;
    private int codeICMP;
    private TMPLSLabelStack payloadICMP;
}
