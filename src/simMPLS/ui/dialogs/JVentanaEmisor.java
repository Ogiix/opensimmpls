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
package simMPLS.ui.dialogs;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import simMPLS.scenario.TSenderNode;
import simMPLS.scenario.TTopology;
import simMPLS.scenario.TNode;
import simMPLS.ui.simulator.JPanelDisenio;
import simMPLS.ui.utils.TImagesBroker;

/**
 * Esta clase implementa una ventana de configuraci�n de un nodo emisor de la
 * topolog�a.
 * @author <B>Manuel Dom�nguez Dorado</B><br><A
 * href="mailto:ingeniero@ManoloDominguez.com">ingeniero@ManoloDominguez.com</A><br><A href="http://www.ManoloDominguez.com" target="_blank">http://www.ManoloDominguez.com</A>
 * @since 1.0
 */
public class JVentanaEmisor extends javax.swing.JDialog {

    /**
     * Esta instancia crea una nueva instancia de JVentanaEmisor
     * @param t Topolog�a dentro de la cual se encuentra el nodo emisor que queremos configurar.
     * @param pad Panel de dise�o dentro del cual estamos dise�ando el nodo emisor.
     * @param di Dispensador de im�genes global de la aplicaci�n.
     * @param parent Ventana padre donde se mostrar� esta ventana de tipo JVentanaEmisor.
     * @param modal TRUE indica que la ventana impedir� que se pueda seleccionar cualquier parte de
     * la interfaz hasta que se cierre. FALSE indica todo lo contrario.
     * @since 1.0
     */
    public JVentanaEmisor(TTopology t, JPanelDisenio pad, TImagesBroker di, java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        ventanaPadre = parent;
        dispensadorDeImagenes = di;
        pd = pad;
        topo = t;
        initComponents();
        initComponents2();
    }

