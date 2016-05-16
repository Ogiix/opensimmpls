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
import simMPLS.protocols.TICMPPDU;
import simMPLS.protocols.TAbstractPDU;
import simMPLS.protocols.TMPLSLabel;
import simMPLS.protocols.TMPLSPDU;
import simMPLS.protocols.TTLDPPayload;
import simMPLS.hardware.timer.TTimerEvent;
import simMPLS.hardware.timer.ITimerEventListener;
import simMPLS.hardware.ports.TFIFOPort;
import simMPLS.hardware.tldp.TSwitchingMatrix;
import simMPLS.hardware.tldp.TSwitchingMatrixEntry;
import simMPLS.hardware.ports.TFIFOPortSet;
import simMPLS.hardware.ports.TPort;
import simMPLS.hardware.ports.TPortSet;
import simMPLS.utils.TIDGenerator;
import simMPLS.utils.TLongIDGenerator;
import java.awt.*;
import java.util.*;
import org.jfree.chart.*;
import org.jfree.data.*;
import simMPLS.hardware.tldp.IPAddress;
import simMPLS.protocols.TIPv4PDU;
import simMPLS.utils.EIDGeneratorOverflow;


/**
 * Esta clase implementa un nodo LSR; un conmutador interno a un dominio MPLS.
 * @author <B>Manuel Dom�nguez Dorado</B><br><A
 * href="mailto:ingeniero@ManoloDominguez.com">ingeniero@ManoloDominguez.com</A><br><A href="http://www.ManoloDominguez.com" target="_blank">http://www.ManoloDominguez.com</A>
 * @version 1.0
 */
public class TLSRNode extends TNode implements ITimerEventListener, Runnable {
    
    /**
     * Crea una nueva instancia de TNodoLSR
     * @param identificador Identificador unico del nodo en la topolog�a.
     * @param d Direcci�n IP del nodo.
     * @param il Generador de identificadores para los eventos generados por el nodo.
     * @param t Topolog�a dentro de la cual se encuentra el nodo.
     * @since 1.0
     */
    public TLSRNode(int identificador, String d, TLongIDGenerator il, TTopology t) {
        super(identificador, d, il, t);
        this.setPorts(super.NUM_PUERTOS_LSR);
        matrizConmutacion = new TSwitchingMatrix();
        gIdent = new TLongIDGenerator();
        gIdentLDP = new TIDGenerator();
        potenciaEnMb = 512;
        estadisticas = new TLSRStats();
        rfc4950 = false;
        propagateTTL = false;
    }
    
    /**
     * Este m�todo obtiene el n�mero de nanosegundos que son necesarios para conmutar
     * un bit.
     * @return El n�mero de nanosegundos necesarios para conmutar un bit.
     * @since 1.0
     */
    public double obtenerNsPorBit() {
        long tasaEnBitsPorSegundo = (long) (this.potenciaEnMb*1048576L);
        double nsPorCadaBit = (double) ((double)1000000000.0/(long)tasaEnBitsPorSegundo);
        return nsPorCadaBit;
    }
    
    /**
     * Este m�todo calcula el n�mero de nanosegundos necesarios para conmutar un n�mero
     * determinado de octetos.
     * @param octetos N�mero de octetos que queremos conmutar.
     * @return N�mero de nanosegundos necesarios para conmutar los octetos especificados.
     * @since 1.0
     */
    public double obtenerNsUsadosTotalOctetos(int octetos) {
        double nsPorCadaBit = obtenerNsPorBit();
        long bitsOctetos = (long) ((long)octetos*(long)8);
        return (double)((double)nsPorCadaBit*(long)bitsOctetos);
    }
    
    /**
     * Este m�todo devuelve el n�mero de bits que se pueden conmutar con el n�mero de
     * nanosegundos de los que dispone actualmente el nodo.
     * @return N�mero de bits m�ximos que puede conmutar el nodo en un instante.
     * @since 1.0
     */
    public int obtenerLimiteBitsTransmitibles() {
        double nsPorCadaBit = obtenerNsPorBit();
        double maximoBits = (double) ((double)availableNs/(double)nsPorCadaBit);
        return (int) maximoBits;
    }
    
