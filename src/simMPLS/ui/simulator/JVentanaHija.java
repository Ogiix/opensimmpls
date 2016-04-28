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
package simMPLS.ui.simulator;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.*;
import simMPLS.hardware.timer.EProgressEventGeneratorOnlyAllowASingleListener;
import simMPLS.hardware.timer.TTimestamp;
import simMPLS.io.osm.TOSMSaver;
import simMPLS.scenario.TExternalLink;
import simMPLS.scenario.TInternalLink;
import simMPLS.scenario.TActiveLERNode;
import simMPLS.scenario.TLERNode;
import simMPLS.scenario.TActiveLSRNode;
import simMPLS.scenario.TLSRNode;
import simMPLS.scenario.TLinkConfig;
import simMPLS.scenario.TReceiverNode;
import simMPLS.scenario.TScenario;
import simMPLS.scenario.TSenderNode;
import simMPLS.scenario.TStats;
import simMPLS.scenario.TTopology;
import simMPLS.scenario.TTopologyElement;
import simMPLS.scenario.TLink;
import simMPLS.scenario.TNode;
import simMPLS.ui.dialogs.JVentanaAdvertencia;
import simMPLS.ui.dialogs.JVentanaBooleana;
import simMPLS.ui.dialogs.JVentanaEmisor;
import simMPLS.ui.dialogs.JVentanaEnlace;
import simMPLS.ui.dialogs.JVentanaError;
import simMPLS.ui.dialogs.JVentanaLER;
import simMPLS.ui.dialogs.JVentanaLERA;
import simMPLS.ui.dialogs.JVentanaLSR;
import simMPLS.ui.dialogs.JVentanaLSRA;
import simMPLS.ui.dialogs.JVentanaReceptor;
import simMPLS.ui.dialogs.JWindowLinkDump;
import simMPLS.ui.utils.TImagesBroker;
import simMPLS.utils.JOSMFilter;
import simMPLS.utils.TProgressEventListener;

/**
 * Esta clase implementa una ventana que save� un escenario completo y dar�
 soporte a la simulaci�n, an�lisis y dise�o de la topology.
 * @author <B>Manuel Dom�nguez Dorado</B><br><A
 * href="mailto:ingeniero@ManoloDominguez.com">ingeniero@ManoloDominguez.com</A><br><A href="http://www.ManoloDominguez.com" target="_blank">http://www.ManoloDominguez.com</A>
 * @version 1.0
 */
public class JVentanaHija extends javax.swing.JInternalFrame {
    
    /** Este m�todo es el constructor de la clase. Crea una nueva instancia de
     * JVentanaHija.
     * @since 1.0
     * @param padre Ventana padre dentro de la cual se va a ubicar este ventana hija.
     * @param di Dispensador de im�genes de donde se obtendr�n todas las im�genes que se
     * necesiten.
     */
    public JVentanaHija(JSimulador padre, TImagesBroker di) {
        dispensadorDeImagenes = di;
        VentanaPadre = padre;
        initComponents();
        initComponents2();
    }
    
    /**
     * Este m�todo es el constructor de la clase. Crea una nueva instancia de
     * JVentanaHija.
     * @since 1.0
     * @param titulo T�tulo que deseamos que tenga la ventana hija. Se usar� tambi�n para save el
 escenario en disco.
     * @param padre Ventana padre dentro de la cual se va a ubicar este ventana hija.
     * @param di Dispensador de im�genes de donde se obtendr�n todas las im�genes que se
     * necesiten.
     */
    public JVentanaHija(JSimulador padre, TImagesBroker di, java.lang.String titulo) {
        dispensadorDeImagenes = di;
        VentanaPadre = padre;
        initComponents();
        initComponents2();
        this.setTitle(titulo);
    }
    
    /**
     * Este m�todo es el constructor de la clase. Crea una nueva instancia de
     * JVentanaHija y la inicializa con los valores de un nodo existente.
     * @param padre Ventana padre dentro de la cual se va a ubicar este ventana hija.
     * @param di Dispensador de im�genes de donde se obtendr�n todas las im�genes que se
     * necesiten.
     * @param esc Escenario ya creado al que se va a asociar esta ventana hija y que contendr� un
     * escenario y todos sus datos.
     * @since 1.0
     */    
    public JVentanaHija(JSimulador padre, TImagesBroker di, TScenario esc) {
        dispensadorDeImagenes = di;
        VentanaPadre = padre;
        initComponents();
        initComponents2();
        escenario = esc;
    }
    