    /**
     * Este m�todo terminar� de configurar algunos aspectos que no hayan quedado
     * terminados de configurar en el constructor.
     * @since 1.0
     */    
    public void initComponents2() {
        panelCoordenadas.ponerPanelOrigen(pd);
        java.awt.Dimension tamFrame=this.getSize();
        java.awt.Dimension tamPadre=ventanaPadre.getSize();
        setLocation((tamPadre.width-tamFrame.width)/2, (tamPadre.height-tamFrame.height)/2);
        emisor = null;
        coordenadaX.setText(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaEmisor.X=_") + panelCoordenadas.obtenerXReal());
        coordenadaY.setText(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaEmisor.Y=_") + panelCoordenadas.obtenerYReal());
        Iterator it = topo.getNodesIterator();
        selectorDelReceptor.removeAllItems();
        selectorDelReceptor.addItem("");
        TNode nt;
        while (it.hasNext()) {
            nt = (TNode) it.next();
            if(nt.getNodeType() != TNode.SENDER)
                selectorDelReceptor.addItem(nt.getName());
        }
        this.selectorDeGoS.removeAllItems();
        this.selectorDeGoS.addItem(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaEmisor.None"));
        this.selectorDeGoS.addItem(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaEmisor.Level_1"));
        this.selectorDeGoS.addItem(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaEmisor.Level_2"));
        this.selectorDeGoS.addItem(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaEmisor.Level_3"));
        this.selectorDeGoS.setSelectedIndex(0);
        this.selectorSencilloTrafico.removeAllItems();
        this.selectorSencilloTrafico.addItem(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaEmisor.Personalized"));
        this.selectorSencilloTrafico.addItem(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaEmisor.Email"));
        this.selectorSencilloTrafico.addItem(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaEmisor.Web"));
        this.selectorSencilloTrafico.addItem(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaEmisor.P2P_file_sharing"));
        this.selectorSencilloTrafico.addItem(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaEmisor.Bank_data_transaction"));
        this.selectorSencilloTrafico.addItem(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaEmisor.Tele-medical_video"));
        this.selectorSencilloTrafico.addItem(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaEmisor.Bulk_traffic"));
        this.selectorSencilloTrafico.addItem("Ping");
        this.selectorSencilloTrafico.addItem("Traceroute");
        this.selectorSencilloTrafico.setSelectedIndex(0);
        selectorDelReceptor.setSelectedIndex(0);
        BKUPTTL=255;
        BKUPDestino = "";
        BKUPLSPDeBackup = false;
        BKUPMostrarNombre = true;
        BKUPNivelDeGos = 0;
        BKUPNombre = "";
        BKUPTasaTrafico = 1000;
        BKUPTipoTrafico = TSenderNode.CONSTANTE;
        BKUPGenerarEstadisticas = false;
        BKUPTamDatosConstante = 1024;
        BKUPEncapsularEnMPLS = false;
        reconfigurando = false;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel10 = new javax.swing.JLabel();
        panelPrincipal = new javax.swing.JPanel();
        panelPestanias = new javax.swing.JTabbedPane();
        panelGeneral = new javax.swing.JPanel();
        iconoEmisor = new javax.swing.JLabel();
        etiquetaNombre = new javax.swing.JLabel();
        nombreNodo = new javax.swing.JTextField();
        panelPosicion = new javax.swing.JPanel();
        coordenadaX = new javax.swing.JLabel();
        coordenadaY = new javax.swing.JLabel();
        panelCoordenadas = new simMPLS.ui.dialogs.JPanelCoordenadas();
        verNombre = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        selectorDelReceptor = new javax.swing.JComboBox();
        panelRapido = new javax.swing.JPanel();
        iconoEnlace1 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        selectorDeGenerarEstadisticasSencillo = new javax.swing.JCheckBox();
        selectorSencilloTrafico = new javax.swing.JComboBox();
        panelAvanzado = new javax.swing.JPanel();
        iconoEnlace2 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        etiquetaTasa = new javax.swing.JLabel();
        selectorDeGenerarEstadisticas = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        selectorDeGoS = new javax.swing.JComboBox();
        selectorLSPDeRespaldo = new javax.swing.JCheckBox();
        jLabel5 = new javax.swing.JLabel();
        traficoConstante = new javax.swing.JRadioButton();
        traficoVariable = new javax.swing.JRadioButton();
        encapsularSobreMPLS = new javax.swing.JCheckBox();
        selectorDeTasa = new javax.swing.JSlider();
        selectorDeTamPaquete = new javax.swing.JSlider();
        etiquetaOctetos = new javax.swing.JLabel();
        etiquetaTamPaquete = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        ipAddress = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        ttlSelector = new javax.swing.JSpinner();
        jPanel2 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        icmpRadioButton = new javax.swing.JRadioButton();
        jLabel9 = new javax.swing.JLabel();
        icmpTypeSelector = new javax.swing.JSpinner();
        jLabel12 = new javax.swing.JLabel();
        tracerouteRadioButton = new javax.swing.JRadioButton();
        jLabel13 = new javax.swing.JLabel();
        saveRadioButton = new javax.swing.JRadioButton();
        panelBotones = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jScrollPane1.setViewportView(jTextArea1);

        jLabel10.setText("TTL");

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes"); // NOI18N
        setTitle(bundle.getString("VentanaEmisor.TituloVentana")); // NOI18N
        setModal(true);
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        panelPrincipal.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        panelPestanias.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N

        panelGeneral.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        iconoEmisor.setIcon(dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.EMISOR));
        iconoEmisor.setText(bundle.getString("VentanaEmisor.DescripcionNodo")); // NOI18N
        panelGeneral.add(iconoEmisor, new org.netbeans.lib.awtextra.AbsoluteConstraints(15, 20, 335, -1));

        etiquetaNombre.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        etiquetaNombre.setText(bundle.getString("VentanaEmisor.Etiqueta.NombreNodo")); // NOI18N
        panelGeneral.add(etiquetaNombre, new org.netbeans.lib.awtextra.AbsoluteConstraints(215, 105, 120, -1));

        nombreNodo.setToolTipText(bundle.getString("VentanaEmisor.tooltip.Nombre")); // NOI18N
        panelGeneral.add(nombreNodo, new org.netbeans.lib.awtextra.AbsoluteConstraints(215, 130, 125, -1));

        panelPosicion.setBorder(javax.swing.BorderFactory.createTitledBorder(bundle.getString("VentanaEmisor.Etiqueta.Posicion"))); // NOI18N
        panelPosicion.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        coordenadaX.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        coordenadaX.setText(bundle.getString("VentanaEmisor.X=_45")); // NOI18N
        panelPosicion.add(coordenadaX, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 100, -1, -1));

        coordenadaY.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        coordenadaY.setText(bundle.getString("VentanaEmisor.Y=_1024")); // NOI18N
        panelPosicion.add(coordenadaY, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 100, -1, -1));

        panelCoordenadas.setBackground(new java.awt.Color(255, 255, 255));
        panelCoordenadas.setToolTipText(bundle.getString("VentanaEmisor.tooltip.posicion")); // NOI18N
        panelCoordenadas.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                clicEnPanelCoordenadas(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ratonEntraEnPanelCoordenadas(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ratonSaleDePanelCoordenadas(evt);
            }
        });
        panelPosicion.add(panelCoordenadas, new org.netbeans.lib.awtextra.AbsoluteConstraints(25, 25, 130, 70));

        panelGeneral.add(panelPosicion, new org.netbeans.lib.awtextra.AbsoluteConstraints(15, 100, 180, 125));

        verNombre.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        verNombre.setSelected(true);
        verNombre.setText(bundle.getString("VentanaEmisor.Etiqueta.VerNombre")); // NOI18N
        verNombre.setToolTipText(bundle.getString("VentanaEmisor.tooltip.VerNombre")); // NOI18N
        panelGeneral.add(verNombre, new org.netbeans.lib.awtextra.AbsoluteConstraints(215, 175, -1, -1));

        jLabel6.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel6.setText(bundle.getString("VentanaEmisor.DestinoTrafico")); // NOI18N
        panelGeneral.add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 245, 170, -1));

        selectorDelReceptor.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        selectorDelReceptor.setToolTipText(bundle.getString("VentanaEmisor.tooltip.destinodeltrafico")); // NOI18N
        panelGeneral.add(selectorDelReceptor, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 240, -1, -1));

        panelPestanias.addTab(bundle.getString("VentanaEmisor.Tab.General"), panelGeneral); // NOI18N

        panelRapido.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        iconoEnlace1.setIcon(dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.ASISTENTE));
        iconoEnlace1.setText(bundle.getString("VentanaEmisor.configuracionRapida")); // NOI18N
        panelRapido.add(iconoEnlace1, new org.netbeans.lib.awtextra.AbsoluteConstraints(15, 20, 335, -1));

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText(bundle.getString("VentanaEmisor.TipoDeTrafico1")); // NOI18N
        panelRapido.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 125, 115, -1));

        selectorDeGenerarEstadisticasSencillo.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        selectorDeGenerarEstadisticasSencillo.setText(bundle.getString("VentanaEmisor.GenerarEstadisticas1")); // NOI18N
        selectorDeGenerarEstadisticasSencillo.setToolTipText(bundle.getString("VentanaEmisor.GenerarEstadisticas1")); // NOI18N
        selectorDeGenerarEstadisticasSencillo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnGenerarEstadisticasSencillo(evt);
            }
        });
        panelRapido.add(selectorDeGenerarEstadisticasSencillo, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 195, -1, -1));

