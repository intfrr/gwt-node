/*
 * Copyright 2011 Chad Retz
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.gwtnode.examples.oophmproxy.client;

import org.gwtnode.client.JavaScriptUtils;
import org.gwtnode.client.debug.oophm.OophmStream;
import org.gwtnode.client.debug.oophm.OophmStream.StreamIndexOutOfBoundsException;
import org.gwtnode.client.debug.oophm.message.Message;
import org.gwtnode.client.debug.oophm.message.MessageType;
import org.gwtnode.client.node.buffer.Buffer;
import org.gwtnode.client.node.event.BooleanEventHandler;
import org.gwtnode.client.node.event.StringOrBufferEventHandler;
import org.gwtnode.client.node.fs.Fs;
import org.gwtnode.client.node.net.Net;
import org.gwtnode.client.node.net.Server;
import org.gwtnode.client.node.net.Socket;
import org.gwtnode.client.node.net.StreamEventHandler;
import org.gwtnode.client.node.stdio.Console;
import org.gwtnode.client.node.stream.WritableStream;

/**
 * @author Chad Retz
 */
class OophmProxyServer {
    
    private final Server server;
    private final int proxyPort;
    private Socket proxySocket;
    private Socket gwtCodeSocket;
    private final OophmStream proxyStream = new OophmStream();
    private final OophmStream gwtCodeStream = new OophmStream();
    private WritableStream logFile;
    
    public OophmProxyServer(int proxyPort, final String gwtCodeHost, final int gwtCodePort, 
            final String logFilename) {
        server = Net.get().createServer(new StreamEventHandler() {
            @Override
            protected void onEvent(Socket stream) {
                if (logFilename != null) {
                    logFile = Fs.get().createWriteStream(logFilename);
                }
                proxySocket = stream;
                gwtCodeSocket = Socket.create();
                gwtCodeSocket.connect(gwtCodePort, gwtCodeHost);
                proxySocket.onData(new StringOrBufferEventHandler() {
                    @Override
                    protected void onEvent() {
                        Buffer buffer = getBuffer();
                        proxyStream.append(buffer);
                        Message message;
                        do {
                            message = logMessage(proxyStream, true);
                            if (message != null) {
                                gwtCodeSocket.write(message.toBuffer());
                            }
                        } while (message != null);
                        //gwtCodeSocket.write(buffer);
                    }
                });
                gwtCodeSocket.onData(new StringOrBufferEventHandler() {
                    @Override
                    protected void onEvent() {
                        Buffer buffer = getBuffer();
                        gwtCodeStream.append(buffer);
                        Message message;
                        do {
                            message = logMessage(gwtCodeStream, false);
                            if (message != null) {
                                proxySocket.write(message.toBuffer());
                            }
                        } while (message != null);
                        //proxySocket.write(buffer);
                    }
                });
                proxySocket.onClose(new BooleanEventHandler() {
                    @Override
                    protected void onEvent(boolean value) {
                        gwtCodeSocket.end();
                        if (logFile != null) {
                            logFile.end();
                            logFile = null;
                        }
                    }
                });
                gwtCodeSocket.onClose(new BooleanEventHandler() {
                    @Override
                    protected void onEvent(boolean value) {
                        proxySocket.end();
                        if (logFile != null) {
                            logFile.end();
                            logFile = null;
                        }
                    }
                });
            }
        });
        this.proxyPort = proxyPort;
    }
    
    private Message logMessage(OophmStream stream, boolean fromClient) {
        try {
            stream.beginTransaction();
            MessageType type = MessageType.getMessageType(stream);
            Message message = type.createMessage(stream, fromClient);
            if (logFile != null) {
                logFile.write((fromClient ? "fromJS ** " : "toJS ** ") + message.toString() + "\n");
            } else {
                Console.get().info((fromClient ? "fromJS ** " : "toJS ** ") + message.toString());
            }
            stream.commitTransaction();
            return message;
        } catch (StreamIndexOutOfBoundsException e) {
            stream.rollbackTransaction();
            return null;
        } catch (Exception e) {
            logFile.write("Error: " + JavaScriptUtils.appendException(e, new StringBuilder()) + "\n");
            stream.rollbackTransaction();
            return null;
        }
    }
    
    public void listen() {
        server.listen(proxyPort);
        Console.get().info("Listening on port: " + proxyPort);
    }
}