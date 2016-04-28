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

import simMPLS.protocols.TGPSRPPDU;
import simMPLS.protocols.TTLDPPDU;
import simMPLS.protocols.TAbstractPDU;
import simMPLS.protocols.TMPLSLabel;
import simMPLS.protocols.TMPLSPDU;
import simMPLS.protocols.TTLDPPayload;
import simMPLS.protocols.TIPv4PDU;
import simMPLS.protocols.TICMPPDU;
import simMPLS.hardware.timer.TTimerEvent;
import simMPLS.hardware.timer.ITimerEventListener;
import simMPLS.hardware.ports.TFIFOPort;
import simMPLS.hardware.tldp.TSwitchingMatrix;
import simMPLS.hardware.tldp.TSwitchingMatrixEntry;
import simMPLS.hardware.ports.TFIFOPortSet;
import simMPLS.hardware.ports.TPort;
import simMPLS.hardware.ports.TPortSet;
import simMPLS.utils.EIDGeneratorOverflow;
import simMPLS.utils.TIDGenerator;
import simMPLS.utils.TLongIDGenerator;
import java.awt.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.*;
import org.jfree.data.*;
import simMPLS.hardware.tldp.IPAddress;

/**
 * Esta clase implementa un Label Edge Router (LER) de entrada/salida del dominio
 * MPLS.
 * @author <B>Manuel Dom�nguez Dorado</B><br><A
 * href="mailto:ingeniero@ManoloDominguez.com">ingeniero@ManoloDominguez.com</A><br><A href="http://www.ManoloDominguez.com" target="_blank">http://www.ManoloDominguez.com</A>
 * @version 1.0
 */
public class TLERNode extends TNode implements ITimerEventListener, Runnable {
    
    /**
     * Este m�todo es el constructor de la clase. Crea una nueva instancia de TNodoLER
     * y otorga unos valores iniciales a los atributos.
     * @param identificador Clabve primaria que permite buscar, encontrar y ordenadr dentro de la topolog�a
     * a esta instancia del LER. Identifica el nodo como unico.
     * @param d Direcci�n IP �nica que tendr� el nodo.
     * @param il generador de identificadores largos. Se usa para que el LER pueda obtener un
 id unico para cada evento que genere.
     * @param t Referencia a la topolog�a a la que pertenece el LER. Le permite hacer
     * comprobaciones, calcular rutas, etc�tera.
     * @since 1.0
     */
    public TLERNode(int identificador, String d, TLongIDGenerator il, TTopology t) {
        super(identificador, d, il, t);
        this.setPorts(TNode.NUM_PUERTOS_LER);
        matrizConmutacion = new TSwitchingMatrix();
        gIdent = new TLongIDGenerator();
        gIdentLDP = new TIDGenerator();
        potenciaEnMb = 512;
        estadisticas = new TLERStats();
        propagateTTL = false;
        doneLDP=false;
        PHP = true;
    }
    
    /**
     * Este m�todo calcula el n�mero de nanosegundos que se necesitan para conmutar un
     * bit. Se basa en la potencia de conmutaci�n configurada para el LER.
     * @return El n�mero de nanosegundos necesarios para conmutar un bit.
     * @since 1.0
     */
    public double obtenerNsPorBit() {
        long tasaEnBitsPorSegundo = this.potenciaEnMb*1048576L;
        double nsPorCadaBit = (1000000000.0/tasaEnBitsPorSegundo);
        return nsPorCadaBit;
    }
    
    /**
     * Este m�todo calcula el numero de nanosegundos que son necesarios para conmutar
     * un determinado n�mero de octetos.
     * @param octetos El n�mero de octetos que queremos conmutar.
     * @return El n�mero de nanosegundos necesarios para conmutar el n�mero de octetos
     * especificados.
     * @since 1.0
     */
    public double obtenerNsUsadosTotalOctetos(int octetos) {
        double nsPorCadaBit = obtenerNsPorBit();
        long bitsOctetos = (octetos*8);
        return (nsPorCadaBit*bitsOctetos);
    }
    
    /**
     * Este m�todo calcula el n�mero de bits que puede conmutar el nodo con el n�mero
     * de nanosegundos de que dispone actualmente.
     * @return El n�mero de bits m�ximo que puede conmutar el nodo con los nanosegundos de que
     * dispone actualmente.
     * @since 1.0
     */
    public int obtenerLimiteBitsTransmitibles() {
        double nsPorCadaBit = obtenerNsPorBit();
        double maximoBits = (availableNs/nsPorCadaBit);
        return (int) maximoBits;
    }
    
    /**
     * Este m�todo calcula el n�mero de octetos completos que puede transmitir el nodo
     * con el n�mero de nanosegundos de que dispone.
     * @return El n�mero de octetos completos que puede transmitir el nodo en un momento dado.
     * @since 1.0
     */
    public int obtenerOctetosTransmitibles() {
        double maximoBytes = ((double)obtenerLimiteBitsTransmitibles()/8.0);
        return (int) maximoBytes;
    }
    
    /**
     * Este m�todo obtiene la potencia em Mbps con que est� configurado el nodo.
     * @return La potencia de conmutaci�n del nodo en Mbps.
     * @since 1.0
     */
    public int obtenerPotenciaEnMb() {
        return this.potenciaEnMb;
    }
    
    /**
     * Este m�todo permite establecer la potencia de conmutaci�n del nodo en Mbps.
     * @param pot Potencia deseada para el nodo en Mbps.
     * @since 1.0
     */
    public void ponerPotenciaEnMb(int pot) {
        this.potenciaEnMb = pot;
    }
    
    /**
     * Este m�todo obtiene el tama�o del buffer del nodo.
     * @return Tama�o del buffer del nodo en MB.
     * @since 1.0
     */
    public int obtenerTamanioBuffer() {
        return this.getPorts().getBufferSizeInMB();
    }
    
    /**
     * Este m�todo permite establecer el tama�o del buffer del nodo.
     * @param tb Tama�o el buffer deseado para el nodo, en MB.
     * @since 1.0
     */
    public void ponerTamanioBuffer(int tb) {
        this.getPorts().setBufferSizeInMB(tb);
    }
    
    /**
     * Este m�todo reinicia los atributos de la clase como si acabasen de ser creados
     * por el constructor.
     * @since 1.0
     */
    public void reset() {
        this.ports.reset();
        if(this.isLDP())
            matrizConmutacion.reset();
        this.doneLDP=false;
        gIdent.reset();
        gIdentLDP.reset();
        estadisticas.reset();
        estadisticas.activateStats(this.isGeneratingStats());
        this.resetStepsWithoutEmittingToZero();
    }
    
    /**
     * Este m�todo indica el tipo de nodo de que se trata la instancia actual.
     * @return LER. Indica que el nodo es de este tipo.
     * @since 1.0
     */
    public int getNodeType() {
        return TNode.LER;
    }
    
    /**
     * Este m�todo inicia el hilo de ejecuci�n del LER, para que entre en
     * funcionamiento. Adem�s controla el tiempo de que dispone el LER para conmutar
     * paquetes.
     * @param evt Evento de reloj que sincroniza la ejecuci�n de los elementos de la topology.
     * @since 1.0
     */
    public void receiveTimerEvent(TTimerEvent evt) {
        this.setStepDouration(evt.getStepDuration());
        this.setTimeInstant(evt.getUpperLimit());
        if (this.getPorts().isThereAnyPacketToRoute()) {
            this.availableNs += evt.getStepDuration();
        } else {
            this.resetStepsWithoutEmittingToZero();
            this.availableNs = evt.getStepDuration();
        }
        this.startOperation();
    }
    
    /**
     * Llama a las acciones que se tienen que ejecutar en el transcurso del tic de
     * reloj que el LER estar� en funcionamiento.
     * @since 1.0
     */
    public void run() {
        // Acciones a llevar a cabo durante el tic.
        if (this.getPorts().isArtificiallyCongested()) {
            try {
                this.generateSimulationEvent(new TSENodeCongested(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), this.getPorts().getCongestionLevel()));
            } catch (Exception e) {
                e.printStackTrace(); 
            }
        }
        comprobarElEstadoDeLasComunicaciones();
        decrementarContadores();
        if(!this.doneLDP){
            if(this.isLDP()){
                this.initializeLDP();
            }else {
                this.initializeLocal();
            }
        }
        encaminarPaquetes();
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
     * Este m�todo comprueba que haya conectividad con sus nodos adyacentes, es decir,
     * que no haya caido ning�n enlace. Si ha caido alg�n enlace, entonces genera la
     * correspondiente se�alizaci�n para notificar este hecho.
     * This method verify that there is connectivity to its adjacent nodes.
     * @since 1.0
     */
    public void comprobarElEstadoDeLasComunicaciones() {
        boolean eliminar = false;
        TSwitchingMatrixEntry emc = null;
        int idPuerto = 0;
        TPort puertoSalida = null;
        TLink et = null;
        matrizConmutacion.getMonitor().lock();
        Iterator it = matrizConmutacion.getEntriesIterator();
        while (it.hasNext()) {
            emc = (TSwitchingMatrixEntry) it.next();
            if (emc != null) {
                idPuerto = emc.getOutgoingPortID();
                if ((idPuerto >= 0) && (idPuerto < this.ports.getNumberOfPorts())) {
                    puertoSalida = this.ports.getPort(idPuerto);
                    if (puertoSalida != null) {
                        et = puertoSalida.getLink();
                        if (et != null) {
                            if ((et.isBroken()) && (emc.getOutgoingLabel() != TSwitchingMatrixEntry.REMOVING_LABEL)) {
                                if (this.isLDP()) {
                                    labelWithdrawal(emc);
                                } else {
                                    eliminar = true;
                                }
                            }
                        }
                    }
                }
                if (eliminar) {
                    it.remove();
                }
            } else {
                it.remove();
            }
        }
        matrizConmutacion.getMonitor().unLock();
    }
    