        selectorSencilloTrafico.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        selectorSencilloTrafico.setMaximumRowCount(9);
        selectorSencilloTrafico.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Personalized", "Email", "Web", "P2P file sharing", "Bank data transaction", "Tele-medical video", "Bulk traffic", "Ping" }));
        selectorSencilloTrafico.setToolTipText(bundle.getString("VentanaEmisor.TipoDeTrafico1")); // NOI18N
        selectorSencilloTrafico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnSelectorSencilloTrafico(evt);
            }
        });
        panelRapido.add(selectorSencilloTrafico, new org.netbeans.lib.awtextra.AbsoluteConstraints(145, 120, -1, -1));

        panelPestanias.addTab(bundle.getString("VentanaEmisor.Tab.Rapida"), panelRapido); // NOI18N

        panelAvanzado.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        iconoEnlace2.setIcon(dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.AVANZADA));
        iconoEnlace2.setText(bundle.getString("VentanaEmisor.ConfiguracionAvanzada")); // NOI18N
        panelAvanzado.add(iconoEnlace2, new org.netbeans.lib.awtextra.AbsoluteConstraints(15, 20, 335, -1));

        jLabel2.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText(bundle.getString("VentanaEmisor.TasaDeTrafico")); // NOI18N
        panelAvanzado.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(15, 90, 100, -1));

        etiquetaTasa.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        etiquetaTasa.setForeground(new java.awt.Color(102, 102, 102));
        etiquetaTasa.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        etiquetaTasa.setText(bundle.getString("VentanaEmisor.Kbpsinicial")); // NOI18N
        panelAvanzado.add(etiquetaTasa, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 90, 70, -1));

        selectorDeGenerarEstadisticas.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        selectorDeGenerarEstadisticas.setText(bundle.getString("VentanaEmisor.GenerarEstadisticas2")); // NOI18N
        selectorDeGenerarEstadisticas.setToolTipText(bundle.getString("VentanaEmisor.tooltip.GenerarEstadisticas2")); // NOI18N
        selectorDeGenerarEstadisticas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnGenerarEstadisticasAvanzado(evt);
            }
        });
        panelAvanzado.add(selectorDeGenerarEstadisticas, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 270, -1, -1));

        jLabel4.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel4.setText(bundle.getString("VentanaEmisor.NivelDeGoS")); // NOI18N
        panelAvanzado.add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 240, 85, -1));

        selectorDeGoS.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        selectorDeGoS.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "None", "Level 1", "Level 2", "Level 3" }));
        selectorDeGoS.setToolTipText(bundle.getString("VentanaEmisor.tooltip.nivelDeGoS")); // NOI18N
        selectorDeGoS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnNivelGoS(evt);
            }
        });
        panelAvanzado.add(selectorDeGoS, new org.netbeans.lib.awtextra.AbsoluteConstraints(100, 230, -1, -1));

        selectorLSPDeRespaldo.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        selectorLSPDeRespaldo.setText(bundle.getString("VentanaEmisor.CrearLSPBackup")); // NOI18N
        selectorLSPDeRespaldo.setToolTipText(bundle.getString("VentanaEmisor.tooltip.crearUnLSPdeBackup")); // NOI18N
        selectorLSPDeRespaldo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnLSPDeRespaldo(evt);
            }
        });
        panelAvanzado.add(selectorLSPDeRespaldo, new org.netbeans.lib.awtextra.AbsoluteConstraints(200, 230, -1, -1));

        jLabel5.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel5.setText(bundle.getString("VentanaEmisor.TipoDeTrafico3")); // NOI18N
        panelAvanzado.add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(15, 125, 100, -1));

        buttonGroup1.add(traficoConstante);
        traficoConstante.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        traficoConstante.setText(bundle.getString("VentanaEmisor.TraficoConstante")); // NOI18N
        traficoConstante.setToolTipText(bundle.getString("VentanaEmisor.tooltip.traficoConstante")); // NOI18N
        traficoConstante.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnTraficoConstante(evt);
            }
        });
        panelAvanzado.add(traficoConstante, new org.netbeans.lib.awtextra.AbsoluteConstraints(125, 125, -1, 20));

        buttonGroup1.add(traficoVariable);
        traficoVariable.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        traficoVariable.setSelected(true);
        traficoVariable.setText(bundle.getString("VentanaEmisor.TraficoVariable")); // NOI18N
        traficoVariable.setToolTipText(bundle.getString("VentanaEmisor.tooltip.traficovariable")); // NOI18N
        traficoVariable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnTraficoVariable(evt);
            }
        });
        panelAvanzado.add(traficoVariable, new org.netbeans.lib.awtextra.AbsoluteConstraints(225, 125, -1, 20));

        encapsularSobreMPLS.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        encapsularSobreMPLS.setText(bundle.getString("VentanaEmisor.EncapsularSobreMPLS")); // NOI18N
        encapsularSobreMPLS.setToolTipText(bundle.getString("VentanaEmisor.tooltip.encapsularsobrempls")); // NOI18N
        encapsularSobreMPLS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnEncapsularSobreMPLS(evt);
            }
        });
        panelAvanzado.add(encapsularSobreMPLS, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 190, -1, -1));

        selectorDeTasa.setMajorTickSpacing(1000);
        selectorDeTasa.setMaximum(10240);
        selectorDeTasa.setMinimum(1);
        selectorDeTasa.setMinorTickSpacing(100);
        selectorDeTasa.setToolTipText(bundle.getString("VentanaEmisor.tooltipo.CambiarTasa")); // NOI18N
        selectorDeTasa.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                cambioEnSelectorDeTasa(evt);
            }
        });
        panelAvanzado.add(selectorDeTasa, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 90, 165, -1));

        selectorDeTamPaquete.setMajorTickSpacing(1000);
        selectorDeTamPaquete.setMaximum(65495);
        selectorDeTamPaquete.setMinorTickSpacing(100);
        selectorDeTamPaquete.setToolTipText(bundle.getString("VentanaEmisor.tooltipo.CambiarTasa")); // NOI18N
        selectorDeTamPaquete.setValue(1024);
        selectorDeTamPaquete.setEnabled(false);
        selectorDeTamPaquete.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                clicEnSelectorDeTamPaquete(evt);
            }
        });
        panelAvanzado.add(selectorDeTamPaquete, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, 160, 120, -1));

        etiquetaOctetos.setFont(new java.awt.Font("Dialog", 0, 10)); // NOI18N
        etiquetaOctetos.setForeground(new java.awt.Color(102, 102, 102));
        etiquetaOctetos.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        etiquetaOctetos.setText("null");
        etiquetaOctetos.setEnabled(false);
        panelAvanzado.add(etiquetaOctetos, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 160, 70, -1));

        etiquetaTamPaquete.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        etiquetaTamPaquete.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        etiquetaTamPaquete.setText(bundle.getString("JVentanaEmisor.TamCargaUtil")); // NOI18N
        etiquetaTamPaquete.setEnabled(false);
        panelAvanzado.add(etiquetaTamPaquete, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 160, 140, -1));

        panelPestanias.addTab(bundle.getString("VentanaEmisor.Tab.Avanzada"), panelAvanzado); // NOI18N

        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel3.setText("Configuration of the node");
        jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 10, -1, -1));

        jLabel7.setText("IP address :");
        jPanel1.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 50, -1, -1));
        jPanel1.add(ipAddress, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 50, 160, -1));

        jLabel11.setText("TTL");
        jPanel1.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 100, -1, -1));

        ttlSelector.setModel(new javax.swing.SpinnerNumberModel(255, 1, 255, 1));
        jPanel1.add(ttlSelector, new org.netbeans.lib.awtextra.AbsoluteConstraints(150, 100, 70, -1));

        panelPestanias.addTab("Configuration", jPanel1);

        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel8.setText("Activate ICMP");
        jPanel2.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 40, -1, -1));

        icmpRadioButton.setText("ICMP");
        jPanel2.add(icmpRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 40, -1, -1));

        jLabel9.setText("ICMP Type");
        jPanel2.add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 100, -1, -1));

        icmpTypeSelector.setModel(new javax.swing.SpinnerNumberModel(0, null, 300, 1));
        jPanel2.add(icmpTypeSelector, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 100, -1, -1));

        jLabel12.setText("Traceroute");
        jPanel2.add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 150, -1, -1));
        jPanel2.add(tracerouteRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 150, -1, -1));

        jLabel13.setText("Save ICMP trace");
        jPanel2.add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 190, -1, -1));
        jPanel2.add(saveRadioButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(180, 190, -1, -1));

        panelPestanias.addTab("ICMP", jPanel2);

        panelPrincipal.add(panelPestanias, new org.netbeans.lib.awtextra.AbsoluteConstraints(15, 15, 370, 330));

        getContentPane().add(panelPrincipal, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 400, 350));

        panelBotones.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jButton2.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButton2.setIcon(dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.ACEPTAR));
        jButton2.setMnemonic(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaEmisor.botones.Aceptar").charAt(0));
        jButton2.setText(bundle.getString("VentanaEmisor.Boton.Aceptar.Texto")); // NOI18N
        jButton2.setToolTipText(bundle.getString("VentanaEmisor.tooltip.Aceptar")); // NOI18N
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnAceptar(evt);
            }
        });
        panelBotones.add(jButton2, new org.netbeans.lib.awtextra.AbsoluteConstraints(15, 15, 110, -1));

        jButton3.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButton3.setIcon(dispensadorDeImagenes.obtenerIcono(simMPLS.ui.utils.TImagesBroker.CANCELAR));
        jButton3.setMnemonic(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaEmisor.botones.Cancelar").charAt(0));
        jButton3.setText(bundle.getString("VentanaEmisor.Boton.Cancelar.Texto")); // NOI18N
        jButton3.setToolTipText(bundle.getString("VentanaEmisor.tooltip.Cancelar")); // NOI18N
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clicEnCancelar(evt);
            }
        });
        panelBotones.add(jButton3, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 15, 110, -1));

        getContentPane().add(panelBotones, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 340, 400, 50));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void clicEnSelectorSencilloTrafico(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnSelectorSencilloTrafico
        int seleccionado = this.selectorSencilloTrafico.getSelectedIndex();
        if (seleccionado > 0) {
            if (seleccionado == 1) {
                this.selectorLSPDeRespaldo.setSelected(false);
                this.selectorDeGoS.setSelectedIndex(0);
                this.traficoConstante.setSelected(false);
                this.traficoVariable.setSelected(true);
                this.selectorDeTamPaquete.setValue(0);
                this.selectorDeTasa.setValue(1);
                this.selectorDeTamPaquete.setEnabled(false);
                this.icmpRadioButton.setSelected(false);
                this.ttlSelector.setValue(255);
                this.tracerouteRadioButton.setSelected(false);
            } else if (seleccionado == 2) {
                this.selectorLSPDeRespaldo.setSelected(false);
                this.selectorDeGoS.setSelectedIndex(0);
                this.traficoConstante.setSelected(false);
                this.traficoVariable.setSelected(true);
                this.selectorDeTamPaquete.setValue(0);
                this.selectorDeTasa.setValue(7);
                this.selectorDeTamPaquete.setEnabled(false);
                this.icmpRadioButton.setSelected(false);
                this.ttlSelector.setValue(255);
                this.tracerouteRadioButton.setSelected(false);
            } else if (seleccionado == 3) {
                this.selectorLSPDeRespaldo.setSelected(false);
                this.selectorDeGoS.setSelectedIndex(0);
                this.traficoConstante.setSelected(false);
                this.traficoVariable.setSelected(true);
                this.selectorDeTamPaquete.setValue(0);
                this.selectorDeTasa.setValue(3413);
                this.selectorDeTamPaquete.setEnabled(false);
                this.icmpRadioButton.setSelected(false);
                this.ttlSelector.setValue(255);
                this.tracerouteRadioButton.setSelected(false);
            } else if (seleccionado == 4) {
                this.selectorLSPDeRespaldo.setSelected(true);
                this.selectorDeGoS.setSelectedIndex(0);
                this.traficoConstante.setSelected(false);
                this.traficoVariable.setSelected(true);
                this.selectorDeTamPaquete.setValue(0);
                this.selectorDeTasa.setValue(10240);
                this.selectorDeTamPaquete.setEnabled(false);
                this.icmpRadioButton.setSelected(false);
                this.ttlSelector.setValue(255);
                this.tracerouteRadioButton.setSelected(false);
            } else if (seleccionado == 5) {
                this.selectorLSPDeRespaldo.setSelected(true);
                this.selectorDeGoS.setSelectedIndex(2);
                this.traficoConstante.setSelected(false);
                this.traficoVariable.setSelected(true);
                this.selectorDeTamPaquete.setValue(0);
                this.selectorDeTasa.setValue(341);
                this.selectorDeTamPaquete.setEnabled(false);
                this.icmpRadioButton.setSelected(false);
                this.ttlSelector.setValue(255);
                this.tracerouteRadioButton.setSelected(false);
            } else if (seleccionado == 6) {
                this.selectorLSPDeRespaldo.setSelected(false);
                this.selectorDeGoS.setSelectedIndex(0);
                this.traficoConstante.setSelected(false);
                this.traficoVariable.setSelected(true);
                this.selectorDeTamPaquete.setValue(0);
                this.selectorDeTasa.setValue(6827);
                this.selectorDeTamPaquete.setEnabled(false);
                this.icmpRadioButton.setSelected(false);
                this.ttlSelector.setValue(255);
                this.tracerouteRadioButton.setSelected(false);
            } else if (seleccionado == 7) {
                this.selectorLSPDeRespaldo.setSelected(false);
                this.selectorDeGoS.setSelectedIndex(0);
                this.traficoConstante.setSelected(true);
                this.traficoVariable.setSelected(false);
                this.selectorDeTamPaquete.setValue(0);
                this.selectorDeTasa.setValue(7);
                this.selectorDeTamPaquete.setEnabled(false);
                this.icmpRadioButton.setSelected(true);
                this.icmpTypeSelector.setValue(8);
                this.ttlSelector.setValue(255);
                this.tracerouteRadioButton.setSelected(false);
            } else if (seleccionado == 8) {
                this.selectorLSPDeRespaldo.setSelected(false);
                this.selectorDeGoS.setSelectedIndex(0);
                this.traficoConstante.setSelected(true);
                this.traficoVariable.setSelected(false);
                this.selectorDeTamPaquete.setValue(0);
                this.selectorDeTasa.setValue(7);
                this.selectorDeTamPaquete.setEnabled(false);
                this.icmpRadioButton.setSelected(true);
                this.icmpTypeSelector.setValue(8);
                this.ttlSelector.setValue(255);
                this.tracerouteRadioButton.setSelected(true);
            }
        }
        this.selectorSencilloTrafico.setSelectedIndex(seleccionado);
    }//GEN-LAST:event_clicEnSelectorSencilloTrafico

    private void clicEnSelectorDeTamPaquete(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_clicEnSelectorDeTamPaquete
    int tamSeleccionado = this.selectorDeTamPaquete.getValue();
    String unidades = java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaEmisor.Octetos");
    this.etiquetaOctetos.setText(tamSeleccionado + " " +unidades);
    }//GEN-LAST:event_clicEnSelectorDeTamPaquete

