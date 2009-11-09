/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log4j.scribe;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import scribe.LogEntry;
import scribe.scribe.Client;

import java.util.List;
import java.util.ArrayList;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.Socket;
import java.io.IOException;


public class ScribeAppender extends AppenderSkeleton {
    private List<LogEntry> logEntries;

    private String hostname;
    private String scribe_host = "127.0.0.1";
    private int scribe_port = 1463;
    private String scribe_category = "scribe";

    private Client client;
    private TFramedTransport transport;

    public String getScribe_host() {
        return scribe_host;
    }

    public void setScribe_host(String scribe_host) {
        this.scribe_host = scribe_host;
    }

    public int getScribe_port() {
        return scribe_port;
    }

    public void setScribe_port(int scribe_port) {
        this.scribe_port = scribe_port;
    }

    public String getScribe_category() {
        return scribe_category;
    }

    public void setScribe_category(String scribe_category) {
        this.scribe_category = scribe_category;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }


public void configureScribe() {
        try {
            synchronized(this) {
                if (hostname == null) {
                    try {
                        hostname = InetAddress.getLocalHost().getCanonicalHostName();
                    } catch (UnknownHostException e) {
                        // can't get hostname
                    }
                }

                // Thrift boilerplate code
                logEntries = new ArrayList<LogEntry>(1);
                TSocket sock = new TSocket(new Socket(scribe_host, scribe_port));
                transport = new TFramedTransport(sock);
                TBinaryProtocol protocol = new TBinaryProtocol(transport, false, false);
                client = new Client(protocol, protocol);
            }
        } catch (TTransportException e) {
            System.err.println(e);
        } catch (UnknownHostException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /*
    * Appends a log message to Scribe
    */
    @Override
    public void append(LoggingEvent loggingEvent) {
        synchronized(this) {

            connect();

            try {
                String message = String.format("%s %s", hostname, layout.format(loggingEvent));
                LogEntry entry = new LogEntry(scribe_category, message);

                logEntries.add(entry);
                client.Log(logEntries);
            } catch (TTransportException e) {
                transport.close();
            } catch (Exception e) {
                System.err.println(e);
            } finally {
                logEntries.clear();
            }
        }
    }
    /*
    * Connect to scribe if not open, reconnect if failed.
    */
    public void connect() {
        if (transport != null && transport.isOpen())
            return;

        if (transport != null && transport.isOpen() == false)
        {
            transport.close();

        }
        configureScribe();
    }

    public void close() {
        if (transport != null && transport.isOpen()) {
            transport.close();
        }
    }

    public boolean requiresLayout() {
        return true;
    }
}