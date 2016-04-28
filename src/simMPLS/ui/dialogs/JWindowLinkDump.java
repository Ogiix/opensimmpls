/* 
 * Copyright 2016 (C) Gaetan Bulpa - gaetan.bulpa@gmail.com.
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

import java.awt.Frame;
import java.util.Iterator;
import java.util.SortedSet;
import simMPLS.protocols.TAbstractPDU;
import simMPLS.protocols.TICMPPDU;
import simMPLS.protocols.TMPLSLabelStack;
import simMPLS.protocols.TMPLSPDU;
import simMPLS.protocols.TTLDPPDU;
import simMPLS.scenario.TLink;
import simMPLS.scenario.TLinkBufferEntry;

/**
 *
 * @author Gaet
 */
public class JWindowLinkDump extends javax.swing.JDialog  {

    /**
     * Creates new customizer JWindowLinkDump
     * @author Gaetan Bulpa
     * @version 1.0
     * @param parent The frame which calls this window
     * @param modal 
     * @param link The link to dump
     */
    public JWindowLinkDump(java.awt.Frame parent, boolean modal, TLink link) {
        super(parent, modal);
        this.parentWindow = parent;
        this.link = link;
        this.buffer = this.link.getBuffer();
        initComponents();
        initComponents2();
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the FormEditor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jComboBoxPacketSelector = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaPacketInfo = new javax.swing.JTextArea();
        jButton1 = new javax.swing.JButton();

        setName(""); // NOI18N
        setLayout(new java.awt.BorderLayout());

        jPanel1.setMinimumSize(new java.awt.Dimension(300, 300));
        jPanel1.setName(""); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(400, 300));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setText("Select the packet you want to dump");
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 20, -1, -1));

        jComboBoxPacketSelector.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBoxPacketSelector.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxPacketSelectorActionPerformed(evt);
            }
        });
        jPanel1.add(jComboBoxPacketSelector, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 50, 180, -1));

        jTextAreaPacketInfo.setColumns(20);
        jTextAreaPacketInfo.setRows(5);
        jTextAreaPacketInfo.setText("Info on packet ...");
        jScrollPane1.setViewportView(jTextAreaPacketInfo);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 90, 290, 150));

        jButton1.setText("Ok");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 260, 80, -1));

        add(jPanel1, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jComboBoxPacketSelectorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxPacketSelectorActionPerformed
        int id = this.jComboBoxPacketSelector.getSelectedIndex();
        Iterator it = this.buffer.iterator();
        int i = 0;
        while (i<id && it.hasNext()) {
            it.next();
            i++;
        }
        if(it.hasNext()){
            TLinkBufferEntry ebe = (TLinkBufferEntry) it.next();
            TAbstractPDU packet = ebe.obtenerPaquete();
            jTextAreaPacketInfo.setText("Info on packet ...");
            this.jTextAreaPacketInfo.append("\nID : "+packet.getID());
            this.jTextAreaPacketInfo.append("\nSource: "+packet.getIPv4Header().getOriginIPAddress());
            this.jTextAreaPacketInfo.append("\nDestination: "+packet.getIPv4Header().getTargetIPv4Address());
            switch(packet.getSubtype()){
                case TAbstractPDU.IPV4 : 
                    this.jTextAreaPacketInfo.append("\nTTL : "+packet.getIPv4Header().getTTL());
                    this.jTextAreaPacketInfo.append("\nProtocol : IPv4");
                    break;
                case TAbstractPDU.ICMP : 
                    this.jTextAreaPacketInfo.append("\nTTL : "+packet.getIPv4Header().getTTL());
                    this.jTextAreaPacketInfo.append("\nProtocol : ICMP");
                    TICMPPDU icmpPacket = (TICMPPDU) packet;
                    this.jTextAreaPacketInfo.append("\nICMP informations :");
                    this.jTextAreaPacketInfo.append("\nType : "+icmpPacket.getTypeICMP());
                    this.jTextAreaPacketInfo.append("\nCode : "+icmpPacket.getCodeICMP());
                    break;
                case TAbstractPDU.MPLS : 
                    this.jTextAreaPacketInfo.append("\nProtocol : MPLS");
                    TMPLSPDU mplsPacket = (TMPLSPDU) packet;
                    this.jTextAreaPacketInfo.append("\nMPLS informations :");
                    TMPLSLabelStack stackToDump = mplsPacket.getLabelStack();
                    for(int j = stackToDump.getSize()-1; j>=0; j--){
                        this.jTextAreaPacketInfo.append("\nLabel : "+stackToDump.getLabelFromID(j).getLabel());
                        this.jTextAreaPacketInfo.append("\nExp : "+stackToDump.getLabelFromID(j).getEXP());
                        this.jTextAreaPacketInfo.append("\nTTL : "+stackToDump.getLabelFromID(j).getTTL());
                    }
                    break;
                case TAbstractPDU.TLDP : 
                    this.jTextAreaPacketInfo.append("\nProtocol : LDP");
                    TTLDPPDU tldpPacket = (TTLDPPDU) packet;
                    this.jTextAreaPacketInfo.append("\nLDP informations :");
                    this.jTextAreaPacketInfo.append("\nLabel requested : "+tldpPacket.getTLDPPayload().getLabel());
                    this.jTextAreaPacketInfo.append("\nRequest type : "+tldpPacket.getTLDPPayload().getTLDPMessageType());
                    break;
            }
        }else {
            jTextAreaPacketInfo.setText("No packet on this link.");
        }
    }//GEN-LAST:event_jComboBoxPacketSelectorActionPerformed

    /**
     * Este m�todo configura aspectos de la ventana que no han podido ser configurados
     * en el constructor.
     * @since 1.0
     */    
    public void initComponents2() {
        java.awt.Dimension tamFrame=this.getSize();
        java.awt.Dimension tamPadre=parentWindow.getSize();
        setLocation((tamPadre.width-tamFrame.width)/2, (tamPadre.height-tamFrame.height)/2);
        this.setTitle("Packet dumper");
        this.setSize(new java.awt.Dimension(400, 350));
        this.jComboBoxPacketSelector.removeAllItems();
        Iterator it = this.buffer.iterator();
        TLinkBufferEntry ebe = null;
        while (it.hasNext()) {
            ebe = (TLinkBufferEntry) it.next();
            this.jComboBoxPacketSelector.addItem(""+ebe.obtenerPaquete().getID());
        }
        if(ebe != null)
            this.jComboBoxPacketSelector.setSelectedIndex(0);
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JComboBox<String> jComboBoxPacketSelector;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextAreaPacketInfo;
    // End of variables declaration//GEN-END:variables

    private TLink link;
    private Frame parentWindow;
    private SortedSet buffer;
    
}