    /**
     * Este m�todo lee del puerto que corresponda seg�n el turno Round Robin
     * consecutivamente hasta que se termina el cr�dito. Si tiene posibilidad de
     * conmutar y/o encaminar un paquete, lo hace, llamando para ello a los m�todos
     * correspondiente segun el paquete. Si el paquete est� mal formado o es
     * desconocido, lo descarta.
     * This method reads the corresponding port by shift Round Robin consecutively until the credit is over. 
     * If you have possibility of switching and / or routing a packet, it does, calling for it to corresponding methods 
     * according to the package. If the package is malformed or unknown, discard it.
     * @since 1.0
     */
    public void encaminarPaquetes() {
        boolean conmute = false;
        int puertoLeido = 0;
        TAbstractPDU paquete = null;
        int octetosQuePuedoMandar = this.obtenerOctetosTransmitibles();
        while (this.getPorts().canSwitchPacket(octetosQuePuedoMandar)) {
            conmute = true;
            paquete = this.ports.getNextPacket();
            puertoLeido = ports.getReadPort();
            if (paquete != null) {
                if (paquete.getType() == TAbstractPDU.IPV4 && !paquete.getIPv4Header().getTargetIPv4Address().equals(this.getIPAddress())) {
                    conmutarIPv4((TIPv4PDU) paquete, puertoLeido);
                } else if (paquete.getType() == TAbstractPDU.TLDP && this.isLDP()) {
                    conmutarTLDP((TTLDPPDU) paquete, puertoLeido);
                } else if (paquete.getType() == TAbstractPDU.MPLS) {
                    conmutarMPLS((TMPLSPDU) paquete, puertoLeido);
                } else if (paquete.getType() == TAbstractPDU.GPSRP) {
                    conmutarGPSRP((TGPSRPPDU) paquete, puertoLeido);
                } else if (paquete.getType() == TAbstractPDU.ICMP) {
                    conmutarICMP((TICMPPDU) paquete, puertoLeido);
                } else {
                    this.availableNs += obtenerNsUsadosTotalOctetos(paquete.getSize());
                    discardPacket(paquete);
                }
                this.availableNs -= obtenerNsUsadosTotalOctetos(paquete.getSize());
                octetosQuePuedoMandar = this.obtenerOctetosTransmitibles();
            }
        }
        if (conmute) {
            this.resetStepsWithoutEmittingToZero();
        } else {
            this.increaseStepsWithoutEmitting();
        }
    }
    
