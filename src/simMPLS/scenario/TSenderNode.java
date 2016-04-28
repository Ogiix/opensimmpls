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
import simMPLS.protocols.TMPLSLabel;
import simMPLS.protocols.TMPLSPDU;
import simMPLS.protocols.TIPv4PDU;
import simMPLS.protocols.TICMPPDU;
import simMPLS.hardware.timer.TTimerEvent;
import simMPLS.hardware.timer.ITimerEventListener;
import simMPLS.hardware.ports.TFIFOPortSet;
import simMPLS.hardware.ports.TPort;
import simMPLS.hardware.ports.TPortSet;
import simMPLS.utils.EIDGeneratorOverflow;
import simMPLS.utils.TLongIDGenerator;
import simMPLS.utils.TRotaryIDGenerator;
import java.awt.*;
import java.util.*;
import simMPLS.utils.TraceResultSaver;


/**
 * Esta clase implementa un nodo emisor de tr�fico.
 * @author <B>Manuel Dom�nguez Dorado</B><br><A
 * href="mailto:ingeniero@ManoloDominguez.com">ingeniero@ManoloDominguez.com</A><br><A href="http://www.ManoloDominguez.com" target="_blank">http://www.ManoloDominguez.com</A>
 * @version 1.0
 */
public class TSenderNode extends TNode implements ITimerEventListener, Runnable {
    
    /**
     * Crea una nueva instanci de TNodoEmisor
     * @param identificador Identificar �nico del nodo en la topolog�a.
     * @param d Direcci�n IP del nodo.
     * @param il Generador de identigficadores para los eventos generados por el nodo.
     * @param t Topolog�a dentro de la cual se encuentra el nodo.
     * @since 1.0
     */
    public TSenderNode(int identificador, String d, TLongIDGenerator il, TTopology t) {
        super(identificador, d, il, t);
        this.setPorts(super.NUM_PUERTOS_EMISOR);
        gIdent = new TLongIDGenerator();
        gIdGoS = new TRotaryIDGenerator();
        String IPDestino = "";
        tasaTransferencia = 10;
        tipoTrafico = TSenderNode.CONSTANTE;
        encapsularSobreMPLS = false;
        nivelDeGoS = 0;
        customTTL=0;
        traceroute=false;
        tracerouteTTL=1;
        destinationReached=false;
        ready=true;
        LSPDeBackup = false;
        generadorDeAleatorios = new Random();
        etiquetaDeEmision = (16 + generadorDeAleatorios.nextInt(1000000));
        tamDatosConstante = 0;
        tamDatosVariable = 0;
        estadisticas = new TSenderStats();
        estadisticas.activateStats(this.isGeneratingStats());
        traceSaver = null;
        saving = false;
    }
    
    /**
     * Este m�todo obtiene el tamanio de la carga �til los paquetes de datos constantes
     * que debe generar el nodo si est� configurado para tr�fico constante.
     * @return El tama�o de la carga �til de los paquetes constantes.
     * @since 1.0
     */    
    public int obtenerTamDatosConstante() {
        return this.tamDatosConstante;
    }
    
    /**
     * Este m�todo permite establecer el tama�o de la carga util de los paquetes constantes que debe
     * generar el nodo.
     * @param tdc Tamanio de la carga util de los paquetes para tr�fico constante.
     * @since 1.0
     */    
    public void ponerTamDatosConstante(int tdc) {
        this.tamDatosConstante = tdc;
    }
    
    /**
     * Este m�todo permite establecer el nodo destino del tr�fico generado.
     * @param d IP del nodo destino del tr�fico.
     * @since 1.0
     */
    public void ponerDestino(String d) {
        if (!d.equals("")) {
            TNode nt = this.topology.setFirstNodeNamed(d);
            if (nt != null) {
                IPDestino = nt.getIPAddress();
            }
        }
    }
    
    /**
     * Este m�todo permite obtener la IP del nodo destino del tr�fico generado.
     * @return La IP del nodo destino del tr�fico generado.
     * @since 1.0
     */
    public String obtenerDestino() {
        return IPDestino;
    }
    
    /**
     * Este m�todo permite establecer la tasa de generaci�n de tr�fico del nodo.
     * @param t Tasa de generaci�n de tr�fico elegida para el nodo. En Mbps.
     * @since 1.0
     */
    public void ponerTasaTrafico(int t) {
        tasaTransferencia = t;
    }
    
    /**
     * Este m�todo permite obtener la tasa de generaci�n de tr�fico del nodo.
     * @return Tasa de generaci�n de tr�fico el nodo. En Mbps.
     * @since 1.0
     */
    public int obtenerTasaTrafico() {
        return tasaTransferencia;
    }
    
