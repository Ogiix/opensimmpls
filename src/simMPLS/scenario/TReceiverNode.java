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
package simMPLS.scenario;

import simMPLS.protocols.TAbstractPDU;
import simMPLS.protocols.TMPLSPDU;
import simMPLS.protocols.TICMPPDU;
import simMPLS.hardware.timer.TTimerEvent;
import simMPLS.hardware.timer.ITimerEventListener;
import simMPLS.hardware.ports.TFIFOPortSet;
import simMPLS.hardware.ports.TPort;
import simMPLS.hardware.ports.TPortSet;
import simMPLS.utils.TLongIDGenerator;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.*;
import org.jfree.data.*;
import simMPLS.utils.EIDGeneratorOverflow;


/**
 * Esta clase implementa un nodo receptor de tr�fico.
 * @author <B>Manuel Dom�nguez Dorado</B><br><A
 * href="mailto:ingeniero@ManoloDominguez.com">ingeniero@ManoloDominguez.com</A><br><A href="http://www.ManoloDominguez.com" target="_blank">http://www.ManoloDominguez.com</A>
 * @version 1.0
 */
public class TReceiverNode extends TNode implements ITimerEventListener, Runnable {

    /**
     * Crea una nueva instancia de TNodoReceptor
     * @param identificador Identificador unico para el nodo en la topolog�a.
     * @param d IP address of the node.
     * @param il Generador de identificadores para los eventos que tenga que genrar el nodo.
     * @param t Topologia dentro de la cual se encuentra el nodo.
     * @since 1.0
     */
    public TReceiverNode(int identificador, String d, TLongIDGenerator il, TTopology t) {
        super(identificador, d, il, t);
        this.setPorts(super.NUM_PUERTOS_RECEPTOR);
        this.ports.setUnlimitedBuffer(true);
        this.estadisticas = new TReceiverStats();
    }

    /**
     * Este m�todo reinicia los atributos de la clase y los deja como recien creadops
     * por el constructor.
     * @since 1.0
     */    
    public void reset() {
        this.ports.reset();
        this.estadisticas.reset();
        estadisticas.activateStats(this.isGeneratingStats());
    }
    
    /**
     * Este m�todo devuelve el tipo del nodo.
     * @return TNode.RECEIVER, indicando que se trata de un nodo receptor.
     * @since 1.0
     */    
    public int getNodeType() {
        return super.RECEIVER;
    }

    /**
     * Este m�todo permite obtener eventos enviados desde el reloj del simulador.
     * @param evt Evento enviado desde el reloj principal del simulador.
     * @since 1.0
     */    
    public void receiveTimerEvent(TTimerEvent evt) {
        this.setStepDouration(evt.getStepDuration());
        this.setTimeInstant(evt.getUpperLimit());
        this.startOperation();
    }