private void clicEnGenerarEstadisticasAvanzado(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnGenerarEstadisticasAvanzado
    this.selectorDeGenerarEstadisticasSencillo.setSelected(this.selectorDeGenerarEstadisticas.isSelected());
}//GEN-LAST:event_clicEnGenerarEstadisticasAvanzado

private void clicEnGenerarEstadisticasSencillo(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnGenerarEstadisticasSencillo
    this.selectorDeGenerarEstadisticas.setSelected(this.selectorDeGenerarEstadisticasSencillo.isSelected());
}//GEN-LAST:event_clicEnGenerarEstadisticasSencillo

private void clicEnLSPDeRespaldo(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnLSPDeRespaldo
    this.selectorSencilloTrafico.setSelectedIndex(0);
}//GEN-LAST:event_clicEnLSPDeRespaldo

private void clicEnNivelGoS(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnNivelGoS
    this.selectorSencilloTrafico.setSelectedIndex(0);
}//GEN-LAST:event_clicEnNivelGoS

private void clicEnEncapsularSobreMPLS(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnEncapsularSobreMPLS
    this.selectorSencilloTrafico.setSelectedIndex(0);
}//GEN-LAST:event_clicEnEncapsularSobreMPLS

private void clicEnTraficoVariable(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnTraficoVariable
    this.selectorSencilloTrafico.setSelectedIndex(0);
    this.selectorDeTamPaquete.setEnabled(false);
    this.etiquetaOctetos.setEnabled(false);
    this.etiquetaTamPaquete.setEnabled(false);
}//GEN-LAST:event_clicEnTraficoVariable