    private int obtenerCodificacionEXP() {
        if ((this.nivelDeGoS == 0) && (this.LSPDeBackup)) {
            return TAbstractPDU.EXP_LEVEL0_WITH_BACKUP_LSP;
        } else if ((this.nivelDeGoS == 1) && (this.LSPDeBackup)) {
            return TAbstractPDU.EXP_LEVEL1_WITH_BACKUP_LSP;
        } else if ((this.nivelDeGoS == 2) && (this.LSPDeBackup)) {
            return TAbstractPDU.EXP_LEVEL2_WITH_BACKUP_LSP;
        } else if ((this.nivelDeGoS == 3) && (this.LSPDeBackup)) {
            return TAbstractPDU.EXP_LEVEL3_WITH_BACKUP_LSP;
        } else if ((this.nivelDeGoS == 0) && (!this.LSPDeBackup)) {
            return TAbstractPDU.EXP_LEVEL0_WITHOUT_BACKUP_LSP;
        } else if ((this.nivelDeGoS == 1) && (!this.LSPDeBackup)) {
            return TAbstractPDU.EXP_LEVEL1_WITHOUT_BACKUP_LSP;
        } else if ((this.nivelDeGoS == 2) && (!this.LSPDeBackup)) {
            return TAbstractPDU.EXP_LEVEL2_WITHOUT_BACKUP_LSP;
        } else if ((this.nivelDeGoS == 3) && (!this.LSPDeBackup)) {
            return TAbstractPDU.EXP_LEVEL3_WITHOUT_BACKUP_LSP;
        }
        return TAbstractPDU.EXP_LEVEL0_WITHOUT_BACKUP_LSP;
    }
    
    /**
     * Este m�todo permite establecer qu� tipo de tr�fico generar� el nodo.
     * @param t Tipo de tr�fico generado por el nodo. Una de las constantes definidas en esta
     * clase.
     * @since v
     */
    public void ponerTipoTrafico(int t) {
        tipoTrafico = t;
    }
    
    /**
     * Este m�todo permite obtener el tipo de tr�fico que est� generando el nodo.
     * @return Tipo de tr�fico generado por el nodo. Una de las constantes de esta clase.
     * @since 1.0
     */
    public int obtenerTipoTrafico() {
        return tipoTrafico;
    }
    
    /**
     * Este m�todo permite establecer si el tr�fico generado est� ya estiquetado en
     * MPLS o no. Lo que es lo mismo, si el tr�fico proviene de otro dominio MPLS o no.
     * @param mpls TRUE, si el nodo debe generar tr�fico MPLS. FALSE en caso contrario.
     * @since 1.0
     */
    public void ponerSobreMPLS(boolean mpls) {
        encapsularSobreMPLS = mpls;
    }
    
    /**
     * Este m�todo permite obtener si el tr�fico que est� generando el nodo es ya
     * tr�fico MPLS o no.
     * @return TRUE, si el tr�fico que est� generando el nodo es MPLS. FALSE en caso contrario.
     * @since 1.0
     */
    public boolean obtenerSobreMPLS() {
        return encapsularSobreMPLS;
    }
    
    /**
     * Este m�todo permite establecer el nivel de garant�a de servicio con el que el
     * nodo debe marcar los paquetes generados.
     * @param gos Nivel de garant�a de servicio.
     * @since 1.0
     */
    public void ponerNivelDeGoS(int gos) {
        nivelDeGoS = gos;
    }
    
    /**
     * Este m�todo permite obtener el nivel de garant�a de servicio con el que el nodo
     * est� marcando los paquetes generados.
     * @return El nivel de garant�a de servicio con el que el nodo est� marcando los paquetes
     * generados.
     * @since 1.0
     */
    public int obtenerNivelDeGoS() {
        return nivelDeGoS;
    }
    
    /**
     * Este m�todo permite establecer si los paquetes generados ser�n marcdos para
     * requerir un LSP de respaldo en el dominio MPLS o no.
     * @param l TRUE si los paqutes requerir�n LSP de respaldo. FALSE en caso contrario.
     * @since 1.0
     */
    public void ponerLSPDeBackup(boolean l) {
        LSPDeBackup = l;
    }
    
    /**
     * Este m�todo permite saber si los paquetes generados est�n siendo marcados para
     * requerir un LSP de respaldo o no.
     * @return TRUE, si los paquetes es�n siendo marcados para requerir un LSP de respaldo.
     * FALSE en caso contrario.
     * @since 1.0
     */
    public boolean obtenerLSPDeBackup() {
        return LSPDeBackup;
    }
    
    /**
     * Este m�todo permite obtener el tipo de nodo del que se trata esta instancia.
     * @return TNode.SENDER, indicando que es un generador y emisor de tr�fico.
     * @since 1.0
     */
    public int getNodeType() {
        return super.SENDER;
    }
    