    /**
     * Este m�todo calcula el n�mero m�ximo de octetos completos que puede conmtuar el
     * nodo.
     * @return El n�mero m�ximo de octetos que puede conmutar el nodo.
     * @since 1.0
     */
    public int obtenerOctetosTransmitibles() {
        double maximoBytes = ((double)obtenerLimiteBitsTransmitibles()/(double)8.0);
        return (int) maximoBytes;
    }
    
    /**
     * Este m�todo devuelve la potencia de conmutaci�n con la que est� configurado el
     * nodo.
     * @return Potencia de conmutaci�n en Mbps.
     * @since 1.0
     */
    public int obtenerPotenciaEnMb() {
        return this.potenciaEnMb;
    }
    
    /**
     * Este m�todo establece la potencia de conmutaci�n para el nodo.
     * @param pot Potencia de conmutaci�n en Mbps deseada para el nodo.
     * @since 1.0
     */
    public void ponerPotenciaEnMb(int pot) {
        this.potenciaEnMb = pot;
    }
    
    /**
     * Este m�todo permite obtener el tamanio el buffer del nodo.
     * @return Tamanio del buffer en MB.
     * @since 1.0
     */
    public int obtenerTamanioBuffer() {
        return this.getPorts().getBufferSizeInMB();
    }
    
    /**
     * Este m�todo permite establecer el tamanio del buffer del nodo.
     * @param tb Tamanio deseado para el buffer del nodo en MB.
     * @since 1.0
     */
    public void ponerTamanioBuffer(int tb) {
        this.getPorts().setBufferSizeInMB(tb);
    }
    
    /**
     * Este m�todo reinicia los atributos del nodo hasta dejarlos como si acabasen de
     * ser creados por el Constructor.
     * @since 1.0
     */
    public void reset() {
        this.ports.reset();
        if(this.isLDP())
            matrizConmutacion.reset();
        gIdent.reset();
        gIdentLDP.reset();
        estadisticas.reset();
        estadisticas.activateStats(this.isGeneratingStats());
        this.resetStepsWithoutEmittingToZero();
    }
    
    /**
     * Este m�todo permite obtener el tipo del nodo.
     * @return TNode.LSR, indicando que se trata de un nodo LSR.
     * @since 1.0
     */
    public int getNodeType() {
        return super.LSR;
    }
    
    /**
     * Este m�todo permite obtener eventos de sincronizaci�n del reloj del simulador.
     * @param evt Evento de sincronizaci�n que env�a el reloj del simulador.
     * @since 1.0
     */
    public void receiveTimerEvent(TTimerEvent evt) {
        this.setStepDouration(evt.getStepDuration());
        this.setTimeInstant(evt.getUpperLimit());
        if (this.getPorts().isAnyPacketToSwitch()) {
            this.availableNs += evt.getStepDuration();
        } else {
            this.resetStepsWithoutEmittingToZero();
            this.availableNs = evt.getStepDuration();
        }
        this.startOperation();
    }
    