private void clicEnTraficoConstante(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnTraficoConstante
    this.selectorSencilloTrafico.setSelectedIndex(0);
    this.selectorDeTamPaquete.setEnabled(true);
    this.etiquetaOctetos.setEnabled(true);
    this.etiquetaTamPaquete.setEnabled(true);
}//GEN-LAST:event_clicEnTraficoConstante

private void cambioEnSelectorDeTasa(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_cambioEnSelectorDeTasa
    this.selectorSencilloTrafico.setSelectedIndex(0);
    int tasaSeleccionada = this.selectorDeTasa.getValue();
    String unidades = java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaEmisor.unidades.kbps");
    this.etiquetaTasa.setText(tasaSeleccionada + " " +unidades);
}//GEN-LAST:event_cambioEnSelectorDeTasa

private void clicEnCancelar(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnCancelar
    if (reconfigurando) {
        emisor.ponerDestino(BKUPDestino);
        emisor.ponerLSPDeBackup(BKUPLSPDeBackup);
        emisor.setShowName(BKUPMostrarNombre);
        emisor.ponerNivelDeGoS(BKUPNivelDeGos);
        emisor.setName(BKUPNombre);
        emisor.setIPAddress(BKUPIP);
        emisor.ponerTasaTrafico(BKUPTasaTrafico);
        emisor.ponerTipoTrafico(BKUPTipoTrafico);
        emisor.setIsICMP(BKUPIsICMP);
        emisor.setTypeICMP(BKUPICMPType);
        emisor.setCustomTTL(BKUPTTL);
        emisor.setTraceroute(BKUPTraceroute);
        emisor.setSaving(BKUPSave);
        emisor.setGenerateStats(BKUPGenerarEstadisticas);
        emisor.ponerTamDatosConstante(BKUPTamDatosConstante);
        emisor.setWellConfigured(true);
        emisor.ponerSobreMPLS(BKUPEncapsularEnMPLS);
        reconfigurando = false;
    } else {
        emisor.setWellConfigured(false);
    }
    this.setVisible(false);
    this.dispose();
}//GEN-LAST:event_clicEnCancelar

