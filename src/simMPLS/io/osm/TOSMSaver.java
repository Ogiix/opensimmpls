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
package simMPLS.io.osm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.zip.CRC32;
import simMPLS.scenario.TScenario;
import simMPLS.scenario.TLink;
import simMPLS.scenario.TNode;

/**
 * This class implements a class that stores a scenario to disk in OSM (Open
 * SimMPLS format).
 *
 * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
 * @version 1.1
 */
public class TOSMSaver {

    /**
     * This method is the constructor of the class. It creates a new instance of
     * TOSMSaver.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @param scenario The TScenario object to be stored in disk.
     * @since 1.0
     */
    public TOSMSaver(TScenario scenario) {
        this.scenario = scenario;
        this.outputStream = null;
        this.output = null;
        this.scenarioCRC = new CRC32();
    }

    /**
     * This method saves a scenario to a disk file.
     *
     * @author Manuel Domínguez Dorado - ingeniero@ManoloDominguez.com
     * @param outputFile The file where the scenario will be stored.
     * @param createCRC If true, a CRC hash of the file will be computed to
     * assure no manual modifications. If false, no CRC will be computed.
     * @param saveTables true if you want to save the routing tables of the nodes.
     * @return True, if the scenario can be saved successful. Otherwise, returns
     * false.
     * @since 1.0
     */
    public boolean save(File outputFile, boolean createCRC, boolean saveTables) {
        try {
            TNode auxNode;
            TLink auxLink;
            Iterator auxIterator;
            this.outputStream = new FileOutputStream(outputFile);
            this.output = new PrintStream(this.outputStream);
            this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.asteriscos"));
            this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.GeneradoPor"));
            this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.blanco"));
            this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.NoSeDebeModificarEsteFichero"));
            this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.PorqueIncorporaUnCodigoCRCParaQue"));
            this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.SimuladorPuedaComprobarSuIntegridad"));
            this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.ElSimuladorLoPodriaDetectarComoUn"));
            this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.FicheroCorrupto"));
            this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.asteriscos"));
            this.output.println();
            this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.asteriscos"));
            this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.DefinicionGlobalDelEscenario"));
            this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.asteriscos"));
            this.output.println();
            this.output.println("@?Scenario");
            this.scenarioCRC.update("@?Scenario".getBytes());
            this.output.println();
            this.output.println(this.scenario.marshallTitle());
            this.scenarioCRC.update(this.scenario.marshallTitle().getBytes());
            this.output.println(this.scenario.marshallAuthor());
            this.scenarioCRC.update(this.scenario.marshallAuthor().getBytes());
            this.output.println(this.scenario.marshallDescription());
            this.scenarioCRC.update(this.scenario.marshallDescription().getBytes());
            this.output.println(this.scenario.getSimulation().marshallTimeParameters());
            this.scenarioCRC.update(this.scenario.getSimulation().marshallTimeParameters().getBytes());
            this.output.println();
            this.output.println("@!Scenario");
            this.scenarioCRC.update("@!Scenario".getBytes());
            this.output.println();
            this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.asteriscos"));
            this.output.println("// Definition of the nodes, have to follow this pattern :");
            this.output.println("// #Receiver#id#Name#IP#Show name ?#generate stats ?#x position#y position#");
            this.output.println("// #LER#id#Name#IP#Show name ?#generate stats ?#x position#y position#MB potential#MB ports#Propagate TTL#Penultimate Hop Popping#");
            this.output.println("// #LSR#id#Name#IP#Show name ?#generate stats ?#x position#y position#MB potential#MB ports#RFC 4950#Propagate TTL#");
            this.output.println("// #Sender#id#Name#IP#Show name ?#generate stats ?#x position#y position#Destination IP#Backup LSP#Level of GoS#Put on MPLS#Traffic rate#Type of traffic#Constant traffic#Custom TTL#ICMP#ICMP type#Traceroute#");
            this.output.println("// #InternalLink#id#Name#Show name ?#Delay#Name of node 1#Port of node 1#Name of node 2#Port of node 2#");
            this.output.println("// #ExternalLink#id#Name#Show name ?#Delay#Name of node 1#Port of node 1#Name of node 2#Port of node 2#");
            this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.asteriscos"));
            this.output.println();
            this.output.println("@?Topology");
            this.scenarioCRC.update("@?Topology".getBytes());
            this.output.println();
            // Saving traffic receivers.
            auxIterator = this.scenario.getTopology().getNodesIterator();
            while (auxIterator.hasNext()) {
                auxNode = (TNode) auxIterator.next();
                if (auxNode != null) {
                    if (auxNode.getNodeType() == TNode.RECEIVER) {
                        String marshall=auxNode.marshall();
                        this.output.println(marshall);
                        this.scenarioCRC.update(marshall.getBytes());
                    }
                }
            }
            // Saving other nodes.
            auxIterator = this.scenario.getTopology().getNodesIterator();
            while (auxIterator.hasNext()) {
                auxNode = (TNode) auxIterator.next();
                if (auxNode != null) {
                    if (auxNode.getNodeType() != TNode.RECEIVER) {
                        String marshall=auxNode.marshall();
                        this.output.println(marshall);
                        this.scenarioCRC.update(marshall.getBytes());
                    }
                }
            }
            // Saving links
            auxIterator = this.scenario.getTopology().getLinksIterator();
            while (auxIterator.hasNext()) {
                auxLink = (TLink) auxIterator.next();
                if (auxLink != null) {
                    String marshall=auxLink.marshall();
                    this.output.println(marshall);
                    this.scenarioCRC.update(marshall.getBytes());
                }
            }
            this.output.println();
            this.output.println("@!Topology");
            this.scenarioCRC.update("@!Topology".getBytes());
            this.output.println();
            this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.asteriscos"));
            this.output.println("// Definition of the switching table, have to follow this pattern :");
            this.output.println("// #PUSH#Destination Subnet#Mask#Next Hop IP#Label out#");
            this.output.println("// #SWAP#Label IN#Label out#Output port#");
            this.output.println("// #POP#Label IN#Output port#");
            this.output.println("// #ROUTE#Destination Subnet#Mask#Next Hop IP#");
            this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.asteriscos"));
            this.output.println();
            if(saveTables){
                auxIterator = this.scenario.getTopology().getNodesIterator();
                while (auxIterator.hasNext()) {
                    auxNode = (TNode) auxIterator.next();
                    if (auxNode != null) {
                        if (auxNode.getNodeType() != TNode.RECEIVER && auxNode.getNodeType() != TNode.SENDER) {
                            String tableEntry = "@?Tables@"+auxNode.getName();
                            this.output.println(tableEntry);
                            this.scenarioCRC.update(tableEntry.getBytes());
                            String marshall=auxNode.saveTableEntry();
                            marshall += "@!Tables";
                            this.output.println(marshall);
                            this.scenarioCRC.update(marshall.getBytes());
                            this.output.println();
                        }
                    }
                }
            }
            if (createCRC) {
                String auxCRCHash = Long.toString(this.scenarioCRC.getValue());
                this.output.println();
                this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.asteriscos"));
                this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.CodigoCRCParaLaIntegridadDelFichero"));
                this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.asteriscos"));
                this.output.println();
                this.output.println("@CRC#" + auxCRCHash);
            }
            this.outputStream.close();
            this.output.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private CRC32 scenarioCRC;
    private TScenario scenario;
    private FileOutputStream outputStream;
    private PrintStream output;
}