    /**
     * Este m�todo se llama cuando se inicia el hilo independiente del nodo y es en el
     * que se implementa toda la funcionalidad.
     * @since 1.0
     */
    @Override
    public void run() {
        if (this.getPorts().isArtificiallyCongested()) {
            try {
                this.generateSimulationEvent(new TSENodeCongested(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), this.getPorts().getCongestionLevel()));
            } catch (Exception e) {
                e.printStackTrace(); 
            }
        }
        comprobarElEstadoDeLasComunicaciones();
        decrementarContadores();
        conmutarPaquete();
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
     * Este m�todo se encarga de validateConfig que los enlaces que unen al nodo con sus
 adyacentes, funcionan correctamente. Y si no es asi y es necesario, env�a la
     * se�alizaci�n correspondiente para reparar la situaci�n.
     * @since 1.0
     */
    public void comprobarElEstadoDeLasComunicaciones() {
        TSwitchingMatrixEntry emc = null;
        boolean eliminar = false;
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
     * Este m�todo conmuta paquetes del buffer de entrada.
     * @since 1.0
     */
    public void conmutarPaquete() {
        boolean conmute = false;
        int puertoLeido = 0;
        TAbstractPDU paquete = null;
        int octetosQuePuedoMandar = this.obtenerOctetosTransmitibles();
        while (this.getPorts().canSwitchPacket(octetosQuePuedoMandar)) {
            conmute = true;
            paquete = this.ports.getNextPacket();
            puertoLeido = ports.getReadPort();
            if (paquete != null) {
                if (paquete.getType() == TAbstractPDU.TLDP && this.isLDP()) {
                    conmutarTLDP((TTLDPPDU) paquete, puertoLeido);
                } else if (paquete.getType() == TAbstractPDU.MPLS) {
                    conmutarMPLS((TMPLSPDU) paquete, puertoLeido);
                } else if (paquete.getType() == TAbstractPDU.GPSRP) {
                    conmutarGPSRP((TGPSRPPDU) paquete, puertoLeido);
                } else if(paquete.getType() == TAbstractPDU.IPV4 && !paquete.getIPv4Header().getTargetIPv4Address().equals(this.getIPAddress())) {
                    conmutarIPv4((TIPv4PDU) paquete, puertoLeido);
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
     * @since 1.0
     * @param paquete Paquete GPSRP a conmutar.
     * @param pEntrada Puerto por el que ha llegado el paquete.
     */
    public void conmutarGPSRP(TGPSRPPDU paquete, int pEntrada) {
        if (paquete != null) {
            int mensaje = paquete.getGPSRPPayload().getGPSRPMessageType();
            int flujo = paquete.getGPSRPPayload().getFlowID();
            int idPaquete = paquete.getGPSRPPayload().getPacketID();
            String IPDestinoFinal = paquete.getIPv4Header().getTargetIPv4Address();
            TFIFOPort pSalida = null;
            if (IPDestinoFinal.equals(this.getIPAddress())) {
                // Un LSR no entiende peticiones GPSRP, por tanto no pueder
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
                            discardPacket(paquete);
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
                            discardPacket(paquete);
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
     * Este m�todo trata un paquete TLDP que ha llegado.
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
     * Este m�todo trata un paquete MPLS que ha llegado.
     * @param paquete Paquete MPLS recibido.
     * @param pEntrada Puerto por el que se ha recibido el paquete MPLS.
     * @since 1.0
     */
    public void conmutarMPLS(TMPLSPDU paquete, int pEntrada) {
        TMPLSLabel eMPLS = null;
        TSwitchingMatrixEntry emc = null;
        boolean conEtiqueta1 = false;
        if (paquete.getLabelStack().getTop().getLabel() == 1) {
            eMPLS = paquete.getLabelStack().getTop();
            paquete.getLabelStack().popTop();
            conEtiqueta1 = true;
        }
        int valorLABEL = paquete.getLabelStack().getTop().getLabel();
        emc = matrizConmutacion.getEntry(valorLABEL, TSwitchingMatrixEntry.LABEL_ENTRY);
        if (emc == null) {
            if (conEtiqueta1) {
                paquete.getLabelStack().pushTop(eMPLS);
            }
            discardPacket(paquete);
        } else {
            int etiquetaActual = emc.getOutgoingLabel();
            if (etiquetaActual == TSwitchingMatrixEntry.UNDEFINED) {
                if(this.isLDP()){
                    emc.setOutgoingLabel(TSwitchingMatrixEntry.LABEL_REQUESTED);
                    solicitarTLDP(emc);
                }
                if (conEtiqueta1) {
                    paquete.getLabelStack().pushTop(eMPLS);
                }
                this.ports.getPort(pEntrada).reEnqueuePacket(paquete);
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
                        empls.setTTL(paquete.getLabelStack().getTop().getTTL()-1);
                        if(empls.getTTL()<1){
                            paquete.getIPv4Header().setTargetIP(paquete.getIPv4Header().getOriginIPAddress());
                            paquete.getIPv4Header().setOriginIP(this.getIPAddress());
                            paquete.getIPv4Header().setTTL(255);
                        }
                        paquete.getLabelStack().pushTop(empls);
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
                        
                    } else if (operacion == TSwitchingMatrixEntry.POP_LABEL) {
                        if(paquete.getLabelStack().getTop().getTTL()<=1){
                            TICMPPDU packetToSend = this.replyICMP(paquete, emc);
                            packetToSend.setSubtype(TAbstractPDU.ICMPTOREROUTE);
                            TPort pSalida = ports.getPort(emc.getOutgoingPortID());
                            pSalida.putPacketOnLink(packetToSend, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                        }else {
                            if(paquete.getLabelStack().getSize()<=1){
                                TAbstractPDU packetToSend = paquete.getCarriedPacket();
                                if(propagateTTL)
                                    packetToSend.getIPv4Header().setTTL(paquete.getLabelStack().getTop().getTTL()-1);
                                else if(packetToSend.getIPv4Header().getTTL()<=1){
                                    TICMPPDU packetICMPToSend = this.replyICMP(paquete, emc);
                                    packetICMPToSend.setSubtype(TAbstractPDU.ICMPTOREROUTE);
                                    TPort pSalida = ports.getPort(emc.getOutgoingPortID());
                                    pSalida.putPacketOnLink(packetICMPToSend, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                                    return;
                                }else {
                                    paquete.getIPv4Header().setTTL(paquete.getIPv4Header().getTTL()-1);
                                }
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
                    } else if (operacion == TSwitchingMatrixEntry.SWAP_LABEL) {
                        if (conEtiqueta1) {
                            paquete.getLabelStack().pushTop(eMPLS);
                        }
                        TPort pSalida;
                        if(paquete.getLabelStack().getTop().getTTL()<=1){
                            TMPLSPDU packetToSend = this.crearPaqueteMPLS(paquete, emc);
                            packetToSend.setCarriedPacket(this.replyICMP(paquete, emc));
                            packetToSend.getCarriedPacket().setSubtype(TAbstractPDU.ICMPTOREROUTE);
                            pSalida = ports.getPort(emc.getOutgoingPortID());
                            pSalida.putPacketOnLink(packetToSend, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                        }else {
                            paquete.getLabelStack().getTop().setTTL(paquete.getLabelStack().getTop().getTTL()-1);
                            paquete.getLabelStack().getTop().setLabel(emc.getOutgoingLabel());
                            pSalida = ports.getPort(emc.getOutgoingPortID());
                            pSalida.putPacketOnLink(paquete, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                        }
                        if (emc.aBackupLSPHasBeenRequested()) {
                            TInternalLink ei = (TInternalLink) pSalida.getLink();
                            ei.setLSPUp();
                            ei.setBackupLSPDown();
                            emc.setEntryIsForBackupLSP(false);
                        }
                        try {
                            this.generateSimulationEvent(new TSEPacketSwitched(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), paquete.getSubtype()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        
                    } else if (operacion == TSwitchingMatrixEntry.NOOP) {
                        TPort pSalida = ports.getPort(emc.getOutgoingPortID());
                        pSalida.putPacketOnLink(paquete, pSalida.getLink().getTargetNodeIDOfTrafficSentBy(this));
                        try {
                            this.generateSimulationEvent(new TSEPacketSwitched(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), paquete.getSubtype()));
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
     * Este m�todo solicita una etiqueta al nodo que se especifica en la entrada de la
     * matriz de conmutaci�n correspondiente.
     * @param emc Entrada en la matriz de conmutaci�n especificada.
     * @param paquete
     * @since 1.0
     */
    public void sendLDP(TSwitchingMatrixEntry emc, TTLDPPDU paquete, int entryPort) {
        int j;
        for(j=0;j<this.ports.getNumberOfPorts();j++){
            if(j!=entryPort){
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
                            paqueteTLDP.getTLDPPayload().setTargetIPAddress(paquete.getTLDPPayload().getTargetIPAddress());
                            paqueteTLDP.getTLDPPayload().setTargetMask(paquete.getTLDPPayload().getTargetMask());
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
            emc = crearEntradaAPartirDeTLDP(paquete, pEntrada);
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
                sendLDP(emc, paquete, pEntrada);
            } else if (etiquetaActual == TSwitchingMatrixEntry.REMOVING_LABEL) {
                labelWithdrawal(emc);
            } else if (etiquetaActual > 15) {
                //enviarSolicitudOkTLDP(emc, paquete.getIPv4Header().getOriginIPAddress());
                sendLDP(emc, paquete, pEntrada);
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
            } else if (etiquetaActual == TSwitchingMatrixEntry.REMOVING_LABEL) {
                enviarEliminacionOkTLDP(emc, pEntrada);
            } else if (etiquetaActual > 15) {
                emc.setOutgoingLabel(TSwitchingMatrixEntry.REMOVING_LABEL);
                enviarEliminacionOkTLDP(emc, pEntrada);
                labelWithdrawal(emc);
            } else {
                discardPacket(paquete);
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
                if(paquete.getTLDPPayload().getLabel() == 3){
                    emc.setOutgoingLabel(0);
                    emc.setLabelStackOperation(TSwitchingMatrixEntry.POP_LABEL);
                }
                else
                    emc.setOutgoingLabel(paquete.getTLDPPayload().getLabel());
                if (emc.getLabelOrFEC() == TSwitchingMatrixEntry.UNDEFINED) {
                    emc.setLabelOrFEC(matrizConmutacion.getNewLabel());
                }
                TInternalLink et = (TInternalLink) ports.getPort(pEntrada).getLink();
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
                TPort pSalida = ports.getPort(pEntrada);
                TInternalLink ei = (TInternalLink) pSalida.getLink();
                if (emc.aBackupLSPHasBeenRequested()) {
                    ei.setBackupLSPDown();
                } else {
                    ei.quitarLSP();
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
     * @param IPDestino destination of this LDP reply.
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
     * @param puerto Puerto por el que se debe enviar la confirmaci�n de eliminaci�n.
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
 entrada de la matriz de conmutaci�n a todos los ports necesarios.
     * @param emc Entrada de la matriz de conmutaci�n especificada.
     * @since 1.0
     */
    public void eliminarTLDPTrasTimeout(TSwitchingMatrixEntry emc){
        labelWithdrawal(emc);
    }
    
    /**
     * Este m�todo decrementa los contadores para la retransmisi�n.
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
                        eliminarTLDPTrasTimeout(emc);
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
     * Este m�todo crea una nueva entrada en la matriz de conmutaci�n a partir de una
     * solicitud de etiqueta recibida.
     * @param paqueteSolicitud Solicitud de etiqueta recibida.
     * @param pEntrada Puerto por el que se ha recibido la solicitud.
     * @return La nueva entrada en la matriz de conmutaci�n, creda, insertada e inicializada.
     * @since 1.0
     */
//    public TSwitchingMatrixEntry crearEntradaAPartirDeTLDP(TTLDPPDU paqueteSolicitud, int pEntrada) {
//        TSwitchingMatrixEntry emc = null;
//        int IdTLDPAntecesor = paqueteSolicitud.getTLDPPayload().getTLDPIdentifier();
//        TPort puertoEntrada = ports.getPort(pEntrada);
//        String IPDestinoFinal = paqueteSolicitud.getTLDPPayload().getTargetIPAddress();
//        String IPSalto = topology.obtenerIPSalto(this.getIPAddress(), IPDestinoFinal);
//        if (IPSalto != null) {
//            TPort puertoSalida = ports.getLocalPortConnectedToANodeWithIPAddress(IPSalto);
//            emc = new TSwitchingMatrixEntry();
//            emc.setUpstreamTLDPSessionID(IdTLDPAntecesor);
//            emc.setTailEndIPAddress(IPDestinoFinal);
//            emc.setOutgoingLabel(TSwitchingMatrixEntry.UNDEFINED);
//            emc.setLabelOrFEC(TSwitchingMatrixEntry.UNDEFINED);
//            emc.setEntryIsForBackupLSP(paqueteSolicitud.getLSPType());
//            if (puertoSalida != null) {
//                emc.setOutgoingPortID(puertoSalida.getPortID());
//            } else {
//                emc.setOutgoingPortID(TSwitchingMatrixEntry.UNDEFINED);
//            }
//            emc.setEntryType(TSwitchingMatrixEntry.LABEL_ENTRY);
//            emc.setLabelStackOperation(TSwitchingMatrixEntry.SWAP_LABEL);
//            try {
//                emc.setLocalTLDPSessionID(gIdentLDP.getNew());
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            matrizConmutacion.addEntry(emc);
//        }
//        return emc;
//    }
    public TSwitchingMatrixEntry crearEntradaAPartirDeTLDP(TTLDPPDU paqueteSolicitud, int pEntrada) {
        int IdTLDPAntecesor = paqueteSolicitud.getTLDPPayload().getTLDPIdentifier();
        TSwitchingMatrixEntry emc = null;
        if(paqueteSolicitud.getTLDPPayload().getLabel()==3){
            emc = this.crearEntradaInicialEnMatrizLABEL(this.topology.getNewLabel(), pEntrada, TSwitchingMatrixEntry.POP_LABEL);
            emc.setOutgoingLabel(TSwitchingMatrixEntry.LABEL_ASSIGNED);
        }else {
            emc = this.crearEntradaInicialEnMatrizLABEL(TSwitchingMatrixEntry.UNDEFINED, pEntrada, TSwitchingMatrixEntry.SWAP_LABEL);
            emc.setOutgoingLabel(paqueteSolicitud.getTLDPPayload().getLabel());
            emc.setLabelOrFEC(this.topology.getNewLabel());
        }
        emc.setUpstreamTLDPSessionID(IdTLDPAntecesor);
        emc.setEntryIsForBackupLSP(paqueteSolicitud.getLSPType());
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
     * @param operation operation The operation to do with this table entry. 
     * @param pExit Output interface.
     * @param labelIN label in for this entry
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
     * @param paquete Paquete IPv4 que se debe etiquetar.
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
     * @param emc Entrada de la matriz de conmutaci�n asociada al paquete MPLS.
     * @return Paquete IPv4 que corresponde al paquete MPLS una vez que se ha eliminado toda la
     * informaci�n MLPS; que se ha desetiquetado.
     * @since 1.0
     */
    public TICMPPDU replyICMP(TMPLSPDU paquete, TSwitchingMatrixEntry emc) {
        TICMPPDU paqueteICMP = null;
        try {
            paqueteICMP= new TICMPPDU(gIdent.getNextID(), this.getIPAddress(), paquete.getIPv4Header().getOriginIPAddress(), 11, 0);
            paqueteICMP.setSubtype(TAbstractPDU.ICMP);
            if(this.rfc4950){
                paqueteICMP.setPayloadICMP(paquete.getLabelStack());
            }
            this.generateSimulationEvent(new TSEPacketGenerated(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), paqueteICMP.getSubtype(), paqueteICMP.getSize()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        paquete = null;
        return paqueteICMP;
    }
    
    /**
     * Este m�todo descarta un paquete del ndo y refleja este descarte en las
     * estad�sticas del nodo.
     * @param paquete Paquete que queremos descartar.
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
     * Este m�todo permite acceder a los ports del nodo directamtne.
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
     * Este m�todo devuelve el peso del nodo, que debe ser tomado en cuenta por lo
     * algoritmos de encaminamiento para calcular las rutas.
     * @return El peso del LSR.
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
     * Este m�todo calcula si el nodo est� bien configurado o no.
     * @return TRUE, si el ndoo est� bien configurado. FALSE en caso contrario.
     * @since 1.0
     */
    public boolean isWellConfigured() {
        return this.wellConfigured;
    }
    /**
     * Este m�todo devuelve si el nodo est� bien configurado y si no, la raz�n.
     * @param t La topolog�a donde est� el nodo incluido.
     * @param recfg TRUE, si se est� reconfigurando el LSR. FALSE si se est� configurando por
     * primera vez.
     * @return CORRECTA, si el nodo est� bien configurado. Un c�digo de error en caso
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
        return this.CORRECTA;
    }
    
    /**
     * Este m�todo transforma el c�digo de error de configuraci�n del nodo en un
     * mensaje aclaratorio.
     * @param e C�digo de error.
     * @return Texto explicativo del c�digo de error.
     * @since 1.0
     */
    public String getErrorMessage(int e) {
        switch (e) {
            case SIN_NOMBRE: return (java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TConfigLSR.FALTA_NOMBRE"));
            case NOMBRE_YA_EXISTE: return (java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TConfigLSR.NOMBRE_REPETIDO"));
            case SOLO_ESPACIOS: return (java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TNodoLSR.NombreNoSoloEspacios"));
            case EMPTY_IP: return "IP Address of the node is not present";
        }
        return ("");
    }
    
    /**
     * Este m�todo permite transformar el nodo en una cadena de texto que se puede
     * volcar f�cilmente a disco.
     * @return Una cadena de texto que representa al nodo.
     * @since 1.0
     */
    public String marshall() {
        String cadena = "#LSR#";
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
        cadena += this.rfc4950;
        cadena += "#";
        cadena += this.propagateTTL;
        cadena += "#";
        return cadena;
    }
    
    /**
     * Este m�todo permite construir sobre la instancia actual, un LSR partiendo de la
     * representaci�n serializada de otro.
     * @param elemento �lemento serializado que se desea deserializar.
     * @return TRUE, si se ha conseguido deserializar correctamente. FALSE en caso contrario.
     * @since 1.0
     */
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
        this.setGenerateStats(Boolean.parseBoolean(valores[6]));
        int posX = Integer.parseInt(valores[7]);
        int posY = Integer.parseInt(valores[8]);
        this.setPosition(new Point(posX+24, posY+24));
        this.potenciaEnMb = Integer.parseInt(valores[9]);
        this.getPorts().setBufferSizeInMB(Integer.parseInt(valores[10]));
        this.rfc4950 = Boolean.parseBoolean(valores[11]);
        this.propagateTTL = Boolean.parseBoolean(valores[12]);
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
     * Este m�todo permite acceder directamente a las estad�sticas del nodo.
     * @return Las estad�sticas del nodo.
     * @since 1.0
     */
    public TStats getStats() {
        return estadisticas;
    }
    
    /**
     * Este m�todo permite establecer el n�mero de ports que tendr� el nodo.
     * @param num N�mero de ports del nodo. Como mucho 8.
     * @since 1.0
     */
    public synchronized void setPorts(int num) {
        ports = new TFIFOPortSet(num, this);
    }
    
    /**
     * @return the rfc4950
     */
    public boolean isRfc4950() {
        return rfc4950;
    }

    /**
     * @param rfc4950 the rfc4950 to set
     */
    public void setRfc4950(boolean rfc4950) {
        this.rfc4950 = rfc4950;
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
     * Este m�todo no hace nada en un LSR. En un nodo activoPermitir� solicitar
     * a un nodo activo la retransmisi�n de un paquete.
     * @param paquete Paquete cuya retransmisi�n se est� solicitando.
     * @param pSalida Puerto por el que se enviar� la solicitud.
     * @since 1.0
     */
    public void runGoSPDUStoreAndRetransmitProtocol(TMPLSPDU paquete, int pSalida) {
    }
    
    /**
     * Esta constante indica que la configuraci�n del nodo es correcta.
     * @since 1.0
     */
    public static final int CORRECTA = 0;
    /**
     * Esta constante indica que en la configuraci�n del nodo, falta el nombre.
     * @since 1.0
     */
    public static final int SIN_NOMBRE = 1;
    /**
     * Esta constante indica que, en la configuraci�n del nodo, se ha elegido un nombre
     * que ya est� siendo usado.
     * @since 1.0
     */
    public static final int NOMBRE_YA_EXISTE = 2;
    /**
     * Esta constante indica que en la configuraci�n del nodo, el nombre elegido es
     * err�neo porque solo cuenta con espacios.
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
    private TLSRStats estadisticas;
    private boolean rfc4950;
    private boolean propagateTTL;

}