    /**
     * Este m�todo conmuta un paquete GPSRP.
     * @param paquete Paquete GPSRP a conmutar.
     * @param pEntrada Puerto por el que ha entrado el paquete.
     * @since 1.0
     */
    public void conmutarGPSRP(TGPSRPPDU paquete, int pEntrada) {
        if (paquete != null) {
            int mensaje = paquete.getGPSRPPayload().getGPSRPMessageType();
            int flujo = paquete.getGPSRPPayload().getFlowID();
            int idPaquete = paquete.getGPSRPPayload().getPacketID();
            String IPDestinoFinal = paquete.getIPv4Header().getTargetIPv4Address();
            TFIFOPort pSalida = null;
            if (IPDestinoFinal.equals(this.getIPAddress())) {
                // Un LER no entiende peticiones GPSRP, por tanto no pueder
                // haber mensajes GPSRP dirigidos a �l.
                this.discardPacket(paquete);
            } else {
                String IPSalida = this.topology.obtenerIPSalto(this.getIPAddress(), IPDestinoFinal);
                pSalida = (TFIFOPort) this.ports.getLocalPortConnectedToANodeWithIPAddress(IPSalida);
                if (pSalida != null) {
                    pSalida.putPacketOnLink(paquete, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                    try {
                        this.generateSimulationEvent(new TSEPacketRouted(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), TAbstractPDU.GPSRP));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    this.discardPacket(paquete);
                }
            }
        }
    }
    
    /**
     * Este m�todo comprueba si existe una entrada en la tabla de encaminamiento para
     * el paquete entrante. Si no es as�, clasifica el paquete y, si es necesario,
     * reencola el paquete y solicita una etiqueta para poder enviarlo. Una vez que
     * tiene entrada en la tabla de encaminamiento, reenv�a el paquete hacia el
     * interior del dominio MPLS o hacia el exterior, segun corresponda.
     * @param paquete Paquete IPv4 de entrada.
     * @param pEntrada Puerto por el que ha accedido al nodo el paquete.
     * @since 1.0
     */
    public void conmutarIPv4(TIPv4PDU paquete, int pEntrada) {
        if(paquete.getIPv4Header().getTTL()-1 <1){
            TICMPPDU paqueteICMP = this.replyICMP(paquete,11,0);
            TPort pSalida = ports.getPort(pEntrada);
            pSalida.putPacketOnLink(paqueteICMP, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
        }else {
            int valorFEC = IPAddress.parseNumericAddress(paquete.getIPv4Header().getTargetIPv4Address());
            TSwitchingMatrixEntry emc = null;
            emc = matrizConmutacion.getEntry(valorFEC, TSwitchingMatrixEntry.FEC_ENTRY);
            if (emc == null) {
                TICMPPDU paqueteICMP = this.replyICMP(paquete,3,0);
                this.conmutarICMP(paqueteICMP, 0);
            }else {
                int etiquetaActual = emc.getOutgoingLabel();
                if (etiquetaActual == TSwitchingMatrixEntry.UNDEFINED) {
                    discardPacket(paquete);
                } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_REQUESTED) {
                    this.ports.getPort(pEntrada).reEnqueuePacket(paquete);
                } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_UNAVAILABLE) {
                    discardPacket(paquete);
                } else if (etiquetaActual == TSwitchingMatrixEntry.REMOVING_LABEL) {
                    discardPacket(paquete);
                } else if ((etiquetaActual > 15) || (etiquetaActual == TSwitchingMatrixEntry.LABEL_ASSIGNED)) {
                    int operacion = emc.getLabelStackOperation();
                    if (operacion == TSwitchingMatrixEntry.UNDEFINED) {
                        discardPacket(paquete);
                    } else {
                        if (operacion == TSwitchingMatrixEntry.PUSH_LABEL) {
                            TMPLSPDU paqueteMPLS = this.crearPaqueteMPLS(paquete, emc);
                            TPort pSalida = ports.getPort(emc.getOutgoingPortID());
                            pSalida.putPacketOnLink(paqueteMPLS, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                            try {
                                this.generateSimulationEvent(new TSEPacketRouted(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), paquete.getSubtype()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (operacion == TSwitchingMatrixEntry.POP_LABEL) {
                            discardPacket(paquete);
                        } else if (operacion == TSwitchingMatrixEntry.SWAP_LABEL) {
                            discardPacket(paquete);
                        } else if (operacion == TSwitchingMatrixEntry.NOOP) {
                            paquete.getIPv4Header().setTTL(paquete.getIPv4Header().getTTL()-1);
                            TPort pSalida = ports.getPort(emc.getOutgoingPortID());
                            pSalida.putPacketOnLink(paquete, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                            try {
                                this.generateSimulationEvent(new TSEPacketRouted(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), paquete.getSubtype()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else {
                    discardPacket(paquete);
                }
            }
        }
    }
    
    /**
     * Este m�todo comprueba si existe una entrada en la tabla de encaminamiento para
     * el paquete entrante. Si no es as�, clasifica el paquete y, si es necesario,
     * reencola el paquete y solicita una etiqueta para poder enviarlo. Una vez que
     * tiene entrada en la tabla de encaminamiento, reenv�a el paquete hacia el
     * interior del dominio MPLS o hacia el exterior, segun corresponda.
     * @param paquete Paquete IPv4 de entrada.
     * @param pEntrada Puerto por el que ha accedido al nodo el paquete.
     * @since 1.0
     */
    public void conmutarICMP(TICMPPDU paquete, int pEntrada) {
        if(paquete.getIPv4Header().getTTL() <=1){
            TICMPPDU paqueteICMP = this.replyICMP(paquete,11,0);
            this.conmutarICMP(paqueteICMP, 0);
        }else if(paquete.getIPv4Header().getTargetIPv4Address().equals(this.getIPAddress())){
            TICMPPDU paqueteICMP = this.replyICMP(paquete,0,0);
            this.conmutarICMP(paqueteICMP, 0);
        }else{
            int valorFEC = IPAddress.parseNumericAddress(paquete.getIPv4Header().getTargetIPv4Address());
            TSwitchingMatrixEntry emc = null;
            emc = matrizConmutacion.getEntry(valorFEC, TSwitchingMatrixEntry.FEC_ENTRY);
            if (emc == null) {
                if(!paquete.getIPv4Header().getOriginIPAddress().equals(this.getIPAddress())){
                    TICMPPDU paqueteICMP = this.replyICMP(paquete,3,0);
                    this.conmutarICMP(paqueteICMP, 0);
                }else
                    discardPacket(paquete);
            }else {
                int etiquetaActual = emc.getOutgoingLabel();
                if (etiquetaActual == TSwitchingMatrixEntry.UNDEFINED) {
                    discardPacket(paquete);
                } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_REQUESTED) {
                    this.ports.getPort(pEntrada).reEnqueuePacket(paquete);
                } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_UNAVAILABLE) {
                    discardPacket(paquete);
                } else if (etiquetaActual == TSwitchingMatrixEntry.REMOVING_LABEL) {
                    discardPacket(paquete);
                } else if ((etiquetaActual > 15) || (etiquetaActual == TSwitchingMatrixEntry.LABEL_ASSIGNED)) {
                    int operacion = emc.getLabelStackOperation();
                    if (operacion == TSwitchingMatrixEntry.UNDEFINED) {
                        discardPacket(paquete);
                    } else {
                        if (operacion == TSwitchingMatrixEntry.PUSH_LABEL) {
                            if(paquete.getSubtype() == TAbstractPDU.ICMPTOREROUTE){
                                paquete.setSubtype(TAbstractPDU.ICMP);
                            }
                            if(emc.getOutgoingPortID() != TSwitchingMatrixEntry.UNDEFINED){
                                TPort pSalida = ports.getPort(emc.getOutgoingPortID());
                                TMPLSPDU paqueteMPLS = this.crearPaqueteMPLS(paquete, emc);
                                pSalida.putPacketOnLink(paqueteMPLS, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                                try {
                                    this.generateSimulationEvent(new TSEPacketRouted(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), paquete.getSubtype()));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } else if (operacion == TSwitchingMatrixEntry.POP_LABEL) {
                            discardPacket(paquete);
                        } else if (operacion == TSwitchingMatrixEntry.SWAP_LABEL) {
                            discardPacket(paquete);
                        } else if (operacion == TSwitchingMatrixEntry.NOOP) {
                            if(paquete.getSubtype() == TAbstractPDU.ICMPTOREROUTE){
                                TICMPPDU packetICMP = paquete;
                                packetICMP.setSubtype(TAbstractPDU.ICMP);
                                this.conmutarICMP(packetICMP, 0);
                            }else {
                                paquete.getIPv4Header().setTTL(paquete.getIPv4Header().getTTL()-1);
                                TPort pSalida = ports.getPort(emc.getOutgoingPortID());
                                pSalida.putPacketOnLink(paquete, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                                try {
                                    this.generateSimulationEvent(new TSEPacketRouted(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), paquete.getSubtype()));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } else {
                    discardPacket(paquete);
                }
            }
        }
    }
    
    /**
     * Este m�todo se llama cuando se recibe un paquete TLDP con informaci�n sobre las
 etiquetas a use. El m�todo realiza sobre las matriz de encaminamiento la
     * operaci�n que sea necesario y propaga el cambio al nodo adyacente que
     * corresponda.
     * @param paquete Paquete TLDP recibido.
     * @param pEntrada Puerto por el que se ha recibido el paquete TLDP.
     * @since 1.0
     */
    public void conmutarTLDP(TTLDPPDU paquete, int pEntrada) {
        if (paquete.getTLDPPayload().getTLDPMessageType() == TTLDPPayload.LABEL_REQUEST) {
            this.tratarSolicitudTLDP(paquete, pEntrada);
        } else if (paquete.getTLDPPayload().getTLDPMessageType() == TTLDPPayload.LABEL_REQUEST_OK) {
            //this.tratarSolicitudOkTLDP(paquete, pEntrada);
        } else if (paquete.getTLDPPayload().getTLDPMessageType() == TTLDPPayload.LABEL_REQUEST_DENIED) {
            this.tratarSolicitudNoTLDP(paquete, pEntrada);
        } else if (paquete.getTLDPPayload().getTLDPMessageType() == TTLDPPayload.LABEL_REMOVAL_REQUEST) {
            this.tratarEliminacionTLDP(paquete, pEntrada);
        } else if (paquete.getTLDPPayload().getTLDPMessageType() == TTLDPPayload.LABEL_REVOMAL_REQUEST_OK) {
            this.tratarEliminacionOkTLDP(paquete, pEntrada);
        }
    }
    
    /**
     * Este m�todo comprueba si existe una entrada en la tabla de encaminamiento para
     * el paquete entrante. Si no es as�, clasifica el paquete y, si es necesario,
     * reencola el paquete y solicita una etiqueta para poder enviarlo. Una vez que
     * tiene entrada en la tabla de encaminamiento, reenv�a el paquete hacia el
     * siguiente nodo del dominio MPLS o hacia el exterior, segun corresponda.
     * @param paquete Paquete MPLS recibido.
     * @param pEntrada Puerto por el que ha llegado el paquete MPLS recibido.
     * @since 1.0
     */
    public void conmutarMPLS(TMPLSPDU paquete, int pEntrada) {
        if(paquete.getLabelStack().getTop().getTTL()-1 <1){
            TICMPPDU paqueteICMP = this.replyICMP(paquete,11,0);
            this.conmutarICMP(paqueteICMP, 0);
        }else {
            TMPLSLabel eMPLS = null;
            TSwitchingMatrixEntry emc = null;
            boolean conEtiqueta1 = false;
            if (paquete.getLabelStack().getTop().getLabel() == 1) {
                eMPLS = paquete.getLabelStack().getTop();
                paquete.getLabelStack().popTop();
                conEtiqueta1 = true;
            }
            int valorLABEL;
            if(paquete.getLabelStack().getTop().getLabel() == 0)
                valorLABEL = 3;
            else
                valorLABEL = paquete.getLabelStack().getTop().getLabel();
            emc = matrizConmutacion.getEntry(valorLABEL, TSwitchingMatrixEntry.LABEL_ENTRY);
            if (emc == null) {
                valorLABEL = IPAddress.parseNumericAddress(paquete.getCarriedPacket().getIPv4Header().getTargetIPv4Address());;
                emc = matrizConmutacion.getEntry(valorLABEL, TSwitchingMatrixEntry.FEC_ENTRY);
                if (emc == null) {
                    TICMPPDU paqueteICMP = this.replyICMP(paquete,3,0);
                    this.conmutarICMP(paqueteICMP, 0);
                    return;
                }
            }
            int etiquetaActual = emc.getOutgoingLabel();
            if (etiquetaActual == TSwitchingMatrixEntry.UNDEFINED) {
                discardPacket(paquete);
            } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_REQUESTED) {
                if (conEtiqueta1) {
                    paquete.getLabelStack().pushTop(eMPLS);
                }
                this.ports.getPort(pEntrada).reEnqueuePacket(paquete);
            } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_UNAVAILABLE) {
                if (conEtiqueta1) {
                    paquete.getLabelStack().pushTop(eMPLS);
                }
                discardPacket(paquete);
            } else if (etiquetaActual == TSwitchingMatrixEntry.REMOVING_LABEL) {
                if (conEtiqueta1) {
                    paquete.getLabelStack().pushTop(eMPLS);
                }
                discardPacket(paquete);
            } else if ((etiquetaActual >= 0) || (etiquetaActual == TSwitchingMatrixEntry.LABEL_ASSIGNED)) {
                int operacion = emc.getLabelStackOperation();
                if (operacion == TSwitchingMatrixEntry.UNDEFINED) {
                    if (conEtiqueta1) {
                        paquete.getLabelStack().pushTop(eMPLS);
                    }
                    discardPacket(paquete);
                } else {
                    if (operacion == TSwitchingMatrixEntry.PUSH_LABEL) {
                        TMPLSLabel empls = new TMPLSLabel();
                        empls.setBoS(false);
                        empls.setEXP(0);
                        empls.setLabel(emc.getOutgoingLabel());
                        if(propagateTTL)
                            empls.setTTL(paquete.getLabelStack().getTop().getTTL()-1);
                        else
                            empls.setTTL(255);
                        if(empls.getTTL()<1){
                            TICMPPDU paqueteICMP = this.replyICMP(paquete,11,0);
                            TPort pSalida = ports.getPort(pEntrada);
                            pSalida.putPacketOnLink(paqueteICMP, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                        }else{
                            paquete.getLabelStack().pushTop(empls);
                            if (conEtiqueta1) {
                                paquete.getLabelStack().pushTop(eMPLS);
                                paquete.setSubtype(TAbstractPDU.MPLS_GOS);
                            } else {
                                paquete.setSubtype(TAbstractPDU.MPLS);
                            }
                            TPort pSalida = ports.getPort(emc.getOutgoingPortID());
                            pSalida.putPacketOnLink(paquete, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                            try {
                                this.generateSimulationEvent(new TSEPacketRouted(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), paquete.getSubtype()));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }                        
                    } else if (operacion == TSwitchingMatrixEntry.POP_LABEL) {
//                            if (paquete.getLabelStack().getTop().getBoS()) {
                            if(paquete.getCarriedPacket().getSubtype() == TAbstractPDU.ICMPTOREROUTE){
                                TICMPPDU packetICMP = (TICMPPDU)paquete.getCarriedPacket();
                                packetICMP.setSubtype(TAbstractPDU.ICMP);
                                this.conmutarICMP(packetICMP, 0);
                            }else{
                                if(paquete.getLabelStack().getSize()<=1){
                                    TAbstractPDU packetToSend = paquete.getCarriedPacket();
                                    if(propagateTTL)
                                        packetToSend.getIPv4Header().setTTL(paquete.getLabelStack().getTop().getTTL()-1);
                                    if (conEtiqueta1) {
                                        packetToSend.setSubtype(TAbstractPDU.IPV4_GOS);
                                    }
                                    try {
                                        this.generateSimulationEvent(new TSEPacketGenerated(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), packetToSend.getSubtype(), packetToSend.getSize()));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    TPort pSalida = ports.getPort(emc.getOutgoingPortID());
                                    pSalida.putPacketOnLink(packetToSend, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                                }else {
                                    if(propagateTTL){
                                        int ttl = paquete.getLabelStack().getTop().getTTL()-1;
                                        paquete.getLabelStack().popTop();
                                        paquete.getLabelStack().getTop().setTTL(ttl);
                                    }else {
                                        paquete.getLabelStack().popTop();
                                    }
                                    if (conEtiqueta1) {
                                        paquete.getLabelStack().pushTop(eMPLS);
                                    }
                                    TPort pSalida = ports.getPort(emc.getOutgoingPortID());
                                    pSalida.putPacketOnLink(paquete, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                                    try {
                                        this.generateSimulationEvent(new TSEPacketSwitched(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), paquete.getSubtype()));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
//                            } else {
//                                paquete.getLabelStack().popTop();
//                                if (conEtiqueta1) {
//                                    paquete.getLabelStack().pushTop(eMPLS);
//                                }
//                                TPort pSalida = ports.getPort(emc.getOutgoingPortID());
//                                pSalida.putPacketOnLink(paquete, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
//                            }
                        try {
                            this.generateSimulationEvent(new TSEPacketRouted(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), paquete.getSubtype()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (operacion == TSwitchingMatrixEntry.SWAP_LABEL) {
                        paquete.getLabelStack().getTop().setLabel(emc.getOutgoingLabel());
                        if (conEtiqueta1) {
                            paquete.getLabelStack().pushTop(eMPLS);
                        }
                        TPort pSalida = ports.getPort(emc.getOutgoingPortID());
                        pSalida.putPacketOnLink(paquete, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                        try {
                            this.generateSimulationEvent(new TSEPacketRouted(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), paquete.getSubtype()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (operacion == TSwitchingMatrixEntry.NOOP) {
                        TPort pSalida = ports.getPort(emc.getOutgoingPortID());
                        pSalida.putPacketOnLink(paquete, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                        try {
                            this.generateSimulationEvent(new TSEPacketRouted(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), paquete.getSubtype()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                if (conEtiqueta1) {
                    paquete.getLabelStack().pushTop(eMPLS);
                }
                discardPacket(paquete);
            }  
        }
    }
    
    /**
     * This method will launch the LDP process for this node.
     * @since 2.0
     */
    public void initializeLDP(){
        int i;
        for(i=0;i<this.ports.getNumberOfPorts();i++){
            TPort portLocal = ports.getPort(i);
            if (portLocal != null && !portLocal.isAvailable()) {
                TLink et = portLocal.getLink();
                if(!et.isBroken() && et.getLinkType()==TLink.EXTERNAL){
                    TNode oppositeNode = et.getTargetNodeOfTrafficSentBy(this);
                    int ipToAnnounce= IPAddress.getSubnet(oppositeNode.getIPAddress(), oppositeNode.getMask());
                    String maskToAnnounce = oppositeNode.getMask(); 
                    TSwitchingMatrixEntry emc = null;
                    if(this.isPHP())
                        emc = crearEntradaInicialEnMatrizFEC(IPAddress.intToIp(ipToAnnounce), maskToAnnounce, oppositeNode.getIPAddress(), TSwitchingMatrixEntry.NOOP);
                    else{
                        TSwitchingMatrixEntry sme = crearEntradaInicialEnMatrizFEC(IPAddress.intToIp(ipToAnnounce), maskToAnnounce, oppositeNode.getIPAddress(), TSwitchingMatrixEntry.NOOP);
                        sme.setOutgoingLabel(TSwitchingMatrixEntry.LABEL_ASSIGNED);
                        emc = crearEntradaInicialEnMatrizLABEL(this.topology.getNewLabel(), i, TSwitchingMatrixEntry.POP_LABEL);
                    }
                    emc.setOutgoingLabel(TSwitchingMatrixEntry.LABEL_ASSIGNED);
                    try {
                        emc.setLocalTLDPSessionID(this.topology.getgIdentLDP().getNew());
                    } catch (EIDGeneratorOverflow ex) {
                        Logger.getLogger(TLERNode.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (emc != null) {
                        sendLDP(emc, IPAddress.intToIp(ipToAnnounce), maskToAnnounce);
                    } 
                }
            }
        }
        this.doneLDP=true;
    }
    
    /**
     * This method will launch the LDP process for this node.
     * @since 2.0
     */
    public void initializeLocal(){
        int i;
        for(i=0;i<this.ports.getNumberOfPorts();i++){
            TPort portLocal = ports.getPort(i);
            if (portLocal != null && !portLocal.isAvailable()) {
                TLink et = portLocal.getLink();
                if(!et.isBroken() && et.getLinkType()==TLink.EXTERNAL){
                    TNode oppositeNode = et.getTargetNodeOfTrafficSentBy(this); 
                    int valorFEC = IPAddress.parseNumericAddress(oppositeNode.getIPAddress());
                    TSwitchingMatrixEntry emc = null;
                    emc = matrizConmutacion.getEntry(valorFEC, TSwitchingMatrixEntry.FEC_ENTRY);
                    if(emc == null){
                        int ipToAnnounce= IPAddress.getSubnet(oppositeNode.getIPAddress(), oppositeNode.getMask());
                        String maskToAnnounce = oppositeNode.getMask();
                        emc = crearEntradaInicialEnMatrizFEC(IPAddress.intToIp(ipToAnnounce), maskToAnnounce, oppositeNode.getIPAddress(), TSwitchingMatrixEntry.NOOP);
                        emc.setOutgoingLabel(TSwitchingMatrixEntry.LABEL_ASSIGNED); 
                    }
                }
            }
        }
        this.doneLDP=true;
    }
    
    /**
     * Este m�todo trata una petici�n de etiquetas.
     * @param paquete Petici�n de etiquetas recibida de otro nodo.
     * @param pEntrada Puerto de entrada de la petici�n de etiqueta.
     * @since 1.0
     */
    public void tratarSolicitudTLDP(TTLDPPDU paquete, int pEntrada) {
        TSwitchingMatrixEntry emc = null;
        emc = matrizConmutacion.getEntryUpStreamTLDP(paquete.getTLDPPayload().getTLDPIdentifier());
        if (emc == null) {
            emc = this.crearEntradaAPartirDeTLDP(paquete, pEntrada);
        }
        if (emc != null) {
            int etiquetaActual = emc.getOutgoingLabel();
            if (etiquetaActual == TSwitchingMatrixEntry.UNDEFINED) {
                emc.setOutgoingLabel(TSwitchingMatrixEntry.LABEL_REQUESTED);
                this.solicitarTLDP(emc);
            } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_REQUESTED) {
                // no hago nada. Se est� esperando una etiqueta.);
            } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_UNAVAILABLE) {
                enviarSolicitudNoTLDP(emc, paquete.getIPv4Header().getOriginIPAddress());
            } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_ASSIGNED) {
                //enviarSolicitudOkTLDP(emc, paquete.getIPv4Header().getOriginIPAddress());
            } else if (etiquetaActual == TSwitchingMatrixEntry.REMOVING_LABEL) {
                labelWithdrawal(emc);
            } else if (etiquetaActual > 15) {
                //enviarSolicitudOkTLDP(emc, paquete.getIPv4Header().getOriginIPAddress());
            } else {
                discardPacket(paquete);
            }
        } else {
            discardPacket(paquete);
        }
    }
    
    /**
     * Este m�todo trata un paquete TLDP de eliminaci�n de etiqueta.
     * @param paquete Eliminaci�n de etiqueta recibida.
     * @param pEntrada Puerto por el que se recibi�n la eliminaci�n de etiqueta.
     * @since 1.0
     */
    public void tratarEliminacionTLDP(TTLDPPDU paquete, int pEntrada) {
        TSwitchingMatrixEntry emc = null;
        if (paquete.getLocalOrigin() == TTLDPPDU.CAME_BY_ENTRANCE) {
            emc = matrizConmutacion.getEntryUpStreamTLDP(paquete.getTLDPPayload().getTLDPIdentifier());
        } else {
            emc = matrizConmutacion.getEntry(paquete.getTLDPPayload().getTLDPIdentifier());
        }
        if (emc == null) {
            discardPacket(paquete);
        } else {
            if (emc.getUpstreamTLDPSessionID() != TSwitchingMatrixEntry.UNDEFINED) {
                int etiquetaActual = emc.getOutgoingLabel();
                if (etiquetaActual == TSwitchingMatrixEntry.UNDEFINED) {
                    emc.setOutgoingLabel(TSwitchingMatrixEntry.REMOVING_LABEL);
                    enviarEliminacionOkTLDP(emc, pEntrada);
                    labelWithdrawal(emc);
                } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_REQUESTED) {
                    emc.setOutgoingLabel(TSwitchingMatrixEntry.REMOVING_LABEL);
                    enviarEliminacionOkTLDP(emc, pEntrada);
                    labelWithdrawal(emc);
                } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_UNAVAILABLE) {
                    emc.setOutgoingLabel(TSwitchingMatrixEntry.REMOVING_LABEL);
                    enviarEliminacionOkTLDP(emc, pEntrada);
                    labelWithdrawal(emc);
                } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_ASSIGNED) {
                    enviarEliminacionOkTLDP(emc, pEntrada);
                    matrizConmutacion.removeEntry(emc.getLabelOrFEC(), emc.getEntryType());
                } else if (etiquetaActual == TSwitchingMatrixEntry.REMOVING_LABEL) {
                    enviarEliminacionOkTLDP(emc, pEntrada);
                } else if (etiquetaActual > 15) {
                    emc.setOutgoingLabel(TSwitchingMatrixEntry.REMOVING_LABEL);
                    enviarEliminacionOkTLDP(emc, pEntrada);
                    labelWithdrawal(emc);
                } else {
                    discardPacket(paquete);
                }
            } else {
                enviarEliminacionOkTLDP(emc, pEntrada);
                matrizConmutacion.removeEntry(emc.getLabelOrFEC(), emc.getEntryType());
            }
        }
    }
    
    /**
     * Este m�todo trata un paquete TLDP de confirmaci�n de etiqueta.
     * @param paquete Confirmaci�n de etiqueta.
     * @param pEntrada Puerto por el que se ha recibido la confirmaci�n de etiquetas.
     * @since 1.0
     */
    public void tratarSolicitudOkTLDP(TTLDPPDU paquete, int pEntrada) {
        TSwitchingMatrixEntry emc = null;
        emc = matrizConmutacion.getEntry(paquete.getTLDPPayload().getTLDPIdentifier());
        if (emc == null) {
            discardPacket(paquete);
        } else {
            int etiquetaActual = emc.getOutgoingLabel();
            if (etiquetaActual == TSwitchingMatrixEntry.UNDEFINED) {
                discardPacket(paquete);
            } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_REQUESTED) {
                emc.setOutgoingLabel(paquete.getTLDPPayload().getLabel());
                if (emc.getLabelOrFEC() == TSwitchingMatrixEntry.UNDEFINED) {
                    emc.setLabelOrFEC(matrizConmutacion.getNewLabel());
                }
                TInternalLink et = (TInternalLink) ports.getPort(emc.getOutgoingPortID()).getLink();
                if (et != null) {
                    if (emc.aBackupLSPHasBeenRequested()) {
                        et.ponerLSPDeBackup();
                    } else {
                        et.setLSPUp();
                    }
                }
                enviarSolicitudOkTLDP(emc, paquete.getIPv4Header().getOriginIPAddress());
            } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_UNAVAILABLE) {
                discardPacket(paquete);
            } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_ASSIGNED) {
                discardPacket(paquete);
            } else if (etiquetaActual == TSwitchingMatrixEntry.REMOVING_LABEL) {
                discardPacket(paquete);
            } else if (etiquetaActual > 15) {
                discardPacket(paquete);
            } else {
                discardPacket(paquete);
            }
        }
    }
    
    /**
     * Este m�todo trata un paquete TLDP de denegaci�n de etiqueta.
     * @param paquete Paquete de denegaci�n de etiquetas recibido.
     * @param pEntrada Puerto por el que se ha recibido la denegaci�n de etiquetas.
     * @since 1.0
     */
    public void tratarSolicitudNoTLDP(TTLDPPDU paquete, int pEntrada) {
        TSwitchingMatrixEntry emc = null;
        emc = matrizConmutacion.getEntry(paquete.getTLDPPayload().getTLDPIdentifier());
        if (emc == null) {
            discardPacket(paquete);
        } else {
            int etiquetaActual = emc.getOutgoingLabel();
            if (etiquetaActual == TSwitchingMatrixEntry.UNDEFINED) {
                discardPacket(paquete);
            } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_REQUESTED) {
                emc.setOutgoingLabel(TSwitchingMatrixEntry.LABEL_UNAVAILABLE);
                enviarSolicitudNoTLDP(emc, paquete.getIPv4Header().getOriginIPAddress());
            } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_UNAVAILABLE) {
                discardPacket(paquete);
            } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_ASSIGNED) {
                discardPacket(paquete);
            } else if (etiquetaActual == TSwitchingMatrixEntry.REMOVING_LABEL) {
                discardPacket(paquete);
            } else if (etiquetaActual > 15) {
                discardPacket(paquete);
            } else {
                discardPacket(paquete);
            }
        }
    }
    
    /**
     * Este m�todo trata un paquete TLDP de confirmaci�n de eliminaci�n de etiqueta.
     * @param paquete Paquete de confirmaci�n e eliminaci�n de etiqueta.
     * @param pEntrada Puerto por el que se ha recibido la confirmaci�n de eliminaci�n de etiqueta.
     * @since 1.0
     */
    public void tratarEliminacionOkTLDP(TTLDPPDU paquete, int pEntrada) {
        TSwitchingMatrixEntry emc = null;
        if (paquete.getLocalOrigin() == TTLDPPDU.CAME_BY_ENTRANCE) {
            emc = matrizConmutacion.getEntryUpStreamTLDP(paquete.getTLDPPayload().getTLDPIdentifier());
        } else {
            emc = matrizConmutacion.getEntry(paquete.getTLDPPayload().getTLDPIdentifier());
        }
        if (emc == null) {
            discardPacket(paquete);
        } else {
            int etiquetaActual = emc.getOutgoingLabel();
            if (etiquetaActual == TSwitchingMatrixEntry.UNDEFINED) {
                discardPacket(paquete);
            } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_REQUESTED) {
                discardPacket(paquete);
            } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_UNAVAILABLE) {
                discardPacket(paquete);
            } else if (etiquetaActual == TSwitchingMatrixEntry.LABEL_ASSIGNED) {
                discardPacket(paquete);
            } else if (etiquetaActual == TSwitchingMatrixEntry.REMOVING_LABEL) {
                if (emc.getOutgoingLabel() != TSwitchingMatrixEntry.LABEL_ASSIGNED) {
                    TPort pSalida = ports.getPort(pEntrada);
                    TLink et = pSalida.getLink();
                    if (et.getLinkType() == TLink.INTERNAL) {
                        TInternalLink ei = (TInternalLink) et;
                        if (emc.aBackupLSPHasBeenRequested()) {
                            ei.setBackupLSPDown();
                        } else {
                            ei.quitarLSP();
                        }
                    }
                }
                matrizConmutacion.removeEntry(emc.getLabelOrFEC(), emc.getEntryType());
             } else if (etiquetaActual > 15) {
                 discardPacket(paquete);
             } else {
                 discardPacket(paquete);
             }
        }
    }
    
    /**
     * Este m�todo env�a una etiqueta al nodo que indique la entrada en la
     * matriz de conmutaci�n especificada.
     * @param emc Entrada de la matriz de conmutaci�n especificada.
     * @param IPDestino destination of this LDP reply
     * @since 1.0
     */
    public void enviarSolicitudOkTLDP(TSwitchingMatrixEntry emc, String IPDestino) {
        if (emc != null) {
            if (emc.getUpstreamTLDPSessionID() != TSwitchingMatrixEntry.UNDEFINED) {
                String IPLocal = this.getIPAddress();
                if (IPDestino != null) {
                    TTLDPPDU nuevoTLDP = null;
                    try {
                        nuevoTLDP = new TTLDPPDU(gIdent.getNextID(), IPLocal, IPDestino);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (nuevoTLDP != null) {
                        nuevoTLDP.getTLDPPayload().setTLDPMessageType(TTLDPPayload.LABEL_REQUEST_OK);
                        nuevoTLDP.getTLDPPayload().setTargetIPAddress(emc.getTailEndIPAddress());
                        nuevoTLDP.getTLDPPayload().setTLDPIdentifier(emc.getUpstreamTLDPSessionID());
                        nuevoTLDP.getTLDPPayload().setLabel(emc.getLabelOrFEC());
                        if (emc.aBackupLSPHasBeenRequested()) {
                            nuevoTLDP.setLocalTarget(TTLDPPDU.DIRECTION_BACKWARD_BACKUP);
                        } else {
                            nuevoTLDP.setLocalTarget(TTLDPPDU.DIRECTION_BACKWARD);
                        }
                        TPort pSalida = ports.getLocalPortConnectedToANodeWithIPAddress(IPDestino);
                        pSalida.putPacketOnLink(nuevoTLDP, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                        try {
                            this.generateSimulationEvent(new TSEPacketGenerated(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), TAbstractPDU.TLDP, nuevoTLDP.getSize()));
                            this.generateSimulationEvent(new TSEPacketSent(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), TAbstractPDU.TLDP));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Este m�todo env�a una denegaci�n de etiqueta al nodo que especifique la entrada
     * de la matriz de conmutaci�n correspondiente.
     * @param emc Entrada de la matriz de conmutaci�n correspondiente.
     * @param IPDestino destination of this LDP reply.
     * @since 1.0
     */
    public void enviarSolicitudNoTLDP(TSwitchingMatrixEntry emc, String IPDestino) {
        if (emc != null) {
            if (emc.getUpstreamTLDPSessionID() != TSwitchingMatrixEntry.UNDEFINED) {
                String IPLocal = this.getIPAddress();
                if (IPDestino != null) {
                    TTLDPPDU nuevoTLDP = null;
                    try {
                        nuevoTLDP = new TTLDPPDU(gIdent.getNextID(), IPLocal, IPDestino);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (nuevoTLDP != null) {
                        nuevoTLDP.getTLDPPayload().setTLDPMessageType(TTLDPPayload.LABEL_REQUEST_DENIED);
                        nuevoTLDP.getTLDPPayload().setTargetIPAddress(emc.getTailEndIPAddress());
                        nuevoTLDP.getTLDPPayload().setTLDPIdentifier(emc.getUpstreamTLDPSessionID());
                        nuevoTLDP.getTLDPPayload().setLabel(TSwitchingMatrixEntry.UNDEFINED);
                        if (emc.aBackupLSPHasBeenRequested()) {
                            nuevoTLDP.setLocalTarget(TTLDPPDU.DIRECTION_BACKWARD_BACKUP);
                        } else {
                            nuevoTLDP.setLocalTarget(TTLDPPDU.DIRECTION_BACKWARD);
                        }
                        TPort pSalida = ports.getLocalPortConnectedToANodeWithIPAddress(IPDestino);
                        pSalida.putPacketOnLink(nuevoTLDP, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                        try {
                            this.generateSimulationEvent(new TSEPacketGenerated(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), TAbstractPDU.TLDP, nuevoTLDP.getSize()));
                            this.generateSimulationEvent(new TSEPacketSent(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), TAbstractPDU.TLDP));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Este m�todo env�a una confirmaci�n de eliminaci�n de etiqueta al nodo que
     * especifique la correspondiente entrada en la matriz de conmutaci�n.
     * @since 1.0
     * @param puerto Puierto por el que se debe enviar la confirmaci�n de eliminaci�n.
     * @param emc Entrada de la matriz de conmutaci�n especificada.
     */
    public void enviarEliminacionOkTLDP(TSwitchingMatrixEntry emc, int puerto) {
        if (emc != null) {
            String IPLocal = this.getIPAddress();
            String IPDestino = ports.getIPOfNodeLinkedTo(puerto);
            if (IPDestino != null) {
                TTLDPPDU nuevoTLDP = null;
                try {
                    nuevoTLDP = new TTLDPPDU(gIdent.getNextID(), IPLocal, IPDestino);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (nuevoTLDP != null) {
                    nuevoTLDP.getTLDPPayload().setTLDPMessageType(TTLDPPayload.LABEL_REVOMAL_REQUEST_OK);
                    nuevoTLDP.getTLDPPayload().setTargetIPAddress(emc.getTailEndIPAddress());
                    nuevoTLDP.getTLDPPayload().setLabel(TSwitchingMatrixEntry.UNDEFINED);
                    if (emc.getOutgoingPortID() == puerto) {
                        nuevoTLDP.getTLDPPayload().setTLDPIdentifier(emc.getLocalTLDPSessionID());
                        nuevoTLDP.setLocalTarget(TTLDPPDU.DIRECTION_FORWARD);
                    } 
                    TPort pSalida = ports.getPort(puerto);
                    pSalida.putPacketOnLink(nuevoTLDP, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                    try {
                        this.generateSimulationEvent(new TSEPacketGenerated(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), TAbstractPDU.TLDP, nuevoTLDP.getSize()));
                        this.generateSimulationEvent(new TSEPacketSent(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), TAbstractPDU.TLDP));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    /**
     * Este m�todo solicita una etiqueta al nodo que se especifica en la entrada de la
     * matriz de conmutaci�n correspondiente.
     * @param emc Entrada en la matriz de conmutaci�n especificada.
     * @since 1.0
     */
    public void solicitarTLDP(TSwitchingMatrixEntry emc) {
        String IPLocal = this.getIPAddress();
        String IPDestinoFinal = emc.getTailEndIPAddress();
        if (emc.getOutgoingLabel() != TSwitchingMatrixEntry.LABEL_ASSIGNED) {
            String IPSalto = topology.obtenerIPSalto(IPLocal, IPDestinoFinal);
            if (IPSalto != null) {
                TTLDPPDU paqueteTLDP = null;
                try {
                    paqueteTLDP = new TTLDPPDU(gIdent.getNextID(), IPLocal, IPSalto);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (paqueteTLDP != null) {
                    paqueteTLDP.getTLDPPayload().setTargetIPAddress(IPDestinoFinal);
                    paqueteTLDP.getTLDPPayload().setTLDPMessageType(TTLDPPayload.LABEL_REQUEST);
                    paqueteTLDP.getTLDPPayload().setTLDPIdentifier(emc.getLocalTLDPSessionID());
                    if (emc.aBackupLSPHasBeenRequested()) {
                        paqueteTLDP.setLSPType(true);
                    } else {
                        paqueteTLDP.setLSPType(false);
                    }
                    paqueteTLDP.setLocalTarget(TTLDPPDU.DIRECTION_FORWARD);
                    TPort pSalida = ports.getLocalPortConnectedToANodeWithIPAddress(IPSalto);
                    if (pSalida != null) {
                        pSalida.putPacketOnLink(paqueteTLDP, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                        try {
                            this.generateSimulationEvent(new TSEPacketGenerated(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), TAbstractPDU.TLDP, paqueteTLDP.getSize()));
                            this.generateSimulationEvent(new TSEPacketSent(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), TAbstractPDU.TLDP));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Este m�todo solicita una etiqueta al nodo que se especifica en la entrada de la
     * matriz de conmutaci�n correspondiente.
     * @param emc Entrada en la matriz de conmutaci�n especificada.
     * @param subnet
     * @param mask
     * @since 1.0
     */
    public void sendLDP(TSwitchingMatrixEntry emc, String subnet, String mask) {
        int j;
        for(j=0;j<this.ports.getNumberOfPorts();j++){
            TPort portToAnnounce = ports.getPort(j);
            if (portToAnnounce != null && !portToAnnounce.isAvailable()) {
                TLink eti = portToAnnounce.getLink();
                if(!eti.isBroken() && eti.getLinkType()==TLink.INTERNAL){
                    TTLDPPDU paqueteTLDP = null;
                    try {
                        paqueteTLDP = new TTLDPPDU(gIdent.getNextID(), this.getIPAddress(), "224.0.0.2");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (paqueteTLDP != null) {
                        paqueteTLDP.getTLDPPayload().setTargetIPAddress(subnet);
                        paqueteTLDP.getTLDPPayload().setTargetMask(mask);
                        if(this.PHP)
                            paqueteTLDP.getTLDPPayload().setLabel(3);
                        else
                            paqueteTLDP.getTLDPPayload().setLabel(emc.getLabelOrFEC());
                        paqueteTLDP.getTLDPPayload().setTLDPMessageType(TTLDPPayload.LABEL_REQUEST);
                        paqueteTLDP.getTLDPPayload().setTLDPIdentifier(emc.getLocalTLDPSessionID());
                        if (emc.aBackupLSPHasBeenRequested()) {
                            paqueteTLDP.setLSPType(true);
                        } else {
                            paqueteTLDP.setLSPType(false);
                        }
                        paqueteTLDP.setLocalTarget(TTLDPPDU.DIRECTION_FORWARD);
                        portToAnnounce.putPacketOnLink(paqueteTLDP, portToAnnounce.getLink().getTargetNodeIDOfTrafficSentBy(this));
                        try {
                            this.generateSimulationEvent(new TSEPacketGenerated(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), TAbstractPDU.TLDP, paqueteTLDP.getSize()));
                            this.generateSimulationEvent(new TSEPacketSent(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), TAbstractPDU.TLDP));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Este m�todo env�a una eliminaci�n de etiqueta al nodo especificado por le
     * entrada de la matriz de conmutaci�n correspondiente.
     * @since 1.0
     * @param emc Entrada en la matriz de conmutaci�n especificada.
     */
    public void labelWithdrawal(TSwitchingMatrixEntry emc) {
        if (emc != null && this.isLDP()) {
            emc.setOutgoingLabel(TSwitchingMatrixEntry.REMOVING_LABEL);
            String IPLocal = this.getIPAddress();
            int i;
            for(i=0;i<this.ports.getNumberOfPorts();i++)
            {
                TPort portToAnnounce = ports.getPort(i);
                if (portToAnnounce != null && !portToAnnounce.isAvailable()) {
                    TLink et = portToAnnounce.getLink();
                    if(!et.isBroken() && et.getLinkType()==TLink.INTERNAL){
                        TTLDPPDU paqueteTLDP = null;
                        try {
                            paqueteTLDP = new TTLDPPDU(gIdent.getNextID(), IPLocal, "224.0.0.2");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (paqueteTLDP != null) {
                            paqueteTLDP.getTLDPPayload().setTLDPMessageType(TTLDPPayload.LABEL_REMOVAL_REQUEST);
                            paqueteTLDP.getTLDPPayload().setTLDPIdentifier(emc.getUpstreamTLDPSessionID());
                            if (emc.aBackupLSPHasBeenRequested()) {
                                paqueteTLDP.setLocalTarget(TTLDPPDU.DIRECTION_BACKWARD_BACKUP);
                            } else {
                                paqueteTLDP.setLocalTarget(TTLDPPDU.DIRECTION_BACKWARD);
                            }
                            portToAnnounce.putPacketOnLink(paqueteTLDP, portToAnnounce.getLink().getTargetNodeIDOfTrafficSentBy(this));
                            try {
                                this.generateSimulationEvent(new TSEPacketGenerated(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), TAbstractPDU.TLDP, paqueteTLDP.getSize()));
                                this.generateSimulationEvent(new TSEPacketSent(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), TAbstractPDU.TLDP));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Este m�todo reenv�a todas las peticiones pendientes de contestaci�n de una
     * entrada de la matriz de conmutaci�n.
     * @param emc Entrada de la matriz de conmutaci�n especificada.
     * @since 1.0
     */
    public void solicitarTLDPTrasTimeout(TSwitchingMatrixEntry emc) {
        if (emc != null) {
            String IPLocal = this.getIPAddress();
            String IPDestinoFinal = emc.getTailEndIPAddress();
            String IPSalto = ports.getIPOfNodeLinkedTo(emc.getOutgoingPortID());
            if (IPSalto != null) {
                TTLDPPDU paqueteTLDP = null;
                try {
                    paqueteTLDP = new TTLDPPDU(gIdent.getNextID(), IPLocal, IPSalto);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (paqueteTLDP != null) {
                    paqueteTLDP.getTLDPPayload().setTargetIPAddress(IPDestinoFinal);
                    paqueteTLDP.getTLDPPayload().setTLDPMessageType(TTLDPPayload.LABEL_REQUEST);
                    paqueteTLDP.getTLDPPayload().setTLDPIdentifier(emc.getLocalTLDPSessionID());
                    if (emc.aBackupLSPHasBeenRequested()) {
                        paqueteTLDP.setLSPType(true);
                    } else {
                        paqueteTLDP.setLSPType(false);
                    }
                    paqueteTLDP.setLocalTarget(TTLDPPDU.DIRECTION_FORWARD);
                    TPort pSalida = ports.getPort(emc.getOutgoingPortID());
                    if (pSalida != null) {
                        pSalida.putPacketOnLink(paqueteTLDP, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                        try {
                            this.generateSimulationEvent(new TSEPacketGenerated(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), TAbstractPDU.TLDP, paqueteTLDP.getSize()));
                            this.generateSimulationEvent(new TSEPacketSent(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), TAbstractPDU.TLDP));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Este m�todo reenv�a todas las eliminaciones de etiquetas pendientes de una
     * entrada de la matriz de conmutaci�n.
     * @since 1.0
     * @param emc Entrada de la matriz de conmutaci�n especificada.
     */
    public void labelWithdrawalAfterTimeout(TSwitchingMatrixEntry emc){
        labelWithdrawal(emc);
    }
    
    /**
     * Este m�todo decrementa los contadores de retransmisi�n existentes para este nodo.
     * This method decrements counters existing transmission for this node.
     * @since 1.0
     */
    public void decrementarContadores() {
        TSwitchingMatrixEntry emc = null;
        this.matrizConmutacion.getMonitor().lock();
        Iterator it = this.matrizConmutacion.getEntriesIterator();
        while (it.hasNext()) {
            emc = (TSwitchingMatrixEntry) it.next();
            if (emc != null) {
                emc.decreaseTimeOut(this.obtenerDuracionTic());
                if (emc.getOutgoingLabel() == TSwitchingMatrixEntry.LABEL_REQUESTED) {
                    if (emc.shouldRetryExpiredTLDPRequest() && this.isLDP()) {
                        emc.resetTimeOut();
                        emc.decreaseAttempts();
                        solicitarTLDPTrasTimeout(emc);
                    }
                } else if (emc.getOutgoingLabel() == TSwitchingMatrixEntry.REMOVING_LABEL) {
                    if (emc.shouldRetryExpiredTLDPRequest() && this.isLDP()) {
                        emc.resetTimeOut();
                        emc.decreaseAttempts();
                        labelWithdrawalAfterTimeout(emc);
                    } else {
                        if (!emc.areThereAvailableAttempts()) {
                            it.remove();
                        }
                    }
                } else {
                    emc.resetTimeOut();
                    emc.resetAttempts();
                }
            }
        }
        this.matrizConmutacion.getMonitor().unLock();
    }
    
    /**
     * Este m�todo crea una nueva entrada en la matriz de conmutaci�n con los datos de
     * un paquete TLDP entrante.
     * @param paqueteSolicitud Paquete TLDP entrante, de solicitud de etiqueta.
     * @param pEntrada Puerto de entrada del paquete TLDP.
     * @return La entrada de la matriz de conmutaci�n, ya creada, insertada e inicializada.
     * @since 1.0
     */
    public TSwitchingMatrixEntry crearEntradaAPartirDeTLDP(TTLDPPDU paqueteSolicitud, int pEntrada) {
        String targetIP = paqueteSolicitud.getTLDPPayload().getTargetIPAddress();
        String targetMask = paqueteSolicitud.getTLDPPayload().getTargetMask();
        int IdTLDPAntecesor = paqueteSolicitud.getTLDPPayload().getTLDPIdentifier();
        TSwitchingMatrixEntry emc = null;
        emc = this.crearEntradaInicialEnMatrizFEC(targetIP, targetMask, paqueteSolicitud.getIPv4Header().getOriginIPAddress(), TSwitchingMatrixEntry.PUSH_LABEL);
        emc.setUpstreamTLDPSessionID(IdTLDPAntecesor);
        emc.setEntryIsForBackupLSP(paqueteSolicitud.getLSPType());
        emc.setOutgoingLabel(paqueteSolicitud.getTLDPPayload().getLabel());
        try {
            emc.setLocalTLDPSessionID(this.topology.getgIdentLDP().getNew());
        } catch (Exception e) {
            e.printStackTrace();
        }
        TInternalLink et = (TInternalLink) ports.getPort(emc.getOutgoingPortID()).getLink();
        if (et != null) {
            if (emc.aBackupLSPHasBeenRequested()) {
                et.ponerLSPDeBackup();
            } else {
                et.setLSPUp();
            }
        }
        return emc;
    }
    
    /**
     * Este m�todo crea una nueva entrada en la matriz de conmutaci�n bas�ndose en un
     * paquete IPv4 recibido.
     * @param destinationIP The IP or subnet of this entry.
     * @param mask The mask for the destination ip.
     * @param nextHopIP IP of the next hop
     * @param operation The operation to do with this table entry.
     * @return La entrada de la matriz de conmutaci�n, creada, insertada e inicializada.
     * @since 1.0
     */
    public TSwitchingMatrixEntry crearEntradaInicialEnMatrizFEC(String destinationIP, String mask, String nextHopIP, int operation) {
        TSwitchingMatrixEntry emc = null;
        String IPSalida = nextHopIP;
        if (IPSalida != null) {
            TPort puertoSalida = ports.getLocalPortConnectedToANodeWithIPAddress(IPSalida);
            emc = new TSwitchingMatrixEntry();
            emc.setUpstreamTLDPSessionID(TSwitchingMatrixEntry.UNDEFINED);
            emc.setMask(IPAddress.parseNumericAddress(mask));
            emc.setNextHopIP(nextHopIP);
            emc.setOutgoingLabel(TSwitchingMatrixEntry.UNDEFINED);
            emc.setLabelOrFEC(IPAddress.parseNumericAddress(destinationIP));
            emc.setEntryIsForBackupLSP(false);
            if (puertoSalida != null) {
                emc.setOutgoingPortID(puertoSalida.getPortID());
            } else {
                emc.setOutgoingPortID(TSwitchingMatrixEntry.UNDEFINED);
            }
            emc.setEntryType(TSwitchingMatrixEntry.FEC_ENTRY);
            emc.setLabelStackOperation(operation);
//            if (puertoEntrada != null) {
//                if(pEntrada == 0)
//                    enlaceOrigen = TLink.EXTERNAL;
//                else
//                    enlaceOrigen = puertoEntrada.getLink().getLinkType();
//            }
//            if ((enlaceOrigen == TLink.EXTERNAL) && (enlaceDestino == TLink.EXTERNAL)) {
//                emc.setEntryType(TSwitchingMatrixEntry.FEC_ENTRY);
//                emc.setLabelStackOperation(TSwitchingMatrixEntry.NOOP);
//            } else if ((enlaceOrigen == TLink.EXTERNAL) && (enlaceDestino == TLink.INTERNAL)) {
//                emc.setEntryType(TSwitchingMatrixEntry.FEC_ENTRY);
//                emc.setLabelStackOperation(TSwitchingMatrixEntry.PUSH_LABEL);
//            } else if ((enlaceOrigen == TLink.INTERNAL) && (enlaceDestino == TLink.EXTERNAL)) {
//                emc.setEntryType(TSwitchingMatrixEntry.LABEL_ENTRY);
//                emc.setLabelStackOperation(TSwitchingMatrixEntry.POP_LABEL);
//                emc.setLabelOrFEC(matrizConmutacion.getNewLabel());
//                emc.setOutgoingLabel(TSwitchingMatrixEntry.LABEL_ASSIGNED);
//            } else if ((enlaceOrigen == TLink.INTERNAL) && (enlaceDestino == TLink.INTERNAL)) {
//                // No es posible
//            }
            matrizConmutacion.addEntry(emc);
        }
        return emc;
    }
    
    /**
     * Este m�todo crea una nueva entrada en la matriz de conmutaci�n bas�ndose en un
     * paquete MPLS recibido.
     * @param operation The operation to do with this table entry.
     * @param labelIN label in for this entry
     * @param pExit exit port for this entry
     * @return La entrada de la matriz de conmutaci�n, creada, insertada e inicializada.
     * @since 1.0
     */
    public TSwitchingMatrixEntry crearEntradaInicialEnMatrizLABEL(int labelIN, int pExit, int operation) {
        TSwitchingMatrixEntry emc = null;
        TPort puertoSalida = ports.getPort(pExit);
        emc = new TSwitchingMatrixEntry();
        emc.setUpstreamTLDPSessionID(TSwitchingMatrixEntry.UNDEFINED);
        emc.setOutgoingLabel(TSwitchingMatrixEntry.UNDEFINED);
        emc.setEntryIsForBackupLSP(false);
        emc.setLabelOrFEC(labelIN);
        if (puertoSalida != null) {
            emc.setOutgoingPortID(puertoSalida.getPortID());
        } else {
            emc.setOutgoingPortID(TSwitchingMatrixEntry.UNDEFINED);
        }
        emc.setEntryType(TSwitchingMatrixEntry.LABEL_ENTRY);
        emc.setLabelStackOperation(operation);
//        if (puertoEntrada != null) {
//            enlaceOrigen = puertoEntrada.getLink().getLinkType();
//        }
//        if ((enlaceOrigen == TLink.EXTERNAL) && (enlaceDestino == TLink.EXTERNAL)) {
//            emc.setEntryType(TSwitchingMatrixEntry.LABEL_ENTRY);
//            emc.setLabelStackOperation(TSwitchingMatrixEntry.NOOP);
//        } else if ((enlaceOrigen == TLink.EXTERNAL) && (enlaceDestino == TLink.INTERNAL)) {
//            emc.setEntryType(TSwitchingMatrixEntry.LABEL_ENTRY);
//            emc.setLabelStackOperation(TSwitchingMatrixEntry.PUSH_LABEL);
//        } else if ((enlaceOrigen == TLink.INTERNAL) && (enlaceDestino == TLink.EXTERNAL)) {
//            emc.setEntryType(TSwitchingMatrixEntry.LABEL_ENTRY);
//            emc.setLabelStackOperation(TSwitchingMatrixEntry.POP_LABEL);
//            emc.setOutgoingLabel(TSwitchingMatrixEntry.LABEL_ASSIGNED);
//        } else if ((enlaceOrigen == TLink.INTERNAL) && (enlaceDestino == TLink.INTERNAL)) {
//            emc.setEntryType(TSwitchingMatrixEntry.LABEL_ENTRY);
//            emc.setLabelStackOperation(TSwitchingMatrixEntry.SWAP_LABEL);
//        }
        matrizConmutacion.addEntry(emc);
        
        return emc;
    }
    
    /**
     * Este m�todo toma un paquete IPv4 y la entrada de la matriz de conmutaci�n
     * asociada al mismo y crea un paquete MPLS etiquetado correctamente que contiene
     * dicho paquete IPv4 listo para ser transmitido hacia el interior del dominio.
     * @param paqueteIPv4 Paquete IPv4 que se debe etiquetar.
     * @param emc Entrada de la matriz de conmutaci�n asociada al paquete IPv4 que se desea
     * etiquetar.
     * @return El paquete IPv4 de entrada, convertido en un paquete MPLS correctamente
     * etiquetado.
     * @since 1.0
     */
    public TMPLSPDU crearPaqueteMPLS(TAbstractPDU paquete, TSwitchingMatrixEntry emc) {
        TMPLSPDU paqueteMPLS = null;
        try {
            paqueteMPLS = new TMPLSPDU(gIdent.getNextID(), paquete.getIPv4Header().getOriginIPAddress(), paquete.getIPv4Header().getTargetIPv4Address(), paquete);
        } catch (EIDGeneratorOverflow e) {
            e.printStackTrace();
        }
        paqueteMPLS.setHeader(paquete.getIPv4Header());
        //paqueteMPLS.setTCPPayload(paqueteIPv4.getTCPPayload());
        TMPLSLabel empls = new TMPLSLabel();
        empls.setBoS(true);
        empls.setEXP(0);
        empls.setLabel(emc.getOutgoingLabel());
        if(propagateTTL)
            empls.setTTL(paquete.getIPv4Header().getTTL()-1);
        else
            empls.setTTL(255); 
        paqueteMPLS.getLabelStack().pushTop(empls);
        paquete = null;
        try {
            this.generateSimulationEvent(new TSEPacketGenerated(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), paqueteMPLS.getSubtype(), paqueteMPLS.getSize()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return paqueteMPLS;
    }
    
    /**
     * Este m�todo toma como par�metro un paquete MPLS y su entrada en la matriz de
     * conmutaci�n asociada. Extrae del paquete MPLS el paquete IP correspondiente y
     * actualiza sus valores correctamente.
     * @param paquete Paquete MPLS cuyo contenido de nivel IPv4 se desea extraer.
     * @param icmpType type of the icmp packet.
     * @param icmpCode code of the icmp packet.
     * @return TICMPPDU that has been created.
     * @since 2.0
     */
    public TICMPPDU replyICMP(TAbstractPDU paquete, int icmpType, int icmpCode) {
        TICMPPDU paqueteICMP = null;
        try {
            paqueteICMP= new TICMPPDU(gIdent.getNextID(), this.getIPAddress(), paquete.getIPv4Header().getOriginIPAddress(), icmpType, icmpCode);
            paqueteICMP.setSubtype(TAbstractPDU.ICMP);
            this.generateSimulationEvent(new TSEPacketGenerated(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), paqueteICMP.getSubtype(), paqueteICMP.getSize()));
        } catch (EIDGeneratorOverflow e) {
            e.printStackTrace();
        }
        
        paquete = null;
        return paqueteICMP;
    }
    
    /**
     * Este m�todo comprueba si un paquete recibido es un paquete del interior del
     * dominio MPLS o es un paquete externo al mismo.
     * @param paquete Paquete que ha llegado al nodo.
     * @param pEntrada Puerto por el que ha llegado el paquete al nodo.
     * @return true, si el paquete es exterior al dominio MPLS. false en caso contrario.
     * @since 1.0
     */
    public boolean esUnPaqueteExterno(TAbstractPDU paquete, int pEntrada) {
        if (paquete.getType() == TAbstractPDU.IPV4)
            return true;
        TPort pe = ports.getPort(pEntrada);
        if (pe.getLink().getLinkType() == TLink.EXTERNAL)
            return true;
        return false;
    }
    
    /**
     * Este m�todo contabiliza un paquete recibido o conmutado en las estad�sticas del
     * nodo.
     * @param paquete paquete que se desa contabilizar.
     * @param deEntrada TRUE, si el paquete se ha recibido en el nodo. FALSE so el paquete ha salido del
     * nodo.
     * @since 1.0
     */
    public void contabilizarPaquete(TAbstractPDU paquete, boolean deEntrada) {
    }
    
    /**
     * Este m�todo descarta un paquete en el nodo y refleja dicho descarte en las
     * estad�sticas del nodo.
     * @param paquete Paquete que se quiere descartar.
     * @since 1.0
     */
    public void discardPacket(TAbstractPDU paquete) {
        try {
            this.generateSimulationEvent(new TSEPacketDiscarded(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), paquete.getSubtype()));
            this.estadisticas.addStatsEntry(paquete, TStats.DISCARD);
        } catch (Exception e) {
            e.printStackTrace();
        }
        paquete = null;
    }
    
    /**
     * Este m�todo toma como parametro un paquete, supuestamente sin etiquetar, y lo
     * clasifica. Esto significa que determina el FEC_ENTRY al que pertenece el paquete.
 Este valor se calcula como el c�digo HASH practicado a la concatenaci�n de la IP
 de origen y la IP de destino. En la pr�ctica esto significa que paquetes con el
 mismo origen y con el mismo destino pertenecer�n al mismo FEC_ENTRY.
     * @param paquete El paquete que se desea clasificar.
     * @return El FEC_ENTRY al que pertenece el paquete pasado por par�metros.
     * @since 1.0
     */
    public int clasificarPaquete(TAbstractPDU paquete) {
        //String IPOrigen = paquete.getIPv4Header().getOriginIPAddress();
        String IPDestino = paquete.getIPv4Header().getTargetIPv4Address();
        String cadenaFEC = IPDestino;
        return cadenaFEC.hashCode();
    }
    
    /**
     * This method return a hash of the IP reveived.
     * This hash is used in the Switching matrix.
     * @param ip The IP to classify.
     * @return The FEC_ENTRY that correspond to the IP
     * @since 2.0
     */
    public int classifyIP(String ip) {
        return ip.hashCode();
    }
    
    /**
     * Este m�todo permite el acceso al conjunto de ports del nodo.
     * @return El conjunto de ports del nodo.
     * @since 1.0
     */
    public TPortSet getPorts() {
        return this.ports;
    }
    
    /**
     * Este m�todo calcula si el nodo tiene ports libres o no.
     * @return true, si el nodo tiene ports libres. false en caso contrario.
     * @since 1.0
     */
    public boolean hasAvailablePorts() {
        return this.ports.hasAvailablePorts();
    }
    
    /**
     * Este m�todo calcula el peso del nodo. Se utilizar� para calcular rutas con costo
     * menor. En el nodo LER el pero ser� siempre nulo (cero).
     * @return 0. El peso siempre ser� nulo en un LER.
     * @since 1.0
     */
    public long getRoutingWeight() {
        long peso = 0;
        long pesoC = (long) (this.ports.getCongestionLevel() * (0.7));
        long pesoMC = (long) ((10*this.matrizConmutacion.getNumberOfEntries())* (0.3));
        peso = pesoC + pesoMC;
        return peso;
    }
    
    /**
     * Este m�todo comprueba si la isntancia actual es el LER de salida del dominio
     * MPLS para una IP dada.
     * @param ip IP de destino del tr�fico, para la cual queremos averiguar si el LER es nodo de
     * salida.
     * @return true, si el LER es de salida del dominio para tr�fico dirigido a esa IP. false
     * en caso contrario.
     * @since 1.0
     */
    public boolean soyLERDeSalida(String ip) {
        TPort p = ports.getLocalPortConnectedToANodeWithIPAddress(ip);
        if (p != null)
            if (p.getLink().getLinkType() == TLink.EXTERNAL)
                return true;
        return false;
    }
    
    /**
     * Este m�todo permite el acceso a la matriz de conmutaci�n de LER.
     * @return La matriz de conmutaci�n del LER.
     * @since 1.0
     */
    public TSwitchingMatrix obtenerMatrizConmutacion() {
        return matrizConmutacion;
    }
    
    /**
     * Este m�todo comprueba que la configuraci�n de LER sea la correcta.
     * @return true, si el LER est� bien configurado. false en caso contrario.
     * @since 1.0
     */
    public boolean isWellConfigured() {
        return this.wellConfigured;
    }
    
    /**
     * Este m�todo comprueba que una cierta configuraci�n es v�lida.
     * @param t Topolog�a a la que pertenece el LER.
     * @param recfg true si se trata de una reconfiguraci�n. false en caso contrario.
     * @return CORRECTA, si la configuraci�n es correcta. Un c�digo de error en caso contrario.
     * @since 1.0
     */
    public int validateConfig(TTopology t, boolean recfg) {
        this.setWellConfigured(false);
        if (this.getName().equals(""))
            return TLERNode.SIN_NOMBRE;
        if (this.getIPAddress().isEmpty() && recfg)
            return TLERNode.EMPTY_IP;
        boolean soloEspacios = true;
        for (int i=0; i < this.getName().length(); i++){
            if (this.getName().charAt(i) != ' ')
                soloEspacios = false;
        }
        if (soloEspacios)
            return TLERNode.SOLO_ESPACIOS;
        if (!recfg) {
            TNode tp = t.setFirstNodeNamed(this.getName());
            if (tp != null)
                return TLERNode.NOMBRE_YA_EXISTE;
        } else {
            TNode tp = t.setFirstNodeNamed(this.getName());
            if (tp != null) {
                if (this.topology.thereIsMoreThanANodeNamed(this.getName())) {
                    return TLERNode.NOMBRE_YA_EXISTE;
                }
            }
        }
        this.setWellConfigured(true);
        return TLERNode.CORRECTA;
    }
    
    /**
     * Este m�todo toma un codigo de error y genera un mensaje textual del mismo.
     * @param e El c�digo de error para el cual queremos una explicaci�n textual.
     * @return Cadena de texto explicando el error.
     * @since 1.0
     */
    public String getErrorMessage(int e) {
        switch (e) {
            case SIN_NOMBRE: return (java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TConfigLER.FALTA_NOMBRE"));
            case NOMBRE_YA_EXISTE: return (java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TConfigLER.NOMBRE_REPETIDO"));
            case SOLO_ESPACIOS: return (java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TNodoLER.NombreNoSoloEspacios"));
            case EMPTY_IP: return "IP Address of the node is not present";
        }
        return ("");
    }
    
    /**
     * Este m�todo forma una cadena de texto que representa al LER y toda su
     * configuraci�n. Sirve para almacenar el LER en disco.
     * @return Una cadena de texto que representa un a este LER.
     * @since 1.0
     */
    @Override
    public String marshall() {
        String cadena = "#LER#";
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
        cadena += this.potenciaEnMb;
        cadena += "#";
        cadena += this.getPorts().getBufferSizeInMB();
        cadena += "#";
        cadena += this.propagateTTL;
        cadena += "#";
        cadena += this.PHP;
        cadena += "#";
        return cadena;
    }
    
    /**
     * Este m�todo toma como par�metro una cadena de texto que debe pertencer a un LER
     * serializado y configura esta instancia con los valores de dicha caddena.
     * @param elemento LER serializado.
     * @return true, si no ha habido errores y la instancia actual est� bien configurada. false
     * en caso contrario.
     * @since 1.0
     */
    @Override
    public boolean unMarshall(String elemento) {
        String valores[] = elemento.split("#");
        if (valores.length != 13) {
            return false;
        }
        this.setID(Integer.parseInt(valores[2]));
        this.setName(valores[3]);
        this.setIPAddress(valores[4]);
        this.setStatus(0);
        this.setShowName(Boolean.parseBoolean(valores[5]));
        this.setGenerateStats(Boolean.valueOf(valores[6]));
        int posX = Integer.parseInt(valores[7]);
        int posY = Integer.valueOf(valores[8]);
        this.setPosition(new Point(posX+24, posY+24));
        this.potenciaEnMb = Integer.valueOf(valores[9]);
        this.getPorts().setBufferSizeInMB(Integer.valueOf(valores[10]));
        this.propagateTTL = Boolean.parseBoolean(valores[11]);
        this.PHP = Boolean.parseBoolean(valores[12]);
        return true;
    }
    
    /**
     * This method will add a new entry into the switching matrix.
     * @param tableEntry information about the entry to add.
     * @return true, if there has been no errors and the current instance is configured correctly.
     * false otherwise.
     * @since 2.0
     */
    @Override
    public boolean addTableEntry(String tableEntry) {
        String valores[] = tableEntry.split("#");
        TSwitchingMatrixEntry emc;
        if(valores[1].equalsIgnoreCase("PUSH")){
            emc = this.crearEntradaInicialEnMatrizFEC(valores[2], valores[3], valores[4], TSwitchingMatrixEntry.PUSH_LABEL);
            emc.setOutgoingLabel(Integer.parseInt(valores[5]));
            return true;
        }
        if(valores[1].equalsIgnoreCase("SWAP")){
            emc = this.crearEntradaInicialEnMatrizLABEL(Integer.parseInt(valores[2]), Integer.parseInt(valores[4]), TSwitchingMatrixEntry.SWAP_LABEL);
            emc.setOutgoingLabel(Integer.parseInt(valores[3]));
            return true;
        }
        if(valores[1].equalsIgnoreCase("POP")){
            emc = this.crearEntradaInicialEnMatrizLABEL(Integer.parseInt(valores[2]), Integer.parseInt(valores[3]), TSwitchingMatrixEntry.POP_LABEL);
            emc.setOutgoingLabel(TSwitchingMatrixEntry.LABEL_ASSIGNED);
            return true;
        }
        if(valores[1].equalsIgnoreCase("ROUTE")){
            emc = this.crearEntradaInicialEnMatrizFEC(valores[2], valores[3], valores[4], TSwitchingMatrixEntry.NOOP);
            emc.setOutgoingLabel(TSwitchingMatrixEntry.LABEL_ASSIGNED);
            return true;
        }
        
        return false;
    }
    
    /**
     * Este m�todo forma una cadena de texto que representa al LER y toda su
     * configuraci�n. Sirve para almacenar el LER en disco.
     * @return Una cadena de texto que representa un a este LER.
     * @since 1.0
     */
    @Override
    public String saveTableEntry() {
        return this.matrizConmutacion.save();
    }
    
    /**
     * Este m�todo permite acceder directamente a las estadisticas del nodo.
     * @return Las estad�sticas del nodo.
     * @since 1.0
     */
    public TStats getStats() {
        return estadisticas;
    }
    
    /**
     * Este m�todo permite establecer el n�mero de ports que tendr� el nodo.
     * @param num N�mero de ports deseado para el nodo. Como mucho, 8 ports.
     * @since 1.0
     */
    public synchronized void setPorts(int num) {
        ports = new TFIFOPortSet(num, this);
    }
    
    /**
     * @return the propagateTTL
     */
    public boolean isPropagateTTL() {
        return propagateTTL;
    }

    /**
     * @param propagateTTL the propagateTTL to set
     */
    public void setPropagateTTL(boolean propagateTTL) {
        this.propagateTTL = propagateTTL;
    }
    
    /**
     * @return Say if the LER wants Penultimate Hop Popping or not.
     * @since 2.0
     */
    public boolean isPHP() {
        return PHP;
    }

    /**
     * @param PHP true is the router wants Penultimate Hop Popping, false if not.
     * @since 2.0
     */
    public void setPHP(boolean PHP) {
        this.PHP = PHP;
    }
    
    /**
     * Este m�todo no hace nada en un LSR. En un nodo activoPermitir� solicitar
     * a un nodo activo la retransmisi�n de un paquete.
     * @param paquete Paquete cuya retransmisi�n se est� solicitando.
     * @param pSalida Puerto por el que se enviar� la solicitud.
     * @since 1.0
     */
    public void runGoSPDUStoreAndRetransmitProtocol(TMPLSPDU paquete, int pSalida) {
    }
    
    /**
     * Esta constante indica que la configuraci�n del nodo LER esta correcta, que no
     * contiene errores.
     * @since 1.0
     */
    public static final int CORRECTA = 0;
    /**
     * Esta constante indica que el nombre del nodo LER no est� definido.
     * @since 1.0
     */
    public static final int SIN_NOMBRE = 1;
    /**
     * Esta constante indica que el nombre especificado para el LER ya est� siendo
 usado por otro nodo de la topology.
     * @since 1.0
     */
    public static final int NOMBRE_YA_EXISTE = 2;
    /**
     * Esta constante indica que el nombre que se ha definido para el LER contiene s�lo
     * constantes.
     * @since 1.0
     */
    public static final int SOLO_ESPACIOS = 3;
    /**
     * IP Address is empty
     * @since 1.0
     */    
    public static final int EMPTY_IP = 4;
    
    private TSwitchingMatrix matrizConmutacion;
    private TLongIDGenerator gIdent;
    private TIDGenerator gIdentLDP;
    private int potenciaEnMb;
    private TLERStats estadisticas;
    private boolean propagateTTL;
    private boolean PHP;
    private boolean doneLDP;

}