    /**
     * Este m�todo se llama autom�ticamente cuando el hilo independiente del nodo se
     * pone en funcionamiento. En �l se codifica toda la funcionalidad del nodo.
     * @since 1.0
     */    
    @Override
    public void run() {
        // Acciones a llevar a cabo durante el tic.
        if (this.getPorts().isArtificiallyCongested()) {
            try {
                this.generateSimulationEvent(new TSENodeCongested(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), this.getPorts().getCongestionLevel()));
            } catch (Exception e) {
                e.printStackTrace(); 
            }
        }
        recibirDatos();
        estadisticas.consolidateData(this.getAvailableTime());
        // Acciones a llevar a cabo durante el tic.
    }
    
    /**
     * This method will put the node to cengested mode
     * @since 2.0
     */ 
    @Override
    public void toCongest(){
        if (this.getPorts().isArtificiallyCongested()) {
            this.getPorts().setArtificiallyCongested(false);
        } else {
            this.getPorts().setArtificiallyCongested(true);
        }
        super.setSayCongested(true);
    }

    /**
     * Este m�todo lee mientras puede los paquetes que hay en el buffer de recepci�n.
     * @since 1.0
     */    
    public void recibirDatos() {
        TPort p = this.ports.getPort(0);
        TAbstractPDU paquete = null;
        if (p != null) {
            while (p.thereIsAPacketWaiting()) {
                paquete = p.getPacket();
                this.contabilizarPaquete(paquete, true);
                if(paquete.getType() == TAbstractPDU.ICMP){
                    this.reply((TICMPPDU)paquete);
                }
                paquete = null;
            }
        }
    }

    /**
     * Este m�todo contabiliza en las estad�sticas del nodo un paquete le�do.
     * @param paquete Paquete que se quiere contabilizar.
     * @param deEntrada TRUE, si el paquete entra en el nodo. FALSE si el paquete sale del nodo.
     * @since 1.0
     */    
    public void contabilizarPaquete(TAbstractPDU paquete, boolean deEntrada) {
        if (deEntrada) {
            if (paquete.getSubtype() == TAbstractPDU.MPLS) {
            } else if (paquete.getSubtype() == TAbstractPDU.MPLS_GOS) {
            } else if (paquete.getSubtype() == TAbstractPDU.IPV4) {
            } else if (paquete.getSubtype() == TAbstractPDU.IPV4_GOS) {
            }
        } else {
            if (paquete.getSubtype() == TAbstractPDU.MPLS) {
            } else if (paquete.getSubtype() == TAbstractPDU.MPLS_GOS) {
            } else if (paquete.getSubtype() == TAbstractPDU.IPV4) {
            } else if (paquete.getSubtype() == TAbstractPDU.IPV4_GOS) {
            }
        }
    }
    
    /**
     * This method reply to the sender of an icmp request
     * @param packet Received ICMP packet
     * @since 2.0
     */    
    public void reply(TICMPPDU packet) {
        try {
            TICMPPDU replyPacket = new TICMPPDU(this.longIdentifierGenerator.getNextID(),this.getIPAddress(),packet.getIPv4Header().getOriginIPAddress(),0,0);
            TPort pt = ports.getPort(0);
            if (pt != null) {
                if (!pt.isAvailable()) {
                    this.generateSimulationEvent(new TSEPacketGenerated(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), replyPacket.getType(), replyPacket.getSize()));
                    this.generateSimulationEvent(new TSEPacketSent(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), replyPacket.getType()));
                    pt.putPacketOnLink(replyPacket, pt.getLink().getTargetNodeIDOfTrafficSentBy(this));
                } else {
                    discardPacket(replyPacket);
                } 
            }
        } catch (EIDGeneratorOverflow ex) {
            Logger.getLogger(TReceiverNode.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Este m�too permite acceder directamente a los ports del nodo.
     * @return El conjunto de ports del nodo.
     * @since 1.0
     */    
    public TPortSet getPorts() {
        return this.ports;
    }

    /**
     * Este m�todo devuelve si el nodo tiene ports libres o no.
     * @return TRUE, si el nodo tiene ports libres. FALSE en caso contrario.
     * @since 1.0
     */    
    public boolean hasAvailablePorts() {
        return this.ports.hasAvailablePorts();
    }

    /**
     * Este m�todo devuelve el peso del nodo que debe ser tenido en cuenta por los
     * algoritmos de encaminamiento para el c�lculo de rutas.
     * @return En el caso de un nodo receptor, siempre es cero.
     * @since 1.0
     */    
    public long getRoutingWeight() {
        return 0;
    }

    /**
     * Devuelve si el nodo est� bien configurado o no.
     * @return TRUE, si el nodo esta bien configurado. FALSE en caso contrario.
     * @since 1.0
     */    
    public boolean isWellConfigured() {
        return this.wellConfigured;
    }
    
    /**
     * Este m�todo comprueba la configuraci�n del nodo y devuelve un c�digo expresando
     * dicho estado.
     * @param t Topolog�a donde est� el nodo.
     * @param recfg TRUE, si se est� reconfigurando el nodo. FALSE si el nodo se est� configurando
     * por primera vez.
     * @return CORRECTA, si el nodo est� bien configurado. Un codigo de error en caso
     * contrario.
     * @since 1.0
     */    
    public int validateConfig(TTopology t, boolean recfg) {
        this.setWellConfigured(false);
        if (this.getName().equals(""))
            return this.SIN_NOMBRE;
        if (this.getIPAddress().isEmpty() && recfg)
            return this.EMPTY_IP;
        boolean soloEspacios = true;
        for (int i=0; i < this.getName().length(); i++){
            if (this.getName().charAt(i) != ' ')
                soloEspacios = false;
        }
        if (soloEspacios)
            return this.SOLO_ESPACIOS;
        if (!recfg) {
            TNode tp = t.setFirstNodeNamed(this.getName());
            if (tp != null)
                return this.NOMBRE_YA_EXISTE;
        } else {
            TNode tp = t.setFirstNodeNamed(this.getName());
            if (tp != null) {
                if (this.topology.thereIsMoreThanANodeNamed(this.getName())) {
                    return this.NOMBRE_YA_EXISTE;
                }
            }
        }
        this.setWellConfigured(true);
        estadisticas.activateStats(this.isGeneratingStats());
        return this.CORRECTA;
    }
    
    /**
     * Este m�todo transforma el codigo de error de configuraci�n del nodo en un texto
     * entendible y explicativo.
     * @param e Codigo de error.
     * @return Texto explicativo del codigo de error.
     * @since 1.0
     */    
    public String getErrorMessage(int e) {
        switch (e) {
            case SIN_NOMBRE: return (java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TConfigReceptor.FALTA_NOMBRE"));
            case NOMBRE_YA_EXISTE: return (java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TConfigReceptor.NOMBRE_REPETIDO"));
            case SOLO_ESPACIOS: return (java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TNodoReceptor.NombreNoSoloEspacios"));
            case EMPTY_IP: return "IP Address of the node is not present";
        }
        return ("");
    }

    /**
     * Este m�todo transforma el nodo en una representaci�n textual volcable a disco.
     * @return La representaci�n textual del nodo.
     * @since 1.0
     */    
    @Override
    public String marshall() {
        String cadena = "#Receiver#";
        cadena += this.getID();
        cadena += "#";
        cadena += this.getName().replace('#', ' ');
        cadena += "#";
        cadena += this.getIPAddress();
        cadena += "#";
        cadena += this.getShowName();
        cadena += "#";
        cadena += this.isGeneratingStats();
        cadena += "#";
        cadena += this.obtenerPosicion().x;
        cadena += "#";
        cadena += this.obtenerPosicion().y;
        cadena += "#";
        return cadena;
    }
    
    /**
     * Este m�todo deserializa un nodo serializado pasado por par�metro,
     * reconstruyendolo en memoria en la instancia actual.
     * @param elemento Nodo receptor serializado.
     * @return TRUE, si se ha podido deserializar correctamente. FALSE en caso contrario.
     * @since 1.0
     */    
    @Override
    public boolean unMarshall(String elemento) {
        String valores[] = elemento.split("#");
        if (valores.length != 9) {
            return false;
        }
        this.setID(Integer.parseInt(valores[2]));
        this.setName(valores[3]);
        this.setIPAddress(valores[4]);
        this.setStatus(0);
        this.setShowName(Boolean.parseBoolean(valores[5]));
        this.setGenerateStats(Boolean.parseBoolean(valores[6]));
        int posX = Integer.parseInt(valores[7]);
        int posY = Integer.parseInt(valores[8]);
        this.setPosition(new Point(posX+24, posY+24));
        return true;
    }
    
    @Override
    public boolean addTableEntry(String tableEntry) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public String saveTableEntry() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    /**
     * Este m�todo permite acceder directamente a las estad�sticas del nodo.
     * @return Las estad�sticas del nodo.
     * @since 1.0
     */    
    public TStats getStats() {
        return this.estadisticas;
    }
    
    /**
     * Este m�todo permite establecer el n�mero de ports que deseamos para el nodo.
     * @param num El n�mero de ports del nodo. Como mucho 8.
     * @since 1.0
     */    
    public synchronized void setPorts(int num) {
        ports = new TFIFOPortSet(num, this);
    }
    
    /**
     * Este m�todo permite descartar nu paquete en el nodo y reflejar este descarte en
     * las estadisticas.
     * @param paquete paquete que deseamos descartar.
     * @since 1.0
     */    
    public void discardPacket(TAbstractPDU paquete) {
        // Un receptor no descarta paquetes, porque tiene un buffer 
        // ilimitado y no analiza el tr�fico. Lo recibe y ya est�.
        paquete = null;
    }

    /**    
    * Este m�todo no hace nada en un Receptor. En un nodo activo permitir�
    * solicitar a un nodo activo la retransmisi�n de un paquete.
    * @param paquete Paquete cuya retransmisi�n se est� solicitando.
    * @param pSalida Puerto por el que se enviar� la solicitud.
    * @since 1.0
    */    
    public void runGoSPDUStoreAndRetransmitProtocol(TMPLSPDU paquete, int pSalida) {
    }
    
    /**
     * Esta constante identifica que la configuraci�n del nodo es correcta.
     * @since 1.0
     */    
    public static final int CORRECTA = 0;
    /**
     * Esta constante identifica que el nodo no tiene nombre.
     * @since 1.0
     */    
    public static final int SIN_NOMBRE = 1;
    /**
     * Esta constante identifica que el nombre del nodo ya existe con anterioridad.
     * @since 1.0
     */    
    public static final int NOMBRE_YA_EXISTE = 2;
    /**
     * Esta constante identifica que el nombre del nodo est� formado s�lo por espacios.
     * @since 1.0
     */    
    public static final int SOLO_ESPACIOS = 3;
    /**
     * IP Address is empty
     * @since 1.0
     */    
    public static final int EMPTY_IP = 4;
    
    private TReceiverStats estadisticas;
}