    /**
     * Este m�todo permite recibir eventos de sincronizaci�n del reloj principal del
     * simulador, que es quien sincroniza todo.
     * @param evt Evento de sincronizaci�n enviado por el reloj principal.
     * @since 1.0
     */
    public void receiveTimerEvent(TTimerEvent evt) {
        this.setStepDouration(evt.getStepDuration());
        this.setTimeInstant(evt.getUpperLimit());
        this.availableNs += evt.getStepDuration();
        this.startOperation();
    }
    
    /**
     * Este m�todo se llama cuando el hilo independiente del nodo se pone en
     * funcionamiento. Es el n�cleo del nodo.
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
        TAbstractPDU paqueteTmp = crearPaquete();
        boolean emito = false;
        if(isTraceroute()){
            if(!destinationReached && ready){
                if(obtenerOctetosTransmitibles() > obtenerTamanioSiguientePaquete(paqueteTmp)){
                    emito = true;
                    ready=false;
                    generarTrafico();
                    tracerouteTTL++;
                }
            }
        }else{
            while (obtenerOctetosTransmitibles() > obtenerTamanioSiguientePaquete(paqueteTmp)) {
                emito = true;
                generarTrafico();
            }
        }
        paqueteTmp = null;
        if (emito) {
            this.resetStepsWithoutEmittingToZero();
        } else {
            this.increaseStepsWithoutEmitting();
        }
        recibirDatos();
        this.estadisticas.consolidateData(this.getAvailableTime());
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
     * Este m�todo obtiene el tama�o que tendr� la carga util del siguiente paquete
     * generado, independientemente de que se est� tratando con tr�fico constante o
     * variable.
     * @return El tama�o de la carga �til del siguiente paquete que generar� el emisor.
     * @since 1.0
     */    
    public int obtenerTamanioDatosSiguientePaquete() {
        if (this.tipoTrafico == TSenderNode.CONSTANTE) {
            return this.tamDatosConstante;
        }
        return this.tamDatosVariable;
    }
    
    /**
     * Este m�todo obtiene el tama�o de la header del sigueinte paquete que se
 generar�, independientemente del tipo de tr�fico del que se trate y de los
 valores de garant�a de Servicio con los que peuda estar marcado.
     * @param paquete Paquete de cuya header queremos conocer el tama�o.
     * @return El tama�o de la header.
     * @since 1.0
     */    
    public int obtenerTamanioCabeceraSiguientePaquete(TAbstractPDU paquete) {
        TMPLSPDU paqueteMPLS = null;
        TICMPPDU paqueteICMP = null;
        TIPv4PDU paqueteIPv4 = null;
        if (paquete.getType() == TAbstractPDU.MPLS) {
            paqueteMPLS = (TMPLSPDU) paquete;
            return paqueteMPLS.getSize();
        } else if(paquete.getType() == TAbstractPDU.ICMP){
            paqueteICMP = (TICMPPDU) paquete;
            return paqueteICMP.getSize();
        } else {
            paqueteIPv4 = (TIPv4PDU) paquete;
            return paqueteIPv4.getSize();
        }
    }
    
    /**
     * Este m�todo calcula, el tama�o del siguiente paquete a generar,
     * independientemente de que se de tipo constante o variable, o de cualquier
     * protocolo de los soportados, e incluso de que nivel de GoS tenga asignado.
     * @param paquete paquete cuyo tamanio se desea calcular.
     * @return El tama�o total del paquete especificado por par�metros.
     * @since 1.0
     */    
    public int obtenerTamanioSiguientePaquete(TAbstractPDU paquete) {
        int tamanioDatos = 0;
        int tamanioCabecera = 0;
        int tamanioFinal = 0;
        tamanioDatos = obtenerTamanioDatosSiguientePaquete();
        tamanioCabecera = obtenerTamanioCabeceraSiguientePaquete(paquete);
        tamanioFinal = tamanioDatos + tamanioCabecera;
        return tamanioFinal;
    }
    