    /** This method ensures start of the class attributes that have not yet been initiated by NetBeans.
     * @since 1.0
     */
    public void initComponents2() {
        panelDisenio.ponerDispensadorDeImagenes(dispensadorDeImagenes);
        panelSimulacion.ponerDispensadorDeImagenes(dispensadorDeImagenes);
        Dimension tamPantalla= VentanaPadre.getSize();
        this.setSize((tamPantalla.width/2), (tamPantalla.height/2));
        Dimension tamFrame=this.getSize();
        this.setLocation((tamPantalla.width-tamFrame.width)/2, (tamPantalla.height-tamFrame.height)/2);
        escenario = new TScenario();
        panelDisenio.ponerTopologia(escenario.getTopology());
        panelSimulacion.ponerTopologia(escenario.getTopology());
        nodoSeleccionado = null;
        elementoDisenioClicDerecho = null;
        aProgresoGeneracion = new TProgressEventListener(barraDeProgreso);
        try {
            escenario.getTopology().obtenerReloj().addProgressEventListener(aProgresoGeneracion);
        } catch (EProgressEventGeneratorOnlyAllowASingleListener e) {
            e.printStackTrace();
        }
        this.mlsPorTic.setValue(1);
        this.pasoNs.setMaximum(duracionMs.getValue()*1000000 + this.duracionNs.getValue());
        this.etiquetaMlsPorTic.setText(this.mlsPorTic.getValue() + java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija.Simulacion.EtiquetaMsTic"));
        this.etiquetaDuracionMs.setText(this.duracionMs.getValue() + java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija._ms."));
        this.etiquetaDuracionNs.setText(this.duracionNs.getValue() + java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija._ns."));
        this.etiquetaPasoNs.setText(this.pasoNs.getValue() + java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija_ns."));
        controlTemporizacionDesactivado = false;
        escenario.ponerPanelSimulacion(this.panelSimulacion);
        panelGrafico1 = null;
        panelGrafico2 = null;
        panelGrafico3 = null;
        panelGrafico4 = null;
        panelGrafico5 = null;
        panelGrafico6 = null;
        grafico1 = null;
        grafico2 = null;
        grafico3 = null;
        grafico4 = null;
        grafico5 = null;
        grafico6 = null;
        this.iconoLERA.setEnabled(false);
        this.iconoLSRA.setEnabled(false);
    }
    
    /** Este m�todo es llamado desde el constructor para actualizar la mayor parte de
     * los atributos de la clase que tienen que ver con la interfaz de usuario. Es un
     * m�todo creado por NetBeans automaticamente.
     * @since 1.0
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        diseElementoPopUp = new javax.swing.JPopupMenu();
        dEliminarMenuItem = new javax.swing.JMenuItem();
        dVerNombreMenuItem = new javax.swing.JCheckBoxMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        dPropiedadesMenuItem = new javax.swing.JMenuItem();
        diseFondoPopUp = new javax.swing.JPopupMenu();
        dVerNombresNodosMenuItem = new javax.swing.JMenuItem();
        dOcultarNombresNodosMenuItem = new javax.swing.JMenuItem();
        dVerNombresEnlacesMenuItem = new javax.swing.JMenuItem();
        dOcultarNombresEnlacesMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        dEliminarTodoMenuItem = new javax.swing.JMenuItem();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        panelDisenioSuperior = new javax.swing.JPanel();
        panelBotonesDisenio = new javax.swing.JPanel();
        iconoEmisor = new javax.swing.JLabel();
        iconoReceptor = new javax.swing.JLabel();
        iconoLER = new javax.swing.JLabel();
        iconoLERA = new javax.swing.JLabel();
        iconoLSR = new javax.swing.JLabel();
        iconoLSRA = new javax.swing.JLabel();
        iconoEnlace = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        panelDisenio = new simMPLS.ui.simulator.JPanelDisenio();
        panelSimulacionSuperior = new javax.swing.JPanel();
        panelBotonesSimulacion = new javax.swing.JPanel();
        iconoComenzar = new javax.swing.JLabel();
        iconoFinalizar = new javax.swing.JLabel();
        iconoReanudar = new javax.swing.JLabel();
        iconoPausar = new javax.swing.JLabel();
        barraDeProgreso = new javax.swing.JProgressBar();
        mlsPorTic = new javax.swing.JSlider();
        etiquetaMlsPorTic = new javax.swing.JLabel();
        crearTraza = new javax.swing.JCheckBox();
        jScrollPane2 = new javax.swing.JScrollPane();
        panelSimulacion = new simMPLS.ui.simulator.JSimulationPanel();
        panelAnalisisSuperior = new javax.swing.JPanel();
        panelSeleccionElemento = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        selectorElementoEstadisticas = new javax.swing.JComboBox();
        jScrollPane4 = new javax.swing.JScrollPane();
        panelAnalisis = new javax.swing.JPanel();
        panelFijo = new javax.swing.JPanel();
        etiquetaEstadisticasTituloEscenario = new javax.swing.JLabel();
        etiquetaEstadisticasNombreAutor = new javax.swing.JLabel();
        areaEstadisticasDescripcion = new javax.swing.JTextArea();
        etiquetaNombreElementoEstadistica = new javax.swing.JLabel();
        panelOpcionesSuperior = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        panelOpciones = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        nombreEscenario = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        nombreAutor = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        descripcionEscenario = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        duracionMs = new javax.swing.JSlider();
        etiquetaDuracionMs = new javax.swing.JLabel();
        duracionNs = new javax.swing.JSlider();
        etiquetaDuracionNs = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        pasoNs = new javax.swing.JSlider();
        etiquetaPasoNs = new javax.swing.JLabel();

        diseElementoPopUp.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N

        dEliminarMenuItem.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        dEliminarMenuItem.setMnemonic(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija.PopUpDisenio.mne.Delete").charAt(0));
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes"); // NOI18N
        dEliminarMenuItem.setText(bundle.getString("VentanaHija.PopUpDisenio.Delete")); // NOI18N
        dEliminarMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnPopUpDisenioEliminar(evt);
            }
        });
        diseElementoPopUp.add(dEliminarMenuItem);

        dVerNombreMenuItem.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        dVerNombreMenuItem.setMnemonic(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija.PopUpDisenio.mne.verNombre").charAt(0));
        dVerNombreMenuItem.setText(bundle.getString("VentanaHija.PopUpDisenio.verNombre")); // NOI18N
        dVerNombreMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnPopUpDisenioVerNombre(evt);
            }
        });
        diseElementoPopUp.add(dVerNombreMenuItem);
        diseElementoPopUp.add(jSeparator1);

        dPropiedadesMenuItem.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        dPropiedadesMenuItem.setMnemonic(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija.PopUpDisenio.mne.Propiedades").charAt(0));
        dPropiedadesMenuItem.setText(bundle.getString("VentanaHija.PopUpDisenio.Propiedades")); // NOI18N
        dPropiedadesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnPropiedadesPopUpDisenioElemento(evt);
            }
        });
        diseElementoPopUp.add(dPropiedadesMenuItem);

        diseFondoPopUp.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N

        dVerNombresNodosMenuItem.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        dVerNombresNodosMenuItem.setMnemonic(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("popUpDisenioFondo.mne.verTodosNodos").charAt(0));
        dVerNombresNodosMenuItem.setText(bundle.getString("popUpDisenioFondo.verTodosNodos")); // NOI18N
        dVerNombresNodosMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnPopUpDisenioFondoVerNombreNodos(evt);
            }
        });
        diseFondoPopUp.add(dVerNombresNodosMenuItem);

        dOcultarNombresNodosMenuItem.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        dOcultarNombresNodosMenuItem.setMnemonic(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("popUpDisenioFondo.mne.ocultarTodosNodos").charAt(0));
        dOcultarNombresNodosMenuItem.setText(bundle.getString("popUpDisenioFondo.ocultarTodosNodos")); // NOI18N
        dOcultarNombresNodosMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnPopUpDisenioFondoOcultarNombreNodos(evt);
            }
        });
        diseFondoPopUp.add(dOcultarNombresNodosMenuItem);

        dVerNombresEnlacesMenuItem.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        dVerNombresEnlacesMenuItem.setMnemonic(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("popUpDisenioFondo.mne.verTodosEnlaces").charAt(0));
        dVerNombresEnlacesMenuItem.setText(bundle.getString("popUpDisenioFondo.verTodosEnlaces")); // NOI18N
        dVerNombresEnlacesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnPopUpDisenioFondoVerNombreEnlaces(evt);
            }
        });
        diseFondoPopUp.add(dVerNombresEnlacesMenuItem);

        dOcultarNombresEnlacesMenuItem.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        dOcultarNombresEnlacesMenuItem.setMnemonic(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("popUpDisenioFondo.mne.ocultarTodosEnlaces").charAt(0));
        dOcultarNombresEnlacesMenuItem.setText(bundle.getString("popUpDisenioFondo.ocultarTodosEnlaces")); // NOI18N
        dOcultarNombresEnlacesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnPopUpDisenioFondoOcultarNombreEnlaces(evt);
            }
        });
        diseFondoPopUp.add(dOcultarNombresEnlacesMenuItem);
        diseFondoPopUp.add(jSeparator2);

        dEliminarTodoMenuItem.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        dEliminarTodoMenuItem.setMnemonic(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("popUpDisenioFondo.mne.eliminarTodo").charAt(0));
        dEliminarTodoMenuItem.setText(bundle.getString("popUpDisenioFondo.borrarTodo")); // NOI18N
        dEliminarTodoMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnPopUpDisenioFondoEliminar(evt);
            }
        });
        diseFondoPopUp.add(dEliminarTodoMenuItem);

        setIconifiable(true);
        setMaximizable(true);
        setResizable(true);
        setTitle(bundle.getString("VentanaHija.Titulo")); // NOI18N
        setAutoscrolls(true);
        setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        setFrameIcon(dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.ICONO_VENTANA_INTERNA_MENU));
        setNormalBounds(new java.awt.Rectangle(10, 10, 100, 100));
        setPreferredSize(new java.awt.Dimension(100, 100));
        setVisible(true);

        jTabbedPane1.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        jTabbedPane1.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N

        panelDisenioSuperior.setLayout(new java.awt.BorderLayout());

        panelBotonesDisenio.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panelBotonesDisenio.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        iconoEmisor.setIcon(dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.EMISOR_MENU));
        iconoEmisor.setToolTipText(bundle.getString("VentanaHija.Topic.Emisor")); // NOI18N
        iconoEmisor.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ratonEntraEnIconoEmisor(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ratonSaleDeIconoEmisor(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                clicEnAniadirEmisorDeTrafico(evt);
            }
        });
        panelBotonesDisenio.add(iconoEmisor);

        iconoReceptor.setIcon(dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.RECEPTOR_MENU));
        iconoReceptor.setToolTipText(bundle.getString("VentanaHija.Topic.Receptor")); // NOI18N
        iconoReceptor.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ratonEntraEnIconoReceptor(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ratonSaleDeIconoReceptor(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                clicEnAniadirReceptor(evt);
            }
        });
        panelBotonesDisenio.add(iconoReceptor);

        iconoLER.setIcon(dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.LER_MENU));
        iconoLER.setToolTipText(bundle.getString("VentanaHija.Topic.LER")); // NOI18N
        iconoLER.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ratonEntraEnIconoLER(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ratonSaleDeIconoLER(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                clicEnAniadirLER(evt);
            }
        });
        panelBotonesDisenio.add(iconoLER);

        iconoLERA.setIcon(dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.LERA_MENU));
        iconoLERA.setToolTipText(bundle.getString("VentanaHija.Topic.LERActivo")); // NOI18N
        iconoLERA.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ratonEntraEnIconoLERA(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ratonSaleDeIconoLERA(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                clicEnAniadirLERA(evt);
            }
        });
        panelBotonesDisenio.add(iconoLERA);

        iconoLSR.setIcon(dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.LSR_MENU));
        iconoLSR.setToolTipText(bundle.getString("VentanaHija.Topic.LSR")); // NOI18N
        iconoLSR.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ratonEntraEnIconoLSR(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ratonSaleDeIconoLSR(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                clicEnAniadirLSR(evt);
            }
        });
        panelBotonesDisenio.add(iconoLSR);

        iconoLSRA.setIcon(dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.LSRA_MENU));
        iconoLSRA.setToolTipText(bundle.getString("VentanaHija.Topic.LSRActivo")); // NOI18N
        iconoLSRA.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ratonEntraEnIconoLSRA(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ratonSaleDeIconoLSRA(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                clicEnAniadirLSRA(evt);
            }
        });
        panelBotonesDisenio.add(iconoLSRA);

        iconoEnlace.setIcon(dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.ENLACE_MENU));
        iconoEnlace.setToolTipText(bundle.getString("VentanaHija.Topic.Enlace")); // NOI18N
        iconoEnlace.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                clicEnAniadirEnlace(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ratonEntraEnIconoEnlace(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ratonSaleDeIconoEnlace(evt);
            }
        });
        panelBotonesDisenio.add(iconoEnlace);

        panelDisenioSuperior.add(panelBotonesDisenio, java.awt.BorderLayout.NORTH);

        jScrollPane1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        panelDisenio.setBackground(java.awt.Color.white);
        panelDisenio.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panelDisenio.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                clicDerechoEnPanelDisenio(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                clicEnPanelDisenio(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                clicSoltadoEnPanelDisenio(evt);
            }
        });
        panelDisenio.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                arrastrandoEnPanelDisenio(evt);
            }
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                ratonSobrePanelDisenio(evt);
            }
        });
        panelDisenio.setLayout(null);
        jScrollPane1.setViewportView(panelDisenio);

        panelDisenioSuperior.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab(bundle.getString("VentanaHija.Tab.Disenio"), dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.DISENIO), panelDisenioSuperior, bundle.getString("VentanaHija.A_panel_to_design_network_topology")); // NOI18N

        panelSimulacionSuperior.setLayout(new java.awt.BorderLayout());

        panelBotonesSimulacion.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panelBotonesSimulacion.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        iconoComenzar.setIcon(dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.BOTON_GENERAR));
        iconoComenzar.setToolTipText(bundle.getString("VentanaHija.Topic.Generar")); // NOI18N
        iconoComenzar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ratonEntraEnIconoComenzar(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ratonSaleDelIconoComenzar(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                clicEnComenzar(evt);
            }
        });
        panelBotonesSimulacion.add(iconoComenzar);

        iconoFinalizar.setIcon(dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.BOTON_PARAR));
        iconoFinalizar.setToolTipText(bundle.getString("VentanaHija.Topic.Finalizar")); // NOI18N
        iconoFinalizar.setEnabled(false);
        iconoFinalizar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ratonEntraEnIconoFinalizar(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ratonSaleDelIconoFinalizar(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                clicEnFinalizar(evt);
            }
        });
        panelBotonesSimulacion.add(iconoFinalizar);

        iconoReanudar.setIcon(dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.BOTON_COMENZAR));
        iconoReanudar.setToolTipText(bundle.getString("VentanaHija.Topic.Simulacion")); // NOI18N
        iconoReanudar.setEnabled(false);
        iconoReanudar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ratonEntraEnIconoReanudar(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ratonSaleDelIconoReanudar(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                clicEnReanudar(evt);
            }
        });
        panelBotonesSimulacion.add(iconoReanudar);

        iconoPausar.setIcon(dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.BOTON_PAUSA));
        iconoPausar.setToolTipText(bundle.getString("VentanaHija.Topic.Detener")); // NOI18N
        iconoPausar.setEnabled(false);
        iconoPausar.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ratonEntraEnIconoPausar(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ratonSaleDelIconoPausar(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                clicAlPausar(evt);
            }
        });
        panelBotonesSimulacion.add(iconoPausar);

        barraDeProgreso.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        barraDeProgreso.setToolTipText(bundle.getString("VentanaHija.BarraProgreso.tooltip")); // NOI18N
        barraDeProgreso.setStringPainted(true);
        panelBotonesSimulacion.add(barraDeProgreso);

        mlsPorTic.setMajorTickSpacing(10);
        mlsPorTic.setMaximum(500);
        mlsPorTic.setMinimum(1);
        mlsPorTic.setMinorTickSpacing(1);
        mlsPorTic.setSnapToTicks(true);
        mlsPorTic.setToolTipText(bundle.getString("VentanaHija.Simulacion.SelectorDeVelocidad.tooltip")); // NOI18N
        mlsPorTic.setPreferredSize(new java.awt.Dimension(100, 20));
        mlsPorTic.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                mlsPorTicCambiado(evt);
            }
        });
        panelBotonesSimulacion.add(mlsPorTic);

        etiquetaMlsPorTic.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        etiquetaMlsPorTic.setForeground(new java.awt.Color(102, 102, 102));
        panelBotonesSimulacion.add(etiquetaMlsPorTic);

        crearTraza.setText(bundle.getString("JVentanaHija.Create_trace_file")); // NOI18N
        panelBotonesSimulacion.add(crearTraza);

        panelSimulacionSuperior.add(panelBotonesSimulacion, java.awt.BorderLayout.NORTH);

        jScrollPane2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        panelSimulacion.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panelSimulacion.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ratonPulsadoYSoltadoEnPanelSimulacion(evt);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                clicEnPanelSimulacion(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                ratonSoltadoEnPanelSimulacion(evt);
            }
        });
        panelSimulacion.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                ratonArrastradoEnPanelSimulacion(evt);
            }
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                ratonSobrePanelSimulacion(evt);
            }
        });
        jScrollPane2.setViewportView(panelSimulacion);

        panelSimulacionSuperior.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab(bundle.getString("VentanaHija.Tab.Simulacion"), dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.SIMULACION), panelSimulacionSuperior, bundle.getString("VentanaHija.A_panel_to_generate_and_play_simulation.")); // NOI18N

        panelAnalisisSuperior.setLayout(new java.awt.BorderLayout());

        panelSeleccionElemento.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        panelSeleccionElemento.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel1.setText(bundle.getString("JVentanaHija.SelcUnElemParaVerDatos")); // NOI18N
        panelSeleccionElemento.add(jLabel1);

        selectorElementoEstadisticas.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "" }));
        selectorElementoEstadisticas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnSeleccionalElementoEstadistica(evt);
            }
        });
        panelSeleccionElemento.add(selectorElementoEstadisticas);

        panelAnalisisSuperior.add(panelSeleccionElemento, java.awt.BorderLayout.NORTH);

        jScrollPane4.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));

        panelAnalisis.setBackground(new java.awt.Color(252, 246, 226));
        panelAnalisis.setLayout(new java.awt.GridBagLayout());

        panelFijo.setBackground(new java.awt.Color(252, 246, 226));
        panelFijo.setLayout(new java.awt.GridBagLayout());

        etiquetaEstadisticasTituloEscenario.setBackground(new java.awt.Color(252, 246, 226));
        etiquetaEstadisticasTituloEscenario.setFont(new java.awt.Font("Arial", 1, 18)); // NOI18N
        etiquetaEstadisticasTituloEscenario.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        etiquetaEstadisticasTituloEscenario.setText(bundle.getString("JVentanaHija.TituloDelEscenario")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        panelFijo.add(etiquetaEstadisticasTituloEscenario, gridBagConstraints);

        etiquetaEstadisticasNombreAutor.setBackground(new java.awt.Color(252, 246, 226));
        etiquetaEstadisticasNombreAutor.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        etiquetaEstadisticasNombreAutor.setForeground(new java.awt.Color(102, 0, 51));
        etiquetaEstadisticasNombreAutor.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        etiquetaEstadisticasNombreAutor.setText(bundle.getString("JVentanaHija.AutorDelEscenario")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        panelFijo.add(etiquetaEstadisticasNombreAutor, gridBagConstraints);

        areaEstadisticasDescripcion.setBackground(new java.awt.Color(252, 246, 226));
        areaEstadisticasDescripcion.setEditable(false);
        areaEstadisticasDescripcion.setFont(new java.awt.Font("MonoSpaced", 0, 11)); // NOI18N
        areaEstadisticasDescripcion.setLineWrap(true);
        areaEstadisticasDescripcion.setRows(3);
        areaEstadisticasDescripcion.setText(bundle.getString("JVentanaHija.DescripcionDelEscenario")); // NOI18N
        areaEstadisticasDescripcion.setWrapStyleWord(true);
        areaEstadisticasDescripcion.setMinimumSize(new java.awt.Dimension(500, 16));
        areaEstadisticasDescripcion.setPreferredSize(new java.awt.Dimension(500, 48));
        areaEstadisticasDescripcion.setAutoscrolls(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        panelFijo.add(areaEstadisticasDescripcion, gridBagConstraints);

        etiquetaNombreElementoEstadistica.setBackground(new java.awt.Color(252, 246, 226));
        etiquetaNombreElementoEstadistica.setFont(new java.awt.Font("Arial", 1, 14)); // NOI18N
        etiquetaNombreElementoEstadistica.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        etiquetaNombreElementoEstadistica.setText(bundle.getString("JVentanaHija.SeleccioneNodoAInspeccionar")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        panelFijo.add(etiquetaNombreElementoEstadistica, gridBagConstraints);

        panelAnalisis.add(panelFijo, new java.awt.GridBagConstraints());

        jScrollPane4.setViewportView(panelAnalisis);

        panelAnalisisSuperior.add(jScrollPane4, java.awt.BorderLayout.CENTER);

        jTabbedPane1.addTab(bundle.getString("JVentanaHija.Analisis"), dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.ANALISIS), panelAnalisisSuperior, bundle.getString("JVentanaHija.Analisis.Tooltip")); // NOI18N

        panelOpcionesSuperior.setLayout(new java.awt.BorderLayout());

        jScrollPane3.setBorder(null);

        panelOpciones.setPreferredSize(new java.awt.Dimension(380, 230));
        panelOpciones.setLayout(new java.awt.GridBagLayout());

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, bundle.getString("VentanaHija.GParameters"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 12))); // NOI18N
        jPanel3.setLayout(new java.awt.GridBagLayout());

        jLabel5.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel5.setText(bundle.getString("VentanaHija.Scene_title")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel3.add(jLabel5, gridBagConstraints);

        nombreEscenario.setToolTipText(bundle.getString("VentanaHija.Type_a__title_of_the_scene")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 200.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel3.add(nombreEscenario, gridBagConstraints);

        jLabel6.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel6.setText(bundle.getString("VentanaHija.Scene_author")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel3.add(jLabel6, gridBagConstraints);

        nombreAutor.setToolTipText(bundle.getString("VentanaHija.Type_de_name_of_the_author")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 200.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel3.add(nombreAutor, gridBagConstraints);

        jLabel7.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel7.setText(bundle.getString("VentanaHija.Description")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel3.add(jLabel7, gridBagConstraints);

        descripcionEscenario.setToolTipText(bundle.getString("VentanaHija.Enter_a_short_description.")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 200.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel3.add(descripcionEscenario, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 350.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panelOpciones.add(jPanel3, gridBagConstraints);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, bundle.getString("VentanaHija.TParameters"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Dialog", 0, 12))); // NOI18N
        jPanel2.setLayout(new java.awt.GridBagLayout());

        jLabel3.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText(bundle.getString("VentanaHija.Duration")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 100.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel2.add(jLabel3, gridBagConstraints);

        duracionMs.setMajorTickSpacing(10);
        duracionMs.setMaximum(10);
        duracionMs.setMinorTickSpacing(1);
        duracionMs.setToolTipText(bundle.getString("VentanaHija.Slide_it_to_change_the_ms._component_of_simulation_duration.")); // NOI18N
        duracionMs.setValue(0);
        duracionMs.setMaximumSize(new java.awt.Dimension(30, 20));
        duracionMs.setMinimumSize(new java.awt.Dimension(30, 24));
        duracionMs.setPreferredSize(new java.awt.Dimension(30, 20));
        duracionMs.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                clicEnDuracionMs(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 150.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel2.add(duracionMs, gridBagConstraints);

        etiquetaDuracionMs.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        etiquetaDuracionMs.setForeground(new java.awt.Color(102, 102, 102));
        etiquetaDuracionMs.setText(bundle.getString("VentanaHija.ms.")); // NOI18N
        etiquetaDuracionMs.setMaximumSize(new java.awt.Dimension(30, 14));
        etiquetaDuracionMs.setMinimumSize(new java.awt.Dimension(30, 14));
        etiquetaDuracionMs.setPreferredSize(new java.awt.Dimension(30, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 40.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel2.add(etiquetaDuracionMs, gridBagConstraints);

        duracionNs.setMajorTickSpacing(1000);
        duracionNs.setMaximum(999999);
        duracionNs.setMinorTickSpacing(100);
        duracionNs.setToolTipText(bundle.getString("VentanaHija.Slide_it_to_change_the_ns._component_of_simulation_duration.")); // NOI18N
        duracionNs.setValue(100000);
        duracionNs.setMaximumSize(new java.awt.Dimension(32767, 20));
        duracionNs.setMinimumSize(new java.awt.Dimension(36, 20));
        duracionNs.setPreferredSize(new java.awt.Dimension(200, 20));
        duracionNs.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                clicEnDuracionNs(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 150.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel2.add(duracionNs, gridBagConstraints);

        etiquetaDuracionNs.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        etiquetaDuracionNs.setForeground(new java.awt.Color(102, 102, 102));
        etiquetaDuracionNs.setText(bundle.getString("VentanaHija.ns.")); // NOI18N
        etiquetaDuracionNs.setMaximumSize(new java.awt.Dimension(40, 14));
        etiquetaDuracionNs.setMinimumSize(new java.awt.Dimension(40, 14));
        etiquetaDuracionNs.setPreferredSize(new java.awt.Dimension(40, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 100.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel2.add(etiquetaDuracionNs, gridBagConstraints);

        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel4.setText(bundle.getString("VentanaHija.Step")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 100.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel2.add(jLabel4, gridBagConstraints);

        pasoNs.setMajorTickSpacing(1000);
        pasoNs.setMaximum(999999);
        pasoNs.setMinimum(1);
        pasoNs.setMinorTickSpacing(100);
        pasoNs.setToolTipText(bundle.getString("VentanaHija.Slide_it_to_change_the_step_duration_(ns)..")); // NOI18N
        pasoNs.setValue(10000);
        pasoNs.setMaximumSize(new java.awt.Dimension(32767, 20));
        pasoNs.setPreferredSize(new java.awt.Dimension(100, 20));
        pasoNs.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                clicEnPasoNs(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel2.add(pasoNs, gridBagConstraints);

        etiquetaPasoNs.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        etiquetaPasoNs.setForeground(new java.awt.Color(102, 102, 102));
        etiquetaPasoNs.setText(bundle.getString("VentanaHija.ns.")); // NOI18N
        etiquetaPasoNs.setMaximumSize(new java.awt.Dimension(40, 14));
        etiquetaPasoNs.setMinimumSize(new java.awt.Dimension(40, 14));
        etiquetaPasoNs.setPreferredSize(new java.awt.Dimension(40, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 100.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel2.add(etiquetaPasoNs, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 350.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        panelOpciones.add(jPanel2, gridBagConstraints);

        jScrollPane3.setViewportView(panelOpciones);

        panelOpcionesSuperior.add(jScrollPane3, java.awt.BorderLayout.NORTH);

        jTabbedPane1.addTab(bundle.getString("VentanaHija.Options"), dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.OPCIONES), panelOpcionesSuperior, bundle.getString("VentanaHija.Options_about_the_scene")); // NOI18N

        getContentPane().add(jTabbedPane1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /** Action when the mouse button is pressed and released
     * @since 1.0
     */
    private void ratonPulsadoYSoltadoEnPanelSimulacion(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonPulsadoYSoltadoEnPanelSimulacion
        if (evt.getButton() == MouseEvent.BUTTON1) {
            TTopologyElement et = escenario.getTopology().obtenerElementoEnPosicion(evt.getPoint());
            if (et != null) {
                if (et.getElementType() == TTopologyElement.NODO) {
                    TNode nt = (TNode) et;
                    nt.toCongest();
                } else if (et.getElementType() == TTopologyElement.LINK) {
//                    TLink ent = (TLink) et;
//                    if (ent.isBroken()) {
//                        ent.ponerEnlaceCaido(false);
//                    } else {
//                        ent.ponerEnlaceCaido(true);
//                    }
                }
            } else {
                    if (this.panelSimulacion.obtenerMostrarLeyenda()) {
                        this.panelSimulacion.ponerMostrarLeyenda(false);
                    } else {
                        this.panelSimulacion.ponerMostrarLeyenda(true);
                    }
            }
        }else if(evt.getButton() == MouseEvent.BUTTON3){
            TTopologyElement et = escenario.getTopology().obtenerElementoEnPosicion(evt.getPoint());
            if(et != null){
                if(et.getElementType() == TTopologyElement.LINK){
                    TLink ent = (TLink) et;
                    if (!ent.isBroken()) {
                        JWindowLinkDump linkWindow = new JWindowLinkDump(VentanaPadre, true, ent);
                        linkWindow.setVisible(true);
                    }
                }
            }
        } else {
            elementoDisenioClicDerecho = null;
            panelDisenio.repaint();
        }
    }//GEN-LAST:event_ratonPulsadoYSoltadoEnPanelSimulacion

    /** Action when selecting static element.
     * @since 1.0
     */
    private void clicEnSeleccionalElementoEstadistica(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnSeleccionalElementoEstadistica
        GridBagConstraints gbc = null;
        if (this.selectorElementoEstadisticas.getSelectedIndex() == 0) {
            this.panelAnalisis.removeAll();
            grafico1 = null;
            grafico2 = null;
            grafico3 = null;
            grafico4 = null;
            grafico5 = null;
            grafico6 = null;
            panelGrafico1 = null;
            panelGrafico2 = null;
            panelGrafico3 = null;
            panelGrafico4 = null;
            panelGrafico5 = null;
            panelGrafico6 = null;
            this.etiquetaEstadisticasTituloEscenario.setText(this.nombreEscenario.getText());
            this.etiquetaEstadisticasNombreAutor.setText(this.nombreAutor.getText());
            this.areaEstadisticasDescripcion.setText(this.descripcionEscenario.getText());
            this.etiquetaNombreElementoEstadistica.setIcon(null);
            this.etiquetaNombreElementoEstadistica.setText(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.SeleccioneElNodoAInspeccionar"));
            gbc = new java.awt.GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(10, 10, 10, 5);
            gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gbc.anchor = java.awt.GridBagConstraints.NORTH;
            this.panelFijo.add(this.etiquetaEstadisticasTituloEscenario, gbc);
            gbc = new java.awt.GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.insets = new Insets(10, 5, 10, 5);
            gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gbc.anchor = java.awt.GridBagConstraints.NORTH;
            this.panelFijo.add(this.etiquetaEstadisticasNombreAutor, gbc);
            gbc = new java.awt.GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.insets = new Insets(10, 5, 10, 5);
            gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gbc.anchor = java.awt.GridBagConstraints.NORTH;
            this.panelFijo.add(this.areaEstadisticasDescripcion,gbc);
            gbc = new java.awt.GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.insets = new Insets(10, 5, 10, 10);
            gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
            gbc.anchor = java.awt.GridBagConstraints.NORTH;
            this.panelFijo.add(this.etiquetaNombreElementoEstadistica, gbc);
            gbc = new java.awt.GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(10, 10, 10, 5);
            gbc.anchor = java.awt.GridBagConstraints.NORTH;
            gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
            this.panelAnalisis.add(this.panelFijo, gbc);
            this.panelAnalisis.repaint();
        } else {
            String nombreEltoSeleccionado = (String) this.selectorElementoEstadisticas.getSelectedItem();
            this.crearEInsertarGraficas(nombreEltoSeleccionado);
        }
    }//GEN-LAST:event_clicEnSeleccionalElementoEstadistica

    /**
     * Este m�todo se llama cuando se arrastra el rat�n sobre el panel de dise�o. Si se
     * hace sobre un elemento que estaba seleccionado, el resultado es que ese elemento
     * se mueve donde vaya el cursor del rat�n.
     * Move an object of the simulation
     * @param evt El evento que provoca la llamada.
     * @since 1.0
     */    
    private void ratonArrastradoEnPanelSimulacion(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonArrastradoEnPanelSimulacion
    if (evt.getModifiersEx() == java.awt.event.InputEvent.BUTTON1_DOWN_MASK) {
            if (nodoSeleccionado != null) {
                TTopology topo = escenario.getTopology();
                Point p2 = evt.getPoint();
                if (p2.x < 0)
                    p2.x = 0;
                if (p2.x > panelDisenio.getSize().width)
                    p2.x = panelDisenio.getSize().width;
                if (p2.y < 0)
                    p2.y = 0;
                if (p2.y > panelDisenio.getSize().height)
                    p2.y = panelDisenio.getSize().height;
                nodoSeleccionado.setPosition(new Point(p2.x, p2.y));
                panelSimulacion.repaint();
                this.escenario.setModified(true);
            }
        }
    }//GEN-LAST:event_ratonArrastradoEnPanelSimulacion

    /**
     * Este m�todo se llama cuando se libera un bot�n del rat�n estando en el panel de
     * simulaci�n. Si se hace sobre un elemento que estaba seleccionado, deja de
     * estarlo.
     * When the mouse button is released on a node
     * @param evt El evento que genera la llamada.
     * @since 1.0
     */    
    private void ratonSoltadoEnPanelSimulacion(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonSoltadoEnPanelSimulacion
        if (evt.getButton() == MouseEvent.BUTTON1) {
            if (nodoSeleccionado != null) {
                nodoSeleccionado.setStatus(TNode.DESELECCIONADO);
                nodoSeleccionado = null;
                this.escenario.setModified(true);
            }
            panelSimulacion.repaint();
        }
    }//GEN-LAST:event_ratonSoltadoEnPanelSimulacion

    /**
     * Este m�todo se llama cuando se presiona un bot�n del rat�n en el panel de
     * simulaci�n. Si se hace sobre un elemento de la topolog�a, lo marca como
     * seleccionado.
     * When the mouse button is pressed on a node
     * @since 1.0
     * @param evt El evento que provoca la llamada.
     */    
    private void clicEnPanelSimulacion(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clicEnPanelSimulacion
        if (evt.getButton() == MouseEvent.BUTTON1) {
            TTopology topo = escenario.getTopology();
            TTopologyElement et = topo.obtenerElementoEnPosicion(evt.getPoint());
            if (et != null) {
                this.setCursor(new Cursor(Cursor.HAND_CURSOR));
                if (et.getElementType() == TTopologyElement.NODO) {
                    TNode nt = (TNode) et;
                    nodoSeleccionado = nt;
                    if (nodoSeleccionado != null) {
                        nodoSeleccionado.setStatus(TNode.SELECCIONADO);
                        this.escenario.setModified(true);
                    }
                }
            } else {
                this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                this.setToolTipText(null);
            }
            panelSimulacion.repaint();
        }       
    }//GEN-LAST:event_clicEnPanelSimulacion
    
    /**
     * Este m�todo se llama cuando se hace clic derecho sobre un elemento en la ventana
     * de dise�o y se selecciona la opci�n "Propiedades" del men� emergente. Se encarga
     * de mostrar en pantalla la ventana de configuraci�n del elemento en cuesti�n.
     * Right clic on a node + properties menu
     * @since 1.0
     * @param evt El evento que provoca la llamada.
     */    
    private void clicEnPropiedadesPopUpDisenioElemento(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnPropiedadesPopUpDisenioElemento
        if (elementoDisenioClicDerecho != null) {
            if (elementoDisenioClicDerecho.getElementType() == TTopologyElement.NODO) {
                TNode nt = (TNode) elementoDisenioClicDerecho;
                if (nt.getNodeType() == TNode.SENDER) {
                    JVentanaEmisor ve = new JVentanaEmisor(escenario.getTopology(), panelDisenio, dispensadorDeImagenes, VentanaPadre, true);
                    ve.ponerConfiguracion((TSenderNode) nt, true);
                    ve.show();
                } else if (nt.getNodeType() == TNode.LER) {
                    JVentanaLER vler = new JVentanaLER(escenario.getTopology(), panelDisenio, dispensadorDeImagenes, VentanaPadre, true);
                    vler.ponerConfiguracion((TLERNode) nt, true);
                    vler.show();
                } else if (nt.getNodeType() == TNode.LERA) {
                    JVentanaLERA vlera = new JVentanaLERA(escenario.getTopology(), panelDisenio, dispensadorDeImagenes, VentanaPadre, true);
                    vlera.ponerConfiguracion((TActiveLERNode) nt, true);
                    vlera.show();
                } else if (nt.getNodeType() == TNode.LSR) {
                    JVentanaLSR vlsr = new JVentanaLSR(escenario.getTopology(), panelDisenio, dispensadorDeImagenes, VentanaPadre, true);
                    vlsr.ponerConfiguracion((TLSRNode) nt, true);
                    vlsr.show();
                } else if (nt.getNodeType() == TNode.LSRA) {
                    JVentanaLSRA vlsra = new JVentanaLSRA(escenario.getTopology(), panelDisenio, dispensadorDeImagenes, VentanaPadre, true);
                    vlsra.ponerConfiguracion((TActiveLSRNode) nt, true);
                    vlsra.show();
                } else if (nt.getNodeType() == TNode.RECEIVER) {
                    JVentanaReceptor vr = new JVentanaReceptor(escenario.getTopology(), panelDisenio, dispensadorDeImagenes, VentanaPadre, true);
                    vr.ponerConfiguracion((TReceiverNode) nt, true);
                    vr.show();
                }
                elementoDisenioClicDerecho = null;
                panelDisenio.repaint();
            } else {
                TLink ent = (TLink) elementoDisenioClicDerecho;
                TLinkConfig tceAux = ent.obtenerConfiguracion();
                JVentanaEnlace ve = new JVentanaEnlace(escenario.getTopology(), dispensadorDeImagenes, VentanaPadre, true);
                ve.ponerConfiguracion(tceAux, true);
                ve.show();
                if (ent.getLinkType() == TLink.EXTERNAL) {
                    TExternalLink ext = (TExternalLink) ent;
                    ext.configurar(tceAux, this.escenario.getTopology(), true);
                } else if (ent.getLinkType() == TLink.INTERNAL) {
                    TInternalLink inte = (TInternalLink) ent;
                    inte.configurar(tceAux, this.escenario.getTopology(), true);
                }
                elementoDisenioClicDerecho = null;
                panelDisenio.repaint();
                int minimoDelay = this.escenario.getTopology().obtenerMinimoDelay();
                int pasoActual = this.pasoNs.getValue();
                if (pasoActual > minimoDelay) {
                    this.pasoNs.setValue(minimoDelay);
                }
            }
            this.escenario.setModified(true);
        }
    }//GEN-LAST:event_clicEnPropiedadesPopUpDisenioElemento
    
    /** Este m�todo se encarga de controlar que la duraci�n de la simulaci�on y del paso
     * de la misma sea acorde con los delays de los enlaces. Adem�s se encarga de la
     * actualizaci�n de la interfaz en esos lugares.
     * Timing controls
     * @since 1.0
     */
    public void controlarParametrosTemporales() {
        if (!controlTemporizacionDesactivado) {
            if (duracionMs.getValue() == 0) {
                duracionNs.setMinimum(1);
            } else {
                duracionNs.setMinimum(0);
            }
            int duracionTotal = duracionMs.getValue()*1000000 + this.duracionNs.getValue();
            int minDelay = escenario.getTopology().obtenerMinimoDelay();
            if (minDelay < duracionTotal) {
                pasoNs.setMaximum(minDelay);
            } else {
                pasoNs.setMaximum(duracionTotal);
            }
            this.etiquetaDuracionMs.setText(this.duracionMs.getValue() + java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija._ms."));
            this.etiquetaDuracionNs.setText(this.duracionNs.getValue() + java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija._ns."));
            this.etiquetaPasoNs.setText(this.pasoNs.getValue() + java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija._ns."));
            escenario.getSimulation().setDuration(new TTimestamp(duracionMs.getValue(), duracionNs.getValue()).getTotalAsNanoseconds());
            escenario.getSimulation().setStep(pasoNs.getValue());
        }
    }
    
    /** Este m�todo se llama autom�ticamente cuando se cambia la duraci�n en
     * nanosegundos del paso de simulaci�n.
     * Changing the timing of the simulation
     * @since 1.0
     * @param evt Evento que hace que el m�todo salte.
     */
private void clicEnPasoNs(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_clicEnPasoNs
    controlarParametrosTemporales();
    this.escenario.setModified(true);
}//GEN-LAST:event_clicEnPasoNs

/** Este m�todo se llama autom�ticamente cuando se cambia la duraci�n de la
 * simulaci�n en nanosegundos.
 * @since 1.0
 * @param evt Evento que hace que se ejecute este m�todo.
 */
private void clicEnDuracionNs(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_clicEnDuracionNs
    controlarParametrosTemporales();
    this.escenario.setModified(true);
}//GEN-LAST:event_clicEnDuracionNs

/** Este m�todo se llama autom�ticamente cuando se cambia la duraci�n de la
 * simulaci�n en milisegundos.
 * @since 1.0
 * @param evt Evento que produce que se ejecute este m�todo.
 */
private void clicEnDuracionMs(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_clicEnDuracionMs
    controlarParametrosTemporales();
    this.escenario.setModified(true);
}//GEN-LAST:event_clicEnDuracionMs

/** Este m�todo se llama autom�ticamente cuando se cambia el tiempo que se detendr�
 * la simulaci�n entre un paso de simulaci�n y otro.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void mlsPorTicCambiado(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_mlsPorTicCambiado
    this.etiquetaMlsPorTic.setText(this.mlsPorTic.getValue() + java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija.Simulacion.etiquetaMsTic"));
    panelSimulacion.ponerMlsPorTic(this.mlsPorTic.getValue());
}//GEN-LAST:event_mlsPorTicCambiado

/** Este m�todo se ejecuta cuando se hace clic en la opci�n de ocultar el nombre de
 * todos los enlaces, en el men� emergente de la pantalla de Disenio.
 * Display node names button
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void clicEnPopUpDisenioFondoOcultarNombreEnlaces(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnPopUpDisenioFondoOcultarNombreEnlaces
    Iterator it = escenario.getTopology().getLinksIterator();
    TLink enlaceAux;
    while (it.hasNext()) {
        enlaceAux = (TLink) it.next();
        enlaceAux.ponerMostrarNombre(false);
    }
    panelDisenio.repaint();
    this.escenario.setModified(true);
}//GEN-LAST:event_clicEnPopUpDisenioFondoOcultarNombreEnlaces

/** Este m�todo se ejecuta cuando se hace clic en la opci�n de ver el nombre de
 * todos los enlaces, en el men� emergente de la pantalla de Disenio.
 * Display node names button
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void clicEnPopUpDisenioFondoVerNombreEnlaces(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnPopUpDisenioFondoVerNombreEnlaces
    Iterator it = escenario.getTopology().getLinksIterator();
    TLink enlaceAux;
    while (it.hasNext()) {
        enlaceAux = (TLink) it.next();
        enlaceAux.ponerMostrarNombre(true);
    }
    panelDisenio.repaint();
    this.escenario.setModified(true);
}//GEN-LAST:event_clicEnPopUpDisenioFondoVerNombreEnlaces

/** Este m�todo se ejecuta cuando se hace clic en la opci�n de ocultar el nombre de
 * todos los nodos, en el men� emergente de la pantalla de Disenio.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void clicEnPopUpDisenioFondoOcultarNombreNodos(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnPopUpDisenioFondoOcultarNombreNodos
    Iterator it = escenario.getTopology().getNodesIterator();
    TNode nodoAux;
    while (it.hasNext()) {
        nodoAux = (TNode) it.next();
        nodoAux.setShowName(false);
    }
    panelDisenio.repaint();
    this.escenario.setModified(true);
}//GEN-LAST:event_clicEnPopUpDisenioFondoOcultarNombreNodos

/** Este m�todo se ejecuta cuando se hace clic en la opci�n de ver el nombre de
 * todos los nodos, en el men� emergente de la pantalla de Disenio.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void clicEnPopUpDisenioFondoVerNombreNodos(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnPopUpDisenioFondoVerNombreNodos
    Iterator it = escenario.getTopology().getNodesIterator();
    TNode nodoAux;
    while (it.hasNext()) {
        nodoAux = (TNode) it.next();
        nodoAux.setShowName(true);
    }
    panelDisenio.repaint();
    this.escenario.setModified(true);
}//GEN-LAST:event_clicEnPopUpDisenioFondoVerNombreNodos

/** Este m�todo se ejecuta cuando se hace clic en la opci�n de eliminar todo el
 * escenario completo, en el men� emergente de la pantalla de Disenio.
 * @since 1.0
 * @param evt Evento que hace que se dispare este m�todo.
 */
private void clicEnPopUpDisenioFondoEliminar(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnPopUpDisenioFondoEliminar
    JVentanaBooleana vb = new JVentanaBooleana(this.VentanaPadre, true, this.dispensadorDeImagenes);
    vb.mostrarPregunta(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.PreguntaBorrarTodo"));
    vb.show();
    boolean respuesta = vb.obtenerRespuesta();
    if (respuesta) {
        escenario.getTopology().eliminarTodo();
        panelDisenio.repaint();
    }
    this.escenario.setModified(true);
}//GEN-LAST:event_clicEnPopUpDisenioFondoEliminar

/**
 * Este m�todo asigna un escenario ya creado a la ventana hija. A partir de ese
 * momento todo lo que se haga en la ventana tendr� su repercusi�n en el escenario.
 * @param esc Escenario ya creado al que se va a asociar esta ventana hija y que contendr� un
 * escenario y todos sus datos.
 * @since 1.0
 */
public void ponerEscenario(TScenario esc) {
    this.controlTemporizacionDesactivado = true;
    long durac = esc.getSimulation().obtenerDuracion();
    long pas = esc.getSimulation().obtenerPaso();
    escenario = esc;
    panelDisenio.ponerTopologia(esc.getTopology());
    panelSimulacion.ponerTopologia(esc.getTopology());
    nodoSeleccionado = null;
    elementoDisenioClicDerecho = null;
    aProgresoGeneracion = new TProgressEventListener(barraDeProgreso);
    try {
        esc.getTopology().obtenerReloj().addProgressEventListener(aProgresoGeneracion);
    } catch (EProgressEventGeneratorOnlyAllowASingleListener e) {
        e.printStackTrace();
    }
    this.duracionMs.setValue((int)(durac/1000000));
    this.duracionNs.setValue((int) (durac-(this.duracionMs.getValue()*1000000)));
    this.pasoNs.setMaximum((int) esc.getSimulation().obtenerDuracion());
    this.pasoNs.setValue((int) pas);
    esc.getSimulation().setDuration(durac);
    esc.getSimulation().setStep(pas);
    this.etiquetaMlsPorTic.setText(this.mlsPorTic.getValue() + java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija.Simulacion.EtiquetaMsTic"));
    this.etiquetaDuracionMs.setText(this.duracionMs.getValue() + java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija._ms."));
    this.etiquetaDuracionNs.setText(this.duracionNs.getValue() + java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija._ns."));
    this.etiquetaPasoNs.setText(this.pasoNs.getValue() + java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija_ns."));
    this.nombreAutor.setText(esc.obtenerAutor());
    this.nombreAutor.setCaretPosition(1);
    this.nombreEscenario.setText(esc.obtenerTitulo());
    this.nombreEscenario.setCaretPosition(1);
    this.descripcionEscenario.setText(esc.obtenerDescripcion());
    this.descripcionEscenario.setCaretPosition(1);
    this.controlTemporizacionDesactivado = false;
    escenario.ponerPanelSimulacion(this.panelSimulacion);
    this.controlarParametrosTemporales();
}

/** 
 * This method is executed when you click on the option to add a new link in the toolbar of the screen design.
 * @since 1.0
 * @param evt Evento que hace que se dispare este m�todo.
 */
private void clicEnAniadirEnlace(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clicEnAniadirEnlace
    if (escenario.getTopology().obtenerNumeroDeNodos() < 2) {
        JVentanaAdvertencia va = new JVentanaAdvertencia(VentanaPadre, true, dispensadorDeImagenes);
        va.mostrarMensaje(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija.ErrorAlMenosDosNodos"));
        va.show();
    } else {
        TLinkConfig config = new TLinkConfig();
        JVentanaEnlace venlace = new JVentanaEnlace(escenario.getTopology(), dispensadorDeImagenes, VentanaPadre, true);
        venlace.ponerConfiguracion(config, false);
        venlace.cargarNodosPorDefecto();
        venlace.show();
        if (config.obtenerValida()) {
            try {
                if (config.obtenerTipo() == TLink.INTERNAL) {
                    TInternalLink enlaceInterno = new TInternalLink(escenario.getTopology().getItemIdentifierGenerator().getNew(), escenario.getTopology().getEventIDGenerator(), escenario.getTopology());
                    enlaceInterno.configurar(config, escenario.getTopology(), false);
                    escenario.getTopology().addLink(enlaceInterno);
                } else {
                    TExternalLink enlaceExterno = new TExternalLink(escenario.getTopology().getItemIdentifierGenerator().getNew(), escenario.getTopology().getEventIDGenerator(), escenario.getTopology());
                    enlaceExterno.configurar(config, escenario.getTopology(), false);
                    escenario.getTopology().addLink(enlaceExterno);
                }
                panelDisenio.repaint();
            } catch (Exception e) {
                JVentanaError err;
                err = new JVentanaError(VentanaPadre, true, dispensadorDeImagenes);
                err.mostrarMensaje(e.toString());
                err.show();
            };
            this.escenario.setModified(true);
        } else {
            config = null;
        }
    }
}//GEN-LAST:event_clicEnAniadirEnlace

/** Este m�todo se ejecuta cuando se hace clic en la opci�n eliminar que aparece en
 * el men� emergente al pulsar con el bot�n derecho sobre un elemento de la
 * topolog�a. En la pantalla de dise�o.
 * @since 1.0
 * @param evt Evento que hace que se dispare este m�todo.
 */
private void clicEnPopUpDisenioEliminar(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnPopUpDisenioEliminar
    JVentanaBooleana vb = new JVentanaBooleana(this.VentanaPadre, true, this.dispensadorDeImagenes);
    vb.mostrarPregunta(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.preguntaAlEliminar"));
    vb.show();
    boolean respuesta = vb.obtenerRespuesta();
    if (respuesta) {
        if (elementoDisenioClicDerecho != null) {
            if (elementoDisenioClicDerecho.getElementType() == TTopologyElement.NODO) {
                TNode nt = (TNode) elementoDisenioClicDerecho;
                if (nt.getNodeType() == TNode.RECEIVER) {
                    if (this.escenario.getTopology().hayTraficoDirigidoAMi((TReceiverNode) nt)) {
                        JVentanaAdvertencia va;
                        va = new JVentanaAdvertencia(VentanaPadre, true, dispensadorDeImagenes);
                        va.mostrarMensaje(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.NoPuedoBorrarReceptor"));
                        va.show();
                        elementoDisenioClicDerecho = null;
                    } else {
                        escenario.getTopology().eliminarNodo(nt);
                        elementoDisenioClicDerecho = null;
                        panelDisenio.repaint();
                    }
                } else {
                    escenario.getTopology().eliminarNodo(nt);
                    elementoDisenioClicDerecho = null;
                    panelDisenio.repaint();
                }
            } else {
                TLink ent = (TLink) elementoDisenioClicDerecho;
                escenario.getTopology().eliminarEnlace(ent);
                elementoDisenioClicDerecho = null;
                panelDisenio.repaint();
            }
            this.escenario.setModified(true);
        }
    }
    
}//GEN-LAST:event_clicEnPopUpDisenioEliminar

/** Este m�todo se ejecuta cuando se hace clic en la opci�n de ver/ocultar nombre
 * que aparece en el men� emergente al pulsar con el bot�n derecho sobre un elemento
 * de la topolog�a. En la pantalla de dise�o.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void clicEnPopUpDisenioVerNombre(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnPopUpDisenioVerNombre
    if (elementoDisenioClicDerecho != null) {
        if (elementoDisenioClicDerecho.getElementType() == TTopologyElement.NODO) {
            TNode nt = (TNode) elementoDisenioClicDerecho;
            nt.setShowName(dVerNombreMenuItem.isSelected());
            elementoDisenioClicDerecho = null;
            panelDisenio.repaint();
        } else {
            TLink ent = (TLink) elementoDisenioClicDerecho;
            ent.ponerMostrarNombre(dVerNombreMenuItem.isSelected());
            elementoDisenioClicDerecho = null;
            panelDisenio.repaint();
        }
        this.escenario.setModified(true);
    }
}//GEN-LAST:event_clicEnPopUpDisenioVerNombre

/** Este m�todo se ejecuta cuando se hace clic con el bot�n derecho en la pantalla
 * de dise�o.
 * @since 1.0
 * @param evt Evento que hace que este m�todo se dispare.
 */
private void clicDerechoEnPanelDisenio(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clicDerechoEnPanelDisenio
    if (evt.getButton() == MouseEvent.BUTTON3) {
        TTopologyElement et = escenario.getTopology().obtenerElementoEnPosicion(evt.getPoint());
        if (et == null) {
            diseFondoPopUp.show(this, evt.getX()+7, evt.getY()-27);
        }
        else {
            if (et.getElementType() == TTopologyElement.NODO) {
                TNode nt = (TNode) et;
                dVerNombreMenuItem.setSelected(nt.getShowName());
                elementoDisenioClicDerecho = et;
                diseElementoPopUp.show(this, evt.getX()+7, evt.getY()+15);
            } else if (et.getElementType() == TTopologyElement.LINK) {
                TLink ent = (TLink) et;
                dVerNombreMenuItem.setSelected(ent.obtenerMostrarNombre());
                elementoDisenioClicDerecho = et;
                diseElementoPopUp.show(this, evt.getX()+7, evt.getY()+15);
            }
        }
    } else {
        elementoDisenioClicDerecho = null;
        panelDisenio.repaint();
    }
}//GEN-LAST:event_clicDerechoEnPanelDisenio

/** Este m�todo se ejecuta cuando se hace clic en la opci�n de a�adir un LSRA
 * nuevo en la barra de herramientas de la pantalla de dise�o.
 * This method is executed when you click on the option to add one LSRA new toolbar on the screen design.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void clicEnAniadirLSRA(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clicEnAniadirLSRA
//    TActiveLSRNode lsra = null;
//    try {
//        lsra = new TActiveLSRNode(escenario.getTopology().getItemIdentifierGenerator().getNew(), escenario.getTopology().getIPAddressGenerator().obtenerIP(), escenario.getTopology().getEventIDGenerator(), escenario.getTopology());
//    } catch (Exception e) {
//        JVentanaError err;
//        err = new JVentanaError(VentanaPadre, true, dispensadorDeImagenes);
//        err.mostrarMensaje(e.toString());
//        err.show();
//    }
//    JVentanaLSRA vlsra = new JVentanaLSRA(escenario.getTopology(), panelDisenio, dispensadorDeImagenes, VentanaPadre, true);
//    vlsra.ponerConfiguracion(lsra, false);
//    vlsra.show();
//    if (lsra.isWellConfigured()) {
//        try {
//            escenario.getTopology().addNode(lsra);
//            panelDisenio.repaint();
//        } catch (Exception e) {
//            JVentanaError err;
//            err = new JVentanaError(VentanaPadre, true, dispensadorDeImagenes);
//            err.mostrarMensaje(e.toString());
//            err.show();
//        };
//        this.escenario.setModified(true);
//    } else {
//        lsra = null;
//    }
}//GEN-LAST:event_clicEnAniadirLSRA

/** Este m�todo se ejecuta cuando se hace clic en la opci�n de a�adir un LSR
 * nuevo en la barra de herramientas de la pantalla de dise�o.
 * @since 1.0
 * @param evt Evento que hace que este m�todo se dispare.
 */
private void clicEnAniadirLSR(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clicEnAniadirLSR
    TLSRNode lsr = null;
    try {
        lsr = new TLSRNode(escenario.getTopology().getItemIdentifierGenerator().getNew(), escenario.getTopology().getIPAddressGenerator().obtenerIP(), escenario.getTopology().getEventIDGenerator(), escenario.getTopology());
    } catch (Exception e) {
        JVentanaError err;
        err = new JVentanaError(VentanaPadre, true, dispensadorDeImagenes);
        err.mostrarMensaje(e.toString());
        err.show();
    }
    JVentanaLSR vlsr = new JVentanaLSR(escenario.getTopology(), panelDisenio, dispensadorDeImagenes, VentanaPadre, true);
    vlsr.ponerConfiguracion(lsr, false);
    vlsr.show();
    if (lsr.isWellConfigured()) {
        try {
            escenario.getTopology().addNode(lsr);
            panelDisenio.repaint();
        } catch (Exception e) {
            JVentanaError err;
            err = new JVentanaError(VentanaPadre, true, dispensadorDeImagenes);
            err.mostrarMensaje(e.toString());
            err.show();
        };
        this.escenario.setModified(true);
    } else {
        lsr = null;
    }
}//GEN-LAST:event_clicEnAniadirLSR

/** Este m�todo se ejecuta cuando se hace clic en la opci�n de a�adir un LSRA
 * nuevo en la barra de herramientas de la pantalla de dise�o.
 * @since 1.0
 * @param evt Evento que hace que se dispare este m�todo.
 */
private void clicEnAniadirLERA(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clicEnAniadirLERA
//    TActiveLERNode lera = null;
//    try {
//        lera = new TActiveLERNode(escenario.getTopology().getItemIdentifierGenerator().getNew(), escenario.getTopology().getIPAddressGenerator().obtenerIP(), escenario.getTopology().getEventIDGenerator(), escenario.getTopology());
//    } catch (Exception e) {
//        JVentanaError err;
//        err = new JVentanaError(VentanaPadre, true, dispensadorDeImagenes);
//        err.mostrarMensaje(e.toString());
//        err.show();
//    }
//    JVentanaLERA vlera = new JVentanaLERA(escenario.getTopology(), panelDisenio, dispensadorDeImagenes, VentanaPadre, true);
//    vlera.ponerConfiguracion(lera, false);
//    vlera.show();
//    if (lera.isWellConfigured()) {
//        try {
//            escenario.getTopology().addNode(lera);
//            panelDisenio.repaint();
//        } catch (Exception e) {
//            JVentanaError err;
//            err = new JVentanaError(VentanaPadre, true, dispensadorDeImagenes);
//            err.mostrarMensaje(e.toString());
//            err.show();
//        };
//        this.escenario.setModified(true);
//    } else {
//        lera = null;
//    }
}//GEN-LAST:event_clicEnAniadirLERA

/** Este m�todo se ejecuta cuando se mueve el rat�n dentro del �rea de simulaci�n ,
 * en la pantalla de simulaci�n. Entre otras cosas, cambia el cursor del rat�n al pasar
 * sobre un elemento, permite mostrar men�s emergentes coherentes con el contexto
 * de donde se encuentra el rat�n, etc�tera.
 * This method is executed when the rapn moves into the area of simulation, on screen 
 * simulation. Among other things, changes the mouse cursor to pass * on an item, can display pop mens consistent with the context 
 * of where the mouse, etcetera is.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void ratonSobrePanelSimulacion(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonSobrePanelSimulacion
    TTopology topo = escenario.getTopology();
    TTopologyElement et = topo.obtenerElementoEnPosicion(evt.getPoint());
    if (et != null) {
        this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (et.getElementType() == TTopologyElement.NODO) {
            TNode nt = (TNode) et;
            if (nt.getPorts().isArtificiallyCongested()) {
                panelSimulacion.setToolTipText(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.Congestion") +nt.getPorts().getCongestionLevel()+ java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.POrcentaje")+java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija.paraDejarDeCongestionar"));
            } else {
                panelSimulacion.setToolTipText(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.Congestion") +nt.getPorts().getCongestionLevel()+ java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.POrcentaje")+java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaHija.paraCongestionar"));
            }
        } else if (et.getElementType() == TTopologyElement.LINK) {
            TLink ent = (TLink) et;
            if (ent.isBroken()) {
                panelSimulacion.setToolTipText(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.EnlaceRoto"));
            } else {
                panelSimulacion.setToolTipText(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.EnlaceFuncionando"));
            }
        }
    } else {
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        this.panelSimulacion.setToolTipText(null); 
        if (!this.panelSimulacion.obtenerMostrarLeyenda()) {
            this.panelSimulacion.setToolTipText(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.VerLeyenda")); 
        } else{
            this.panelSimulacion.setToolTipText(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.OcultarLeyenda")); 
        }
    }
}//GEN-LAST:event_ratonSobrePanelSimulacion

/** Este m�todo se ejecuta cuando se mueve el rat�n dentro del �rea de dise�o,
 * en la pantalla de Dise�o. Entre otras cosas, cambia el cursor del rat�n al pasar
 * sobre un elemento, permite mostrar men�s emergentes coherentes con el contexto
 * de donde se encuentra el rat�n, etc�tera.
 * @since 1.0
 * @param evt Evento que hace que se dispare este m�todo.
 */
private void ratonSobrePanelDisenio(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonSobrePanelDisenio
    TTopology topo = escenario.getTopology();
    TTopologyElement et = topo.obtenerElementoEnPosicion(evt.getPoint());
    if (et != null) {
        this.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (et.getElementType() == TTopologyElement.NODO) {
            TNode nt = (TNode) et;
            panelDisenio.setToolTipText(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.PanelDisenio.IP") + nt.getIPAddress());
        } else if (et.getElementType() == TTopologyElement.LINK) {
            TLink ent = (TLink) et;
            panelDisenio.setToolTipText(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.panelDisenio.Retardo") + ent.obtenerDelay() + java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.panelDisenio.ns"));
        }
    } else {
        this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        panelDisenio.setToolTipText(null);
    }
}//GEN-LAST:event_ratonSobrePanelDisenio

/** Este m�todo se llama autom�ticamente cuando se est� arrastrando el rat�n en la
 * pantalla de dise�o. Se encarga de mover los elementos de un lugar a otro para
 * dise�ar la topolog�a.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void arrastrandoEnPanelDisenio(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_arrastrandoEnPanelDisenio
    if (evt.getModifiersEx() == java.awt.event.InputEvent.BUTTON1_DOWN_MASK) {
        if (nodoSeleccionado != null) {
            TTopology topo = escenario.getTopology();
            Point p2 = evt.getPoint();
            if (p2.x < 0)
                p2.x = 0;
            if (p2.x > panelDisenio.getSize().width)
                p2.x = panelDisenio.getSize().width;
            if (p2.y < 0)
                p2.y = 0;
            if (p2.y > panelDisenio.getSize().height)
                p2.y = panelDisenio.getSize().height;
            nodoSeleccionado.setPosition(new Point(p2.x, p2.y));
            panelDisenio.repaint();
            this.escenario.setModified(true);
        }
    }
}//GEN-LAST:event_arrastrandoEnPanelDisenio

/** Este m�todo se llama autom�ticamente cuando soltamos el bot�n del raton a la
 * rrastrar o al hacer clic. Si el rat�n estaba sobre  un elemento de la topology,
 se marca �ste como no seleccionado.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void clicSoltadoEnPanelDisenio(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clicSoltadoEnPanelDisenio
    if (evt.getButton() == MouseEvent.BUTTON1) {
        if (nodoSeleccionado != null) {
            nodoSeleccionado.setStatus(TNode.DESELECCIONADO);
            nodoSeleccionado = null;
            this.escenario.setModified(true);
        }
        panelDisenio.repaint();
    }
}//GEN-LAST:event_clicSoltadoEnPanelDisenio

/** Este m�todo se llama autom�ticamente cuando se hace un clic con el bot�n
 * izquierdo sobre la pantalla de dise�o.
 * @since 1.0
 * @param evt Evento que hace que se dispare este m�todo.
 */
private void clicEnPanelDisenio(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clicEnPanelDisenio
    if (evt.getButton() == MouseEvent.BUTTON1) {
        TTopology topo = escenario.getTopology();
        nodoSeleccionado = topo.obtenerNodoEnPosicion(evt.getPoint());
        if (nodoSeleccionado != null) {
            nodoSeleccionado.setStatus(TNode.SELECCIONADO);
            this.escenario.setModified(true);
        }
        panelDisenio.repaint();
    }
}//GEN-LAST:event_clicEnPanelDisenio

/** Este m�todo se llama autom�ticamente cuando el rat�n sale del icono de
 * detener en la pantalla de simulaci�n.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void ratonSaleDelIconoPausar(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonSaleDelIconoPausar
    iconoPausar.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.BOTON_PAUSA));
    this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_ratonSaleDelIconoPausar

/** Este m�todo se llama autom�ticamente cuando el rat�n pasa por el icono de
 * detener en la pantalla de simulaci�n.
 * @since 1.0
 * @param evt Evento que hace que se dispare este m�todo.
 */
private void ratonEntraEnIconoPausar(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonEntraEnIconoPausar
    iconoPausar.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.BOTON_PAUSA_BRILLO));
    this.setCursor(new Cursor(Cursor.HAND_CURSOR));
}//GEN-LAST:event_ratonEntraEnIconoPausar

/** Este m�todo se llama autom�ticamente cuando el rat�n sale del icono de finalizar
 * en la pantalla de simulaci�n.
 * @since 1.0
 * @param evt Evento que hace que se dispare este m�todo.
 */
private void ratonSaleDelIconoFinalizar(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonSaleDelIconoFinalizar
    iconoFinalizar.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.BOTON_PARAR));
    this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_ratonSaleDelIconoFinalizar

/** Este m�todo se llama autom�ticamente cuando el rat�n pasa por el icono de finalizar
 * en la pantalla de simulaci�n.
 * @since 1.0
 * @param evt Evento que hace que se dispare este m�todo.
 */
private void ratonEntraEnIconoFinalizar(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonEntraEnIconoFinalizar
    iconoFinalizar.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.BOTON_PARAR_BRILLO));
    this.setCursor(new Cursor(Cursor.HAND_CURSOR));
}//GEN-LAST:event_ratonEntraEnIconoFinalizar

/** Este m�todo se llama autom�ticamente cuando el rat�n sale del icono de comenzar
 * en la pantalla de simulaci�n.
 * @since 1.0
 * @param evt Evento que hace que se dispare este m�todo.
 */
private void ratonSaleDelIconoReanudar(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonSaleDelIconoReanudar
    iconoReanudar.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.BOTON_COMENZAR));
    this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_ratonSaleDelIconoReanudar

/** Este m�todo se llama autom�ticamente cuando el rat�n pasa por el icono de
 * comenzar en la pantalla de simulaci�n.
 * @since 1.0
 * @param evt Evento que hace que se dispare este m�todo.
 */
private void ratonEntraEnIconoReanudar(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonEntraEnIconoReanudar
    iconoReanudar.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.BOTON_COMENZAR_BRILLO));
    this.setCursor(new Cursor(Cursor.HAND_CURSOR));
}//GEN-LAST:event_ratonEntraEnIconoReanudar

/** Este m�todo se llama autom�ticamente cuando el rat�n sale del icono generar en la
 * pantalla de simulaci�n.
 * @since 1.0
 * @param evt Evento que hace que se dispare este m�todo.
 */
private void ratonSaleDelIconoComenzar(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonSaleDelIconoComenzar
    iconoComenzar.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.BOTON_GENERAR));
    this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_ratonSaleDelIconoComenzar

/** Este m�todo se llama autom�ticamente cuando el rat�n pasa por el icono generar en
 * la pantalla de simulaci�n.
 * @since 1.0
 * @param evt Evento que hace que se dispare este m�todo.
 */
private void ratonEntraEnIconoComenzar(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonEntraEnIconoComenzar
    iconoComenzar.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.BOTON_GENERAR_BRILLO));
    this.setCursor(new Cursor(Cursor.HAND_CURSOR));
}//GEN-LAST:event_ratonEntraEnIconoComenzar

/** Este m�todo se ejecuta cuando se hace clic en la opci�n de a�adir un LER
 * nuevo en la barra de herramientas de la pantalla de dise�o.
 * @since 1.0
 * @param evt Evento que hace que se dispare este m�todo.
 */
private void clicEnAniadirLER(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clicEnAniadirLER
    TLERNode ler = null;
    try {
        ler = new TLERNode(escenario.getTopology().getItemIdentifierGenerator().getNew(), escenario.getTopology().getIPAddressGenerator().obtenerIP(), escenario.getTopology().getEventIDGenerator(), escenario.getTopology());
    } catch (Exception e) {
        JVentanaError err;
        err = new JVentanaError(VentanaPadre, true, dispensadorDeImagenes);
        err.mostrarMensaje(e.toString());
        err.show();
    }
    JVentanaLER vler = new JVentanaLER(escenario.getTopology(), panelDisenio, dispensadorDeImagenes, VentanaPadre, true);
    vler.ponerConfiguracion(ler, false);
    vler.show();
    if (ler.isWellConfigured()) {
        try {
            escenario.getTopology().addNode(ler);
            panelDisenio.repaint();
        } catch (Exception e) {
            JVentanaError err;
            err = new JVentanaError(VentanaPadre, true, dispensadorDeImagenes);
            err.mostrarMensaje(e.toString());
            err.show();
        };
        this.escenario.setModified(true);
    } else {
        ler = null;
    }
}//GEN-LAST:event_clicEnAniadirLER

/** Este m�todo se llama autom�ticamente cuando el rat�n sale del icono enlace en
 * la pantalla de dise�o.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo
 */
private void ratonSaleDeIconoEnlace(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonSaleDeIconoEnlace
    iconoEnlace.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.ENLACE_MENU));
    this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_ratonSaleDeIconoEnlace

/** Este m�todo se llama autom�ticamente cuando el rat�n pasa por el icono enlace en
 * la pantalla de dise�o.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void ratonEntraEnIconoEnlace(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonEntraEnIconoEnlace
    iconoEnlace.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.ENLACE_MENU_BRILLO));
    this.setCursor(new Cursor(Cursor.HAND_CURSOR));
}//GEN-LAST:event_ratonEntraEnIconoEnlace

/** Este m�todo se llama autom�ticamente cuando el rat�n sale del icono LSRA en
 * la pantalla de dise�o.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void ratonSaleDeIconoLSRA(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonSaleDeIconoLSRA
    iconoLSRA.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.LSRA_MENU));
    this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_ratonSaleDeIconoLSRA

/** Este m�todo se llama autom�ticamente cuando el rat�n pasa por el icono LSRA en
 * la pantalla de dise�o.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void ratonEntraEnIconoLSRA(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonEntraEnIconoLSRA
    iconoLSRA.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.LSRA_MENU_BRILLO));
    this.setCursor(new Cursor(Cursor.HAND_CURSOR));
}//GEN-LAST:event_ratonEntraEnIconoLSRA

/** Este m�todo se llama autom�ticamente cuando el rat�n sale del icono LSR en
 * la pantalla de dise�o.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void ratonSaleDeIconoLSR(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonSaleDeIconoLSR
    iconoLSR.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.LSR_MENU));
    this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_ratonSaleDeIconoLSR

/** Este m�todo se llama autom�ticamente cuando el rat�n pasa por el icono LSR en
 * la pantalla de dise�o.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void ratonEntraEnIconoLSR(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonEntraEnIconoLSR
    iconoLSR.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.LSR_MENU_BRILLO));
    this.setCursor(new Cursor(Cursor.HAND_CURSOR));
}//GEN-LAST:event_ratonEntraEnIconoLSR

/** Este m�todo se llama autom�ticamente cuando el rat�n sale del icono LERA en
 * la pantalla de dise�o.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void ratonSaleDeIconoLERA(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonSaleDeIconoLERA
    iconoLERA.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.LERA_MENU));
    this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_ratonSaleDeIconoLERA

/** Este m�todo se llama autom�ticamente cuando el rat�n pasa por el icono LERA en
 * la pantalla de dise�o.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void ratonEntraEnIconoLERA(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonEntraEnIconoLERA
    iconoLERA.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.LERA_MENU_BRILLO));
    this.setCursor(new Cursor(Cursor.HAND_CURSOR));
}//GEN-LAST:event_ratonEntraEnIconoLERA

/** Este m�todo se llama autom�ticamente cuando el rat�n sale del icono LER en
 * la pantalla de dise�o.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void ratonSaleDeIconoLER(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonSaleDeIconoLER
    iconoLER.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.LER_MENU));
    this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_ratonSaleDeIconoLER

/** Este m�todo se llama autom�ticamente cuando el rat�n pasa por el icono LER en
 * la pantalla de dise�o.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void ratonEntraEnIconoLER(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonEntraEnIconoLER
    iconoLER.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.LER_MENU_BRILLO));
    this.setCursor(new Cursor(Cursor.HAND_CURSOR));
}//GEN-LAST:event_ratonEntraEnIconoLER

/** Este m�todo se llama autom�ticamente cuando el rat�n sale del icono receptor en
 * la pantalla de dise�o.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void ratonSaleDeIconoReceptor(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonSaleDeIconoReceptor
    iconoReceptor.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.RECEPTOR_MENU));
    this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_ratonSaleDeIconoReceptor

/** Este m�todo se llama autom�ticamente cuando el rat�n pasa por el icono receptor
 * en la pantalla de dise�o.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void ratonEntraEnIconoReceptor(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonEntraEnIconoReceptor
    iconoReceptor.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.RECEPTOR_MENU_BRILLO));
    this.setCursor(new Cursor(Cursor.HAND_CURSOR));
}//GEN-LAST:event_ratonEntraEnIconoReceptor

/** Este m�todo se llama autom�ticamente cuando el rat�n sale del icono emisor en
 * la pantalla de dise�o.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void ratonSaleDeIconoEmisor(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonSaleDeIconoEmisor
    iconoEmisor.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.EMISOR_MENU));
    this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_ratonSaleDeIconoEmisor

/** Este m�todo se llama autom�ticamente cuando el rat�n pasa por el icono emisor en
 * la pantalla de dise�o.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void ratonEntraEnIconoEmisor(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonEntraEnIconoEmisor
    iconoEmisor.setIcon(dispensadorDeImagenes.obtenerIcono(TImagesBroker.EMISOR_MENU_BRILLO));
    this.setCursor(new Cursor(Cursor.HAND_CURSOR));
}//GEN-LAST:event_ratonEntraEnIconoEmisor

/** Este m�todo se llama autom�ticamente cuando se hace clic sobre el icono receptor
 * en la ventana de dise�o. A�ade un receptor nuevo en la topology.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void clicEnAniadirReceptor(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clicEnAniadirReceptor
    TReceiverNode receptor = null;
    try {
        receptor = new TReceiverNode(escenario.getTopology().getItemIdentifierGenerator().getNew(), escenario.getTopology().getIPAddressGenerator().obtenerIP(), escenario.getTopology().getEventIDGenerator(), escenario.getTopology());
    } catch (Exception e) {
        JVentanaError err;
        err = new JVentanaError(VentanaPadre, true, dispensadorDeImagenes);
        err.mostrarMensaje(e.toString());
        err.show();
    }
    JVentanaReceptor vr = new JVentanaReceptor(escenario.getTopology(), panelDisenio, dispensadorDeImagenes, VentanaPadre, true);
    vr.ponerConfiguracion(receptor, false);
    vr.show();
    if (receptor.isWellConfigured()) {
        try {
            escenario.getTopology().addNode(receptor);
            panelDisenio.repaint();
        } catch (Exception e) {
            JVentanaError err;
            err = new JVentanaError(VentanaPadre, true, dispensadorDeImagenes);
            err.mostrarMensaje(e.toString());
            err.show();
        };
        this.escenario.setModified(true);
    } else {
        receptor = null;
    }
}//GEN-LAST:event_clicEnAniadirReceptor

/** Este m�todo se llama autom�ticamente cuando se hace clic sobre el icono emisor
 * en la ventana de dise�o. A�ade un emisor nuevo en la topology.
 * This method is called automatically when you click on the transmitter icon 
 * in the design window. Adds a new issuer in the topology.
 * @since 1.0
 * @param evt El evento que hace que se dispare este m�todo.
 */
private void clicEnAniadirEmisorDeTrafico(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clicEnAniadirEmisorDeTrafico
    TTopology t = escenario.getTopology();
    Iterator it = t.getNodesIterator();
    TNode nt;
    boolean hayDestino = false;
    while (it.hasNext()) {
        nt = (TNode) it.next();
        if (nt.getNodeType() == TNode.RECEIVER)
            hayDestino = true;
    }
    if (!hayDestino) {
        JVentanaAdvertencia va = new JVentanaAdvertencia(VentanaPadre, true, dispensadorDeImagenes);
        va.mostrarMensaje(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.NecesitaHaberUnReceptor"));
        va.show();
    } else {
        TSenderNode emisor = null;
        try {
            emisor = new TSenderNode(escenario.getTopology().getItemIdentifierGenerator().getNew(), escenario.getTopology().getIPAddressGenerator().obtenerIP(), escenario.getTopology().getEventIDGenerator(), escenario.getTopology());
        } catch (Exception e) {
            JVentanaError err;
            err = new JVentanaError(VentanaPadre, true, dispensadorDeImagenes);
            err.mostrarMensaje(e.toString());
            err.show();
        }
        JVentanaEmisor ve = new JVentanaEmisor(escenario.getTopology(), panelDisenio, dispensadorDeImagenes, VentanaPadre, true);
        ve.ponerConfiguracion(emisor, false);
        ve.show();
        if (emisor.isWellConfigured()) {
            try {
                escenario.getTopology().addNode(emisor);
                panelDisenio.repaint();
            } catch (Exception e) {
                JVentanaError err;
                err = new JVentanaError(VentanaPadre, true, dispensadorDeImagenes);
                err.mostrarMensaje(e.toString());
                err.show();
            };
            this.escenario.setModified(true);
        } else {
            emisor = null;
        }
    }
}//GEN-LAST:event_clicEnAniadirEmisorDeTrafico

/** Este m�todo se llama autom�ticamente cuando se hace clic sobre el icono detener
 * en la ventana de simulaci�n. Detiene la simulaci�n o su generaci�n.
 * @since 1.0
 * @param evt Evento que hace que este m�todo se dispare.
 */
private void clicAlPausar(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clicAlPausar
        if (iconoPausar.isEnabled()) {
            this.escenario.getTopology().obtenerReloj().setPaused(true);
            activarOpcionesAlDetener();
        }
}//GEN-LAST:event_clicAlPausar
    
    /** Este m�todo se llama autom�ticamente cuando se hace clic sobre el icono
     * finalizar en la ventana de simulaci�n. Detiene la simulaci�n por completo.
     * @since 1.0
     * @param evt El evento que hace que este m�todo se dispare.
     */
    private void clicEnFinalizar(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clicEnFinalizar
        if (iconoFinalizar.isEnabled()) {
            this.escenario.getTopology().obtenerReloj().reset();
            this.crearTraza.setEnabled(true);
            this.panelSimulacion.ponerFicheroTraza(null);
            activarOpcionesAlFinalizar();
        }
    }//GEN-LAST:event_clicEnFinalizar
    
    /** Este m�todo se llama autom�ticamente cuando se hace clic sobre el icono comenzar
     * en la ventana de simulaci�n. Inicia la simulaci�n.
     * @since 1.0
     * @param evt El evento que hace que este m�todo se dispare.
     */
    private void clicEnReanudar(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clicEnReanudar
        if (iconoReanudar.isEnabled()) {
            activarOpcionesAlComenzar();
            this.escenario.getTopology().obtenerReloj().setPaused(false);
            this.escenario.getTopology().obtenerReloj().restart();
        }
    }//GEN-LAST:event_clicEnReanudar
    
    /** Este m�todo se llama autom�ticamente cuando se hace clic sobre el icono generar
     * en la ventana de simulaci�n. Crea la simulaci�n.
     * @since 1.0
     * @param evt El evento que hace que este m�todo se dispare.
     */
    private void clicEnComenzar(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clicEnComenzar
        if (iconoComenzar.isEnabled()) {
            escenario.reset();
            escenario.ponerDuracionSimulacion(new TTimestamp(duracionMs.getValue(), duracionNs.getValue()));
            escenario.ponerPasoSimulacion(pasoNs.getValue());
            crearListaElementosEstadistica();
            this.escenario.setModified(true);
            this.escenario.getTopology().obtenerReloj().reset();
            panelSimulacion.reset();
            panelSimulacion.repaint();
            escenario.generarSimulacion();
            int minimoDelay = this.escenario.getTopology().obtenerMinimoDelay();
            int pasoActual = this.pasoNs.getValue();
            if (pasoActual > minimoDelay) {
                this.pasoNs.setValue(minimoDelay);
            }
            this.crearTraza.setEnabled(false);
            this.panelSimulacion.ponerFicheroTraza(null);
            if (this.crearTraza.isSelected()) {
                if (this.escenario.obtenerFichero() != null) {
                    File fAux = new File(this.escenario.obtenerFichero().getPath()+".txt");
                    this.panelSimulacion.ponerFicheroTraza(fAux);
                } else {
                    this.panelSimulacion.ponerFicheroTraza(new File(this.getTitle()+".txt"));
                }
            } else {
                this.panelSimulacion.ponerFicheroTraza(null);
            }
            activarOpcionesTrasGenerar();
        }
    }//GEN-LAST:event_clicEnComenzar
    
    /**
     * Este m�todo se llama cuando comienza la simulaci�n del escenario. Crea una lista
     * de todos los nodos que tienen activa la generaci�n de estad�sticas para
     * posteriormente poder elegir uno de ellos y ver sus gr�ficas.
     * @since 1.0
     */    
    public void crearListaElementosEstadistica() {
        Iterator it = null;
        TNode nt = null;
        TLink et = null;
        this.selectorElementoEstadisticas.removeAllItems();
        this.selectorElementoEstadisticas.addItem("");
        it = this.escenario.getTopology().getNodesIterator();
        while (it.hasNext()) {
            nt = (TNode) it.next();
            if (nt.isGeneratingStats()) {
                this.selectorElementoEstadisticas.addItem(nt.getName());
            }
        }
        this.selectorElementoEstadisticas.setSelectedIndex(0);
    }
    
    /** Este m�todo modifica la interfaz para que las opciones que se muestran sean
     * acordes al momento en que la simulaci�n est� detenida.
     * @since 1.0
     */
    private void activarOpcionesAlDetener() {
        iconoComenzar.setEnabled(false);
        iconoReanudar.setEnabled(true);
        iconoFinalizar.setEnabled(true);
        iconoPausar.setEnabled(false);
    }
    
    /** Este m�todo modifica la interfaz para que las opciones que se muestran sean
     * acordes al momento en que la simulaci�n ha finalizado.
     * @since 1.0
     */
    private void activarOpcionesAlFinalizar() {
        iconoComenzar.setEnabled(true);
        iconoReanudar.setEnabled(false);
        iconoFinalizar.setEnabled(false);
        iconoPausar.setEnabled(false);
    }
    
    /** Este m�todo modifica la interfaz para que las opciones que se muestran sean
     * acordes al momento en que la simulaci�n se acaba de generar.
     * @since 1.0
     */
    private void activarOpcionesTrasGenerar() {
        iconoComenzar.setEnabled(false);
        iconoReanudar.setEnabled(false);
        iconoFinalizar.setEnabled(true);
        iconoPausar.setEnabled(true);
    }
    
    /** Este m�todo modifica la interfaz para que las opciones que se muestran sean
     * acordes al momento en que la simulaci�n comienza.
     * @since 1.0
     */
    private void activarOpcionesAlComenzar() {
        iconoComenzar.setEnabled(false);
        iconoReanudar.setEnabled(false);
        iconoFinalizar.setEnabled(true);
        iconoPausar.setEnabled(true);
    }
    
    /** Cierra la ventana hija y pierde o almacena su contenido en funci�n de la
     * elecci�n del usuario.
     * @since 1.0
     */
    public void cerrar() {
        this.setVisible(false);
        this.dispose();
    }
    
    
    /**
     * Este m�todo se encarga de controlar que todo ocurre como debe con respecto al
     * escenario, cuando se pulsa en el men� principal la opci�n de "Guardar como..."
     * @since 1.0
     */    
    public void gestionarGuardarComo() {
        anotarDatosDeEscenario();
        JFileChooser dialogoGuardar = new JFileChooser();
        dialogoGuardar.setFileFilter(new JOSMFilter());
        dialogoGuardar.setDialogType(JFileChooser.CUSTOM_DIALOG);
        dialogoGuardar.setApproveButtonMnemonic('A');
        dialogoGuardar.setApproveButtonText(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.DialogoGuardar.OK"));
        dialogoGuardar.setDialogTitle(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.DialogoGuardar.Almacenar")+ this.getTitle() +java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("-"));
        dialogoGuardar.setAcceptAllFileFilterUsed(false);
        dialogoGuardar.setSelectedFile(new File(this.getTitle()));
        dialogoGuardar.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int resultado = dialogoGuardar.showSaveDialog(VentanaPadre);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            String ext = null;
            String nombreFich = dialogoGuardar.getSelectedFile().getPath();
            int i = nombreFich.lastIndexOf('.');
            if (i > 0 &&  i < nombreFich.length() - 1) {
                ext = nombreFich.substring(i+1).toLowerCase();
            }
            if (ext == null) {
                nombreFich += java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString(".osm");
            } else {
                if (!ext.equals(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("osm"))) {
                    nombreFich += java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString(".osm");
                }
            }
            dialogoGuardar.setSelectedFile(new File(nombreFich));
            escenario.setFile(dialogoGuardar.getSelectedFile());
            this.escenario.setSaved(true);
            this.setTitle(this.escenario.obtenerFichero().getName());
            TOSMSaver almacenador = new TOSMSaver(escenario);
//            JVentanaBooleana vb = new JVentanaBooleana(this.VentanaPadre, true, this.dispensadorDeImagenes);
//            vb.mostrarPregunta(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.PreguntaEmpotrarCRC"));
//            vb.show();
//            boolean conCRC = vb.obtenerRespuesta();
//            boolean correcto = almacenador.save(escenario.obtenerFichero(), conCRC);
            JVentanaBooleana vb = new JVentanaBooleana(this.VentanaPadre, true, this.dispensadorDeImagenes);
            vb.mostrarPregunta("Do you want to save the routing tables ?");
            vb.show();
            boolean saveTables = vb.obtenerRespuesta();
            boolean correcto = almacenador.save(escenario.obtenerFichero(),false ,saveTables);
            if (correcto) {
                this.escenario.setModified(false);
                this.escenario.setSaved(true);
            }
        }
    }
    
    /**
     * Este m�todo se encarga de controlar que todo ocurre como debe con respecto al
     * escenario, cuando se pulsa en el men� principal la opci�n de "Cerrar" o "Salir"
     * y el escenario actual no est� a�n guardado o est� modificado.
     * @since 1.0
     */    
    public void gestionarGuardarParaCerrar() {
        boolean guardado = this.escenario.obtenerGuardado();
        boolean modificado = this.escenario.obtenerModificado();
        anotarDatosDeEscenario();
        
        // Detengo la simulaci�n antes de cerrar, si es necesario.
        if (this.escenario.getTopology().obtenerReloj().isRunning()) {
            panelSimulacion.reset();
            panelSimulacion.repaint();
            escenario.reset();
            escenario.ponerDuracionSimulacion(new TTimestamp(duracionMs.getValue(), duracionNs.getValue()));
            escenario.ponerPasoSimulacion(pasoNs.getValue());
            this.escenario.getTopology().obtenerReloj().setPaused(false);
            activarOpcionesAlFinalizar();
        }
        
        if (!guardado) {
            JVentanaBooleana vb = new JVentanaBooleana(VentanaPadre, true, dispensadorDeImagenes);
            vb.mostrarPregunta(this.getTitle() + java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.DialogoGuardar.GuardarPrimeraVez"));
            vb.show();
            boolean respuesta = vb.obtenerRespuesta();
            vb.dispose();
            if (respuesta) {
                this.gestionarGuardarComo();
            }
        } else if ((guardado) && (!modificado)) {
            // No se hace nada, ya est� todo guardado correctamente.
        } else if ((guardado) && (modificado)) {
            JVentanaBooleana vb = new JVentanaBooleana(VentanaPadre, true, dispensadorDeImagenes);
            vb.mostrarPregunta(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.DialogoGuardar.CambiosSinguardar1")+ " " + this.getTitle()+ " " + java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.DialogoGuardar.CambiosSinguardar2"));
            vb.show();
            boolean respuesta = vb.obtenerRespuesta();
            vb.dispose();
            if (respuesta) {
                TOSMSaver almacenador = new TOSMSaver(escenario);
                JVentanaBooleana vb2 = new JVentanaBooleana(this.VentanaPadre, true, this.dispensadorDeImagenes);
//                vb2.mostrarPregunta(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.PreguntaEmpotrarCRC"));
//                vb2.show();
//                boolean conCRC = vb2.obtenerRespuesta();
//                boolean correcto = almacenador.save(escenario.obtenerFichero(), conCRC);
                vb.mostrarPregunta("Do you want to save the routing tables ?");
                vb.show();
                boolean saveTables = vb.obtenerRespuesta();
                boolean correcto = almacenador.save(escenario.obtenerFichero(),false ,saveTables);
                if (correcto) {
                    this.escenario.setModified(false);
                    this.escenario.setSaved(true);
                }
            }
        }
    }
    
    /**
     * Este m�todo se encarga de controlar que todo ocurre como debe con respecto al
     * escenario, cuando se pulsa en el men� principal la opci�n de "Guardar".
     * @since 1.0
     */    
    public void gestionarGuardar() {
        boolean guardado = this.escenario.obtenerGuardado();
        boolean modificado = this.escenario.obtenerModificado();
        anotarDatosDeEscenario();
        if (!guardado) {
            this.gestionarGuardarComo();
        } else {
            TOSMSaver almacenador = new TOSMSaver(escenario);
            JVentanaBooleana vb = new JVentanaBooleana(this.VentanaPadre, true, this.dispensadorDeImagenes);
//            vb.mostrarPregunta(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaHija.PreguntaEmpotrarCRC"));
//            vb.show();
//            boolean conCRC = vb.obtenerRespuesta();
//            boolean correcto = almacenador.save(escenario.obtenerFichero(), conCRC);
            vb.mostrarPregunta("Do you want to save the routing tables ?");
            vb.show();
            boolean saveTables = vb.obtenerRespuesta();
            boolean correcto = almacenador.save(escenario.obtenerFichero(),false ,saveTables);
            if (correcto) {
                this.escenario.setModified(false);
                this.escenario.setSaved(true);
            }
            this.escenario.setModified(false);
            this.escenario.setSaved(true);
        }
    }
    
    private void crearEInsertarGraficas(String nombre) {
            GridBagConstraints gbc = null;
            this.panelAnalisis.removeAll();
            this.etiquetaEstadisticasTituloEscenario.setText(this.nombreEscenario.getText());
            this.etiquetaEstadisticasNombreAutor.setText(this.nombreAutor.getText());
            this.areaEstadisticasDescripcion.setText(this.descripcionEscenario.getText());
            this.etiquetaNombreElementoEstadistica.setText(nombre);
            TNode nt = this.escenario.getTopology().setFirstNodeNamed(nombre);
            gbc = new java.awt.GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(10, 10, 10, 5);
            gbc.anchor = java.awt.GridBagConstraints.NORTH;
            gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
            this.panelFijo.add(this.etiquetaEstadisticasTituloEscenario, gbc);
            gbc = new java.awt.GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.insets = new Insets(10, 5, 10, 5);
            gbc.anchor = java.awt.GridBagConstraints.NORTH;
            gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
            this.panelFijo.add(this.etiquetaEstadisticasNombreAutor, gbc);
            gbc = new java.awt.GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.insets = new Insets(10, 5, 10, 5);
            gbc.anchor = java.awt.GridBagConstraints.NORTH;
            gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
            this.panelFijo.add(this.areaEstadisticasDescripcion, gbc);
            gbc = new java.awt.GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.insets = new Insets(10, 5, 10, 5);
            gbc.anchor = java.awt.GridBagConstraints.NORTH;
            gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
            this.panelFijo.add(this.etiquetaNombreElementoEstadistica, gbc);
            gbc = new java.awt.GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(10, 10, 10, 5);
            gbc.anchor = java.awt.GridBagConstraints.NORTH;
            gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
            this.panelAnalisis.add(this.panelFijo, gbc);
            if (nt != null) {
                if (nt.getNodeType() == TNode.SENDER) {
                    this.etiquetaNombreElementoEstadistica.setIcon(this.dispensadorDeImagenes.obtenerIcono(TImagesBroker.EMISOR));
                } else if (nt.getNodeType() == TNode.RECEIVER) {
                    this.etiquetaNombreElementoEstadistica.setIcon(this.dispensadorDeImagenes.obtenerIcono(TImagesBroker.RECEPTOR));
                } else if (nt.getNodeType() == TNode.LER) {
                    this.etiquetaNombreElementoEstadistica.setIcon(this.dispensadorDeImagenes.obtenerIcono(TImagesBroker.LER));
                } else if (nt.getNodeType() == TNode.LERA) {
                    this.etiquetaNombreElementoEstadistica.setIcon(this.dispensadorDeImagenes.obtenerIcono(TImagesBroker.LERA));
                } else if (nt.getNodeType() == TNode.LSR) {
                    this.etiquetaNombreElementoEstadistica.setIcon(this.dispensadorDeImagenes.obtenerIcono(TImagesBroker.LSR));
                } else if (nt.getNodeType() == TNode.LSRA) {
                    this.etiquetaNombreElementoEstadistica.setIcon(this.dispensadorDeImagenes.obtenerIcono(TImagesBroker.LSRA));
                }

                int numeroGraficos = nt.getStats().obtenerNumeroGraficas();
                
                if (numeroGraficos > 0) {
                    grafico1 = ChartFactory.createXYLineChart(nt.getStats().obtenerTitulo1(), 
                                                             TStats.TIEMPO,
                                                             TStats.NUMERO_DE_PAQUETES,
                                                             (XYSeriesCollection) nt.getStats().obtenerDatosGrafica1(),
                                                             PlotOrientation.VERTICAL, 
                                                             true, true, true);

                    grafico1.getPlot().setBackgroundPaint(Color.WHITE);
                    grafico1.getPlot().setForegroundAlpha((float)0.5);
                    grafico1.getPlot().setOutlinePaint(new Color(14, 69, 125));
                    grafico1.getXYPlot().setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5, 5, 5, 5));
                    grafico1.setBackgroundPaint(new Color(210, 226, 242));
                    grafico1.setBorderPaint(Color.BLACK);
                    grafico1.getTitle().setPaint(new Color(79, 138, 198));
                    this.panelGrafico1 = new ChartPanel(grafico1);
                    panelGrafico1.setBorder(new LineBorder(Color.BLACK));
                    panelGrafico1.setPreferredSize(new Dimension(600, 300));
                    gbc = new java.awt.GridBagConstraints();
                    gbc.gridx = 0;
                    gbc.gridy = 1;
                    gbc.insets = new Insets(10, 5, 10, 5);
                    gbc.anchor = java.awt.GridBagConstraints.NORTH;
                    gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
                    this.panelAnalisis.add(panelGrafico1, gbc);
                }
                if (numeroGraficos > 1) {
                    grafico2 = ChartFactory.createXYLineChart(nt.getStats().obtenerTitulo2(), 
                                                             TStats.TIEMPO,
                                                             TStats.NUMERO_DE_PAQUETES,
                                                             (XYSeriesCollection) nt.getStats().obtenerDatosGrafica2(),
                                                             PlotOrientation.VERTICAL, 
                                                             true, true, true);
                    grafico2.getPlot().setBackgroundPaint(Color.WHITE);
                    grafico2.getPlot().setForegroundAlpha((float)0.5);
                    grafico2.getPlot().setOutlinePaint(new Color(14, 69, 125));
                    grafico2.getXYPlot().setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5, 5, 5, 5));
                    grafico2.setBackgroundPaint(new Color(210, 226, 242));
                    grafico2.setBorderPaint(Color.BLACK);
                    grafico2.getTitle().setPaint(new Color(79, 138, 198));
                    this.panelGrafico2 = new ChartPanel(grafico2);
                    panelGrafico2.setPreferredSize(new Dimension(600, 300));
                    panelGrafico2.setBorder(new LineBorder(Color.BLACK));
                    gbc = new java.awt.GridBagConstraints();
                    gbc.gridx = 0;
                    gbc.gridy = 2;
                    gbc.insets = new Insets(10, 5, 10, 5);
                    gbc.anchor = java.awt.GridBagConstraints.NORTH;
                    gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
                    this.panelAnalisis.add(panelGrafico2, gbc);
                }
                if (numeroGraficos > 2) {
                    grafico3 = ChartFactory.createXYLineChart(nt.getStats().obtenerTitulo3(), 
                                                             TStats.TIEMPO,
                                                             TStats.NUMERO_DE_PAQUETES,
                                                             (XYSeriesCollection) nt.getStats().obtenerDatosGrafica3(),
                                                             PlotOrientation.VERTICAL, 
                                                             true, true, true);
                    grafico3.getPlot().setBackgroundPaint(Color.WHITE);
                    grafico3.getPlot().setForegroundAlpha((float)0.5);
                    grafico3.getPlot().setOutlinePaint(new Color(14, 69, 125));
                    grafico3.getXYPlot().setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5, 5, 5, 5));
                    grafico3.setBackgroundPaint(new Color(210, 226, 242));
                    grafico3.setBorderPaint(Color.BLACK);
                    grafico3.getTitle().setPaint(new Color(79, 138, 198));
                    this.panelGrafico3 = new ChartPanel(grafico3);
                    panelGrafico3.setBorder(new LineBorder(Color.BLACK));
                    panelGrafico3.setPreferredSize(new Dimension(600, 300));
                    gbc = new java.awt.GridBagConstraints();
                    gbc.gridx = 0;
                    gbc.gridy = 3;
                    gbc.insets = new Insets(10, 5, 10, 5);
                    gbc.anchor = java.awt.GridBagConstraints.NORTH;
                    gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
                    this.panelAnalisis.add(panelGrafico3, gbc);
                }
                if (numeroGraficos > 3) {
                    grafico4 = ChartFactory.createBarChart(nt.getStats().obtenerTitulo4(), 
                                                             TStats.DESCRIPCION,
                                                             TStats.NUMERO,
                                                             (DefaultCategoryDataset) nt.getStats().obtenerDatosGrafica4(),
                                                             PlotOrientation.VERTICAL, 
                                                             true, true, true);
                    grafico4.getPlot().setBackgroundPaint(Color.WHITE);
                    grafico4.getPlot().setForegroundAlpha((float)0.5);
                    grafico4.getPlot().setOutlinePaint(new Color(14, 69, 125));
                    grafico4.setBackgroundPaint(new Color(210, 226, 242));
                    grafico4.setBorderPaint(Color.BLACK);
                    grafico4.getTitle().setPaint(new Color(79, 138, 198));
                    this.panelGrafico4 = new ChartPanel(grafico4);
                    panelGrafico4.setBorder(new LineBorder(Color.BLACK));
                    panelGrafico4.setPreferredSize(new Dimension(600, 300));
                    gbc = new java.awt.GridBagConstraints();
                    gbc.gridx = 0;
                    gbc.gridy = 4;
                    gbc.insets = new Insets(10, 5, 10, 5);
                    gbc.anchor = java.awt.GridBagConstraints.NORTH;
                    gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
                    this.panelAnalisis.add(panelGrafico4, gbc);
                }
                if (numeroGraficos > 4) {
                    grafico5 = ChartFactory.createBarChart(nt.getStats().obtenerTitulo5(), 
                                                             TStats.DESCRIPCION,
                                                             TStats.NUMERO,
                                                             (DefaultCategoryDataset) nt.getStats().obtenerDatosGrafica5(),
                                                             PlotOrientation.VERTICAL, 
                                                             true, true, true);
                    grafico5.getPlot().setBackgroundPaint(Color.WHITE);
                    grafico5.getPlot().setForegroundAlpha((float)0.5);
                    grafico5.getPlot().setOutlinePaint(new Color(14, 69, 125));
                    grafico5.setBackgroundPaint(new Color(210, 226, 242));
                    grafico5.setBorderPaint(Color.BLACK);
                    grafico5.getTitle().setPaint(new Color(79, 138, 198));
                    this.panelGrafico5 = new ChartPanel(grafico5);
                    panelGrafico5.setBorder(new LineBorder(Color.BLACK));
                    panelGrafico5.setPreferredSize(new Dimension(600, 300));
                    gbc = new java.awt.GridBagConstraints();
                    gbc.gridx = 0;
                    gbc.gridy = 5;
                    gbc.insets = new Insets(10, 5, 10, 5);
                    gbc.anchor = java.awt.GridBagConstraints.NORTH;
                    gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
                    this.panelAnalisis.add(panelGrafico5, gbc);
                }
                if (numeroGraficos > 5) {
                    grafico6 = ChartFactory.createXYLineChart(nt.getStats().obtenerTitulo6(), 
                                                             TStats.TIEMPO,
                                                             TStats.NUMERO_DE_PAQUETES,
                                                             (XYSeriesCollection) nt.getStats().obtenerDatosGrafica6(),
                                                             PlotOrientation.VERTICAL, 
                                                             true, true, true);
                    grafico6.getPlot().setBackgroundPaint(Color.WHITE);
                    grafico6.getPlot().setForegroundAlpha((float)0.5);
                    grafico6.getPlot().setOutlinePaint(new Color(14, 69, 125));
                    grafico6.getXYPlot().setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5, 5, 5, 5));
                    grafico6.setBackgroundPaint(new Color(210, 226, 242));
                    grafico6.setBorderPaint(Color.BLACK);
                    grafico6.getTitle().setPaint(new Color(79, 138, 198));
                    this.panelGrafico6 = new ChartPanel(grafico6);
                    panelGrafico6.setBorder(new LineBorder(Color.BLACK));
                    panelGrafico6.setPreferredSize(new Dimension(600, 300));
                    gbc = new java.awt.GridBagConstraints();
                    gbc.gridx = 0;
                    gbc.gridy = 6;
                    gbc.insets = new Insets(10, 5, 10, 10);
                    gbc.anchor = java.awt.GridBagConstraints.NORTH;
                    gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
                    this.panelAnalisis.add(panelGrafico6, gbc);
                }
            }
            this.panelAnalisis.repaint();
    }
    
    /**
     * Este m�todo se encarga de anotar los datos del escenario desde la interfaz de
     * usuario hasta los correspondientes atributos del objeto que almacena el
     * escenario.
     * @since 1.0
     */    
    private void anotarDatosDeEscenario() {
        this.escenario.setTitle(this.nombreEscenario.getText());
        this.escenario.setAuthor(this.nombreAutor.getText());
        this.escenario.setDescription(this.descripcionEscenario.getText());
    }
    
    /** Este atributo es el objeto encargado de actualizar la barra de progreso del
     * escenario que se usa a la hora de generar la simulaci�n y a la hora de
     * ejecutarla.
     * @since 1.0
     */
    private TProgressEventListener aProgresoGeneracion;
    /** Este atributo contendr� todo el escenario completo de la simulaci�n: topology,
 an�lisis y simulaci�n.
     * @since 1.0
     */
    private TScenario escenario;
    /** Este atributo contendr� en todo momento una referencia al nodo del escenario que
     * se est� arrastrando.
     * @since 1.0
     */
    private TNode nodoSeleccionado;
    /** Este atributo contendr� todas las im�genes de Open SimMPLS para poder acceder a
     * ellas de forma m�s r�pida y para no tener que cargar la misma imagen en
     * distintas instancias.
     * @since 1.0
     */
    private TImagesBroker dispensadorDeImagenes;
    /** Este atributo es una referencia a la ventana padre que recoge dentro de si a
     * esta ventana hija.
     * @since 1.0
     */
    private JSimulador VentanaPadre;
    /** Este atributo contiene en todo momento un referencia al elemento de la topolog�a
     * (nodo o enlace) sobre el que se est� intentando abrir un men� contextual (clic
     * con el bot�n derecho).
     * @since 1.0
     */
    private TTopologyElement elementoDisenioClicDerecho;
    
    
    private boolean controlTemporizacionDesactivado;
    
    private ChartPanel panelGrafico1;
    private ChartPanel panelGrafico2;
    private ChartPanel panelGrafico3;
    private ChartPanel panelGrafico4;
    private ChartPanel panelGrafico5;
    private ChartPanel panelGrafico6;
    private JFreeChart grafico1;
    private JFreeChart grafico2;
    private JFreeChart grafico3;
    private JFreeChart grafico4;
    private JFreeChart grafico5;
    private JFreeChart grafico6;
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea areaEstadisticasDescripcion;
    private javax.swing.JProgressBar barraDeProgreso;
    private javax.swing.JCheckBox crearTraza;
    private javax.swing.JMenuItem dEliminarMenuItem;
    private javax.swing.JMenuItem dEliminarTodoMenuItem;
    private javax.swing.JMenuItem dOcultarNombresEnlacesMenuItem;
    private javax.swing.JMenuItem dOcultarNombresNodosMenuItem;
    private javax.swing.JMenuItem dPropiedadesMenuItem;
    private javax.swing.JCheckBoxMenuItem dVerNombreMenuItem;
    private javax.swing.JMenuItem dVerNombresEnlacesMenuItem;
    private javax.swing.JMenuItem dVerNombresNodosMenuItem;
    private javax.swing.JTextField descripcionEscenario;
    private javax.swing.JPopupMenu diseElementoPopUp;
    private javax.swing.JPopupMenu diseFondoPopUp;
    private javax.swing.JSlider duracionMs;
    private javax.swing.JSlider duracionNs;
    private javax.swing.JLabel etiquetaDuracionMs;
    private javax.swing.JLabel etiquetaDuracionNs;
    private javax.swing.JLabel etiquetaEstadisticasNombreAutor;
    private javax.swing.JLabel etiquetaEstadisticasTituloEscenario;
    private javax.swing.JLabel etiquetaMlsPorTic;
    private javax.swing.JLabel etiquetaNombreElementoEstadistica;
    private javax.swing.JLabel etiquetaPasoNs;
    private javax.swing.JLabel iconoComenzar;
    private javax.swing.JLabel iconoEmisor;
    private javax.swing.JLabel iconoEnlace;
    private javax.swing.JLabel iconoFinalizar;
    private javax.swing.JLabel iconoLER;
    private javax.swing.JLabel iconoLERA;
    private javax.swing.JLabel iconoLSR;
    private javax.swing.JLabel iconoLSRA;
    private javax.swing.JLabel iconoPausar;
    private javax.swing.JLabel iconoReanudar;
    private javax.swing.JLabel iconoReceptor;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JSlider mlsPorTic;
    private javax.swing.JTextField nombreAutor;
    private javax.swing.JTextField nombreEscenario;
    private javax.swing.JPanel panelAnalisis;
    private javax.swing.JPanel panelAnalisisSuperior;
    private javax.swing.JPanel panelBotonesDisenio;
    private javax.swing.JPanel panelBotonesSimulacion;
    private simMPLS.ui.simulator.JPanelDisenio panelDisenio;
    private javax.swing.JPanel panelDisenioSuperior;
    private javax.swing.JPanel panelFijo;
    private javax.swing.JPanel panelOpciones;
    private javax.swing.JPanel panelOpcionesSuperior;
    private javax.swing.JPanel panelSeleccionElemento;
    private simMPLS.ui.simulator.JSimulationPanel panelSimulacion;
    private javax.swing.JPanel panelSimulacionSuperior;
    private javax.swing.JSlider pasoNs;
    private javax.swing.JComboBox selectorElementoEstadisticas;
    // End of variables declaration//GEN-END:variables
}
