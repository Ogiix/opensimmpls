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
package simMPLS.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import simMPLS.protocols.TICMPPDU;
import simMPLS.protocols.TMPLSLabelStack;
/**
 * This class save the received packet of a node.
 *
 * @author Gaetan Bulpa
 * @version 1.0
 */
public class TraceResultSaver {

    /**
     * This method is the constructor of the class. It creates a new instance of
     * TraceResultSaver.
     *
     * @author Gaetan Bulpa
     * @param name The name of the file to save
     * @param traceType
     * @since 1.0
     */
    public TraceResultSaver(String name, int traceType ) {
        try {
            this.outputFile = new File(name);
            this.outputStream = new FileOutputStream(outputFile);
            this.output = new PrintStream(this.outputStream);
            this.traceType = traceType;
            this.tracerouteCount = 1;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(TraceResultSaver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * This method creates the first lines of the file.
     *
     * @author Gaetan Bulpa
     * @param traceType 
     * @param nodeName
     * @param target IP of the target
     * @since 1.0
     */
    public void initializeFile(String traceType, String nodeName, String target) {
        this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.asteriscos"));
        this.output.println("This is the result of the "+traceType+" made by "+nodeName+" target was "+target);
        this.output.println(java.util.ResourceBundle.getBundle("simMPLS/lenguajes/lenguajes").getString("TAlmacenadorOSM.asteriscos"));
        this.output.println();
    }
    
    /**
     * This method close the file.
     *
     * @author Gaetan Bulpa
     * @since 1.0
     */
    public void closeFile() {
        try {
            this.outputStream.close();
            this.output.close();
        } catch (IOException ex) {
            Logger.getLogger(TraceResultSaver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * This method saves the result of the packet received.
     *
     * @author Gaetan Bulpa
     * @param packetToDump
     * @since 1.0
     */
    public void save(TICMPPDU packetToDump) {
        String cad = "";
        this.output.println();
        if(this.traceType == TraceResultSaver.PING){
            switch(packetToDump.getTypeICMP()){
                case 0:
                    this.output.println("Reply from "+packetToDump.getIPv4Header().getOriginIPAddress()+": TTL="+packetToDump.getIPv4Header().getTTL());
                    break;
                case 11:
                    this.output.println("Reply from "+packetToDump.getIPv4Header().getOriginIPAddress()+": Request timed out.");
                    break;
                case 3:
                    this.output.println("Reply from "+packetToDump.getIPv4Header().getOriginIPAddress()+": Destination host unreachable.");
                    break;
            }
        }else if(this.traceType == TraceResultSaver.TRACEROUTE){
            switch(packetToDump.getTypeICMP()){
                case 0:
                    cad += tracerouteCount+" "+packetToDump.getIPv4Header().getOriginIPAddress();
                    if(packetToDump.getPayloadICMP() != null){
                        TMPLSLabelStack stackToDump = packetToDump.getPayloadICMP();
                        for(int j = stackToDump.getSize()-1; j>=0; j--){
                            cad += " MPLS label=";
                            cad += stackToDump.getLabelFromID(j).getLabel();
                            cad += " Exp=";
                            cad += stackToDump.getLabelFromID(j).getEXP();
                            cad += " TTL=";
                            cad += stackToDump.getLabelFromID(j).getTTL();
                        }
                    }
                    cad += " END of traceroute.";
                    this.output.println(cad);
                    tracerouteCount++;
                    break;
                case 11:
                    cad += tracerouteCount+" "+packetToDump.getIPv4Header().getOriginIPAddress();
                    if(packetToDump.getPayloadICMP() != null){
                        TMPLSLabelStack stackToDump = packetToDump.getPayloadICMP();
                        for(int j = stackToDump.getSize()-1; j>=0; j--){
                            cad += " MPLS label=";
                            cad += stackToDump.getLabelFromID(j).getLabel();
                            cad += " Exp=";
                            cad += stackToDump.getLabelFromID(j).getEXP();
                            cad += " TTL=";
                            cad += stackToDump.getLabelFromID(j).getTTL();
                        }
                    }
                    this.output.println(cad);
                    tracerouteCount++;
                    break;
                case 3:
                    //this.output.println(tracerouteCount+" "+packetToDump.getIPv4Header().getOriginIPAddress()+": Destination host unreachable.");
                    break;
            }
        }
    }

    private File outputFile;
    private FileOutputStream outputStream;
    private PrintStream output;
    private int traceType;
    private int tracerouteCount;
    
    public static final int PING = 0;
    public static final int TRACEROUTE = 1;
}