    /**
     * Este m�todo crea paquetes de tr�fico acorde a la configuraci�n el emisor de
     * tr�fico y los env�a al receptor destino del tr�fico.
     * @since 1.0
     */    
    public void generarTrafico() {
        TAbstractPDU paquete=null;
        TAbstractPDU paqueteConTamanio=null;
        TPort pt = ports.getPort(0);
        if (pt != null) {
            if (!pt.isAvailable()) {
                paquete = crearPaquete();
                paqueteConTamanio = this.ponerTamanio(paquete);
                if (paqueteConTamanio != null) {
                    try {
                        int tipo = 0;
                        if (paqueteConTamanio.getType() == TAbstractPDU.MPLS) {
                            TMPLSPDU paqueteMPLS = (TMPLSPDU) paqueteConTamanio;
                            tipo = paqueteMPLS.getSubtype();
                        } else if (paqueteConTamanio.getType() == TAbstractPDU.IPV4) {
                            TIPv4PDU paqueteIPv4 = (TIPv4PDU) paqueteConTamanio;
                            tipo = paqueteIPv4.getSubtype();
                        } else if (paqueteConTamanio.getType() == TAbstractPDU.ICMP) {
                            TICMPPDU paqueteICMP = (TICMPPDU) paqueteConTamanio;
                            tipo = paqueteICMP.getSubtype();
                        }
                        this.generateSimulationEvent(new TSEPacketGenerated(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), tipo, paqueteConTamanio.getSize()));
                        this.generateSimulationEvent(new TSEPacketSent(this, this.longIdentifierGenerator.getNextID(), this.getAvailableTime(), tipo));
                    } catch (Exception e) {
                        e.printStackTrace(); 
                    }
                    if (this.topology.obtenerIPSalto(this.getIPAddress(), this.obtenerDestino()) != null) {
                        pt.putPacketOnLink(paqueteConTamanio, pt.getLink().getTargetNodeIDOfTrafficSentBy(this));
                    } else {
                        discardPacket(paqueteConTamanio);
                    }
                } 
            }
        }
    }

    /**
     * Este m�todo contabiliza un paquete y su tama�o asociado, en las estad�sticas del
     * nodo emisor, y sus gr�ficas.
     * @param paquete Paquete que se desea contabilizar.
     * @param deEntrada TRUE indica que se trata de un paquete que ha entrado en el nodo. FALSE indica
     * que se trata de un paquete que ha salido del nodo.
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
     * Este m�todo calcula cu�ntos nanosegundos necesita el nodo emisor para generar y
     * transmitir un bit. Se basa para ello en la tasa de generaci�n de tr�fico del
     * nodo.
     * @return El n�mero de nanosegundos necesarios generar y transmitir un bit.
     * @since 1.0
     */    
    public double obtenerNsPorBit() {
        long tasaEnBitsPorSegundo = (long) (this.tasaTransferencia*1048576L);
        double nsPorCadaBit = (double) ((double)1000000000.0/(long)tasaEnBitsPorSegundo);
        return nsPorCadaBit;
    }
    
    /**
     * Este m�todo calcula el n�mero de nanosegundos necesarios para poder generar y
     * enviar un determinado n�mero de octetos.
     * @param octetos N�mero de octetos que deseamos generar y transmitir.
     * @return N�mero de nanosegundos que necesitamos para los octetos especificados.
     * @since 1.0
     */    
    public double obtenerNsUsadosTotalOctetos(int octetos) {
        double nsPorCadaBit = obtenerNsPorBit();
        long bitsOctetos = (long) ((long)octetos*(long)8);
        return (double)((double)nsPorCadaBit*(long)bitsOctetos);
    }
    
    /**
     * Este m�todo calcula el n�mero de bits que puede generar y transmitir con el
     * n�mero de nanosegundos con los que cuenta.
     * @return N�mero de bits que puede generar y transmitir.
     * @since 1.0
     */    
    public int obtenerLimiteBitsTransmitibles() {
        double nsPorCadaBit = obtenerNsPorBit();
        double maximoBits = (double) ((double)availableNs/(double)nsPorCadaBit);
        return (int) maximoBits;
    }
    
    /**
     * Este metodo calcula el n�mero de octetos completos que puede generar y
     * transmitir el emisor teniendo en cuenta el n�mero de nanosegundos con los que
     * cuenta.
     * @return El n�mero de octetos completos que se pueden generar y transmitir.
     * @since 1.0
     */    
    public int obtenerOctetosTransmitibles() {
        double maximoBytes = ((double)obtenerLimiteBitsTransmitibles()/(double)8.0);
        return (int) maximoBytes;
    }
    
    /**
     * Este m�todo calcula autom�ticamente el tama�o de la carga util del siguiente
     * paquete a generar. Si el tr�fico es constante, devolver� el tama�o de paquete
     * con el que se configur� el emisor. Si el tama�o es variable, generar� tr�fico
     * siguiendo una funci�n de probabilidad en la cual se sigue la siguiente
     * distribuci�n de tama�os:
     *
     * Tama�oPaquete < 100 octetos ---------------------> 47%
     * Tama�oPaquete >= 100 octetos y < 1400 octetos ---> 24%
     * Tama�oPaquete >= 1400 octetos y < 1500 octetos --> 18%
     * Tama�oPaquete >= 1500 octetos -------------------> 1%
     *
     * Esta distribuci�n est� extra�da de las estad�sticas de la red Abilene, que son
     * p�blicas y se pueden observar en http://netflow.internet2.edu/weekly.
     * @return El tama�o que debe tener el siguiente paquete.
     * @since 1.0
     */    
    public int generarTamanioSiguientePaquete() {
        int probabilidad = this.generadorDeAleatorios.nextInt(100);
        int tamanioGenerado = 0;
        if (probabilidad < 47) {
            tamanioGenerado = this.generadorDeAleatorios.nextInt(100);
            tamanioGenerado -= 40;
        } else if ((probabilidad >= 47) && (probabilidad < 71)) {
            tamanioGenerado = (this.generadorDeAleatorios.nextInt(1300) + 100);
            tamanioGenerado -= 40;
        } else if ((probabilidad >= 71) && (probabilidad < 99)) {
            tamanioGenerado = (this.generadorDeAleatorios.nextInt(100) + 1400);
            tamanioGenerado -= 40;
        } else if (probabilidad >= 99) {
            tamanioGenerado = (this.generadorDeAleatorios.nextInt(64035) + 1500);
            tamanioGenerado -= 40;
        }
        return tamanioGenerado;
    }
    
    /**
     * Este m�todo toma como par�metro un paquete vacio y devuelve un paquete con datos
     * insertados. Los datos ser�n del tama�o que se haya estimado en los distintos
     * m�todos de la clase,pero ser� el correcto.
     * Add the data to an empty paquet
     * @param paquete Paquete al que se quiere a�adir datos.
     * @return Paquete con datos insertados del tama�o correcto seg�n el tipo de gr�fico.
     * @since 1.0
     */    
    public TAbstractPDU ponerTamanio(TAbstractPDU paquete) {
        TMPLSPDU paqueteMPLS = null;
        TIPv4PDU paqueteIPv4 = null;
        TICMPPDU paqueteICMP = null;
        int bitsMaximos = obtenerLimiteBitsTransmitibles();
        int tamanioCabecera = 0;
        int tamanioDatos = 0;
        int tamanioTotal = 0;
        double nsUsados = 0;
        tamanioTotal = obtenerTamanioSiguientePaquete(paquete);
        tamanioCabecera = obtenerTamanioCabeceraSiguientePaquete(paquete);
        tamanioDatos = obtenerTamanioDatosSiguientePaquete();
        if (tamanioTotal > obtenerOctetosTransmitibles()) {
            paquete = null;
            return null;
        } else {
            if (paquete.getType() == TAbstractPDU.MPLS) {
                paqueteMPLS = (TMPLSPDU) paquete;
                paqueteMPLS.getTCPPayload().setSize((int) tamanioDatos);
                nsUsados = this.obtenerNsUsadosTotalOctetos(tamanioTotal);
                this.availableNs -= nsUsados;
                if (this.availableNs < 0)
                    this.availableNs = 0;
                if (this.tipoTrafico == this.VARIABLE) {
                    this.tamDatosVariable = this.generarTamanioSiguientePaquete();
                }
                return paqueteMPLS;
            } else if (paquete.getType() == TAbstractPDU.IPV4) {
                paqueteIPv4 = (TIPv4PDU) paquete;
                paqueteIPv4.getTCPPayload().setSize(tamanioDatos);
                nsUsados = this.obtenerNsUsadosTotalOctetos(tamanioTotal);
                this.availableNs -= nsUsados;
                if (this.availableNs < 0)
                    this.availableNs = 0;
                if (this.tipoTrafico == this.VARIABLE) {
                    this.tamDatosVariable = this.generarTamanioSiguientePaquete();
                }
                return paqueteIPv4;
            } else if (paquete.getType() == TAbstractPDU.ICMP) {
                paqueteICMP = (TICMPPDU) paquete;
                nsUsados = this.obtenerNsUsadosTotalOctetos(tamanioTotal);
                this.availableNs -= nsUsados;
                if (this.availableNs < 0)
                    this.availableNs = 0;
                if (this.tipoTrafico == this.VARIABLE) {
                    this.tamDatosVariable = this.generarTamanioSiguientePaquete();
                }
                return paqueteICMP;
            }
        }
        return null;
    }
    
    /**
     * Este m�todo devuelve un paquete vac�o (sin datos) del tipo correcto para el que
     * esta configurado el nodo emisor.
     * @return El paquete creado.
     * @since 1.0
     */    
    public TAbstractPDU crearPaquete() {
        int valorGoS = this.obtenerCodificacionEXP();
        try {
            if (this.encapsularSobreMPLS) {
                if (valorGoS == TAbstractPDU.EXP_LEVEL0_WITHOUT_BACKUP_LSP) {
                    TMPLSPDU paquete = new TMPLSPDU(gIdent.getNextID(), getIPAddress(), this.IPDestino, 0);
                    if(customTTL>0)
                        paquete.getIPv4Header().setTTL(customTTL);
                    TMPLSLabel etiquetaMPLSDeEmision = new TMPLSLabel();
                    etiquetaMPLSDeEmision.setBoS(true);
                    etiquetaMPLSDeEmision.setEXP(0);
                    etiquetaMPLSDeEmision.setLabel(etiquetaDeEmision);
                    etiquetaMPLSDeEmision.setTTL(paquete.getIPv4Header().getTTL());
                    paquete.getLabelStack().pushTop(etiquetaMPLSDeEmision);
                    return paquete;
                } else {
                    TMPLSPDU paquete = new TMPLSPDU(gIdent.getNextID(), getIPAddress(), this.IPDestino, 0);
                    paquete.setSubtype(TAbstractPDU.MPLS_GOS);
                    if(customTTL>0)
                        paquete.getIPv4Header().setTTL(customTTL);
                    paquete.getIPv4Header().getOptionsField().setRequestedGoSLevel(valorGoS);
                    paquete.getIPv4Header().getOptionsField().setPacketLocalUniqueIdentifier(this.gIdGoS.getNextID());
                    TMPLSLabel etiquetaMPLSDeEmision = new TMPLSLabel();
                    etiquetaMPLSDeEmision.setBoS(true);
                    etiquetaMPLSDeEmision.setEXP(0);
                    etiquetaMPLSDeEmision.setLabel(etiquetaDeEmision);
                    etiquetaMPLSDeEmision.setTTL(paquete.getIPv4Header().getTTL());
                    TMPLSLabel etiquetaMPLS1 = new TMPLSLabel();
                    etiquetaMPLS1.setBoS(false);
                    etiquetaMPLS1.setEXP(valorGoS);
                    etiquetaMPLS1.setLabel(1);
                    etiquetaMPLS1.setTTL(paquete.getIPv4Header().getTTL());
                    paquete.getLabelStack().pushTop(etiquetaMPLSDeEmision);
                    paquete.getLabelStack().pushTop(etiquetaMPLS1);
                    return paquete;
                }
            } else if(isICMP){
                TICMPPDU paquete = new TICMPPDU(gIdent.getNextID(), getIPAddress(), this.IPDestino);
                paquete.setSubtype(TAbstractPDU.ICMP);
                if(customTTL>0)
                    paquete.getIPv4Header().setTTL(customTTL);
                if(isTraceroute()){
                    paquete.getIPv4Header().setTTL(this.tracerouteTTL);
                }
                return paquete;
            } else {
                if (valorGoS == TAbstractPDU.EXP_LEVEL0_WITHOUT_BACKUP_LSP) {
                    TIPv4PDU paquete = new TIPv4PDU(gIdent.getNextID(), getIPAddress(), this.IPDestino, 0);
                    if(customTTL>0)
                        paquete.getIPv4Header().setTTL(customTTL);
                    return paquete;
                } else {
                    TIPv4PDU paquete = new TIPv4PDU(gIdent.getNextID(), getIPAddress(), this.IPDestino, 0);
                    paquete.setSubtype(TAbstractPDU.IPV4_GOS);
                    if(customTTL>0)
                        paquete.getIPv4Header().setTTL(customTTL);
                    paquete.getIPv4Header().getOptionsField().setRequestedGoSLevel(valorGoS);
                    paquete.getIPv4Header().getOptionsField().setPacketLocalUniqueIdentifier(this.gIdGoS.getNextID());
                    return paquete;
                }
            }
        } catch (EIDGeneratorOverflow e) {
            e.printStackTrace(); 
        }
        return null;
    }
    
    /**
     * Este m�todo descarta un paquete de cualquier tipo. Adem�s anota los datos
     * relativos en ese descarte en las estad�sticas del nodo.
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
     * Este m�todo lee mientras puede los paquetes que hay en el buffer de recepci�n.
     * @since 1.0
     */    
    public void recibirDatos() {
        TPort p = this.ports.getPort(0);
        TAbstractPDU paquete = null;
        if (p != null) {
            while (p.thereIsAPacketWaiting()) {
                paquete = p.getPacket();
                if(paquete.getType()==TAbstractPDU.ICMP){
                    TICMPPDU icmpPacket = (TICMPPDU) paquete;
                    if(this.saving)
                        this.traceSaver.save(icmpPacket);
                    if(this.traceroute){
                        if(icmpPacket.getTypeICMP()==11 || icmpPacket.getTypeICMP()==0){
                            ready=true;
                            if(paquete.getIPv4Header().getOriginIPAddress().equals(this.IPDestino)){
                                this.destinationReached = true;
                                tracerouteTTL=0;
                            }
                        }else if(icmpPacket.getTypeICMP()==3){
                            this.tracerouteTTL--;
                            this.ready=true;
                        }
                    }
                }
                this.contabilizarPaquete(paquete, true);
                paquete = null;
            }
        }
    }
    
    /**
     * Este m�todo investiga si al nodo le quedan ports libres.
     * @return TRUE, si al nodo le quedan ports libres. FALSE en caso contrario.
     * @since 1.0
     */
    public boolean hasAvailablePorts() {
        return this.ports.hasAvailablePorts();
    }
    
    /**
     * Este m�todo permite acceder a los ports del nodo directamente.
     * @return El conjunto de ports del nodo.
     * @since 1.0
     */
    public TPortSet getPorts() {
        return this.ports;
    }
    
    /**
     * Este m�todo devuelve el peso del nodo, que debe ser tenido en cuenta por el
     * algoritmo e encaminamiento para el c�lculo de rutas.
     * @return En el nodo emisor, siempre es cero.
     * @since 1.0
     */
    public long getRoutingWeight() {
        return 0;
    }
    
    /**
     * Este m�todo devuelve si el nodo est� bien configurado o no.
     * @return TRUE, si el nodo est� bien configurado. FALSE en caso contrario.
     * @since 1.0
     */    
    public boolean isWellConfigured() {
        return this.wellConfigured;
    }
    
    /**
     * Este m�todo calcula si el nodo est� bien configurado o no, actualizando el
     * atributo que indicar� posteriormente este hecho.
     * @param t Topolog�a dentro de la cual est� incluido el nodo emisor.
     * @param recfg TRUE si se est� reconfigurando el nodo emisor. FALSE si est� configurando por
     * primera vez.
     * @return CORRECTA, si el nodo est� bien configurado. Un codigo de error en caso contrario.
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
        
        if (this.obtenerDestino() == null)
            return this.SIN_DESTINO;
        if (this.obtenerDestino().equals(""))
            return this.SIN_DESTINO;
        this.setWellConfigured(true);
        return this.CORRECTA;
    }
    
    /**
     * Este m�todo transforma un codigo de error en un mensaje con similar significado,
     * pero legible por el usuario.
     * @param e C�digo de error cuyo mensaje se desea obtener.
     * @return El mensaje equivalente al codigo de error, pero legible.
     * @since 1.0
     */    
    public String getErrorMessage(int e) {
        switch (e) {
            case SIN_NOMBRE: return (java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TConfigEmisor.FALTA_NOMBRE"));
            case NOMBRE_YA_EXISTE: return (java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TConfigEmisor.NOMBRE_REPETIDO"));
            case SOLO_ESPACIOS: return (java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TNodoEmisor.NoSoloEspacios"));
            case SIN_DESTINO: return (java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TNodoEmisor.DestinoParaElTrafico"));
            case EMPTY_IP: return "IP Address of the node is not present";
        }
        return ("");
    }
    
    /**
     * Este m�todo transforma el nodo emisor en una cadena de caracterres que se puede
     * volcar a disco.
     * @return TRUE, si se ha realizado la resializaci�n correctamente. FALSE en caso
     * contrario.
     * @since 1.0
     */    
    @Override
    public String marshall() {
        String cadena = "#Sender#";
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
        cadena += this.obtenerDestino();
        cadena += "#";
        cadena += this.obtenerLSPDeBackup();
        cadena += "#";
        cadena += this.obtenerNivelDeGoS();
        cadena += "#";
        cadena += this.obtenerSobreMPLS();
        cadena += "#";
        cadena += this.obtenerTasaTrafico();
        cadena += "#";
        cadena += this.obtenerTipoTrafico();
        cadena += "#";
        cadena += this.obtenerTamDatosConstante();
        cadena += "#";
        cadena += this.customTTL;
        cadena += "#";
        cadena += this.isICMP;
        cadena += "#";
        cadena += this.typeICMP;
        cadena += "#";
        cadena += this.traceroute;
        cadena += "#";
        return cadena;
    }
    
    /**
     * Este m�todo toma una cadena de texto que representa a un emisor serializado y
     * construye, en base a ella, el emisor en memoria sobre la instancia actual.
     * @param elemento El nodo emisor serializado.
     * @return TRUE, si se consigue deserializar correctamente. FALSE en caso contrario.
     * @since 1.0
     */    
    @Override
    public boolean unMarshall(String elemento) {
        String valores[] = elemento.split("#");
        if (valores.length != 20) {
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
        this.IPDestino = valores[9];
        this.ponerLSPDeBackup(Boolean.parseBoolean(valores[10]));
        this.ponerNivelDeGoS(Integer.parseInt(valores[11]));
        this.ponerSobreMPLS(Boolean.parseBoolean(valores[12]));
        this.ponerTasaTrafico(Integer.parseInt(valores[13]));
        this.ponerTipoTrafico(Integer.parseInt(valores[14]));
        this.ponerTamDatosConstante(Integer.parseInt(valores[15]));
        this.customTTL = Integer.parseInt(valores[16]);
        this.isICMP = Boolean.parseBoolean(valores[17]);
        this.typeICMP = Integer.parseInt(valores[18]);
        this.traceroute = Boolean.parseBoolean(valores[19]);
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
     * Este m�todo reinicia los atributos de la clase como si acabasen de ser creador
     * por el constructor.
     * @since 1.0
     */    
    public void reset() {
        gIdent.reset();
        gIdGoS.reset();
        this.ports.reset();
        this.estadisticas.reset();
        estadisticas.activateStats(this.isGeneratingStats());
        this.resetStepsWithoutEmittingToZero();
        this.tracerouteTTL=1;
        this.destinationReached=false;
        ready=true;
        if(isICMP && saving){
            String path="";
            if(this.topology.getEscenarioPadre().obtenerFichero() != null)
                path=this.topology.getEscenarioPadre().obtenerFichero().getPath();
            if(this.traceSaver != null)
                this.traceSaver.closeFile();
            if(isTraceroute()){
                this.traceSaver = new TraceResultSaver(path+"."+this.getName()+".traceroute.txt", TraceResultSaver.TRACEROUTE);
                this.traceSaver.initializeFile("traceroute", this.getName(), this.IPDestino);
            }else {
                this.traceSaver = new TraceResultSaver(path+"."+this.getName()+".ping.txt", TraceResultSaver.PING);
                this.traceSaver.initializeFile("ping", this.getName(), this.IPDestino);
            }
        }
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
     * Este m�todo inicia el conjunto de ports del nodo, con el n�mero de ports
 especificado.
     * @param num N�mero de ports que tendr� el nodo. Como m�ximo est� configurado para 8.
     * @since 1.0
     */    
    public synchronized void setPorts(int num) {
        ports = new TFIFOPortSet(num, this);
    }
    
	/**    
    * Este m�todo no hace nada en un Emisor. En un nodo activo permitir�
    * solicitar a un nodo activo la retransmisi�n de un paquete.
    * @param paquete Paquete cuya retransmisi�n se est� solicitando.
    * @param pSalida Puerto por el que se enviar� la solicitud.
    * @since 1.0
    */    
    public void runGoSPDUStoreAndRetransmitProtocol(TMPLSPDU paquete, int pSalida) {
    }
    
    /**
     * @return the typeICMP
     */
    public int getTypeICMP() {
        return typeICMP;
    }

    /**
     * @param typeICMP the typeICMP to set
     */
    public void setTypeICMP(int typeICMP) {
        this.typeICMP = typeICMP;
    }
    
    /**
     * @return the isICMP
     */
    public boolean getIsICMP() {
        return isICMP;
    }

    /**
     * @param isICMP the isICMP to set
     */
    public void setIsICMP(boolean isICMP) {
        this.isICMP = isICMP;
    }
    
    /**
     * @return the customTTL
     */
    public int getCustomTTL() {
        return customTTL;
    }

    /**
     * @param customTTL the customTTL to set
     */
    public void setCustomTTL(int customTTL) {
        this.customTTL = customTTL;
    }
    
    /**
     * @return the traceroute
     */
    public boolean isTraceroute() {
        return traceroute;
    }

    /**
     * @param traceroute the traceroute to set
     */
    public void setTraceroute(boolean traceroute) {
        this.traceroute = traceroute;
    }
    
    /**
     * @return the saving
     */
    public boolean isSaving() {
        return saving;
    }

    /**
     * @param saving the saving to set
     */
    public void setSaving(boolean saving) {
        this.saving = saving;
    }
    
    private String IPDestino;
    private int tasaTransferencia;
    private int tipoTrafico;
    private boolean isICMP;
    private int typeICMP;
    private int customTTL;
    private boolean encapsularSobreMPLS;
    private int nivelDeGoS;
    private boolean LSPDeBackup;
    private boolean traceroute;
    private int tracerouteTTL;
    private boolean destinationReached;
    private boolean ready;
    
    private Random generadorDeAleatorios;
    private int etiquetaDeEmision;
    private TRotaryIDGenerator gIdGoS;
    private int tamDatosConstante;
    private int tamDatosVariable;

    private TLongIDGenerator gIdent;
    
    private TraceResultSaver traceSaver;
    private boolean saving;
    
    /**
     * Este atributo almacenar� las estad�sticas del nodo.
     * @since 1.0
     */    
    public TSenderStats estadisticas;
    
    /**
     * Esta constante identifica que el tr�fico generado ser� constante.
     * @since 1.0
     */
    public static final int CONSTANTE = 0;
    /**
     * Esta constante identifica que el tr�fico generado ser� variable.
     * @since 1.0
     */
    public static final int VARIABLE = 1;
    
    /**
     * Esta constante indica que la configuraci�n del nodo es correcta.
     * @since 1.0
     */    
    public static final int CORRECTA = 0;
    /**
     * Esta constante indica que falta el nombre del nodo.
     * @since v
     */    
    public static final int SIN_NOMBRE = 1;
    /**
     * Esta constante indica que el nombre del nodo ya existe.
     * @since 1.0
     */    
    public static final int NOMBRE_YA_EXISTE = 2;
    /**
     * Esta constante indica que el nombre del nodo est� formado s�lo por espacios.
     * @since 1.0
     */    
    public static final int SOLO_ESPACIOS = 3;
    /**
     * Esta constante indica que no ha seleccionado un destino para el tr�fico generado
     * por el nodo.
     * @since 1.0
     */    
    public static final int SIN_DESTINO = 4;
    /**
     * IP Address is empty
     * @since 1.0
     */    
    public static final int EMPTY_IP = 5;

    
    
}