private void clicEnAceptar(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clicEnAceptar
    emisor.setWellConfigured(true);
    if (!this.reconfigurando){
        emisor.setPosition(new Point(panelCoordenadas.obtenerXReal(),panelCoordenadas.obtenerYReal()));
    }
    emisor.setName(nombreNodo.getText());
    emisor.setShowName(verNombre.isSelected());
    emisor.setGenerateStats(this.selectorDeGenerarEstadisticas.isSelected());
    emisor.ponerTasaTrafico(this.selectorDeTasa.getValue());
    emisor.ponerLSPDeBackup(this.selectorLSPDeRespaldo.isSelected());
    emisor.ponerSobreMPLS(this.encapsularSobreMPLS.isSelected());
    emisor.ponerNivelDeGoS(this.selectorDeGoS.getSelectedIndex());
    emisor.ponerDestino((String) this.selectorDelReceptor.getSelectedItem());
    emisor.ponerTamDatosConstante(this.selectorDeTamPaquete.getValue());
    emisor.setIsICMP(this.icmpRadioButton.isSelected());
    emisor.setTypeICMP((int)this.icmpTypeSelector.getValue());
    emisor.setCustomTTL((int)this.ttlSelector.getValue());
    emisor.setTraceroute(this.tracerouteRadioButton.isSelected());
    emisor.setSaving(this.saveRadioButton.isSelected());
    if(ipAddress.getText().isEmpty()== false || this.reconfigurando){
        emisor.setIPAddress(ipAddress.getText());
    }
    if (this.traficoConstante.isSelected()) {
        emisor.ponerTipoTrafico(TSenderNode.CONSTANTE);
    } else if (this.traficoVariable.isSelected()) {
        emisor.ponerTipoTrafico(TSenderNode.VARIABLE);
    }
    int error = emisor.validateConfig(topo, this.reconfigurando);
    if (error != TSenderNode.CORRECTA) {
        JVentanaAdvertencia va = new JVentanaAdvertencia(ventanaPadre, true, dispensadorDeImagenes);
        va.mostrarMensaje(emisor.getErrorMessage(error));
        va.show();
    } else {
        this.reconfigurando = false;
        this.setVisible(false);
        this.dispose();
    }
}//GEN-LAST:event_clicEnAceptar

