/* -*- indent-tabs-mode:T; c-basic-offset:8; tab-width:8; -*- vi: set ts=8:
	* $Id: UDPWriter.java,v 1.1 2002/07/14 04:31:52 dennisda Exp $
	*
	*  (c) Dennis D'Annunzio <ciogeneral@positivechanges.com>
	*
	*************
	*
	*  This file is part of the autopilot simulation package.
	*  http://autopilot.sf.net
	*
	*  Autopilot is free software; you can redistribute it and/or modify
	*  it under the terms of the GNU General Public License as published by
	*  the Free Software Foundation; either version 2 of the License, or
	*  (at your option) any later version.
	*
	*  Autopilot is distributed in the hope that it will be useful,
	*  but WITHOUT ANY WARRANTY; without even the implied warranty of
	*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	*  GNU General Public License for more details.
	*
	*  You should have received a copy of the GNU General Public License
	*  along with Autopilot; if not, write to the Free Software
	*  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
	*
	*/

package net.sf.autopilot.downlink.uav;

import java.net.ServerSocket;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.io.ObjectOutputStream;
import java.io.BufferedOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.io.InterruptedIOException;

/* Generated by Together */

public class UDPWriter extends Thread {
        ServerSocket serverSocket;
        int threadNumber;
        TelemetryServer systemHandle;
        boolean active;
        PrintStream os;
        DatagramSocket dgSocket;
        InetAddress inetAddr;
        InetAddress group;
        byte[] inData = new byte[1024];
        byte[] outData = new byte[1];
        private DatagramSocket socket;
        private DatagramPacket inPacket = new DatagramPacket(inData, inData.length);
        private DatagramPacket outPacket = new DatagramPacket(outData, outData.length);
        InetAddress remoteHost;
        int remotePort;

        // prob should use obj oriented techniques to implement socket types
        public TelemetryServer getSystemHandle() { return systemHandle; }

        UDPWriter(TelemetryServer systemHandle) {
                super();
                this.systemHandle = systemHandle;
                setName("DG_TH:" + threadNumber);
                active = false;
                try {
                        socket = new DatagramSocket(12367);
                } catch (IOException e) {
                        // yipes
                        System.out.println("yipes, e=" + e);
                }
        }

        public void run() {
                while (true) {
                        try {
                                System.out.println(getName() + " waiting"); // debug output
                                inPacket.setData(inData);
                                inPacket.setLength(inData.length);
                                socket.setSoTimeout(0);
                                socket.setSendBufferSize(70);
                                // this needs to be moved to a thread that only passes back
                                // on a new client type, this will become a polling loop
                                socket.setSoTimeout(0);
                                try {
                                        socket.receive(inPacket);
                                } catch (Exception e) {
                                        System.out.println("Error reading UDP socket on initial read e=" + e);
                                }
                                System.out.println(getName() + " active"); // debug output
                                active = true;
                                remoteHost = inPacket.getAddress();
                                remotePort = inPacket.getPort();
                                System.out.println(remoteHost + "," + remotePort);
                                boolean clientAlive = true;
                                socket.setSoTimeout(3000);
                                while (clientAlive) {
                                        try {
                                                socket.receive(inPacket);
                                        } catch (SocketException se) {
                                                // timed out!!
                                                System.out.println("TIMEDOUT");
                                                clientAlive = false;
                                        }
                                        // got data, but it may be from another machine!!
                                }
                                // dumb loop to hold the "connection" open
                                //		while (true) {
                                //		}
                        } catch (IOException ex) {
                                System.out.println(getName() + ": IO Error on socket " + ex); // error output
                        } finally {
                                active = false;
                        }
                }
        }

        synchronized public void sendLine(String line) {
                try {
                        if (null == line) {
                                // do and attempt nothing
                        } else {
                                // send the packet, soTimeOut is set to 3 seconds
                                outPacket.setData(line.getBytes());
                                outPacket.setLength(line.length());
                                outPacket.setAddress(remoteHost);
                                outPacket.setPort(remotePort);
                                try {
                                        socket.send(outPacket);
                                } catch (InterruptedIOException timeout) {
                                        // 3 second time, log but let the client watchdog
                                        // do the dirty work
                                        System.out.println("sendLine() timeout.");
                                        System.out.flush();
                                }
                        }
                } catch (Exception e) {
                        System.out.println("ERROR: sendLine() main clause exception, " + e);
                        System.out.flush();
                }
        }

        public boolean isActive() { return active; }
}