private void clicEnPanelCoordenadas(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clicEnPanelCoordenadas
    if (evt.getButton() == MouseEvent.BUTTON1) {
        panelCoordenadas.ponerCoordenadasReducidas(evt.getPoint());
        coordenadaX.setText(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaEmisor.X=_") + panelCoordenadas.obtenerXReal());
        coordenadaY.setText(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("VentanaEmisor.Y=_") + panelCoordenadas.obtenerYReal());
        panelCoordenadas.repaint();
    }
}//GEN-LAST:event_clicEnPanelCoordenadas

private void ratonSaleDePanelCoordenadas(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonSaleDePanelCoordenadas
    this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
}//GEN-LAST:event_ratonSaleDePanelCoordenadas

private void ratonEntraEnPanelCoordenadas(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratonEntraEnPanelCoordenadas
    this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
}//GEN-LAST:event_ratonEntraEnPanelCoordenadas

    /** Closes the dialog */
    private void closeDialog(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_closeDialog
        setVisible(false);
        emisor.setWellConfigured(false);
        dispose();
    }//GEN-LAST:event_closeDialog

    /**
     * Este m�todo permite cargar en la ventana la configuraci�n actual del nodo emisor
     * que estamos configurando.
     * @since 1.0
     * @param recfg TRUE indica que estamos reconfigurando el emisor. FALSE, que el emisor est�
     * siendo insertado desde cero.
     * @param tce Nodo emisor que estamos configurando.
     */    
    public void ponerConfiguracion(TSenderNode tce, boolean recfg) {
        emisor = tce;
        reconfigurando = recfg;
        if (reconfigurando) {
            this.panelCoordenadas.setEnabled(false);
            this.panelCoordenadas.setToolTipText(null);
            TNode nt = this.topo.obtenerNodo(emisor.obtenerDestino());
            if (nt != null) {
                BKUPDestino = nt.getName();
            }
            BKUPLSPDeBackup = emisor.obtenerLSPDeBackup();
            BKUPMostrarNombre = emisor.getShowName();
            BKUPNivelDeGos = emisor.obtenerNivelDeGoS();
            BKUPNombre = emisor.getName();
            BKUPIP = emisor.getIPAddress();
            BKUPTasaTrafico = emisor.obtenerTasaTrafico();
            BKUPTipoTrafico = emisor.obtenerTipoTrafico();
            BKUPIsICMP = emisor.getIsICMP();
            BKUPICMPType = emisor.getTypeICMP();
            BKUPTTL = emisor.getCustomTTL();
            BKUPTraceroute = emisor.isTraceroute();
            BKUPSave = emisor.isSaving();
            BKUPGenerarEstadisticas = emisor.isGeneratingStats();
            BKUPTamDatosConstante = emisor.obtenerTamDatosConstante();
            this.BKUPEncapsularEnMPLS = emisor.obtenerSobreMPLS();
            
            
            this.encapsularSobreMPLS.setSelected(BKUPEncapsularEnMPLS);
            this.nombreNodo.setText(BKUPNombre);
            this.ipAddress.setText(BKUPIP);
            if (BKUPTipoTrafico == TSenderNode.CONSTANTE) {
                this.traficoConstante.setSelected(true);
                this.traficoVariable.setSelected(false);
            } else {
                this.traficoConstante.setSelected(false);
                this.traficoVariable.setSelected(true);
            }
            this.icmpRadioButton.setSelected(BKUPIsICMP);
            this.icmpTypeSelector.setValue(BKUPICMPType);
            if(BKUPTTL > 0 && BKUPTTL < 256)
                this.ttlSelector.setValue(BKUPTTL);
            else
                this.ttlSelector.setValue(255);
            this.tracerouteRadioButton.setSelected(BKUPTraceroute);
            this.saveRadioButton.setSelected(BKUPSave);
            this.selectorDeGenerarEstadisticas.setSelected(BKUPGenerarEstadisticas);
            this.selectorDeGenerarEstadisticasSencillo.setSelected(BKUPGenerarEstadisticas);
            this.selectorLSPDeRespaldo.setSelected(BKUPLSPDeBackup);
            this.verNombre.setSelected(BKUPMostrarNombre);
            int numDestinos = selectorDelReceptor.getItemCount();
            int i = 0;
            String destinoAux;
            for (i = 0; i<numDestinos; i++) {
                destinoAux = (String) selectorDelReceptor.getItemAt(i);
                if (destinoAux.equals(BKUPDestino)) {
                    selectorDelReceptor.setSelectedIndex(i);
                }
            }
            if (this.selectorDeGoS.getItemCount() >= BKUPNivelDeGos) {
                this.selectorDeGoS.setSelectedIndex(BKUPNivelDeGos);
            }
            this.selectorSencilloTrafico.setSelectedIndex(0);
            this.selectorDeTasa.setValue(BKUPTasaTrafico);

            if (BKUPTipoTrafico == TSenderNode.CONSTANTE) {
                this.selectorDeTamPaquete.setEnabled(true);
                this.etiquetaOctetos.setEnabled(true);
                this.etiquetaTamPaquete.setEnabled(true);
                String unidades = java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("JVentanaEmisor.Octetos");
                this.etiquetaOctetos.setText(this.BKUPTamDatosConstante + " " +unidades);        }
                this.selectorDeTamPaquete.setValue(this.BKUPTamDatosConstante);
            }
    }

    private TImagesBroker dispensadorDeImagenes;
    private Frame ventanaPadre;
    private JPanelDisenio pd;
    private TSenderNode emisor;
    private TTopology topo;
    
    private String BKUPDestino;
    private boolean BKUPLSPDeBackup;
    private boolean BKUPMostrarNombre;
    private int BKUPNivelDeGos;
    private String BKUPNombre;
    private String BKUPIP;
    private int BKUPTasaTrafico;
    private int BKUPTipoTrafico;
    private boolean BKUPIsICMP;
    private int BKUPICMPType;
    private int BKUPTTL;
    private boolean BKUPTraceroute;
    private boolean BKUPSave;
    private boolean BKUPGenerarEstadisticas;
    private int BKUPTamDatosConstante;
    private boolean BKUPEncapsularEnMPLS;

    private boolean reconfigurando;
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel coordenadaX;
    private javax.swing.JLabel coordenadaY;
    private javax.swing.JCheckBox encapsularSobreMPLS;
    private javax.swing.JLabel etiquetaNombre;
    private javax.swing.JLabel etiquetaOctetos;
    private javax.swing.JLabel etiquetaTamPaquete;
    private javax.swing.JLabel etiquetaTasa;
    private javax.swing.JRadioButton icmpRadioButton;
    private javax.swing.JSpinner icmpTypeSelector;
    private javax.swing.JLabel iconoEmisor;
    private javax.swing.JLabel iconoEnlace1;
    private javax.swing.JLabel iconoEnlace2;
    private javax.swing.JTextField ipAddress;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField nombreNodo;
    private javax.swing.JPanel panelAvanzado;
    private javax.swing.JPanel panelBotones;
    private simMPLS.ui.dialogs.JPanelCoordenadas panelCoordenadas;
    private javax.swing.JPanel panelGeneral;
    private javax.swing.JTabbedPane panelPestanias;
    private javax.swing.JPanel panelPosicion;
    private javax.swing.JPanel panelPrincipal;
    private javax.swing.JPanel panelRapido;
    private javax.swing.JRadioButton saveRadioButton;
    private javax.swing.JCheckBox selectorDeGenerarEstadisticas;
    private javax.swing.JCheckBox selectorDeGenerarEstadisticasSencillo;
    private javax.swing.JComboBox selectorDeGoS;
    private javax.swing.JSlider selectorDeTamPaquete;
    private javax.swing.JSlider selectorDeTasa;
    private javax.swing.JComboBox selectorDelReceptor;
    private javax.swing.JCheckBox selectorLSPDeRespaldo;
    private javax.swing.JComboBox selectorSencilloTrafico;
    private javax.swing.JRadioButton tracerouteRadioButton;
    private javax.swing.JRadioButton traficoConstante;
    private javax.swing.JRadioButton traficoVariable;
    private javax.swing.JSpinner ttlSelector;
    private javax.swing.JCheckBox verNombre;
    // End of variables declaration//GEN-END:variables

}
